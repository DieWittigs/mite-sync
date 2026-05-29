package org.twittig.mite.mitesync.service;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventAttendee;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.api.services.calendar.model.Events;
import java.io.File;
import java.io.FileReader;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.twittig.mite.mitesync.web.model.CalendarEventModel;

/**
 * Service zum Lesen von Google Calendar Events via Calendar API (OAuth2). Beim ersten Aufruf öffnet
 * sich ein Browser-Fenster für den Authorization Code Flow; das Refresh Token wird im
 * tokens-directory persistiert, sodass nachfolgende Aufrufe ohne User-Interaktion laufen.
 */
@Service
public class GoogleCalendarService {

  private static final Logger log = LogManager.getLogger(GoogleCalendarService.class);
  private static final String APPLICATION_NAME = "mite-sync";
  private static final GsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
  private static final List<String> SCOPES = List.of(CalendarScopes.CALENDAR_READONLY);
  private static final ZoneId BERLIN = ZoneId.of("Europe/Berlin");

  @Value("${daily-reports.google-calendar.client-secret-file}")
  private String clientSecretFile;

  @Value("${daily-reports.google-calendar.tokens-directory}")
  private String tokensDirectory;

  @Value("${daily-reports.google-calendar.calendar-id}")
  private String calendarId;

  @Value("#{'${daily-reports.google-calendar.skip-summaries:}'.split(',')}")
  private List<String> skipSummaries;

  private volatile Calendar calendarClient;

  /**
   * Liefert alle Events des angegebenen Tages (nach Aufrundung), filtert skip-summaries raus.
   *
   * <p>Beim ersten Aufruf wird der OAuth-Flow ausgelöst (Browser-Popup zur Autorisierung).
   * Nachfolgende Aufrufe nutzen das gespeicherte Refresh Token.
   */
  public List<CalendarEventModel> getEventsForDay(LocalDate date, int roundingStepMinutes) {
    Calendar client = ensureClient();
    Set<String> skip =
        skipSummaries == null
            ? Collections.emptySet()
            : skipSummaries.stream()
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(java.util.stream.Collectors.toSet());

    DateTime timeMin =
        new DateTime(java.util.Date.from(date.atStartOfDay(BERLIN).toInstant()));
    DateTime timeMax =
        new DateTime(java.util.Date.from(date.plusDays(1).atStartOfDay(BERLIN).toInstant()));

    try {
      Events events =
          client
              .events()
              .list(calendarId)
              .setTimeMin(timeMin)
              .setTimeMax(timeMax)
              .setSingleEvents(true)
              .setOrderBy("startTime")
              .setMaxResults(250)
              .execute();

      List<CalendarEventModel> result = new ArrayList<>();
      for (Event e : events.getItems()) {
        // Nur timed Events (keine all-day)
        if (e.getStart() == null || e.getStart().getDateTime() == null) continue;

        LocalTime start = toLocalTime(e.getStart());
        LocalTime end = toLocalTime(e.getEnd());
        int minutes =
            (int) java.time.Duration.between(start, end).toMinutes();
        // Wenn Event spätabends bzw. über Mitternacht endet (sollte selten sein), clamp auf 24h
        if (minutes < 0) minutes = 0;

        CalendarEventModel m = new CalendarEventModel();
        m.setSummary(e.getSummary() == null ? "(kein Titel)" : e.getSummary());
        m.setStartTime(start);
        m.setEndTime(end);
        m.setMinutes(minutes);
        m.setRoundedMinutes(roundUp(minutes, roundingStepMinutes));
        m.setResponseStatus(extractMyResponseStatus(e));
        m.setSkipped(skip.contains(m.getSummary()));
        result.add(m);
      }
      return result;
    } catch (Exception e) {
      throw new RuntimeException("Google Calendar query failed: " + e.getMessage(), e);
    }
  }

  // ---------------------------------------------------------------------------
  // Internals
  // ---------------------------------------------------------------------------

  /**
   * Lazy + thread-safe Initialization. Beim ersten Aufruf wird der OAuth-Flow ausgelöst (öffnet
   * Browser für Authorization). Nachfolgende Aufrufe sind silent (Refresh Token wird genutzt).
   */
  private Calendar ensureClient() {
    Calendar c = calendarClient;
    if (c != null) return c;
    synchronized (this) {
      if (calendarClient == null) {
        try {
          log.info("Initializing Google Calendar OAuth client (browser popup may appear)...");
          calendarClient = buildClient();
          log.info("Google Calendar client ready for calendar '{}'", calendarId);
        } catch (Exception e) {
          throw new RuntimeException(
              "Google Calendar OAuth setup failed — check ~/.mite-sync/google-client-secret.json "
                  + "and HELP.md for setup instructions. Reason: "
                  + e.getMessage(),
              e);
        }
      }
      return calendarClient;
    }
  }

  private Calendar buildClient() throws Exception {
    NetHttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
    File secretFile = new File(clientSecretFile);
    if (!secretFile.exists()) {
      throw new IllegalStateException(
          "client_secret.json not found at " + clientSecretFile + " — see HELP.md for setup.");
    }
    GoogleClientSecrets clientSecrets;
    try (FileReader reader = new FileReader(secretFile)) {
      clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, reader);
    }
    GoogleAuthorizationCodeFlow flow =
        new GoogleAuthorizationCodeFlow.Builder(httpTransport, JSON_FACTORY, clientSecrets, SCOPES)
            .setDataStoreFactory(new FileDataStoreFactory(new File(tokensDirectory)))
            .setAccessType("offline")
            .build();

    LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
    Credential credential = new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
    return new Calendar.Builder(httpTransport, JSON_FACTORY, credential)
        .setApplicationName(APPLICATION_NAME)
        .build();
  }

  /**
   * Findet den Response-Status des aktuellen Users (per "self": true). Falls nicht vorhanden, gibt
   * "accepted" zurück (z. B. selbst erstellte Termine).
   */
  private String extractMyResponseStatus(Event e) {
    if (e.getAttendees() == null || e.getAttendees().isEmpty()) {
      return "accepted"; // selbst erstellt, kein Attendee-Eintrag
    }
    for (EventAttendee a : e.getAttendees()) {
      if (Boolean.TRUE.equals(a.getSelf())) {
        return a.getResponseStatus() == null ? "needsAction" : a.getResponseStatus();
      }
    }
    return "needsAction";
  }

  private LocalTime toLocalTime(EventDateTime edt) {
    DateTime dt = edt.getDateTime();
    Instant i = Instant.ofEpochMilli(dt.getValue());
    return LocalDateTime.ofInstant(i, BERLIN).toLocalTime();
  }

  /** Rundet auf das nächstgrößere Vielfache des Schrittes (z. B. 50 → 60 bei step=15). */
  static int roundUp(int minutes, int step) {
    if (minutes <= 0) return 0;
    if (step <= 0) return minutes;
    int rem = minutes % step;
    return rem == 0 ? minutes : minutes + (step - rem);
  }
}
