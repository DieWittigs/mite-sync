package org.twittig.mite.mitesync.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventAttendee;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.api.services.calendar.model.Events;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.twittig.mite.mitesync.web.model.CalendarEventModel;

@ExtendWith(MockitoExtension.class)
class GoogleCalendarServiceTest {

  private static final ZoneId BERLIN = ZoneId.of("Europe/Berlin");

  @InjectMocks
  private GoogleCalendarService service;

  @BeforeEach
  void setUp() {
    ReflectionTestUtils.setField(service, "calendarId", "primary");
    ReflectionTestUtils.setField(service, "skipSummaries", List.of());
    ReflectionTestUtils.setField(service, "clientSecretFile", "/dev/null");
    ReflectionTestUtils.setField(service, "tokensDirectory", "/tmp");
  }

  // --- roundUp static utility ---

  @Test
  void roundUp_exactMultiple_unchanged() {
    assertThat(GoogleCalendarService.roundUp(60, 15)).isEqualTo(60);
  }

  @Test
  void roundUp_remainder_roundsUpToNextMultiple() {
    assertThat(GoogleCalendarService.roundUp(50, 15)).isEqualTo(60);
    assertThat(GoogleCalendarService.roundUp(1, 15)).isEqualTo(15);
    assertThat(GoogleCalendarService.roundUp(16, 15)).isEqualTo(30);
  }

  @Test
  void roundUp_zeroMinutes_returnsZero() {
    assertThat(GoogleCalendarService.roundUp(0, 15)).isEqualTo(0);
  }

  @Test
  void roundUp_negativeMinutes_returnsZero() {
    assertThat(GoogleCalendarService.roundUp(-5, 15)).isEqualTo(0);
  }

  @Test
  void roundUp_zeroStep_returnsMinutesUnchanged() {
    assertThat(GoogleCalendarService.roundUp(50, 0)).isEqualTo(50);
  }

  @Test
  void roundUp_step1_neverChanges() {
    assertThat(GoogleCalendarService.roundUp(37, 1)).isEqualTo(37);
  }

  // --- getEventsForDay with injected mock Calendar ---

  @Test
  void getEventsForDay_timedEvent_isMapped() throws Exception {
    LocalDate date = LocalDate.of(2024, 3, 15);
    Event event = buildTimedEvent("Standup",
        LocalDateTime.of(2024, 3, 15, 9, 0),
        LocalDateTime.of(2024, 3, 15, 9, 15));

    injectCalendarMock(date, List.of(event));

    List<CalendarEventModel> result = service.getEventsForDay(date, 15);

    assertThat(result).hasSize(1);
    CalendarEventModel m = result.get(0);
    assertThat(m.getSummary()).isEqualTo("Standup");
    assertThat(m.getMinutes()).isEqualTo(15);
    assertThat(m.getRoundedMinutes()).isEqualTo(15);
    assertThat(m.isSkipped()).isFalse();
  }

  @Test
  void getEventsForDay_45minEvent_roundsUpTo60() throws Exception {
    LocalDate date = LocalDate.of(2024, 3, 15);
    Event event = buildTimedEvent("Review",
        LocalDateTime.of(2024, 3, 15, 10, 0),
        LocalDateTime.of(2024, 3, 15, 10, 45));

    injectCalendarMock(date, List.of(event));

    List<CalendarEventModel> result = service.getEventsForDay(date, 15);

    assertThat(result.get(0).getMinutes()).isEqualTo(45);
    assertThat(result.get(0).getRoundedMinutes()).isEqualTo(45);
  }

  @Test
  void getEventsForDay_allDayEvent_isSkipped() throws Exception {
    LocalDate date = LocalDate.of(2024, 3, 15);
    Event allDay = new Event();
    allDay.setSummary("Holiday");
    EventDateTime start = new EventDateTime();
    // All-day: dateTime is null, only date is set
    allDay.setStart(start);
    allDay.setEnd(new EventDateTime());

    injectCalendarMock(date, List.of(allDay));

    List<CalendarEventModel> result = service.getEventsForDay(date, 15);

    assertThat(result).isEmpty();
  }

  @Test
  void getEventsForDay_nullSummary_replacedWithPlaceholder() throws Exception {
    LocalDate date = LocalDate.of(2024, 3, 15);
    Event event = buildTimedEvent(null,
        LocalDateTime.of(2024, 3, 15, 9, 0),
        LocalDateTime.of(2024, 3, 15, 9, 15));

    injectCalendarMock(date, List.of(event));

    List<CalendarEventModel> result = service.getEventsForDay(date, 15);

    assertThat(result.get(0).getSummary()).isEqualTo("(no title)");
  }

  @Test
  void getEventsForDay_skipSummaryMatch_marksSkipped() throws Exception {
    LocalDate date = LocalDate.of(2024, 3, 15);
    ReflectionTestUtils.setField(service, "skipSummaries", List.of("Personal Time", "Blocker"));
    Event event = buildTimedEvent("Personal Time",
        LocalDateTime.of(2024, 3, 15, 9, 0),
        LocalDateTime.of(2024, 3, 15, 9, 30));

    injectCalendarMock(date, List.of(event));

    List<CalendarEventModel> result = service.getEventsForDay(date, 15);

    assertThat(result.get(0).isSkipped()).isTrue();
  }

  @Test
  void getEventsForDay_noAttendees_responseStatusAccepted() throws Exception {
    LocalDate date = LocalDate.of(2024, 3, 15);
    Event event = buildTimedEvent("1:1",
        LocalDateTime.of(2024, 3, 15, 14, 0),
        LocalDateTime.of(2024, 3, 15, 14, 30));

    injectCalendarMock(date, List.of(event));

    List<CalendarEventModel> result = service.getEventsForDay(date, 15);

    assertThat(result.get(0).getResponseStatus()).isEqualTo("accepted");
  }

  @Test
  void getEventsForDay_selfAttendeeAccepted_responseStatusAccepted() throws Exception {
    LocalDate date = LocalDate.of(2024, 3, 15);
    Event event = buildTimedEvent("Sprint Planning",
        LocalDateTime.of(2024, 3, 15, 9, 0),
        LocalDateTime.of(2024, 3, 15, 11, 0));
    EventAttendee self = new EventAttendee().setSelf(true).setResponseStatus("accepted");
    EventAttendee other = new EventAttendee().setSelf(false).setResponseStatus("declined");
    event.setAttendees(List.of(other, self));

    injectCalendarMock(date, List.of(event));

    List<CalendarEventModel> result = service.getEventsForDay(date, 15);

    assertThat(result.get(0).getResponseStatus()).isEqualTo("accepted");
  }

  @Test
  void getEventsForDay_selfAttendeDeclined_responseStatusDeclined() throws Exception {
    LocalDate date = LocalDate.of(2024, 3, 15);
    Event event = buildTimedEvent("Optional Meeting",
        LocalDateTime.of(2024, 3, 15, 15, 0),
        LocalDateTime.of(2024, 3, 15, 16, 0));
    EventAttendee self = new EventAttendee().setSelf(true).setResponseStatus("declined");
    event.setAttendees(List.of(self));

    injectCalendarMock(date, List.of(event));

    List<CalendarEventModel> result = service.getEventsForDay(date, 15);

    assertThat(result.get(0).getResponseStatus()).isEqualTo("declined");
  }

  @Test
  void getEventsForDay_noSelfAttendee_responseStatusNeedsAction() throws Exception {
    LocalDate date = LocalDate.of(2024, 3, 15);
    Event event = buildTimedEvent("Team Event",
        LocalDateTime.of(2024, 3, 15, 12, 0),
        LocalDateTime.of(2024, 3, 15, 13, 0));
    EventAttendee otherPerson = new EventAttendee().setSelf(false).setResponseStatus("accepted");
    event.setAttendees(List.of(otherPerson));

    injectCalendarMock(date, List.of(event));

    List<CalendarEventModel> result = service.getEventsForDay(date, 15);

    assertThat(result.get(0).getResponseStatus()).isEqualTo("needsAction");
  }

  @Test
  void getEventsForDay_emptyEventList_returnsEmpty() throws Exception {
    LocalDate date = LocalDate.of(2024, 3, 15);
    injectCalendarMock(date, List.of());

    List<CalendarEventModel> result = service.getEventsForDay(date, 15);

    assertThat(result).isEmpty();
  }

  @Test
  void getEventsForDay_calendarThrows_wrapsException() throws Exception {
    LocalDate date = LocalDate.of(2024, 3, 15);

    Calendar calendarMock = mock(Calendar.class);
    Calendar.Events eventsMock = mock(Calendar.Events.class);
    Calendar.Events.List listMock = mock(Calendar.Events.List.class);

    when(calendarMock.events()).thenReturn(eventsMock);
    when(eventsMock.list(anyString())).thenReturn(listMock);
    when(listMock.setTimeMin(any())).thenReturn(listMock);
    when(listMock.setTimeMax(any())).thenReturn(listMock);
    when(listMock.setSingleEvents(anyBoolean())).thenReturn(listMock);
    when(listMock.setOrderBy(anyString())).thenReturn(listMock);
    when(listMock.setMaxResults(anyInt())).thenReturn(listMock);
    when(listMock.execute()).thenThrow(new IOException("Network error"));

    ReflectionTestUtils.setField(service, "calendarClient", calendarMock);

    assertThatThrownBy(() -> service.getEventsForDay(date, 15))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("Google Calendar query failed");
  }

  // --- helpers ---

  private Event buildTimedEvent(String summary, LocalDateTime start, LocalDateTime end) {
    Event event = new Event();
    event.setSummary(summary);

    EventDateTime startEdt = new EventDateTime();
    ZonedDateTime startZdt = start.atZone(BERLIN);
    startEdt.setDateTime(new DateTime(startZdt.toInstant().toEpochMilli()));
    event.setStart(startEdt);

    EventDateTime endEdt = new EventDateTime();
    ZonedDateTime endZdt = end.atZone(BERLIN);
    endEdt.setDateTime(new DateTime(endZdt.toInstant().toEpochMilli()));
    event.setEnd(endEdt);

    return event;
  }

  private void injectCalendarMock(LocalDate date, List<Event> events) throws Exception {
    Calendar calendarMock = mock(Calendar.class);
    Calendar.Events eventsMock = mock(Calendar.Events.class);
    Calendar.Events.List listMock = mock(Calendar.Events.List.class);

    when(calendarMock.events()).thenReturn(eventsMock);
    when(eventsMock.list("primary")).thenReturn(listMock);
    when(listMock.setTimeMin(any())).thenReturn(listMock);
    when(listMock.setTimeMax(any())).thenReturn(listMock);
    when(listMock.setSingleEvents(anyBoolean())).thenReturn(listMock);
    when(listMock.setOrderBy(anyString())).thenReturn(listMock);
    when(listMock.setMaxResults(anyInt())).thenReturn(listMock);

    Events response = new Events();
    response.setItems(events);
    when(listMock.execute()).thenReturn(response);

    ReflectionTestUtils.setField(service, "calendarClient", calendarMock);
  }
}
