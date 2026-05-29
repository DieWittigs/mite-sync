package org.twittig.mite.mitesync.service;

import io.seventytwo.oss.mite.MiteClient;
import io.seventytwo.oss.mite.TimeEntriesRequest;
import io.seventytwo.oss.mite.model.TimeEntries;
import io.seventytwo.oss.mite.model.TimeEntry;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.twittig.mite.mitesync.web.model.BookingResultModel;
import org.twittig.mite.mitesync.web.model.MiteEntryModel;
import org.twittig.mite.mitesync.web.model.ProposalEntryModel;

/**
 * Service zum Lesen und Schreiben von Time-Entries in der SOURCE-Mite-Instanz.
 * Im Gegensatz zu {@link MiteSyncService} schreibt dieser Service in die Quelle (nicht ins Ziel).
 */
@Service
public class MiteBookingService {

  private static final Logger log = LogManager.getLogger(MiteBookingService.class);
  private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE;

  @Value("${mite-sync.source.project-id}")
  private String sourceProjectId;

  @Value("${mite-sync.source.service-id}")
  private String sourceServiceId;

  private final MiteClient sourceMiteClient;

  public MiteBookingService(MiteClient sourceMiteClient) {
    this.sourceMiteClient = sourceMiteClient;
  }

  /** Liest existierende Mite-Einträge für einen bestimmten Tag im Source-Projekt. */
  public List<MiteEntryModel> getEntriesForDate(LocalDate date) {
    var entries =
        sourceMiteClient
            .getTimeEntries(
                new TimeEntriesRequest.Builder()
                    .projectId(sourceProjectId)
                    .from(date.format(ISO))
                    .to(date.format(ISO))
                    .build())
            .getTimeEntry();

    List<MiteEntryModel> result = new ArrayList<>();
    for (TimeEntries.TimeEntry e : entries) {
      MiteEntryModel m = new MiteEntryModel();
      m.setMiteId(e.getId() != null ? e.getId().getValue() : 0);
      m.setMinutes(e.getMinutes() != null ? e.getMinutes().getValue() : 0);
      // note ist im JAXB-Modell als java.lang.Object deklariert (kein Wrapper-Inner-Class).
      m.setNote(e.getNote() == null ? null : e.getNote().toString());
      m.setProjectId(e.getProjectId() != null ? e.getProjectId().getValue() : 0);
      m.setServiceId(e.getServiceId() != null ? e.getServiceId().getValue() : 0);
      result.add(m);
    }
    return result;
  }

  /**
   * Bucht eine Liste von Vorschlags-Einträgen am angegebenen Datum in der Source-Mite. Pro Eintrag
   * wird das Default-Projekt + Service aus der Konfiguration verwendet. Fehler werden gesammelt,
   * aber stoppen den Lauf nicht.
   */
  public BookingResultModel book(LocalDate date, List<ProposalEntryModel> entries) {
    BookingResultModel result = new BookingResultModel();
    result.setDate(date);
    List<MiteEntryModel> created = new ArrayList<>();
    List<BookingResultModel.FailedEntry> failed = new ArrayList<>();
    int totalCreated = 0;

    for (ProposalEntryModel pe : entries) {
      try {
        TimeEntry te = buildTimeEntry(date, pe);
        TimeEntry created1 = sourceMiteClient.createTimeEntry(te);
        MiteEntryModel m = new MiteEntryModel();
        m.setMiteId(created1 != null && created1.getId() != null ? created1.getId().getValue() : 0);
        m.setMinutes(pe.getMinutes());
        m.setNote(pe.getNote());
        m.setProjectId(Integer.parseInt(sourceProjectId));
        m.setServiceId(Integer.parseInt(sourceServiceId));
        created.add(m);
        totalCreated += pe.getMinutes();
      } catch (Exception e) {
        log.error("Booking failed for entry: {} ({} min) — {}", pe.getNote(), pe.getMinutes(), e.getMessage());
        failed.add(new BookingResultModel.FailedEntry(pe.getMinutes(), pe.getNote(), e.getMessage()));
      }
    }

    result.setCreated(created);
    result.setFailed(failed);
    result.setTotalMinutesCreated(totalCreated);
    return result;
  }

  private TimeEntry buildTimeEntry(LocalDate date, ProposalEntryModel pe) {
    TimeEntry te = new TimeEntry();
    te.setBillable(new TimeEntry.Billable());

    // Hinweis JAXB-Modell von mite-java:
    //   ProjectId.setValue(long) — int wird auto-gewidet
    //   ServiceId.setValue(int)
    //   Minutes.setValue(short) — explizit casten
    //   DateAt.setValue(LocalDate) — kein String!

    TimeEntry.ProjectId p = new TimeEntry.ProjectId();
    p.setValue(Integer.parseInt(sourceProjectId));
    te.setProjectId(p);

    TimeEntry.ServiceId s = new TimeEntry.ServiceId();
    s.setValue(Integer.parseInt(sourceServiceId));
    te.setServiceId(s);

    TimeEntry.Minutes m = new TimeEntry.Minutes();
    m.setValue((short) pe.getMinutes());
    te.setMinutes(m);

    TimeEntry.DateAt d = new TimeEntry.DateAt();
    d.setValue(date);
    te.setDateAt(d);

    // note: setNote(Object) — String wird direkt akzeptiert (JAXB-Schema)
    te.setNote(pe.getNote());

    return te;
  }
}
