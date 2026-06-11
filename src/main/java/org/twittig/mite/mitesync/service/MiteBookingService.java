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
import org.springframework.stereotype.Service;
import org.twittig.mite.mitesync.config.DailyReportProperties.Profile;
import org.twittig.mite.mitesync.config.MiteClientRegistry;
import org.twittig.mite.mitesync.web.model.BookingResultModel;
import org.twittig.mite.mitesync.web.model.MiteEntryModel;
import org.twittig.mite.mitesync.web.model.ProposalEntryModel;

/**
 * Reads and writes time entries on the Mite instance configured in the given project profile.
 * Unlike {@link MiteSyncService}, this service writes into the instance it reads from.
 */
@Service
public class MiteBookingService {

  private static final Logger log = LogManager.getLogger(MiteBookingService.class);
  private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE;

  private final MiteClientRegistry miteClientRegistry;

  public MiteBookingService(MiteClientRegistry miteClientRegistry) {
    this.miteClientRegistry = miteClientRegistry;
  }

  /** Reads existing Mite entries for the given day from the profile's project. */
  public List<MiteEntryModel> getEntriesForDate(Profile profile, LocalDate date) {
    var entries =
        miteClientRegistry
            .get(profile.getMiteInstance())
            .getTimeEntries(
                new TimeEntriesRequest.Builder()
                    .projectId(profile.getProjectId())
                    .from(date.format(ISO))
                    .to(date.format(ISO))
                    .build())
            .getTimeEntry();

    List<MiteEntryModel> result = new ArrayList<>();
    for (TimeEntries.TimeEntry e : entries) {
      MiteEntryModel m = new MiteEntryModel();
      m.setMiteId(e.getId() != null ? e.getId().getValue() : 0);
      m.setMinutes(e.getMinutes() != null ? e.getMinutes().getValue() : 0);
      // note is declared as java.lang.Object in the JAXB model (no wrapper inner class).
      m.setNote(e.getNote() == null ? null : e.getNote().toString());
      m.setProjectId(e.getProjectId() != null ? e.getProjectId().getValue() : 0);
      m.setServiceId(e.getServiceId() != null ? e.getServiceId().getValue() : 0);
      result.add(m);
    }
    return result;
  }

  /**
   * Books the supplied proposal entries for the given date in the profile's Mite instance. Each
   * entry uses the project + service from the profile. Per-entry errors are collected and do not
   * abort the run.
   */
  public BookingResultModel book(Profile profile, LocalDate date, List<ProposalEntryModel> entries) {
    MiteClient miteClient = miteClientRegistry.get(profile.getMiteInstance());
    BookingResultModel result = new BookingResultModel();
    result.setDate(date);
    List<MiteEntryModel> created = new ArrayList<>();
    List<BookingResultModel.FailedEntry> failed = new ArrayList<>();
    int totalCreated = 0;

    for (ProposalEntryModel pe : entries) {
      try {
        TimeEntry te = buildTimeEntry(profile, date, pe);
        TimeEntry created1 = miteClient.createTimeEntry(te);
        MiteEntryModel m = new MiteEntryModel();
        m.setMiteId(created1 != null && created1.getId() != null ? created1.getId().getValue() : 0);
        m.setMinutes(pe.getMinutes());
        m.setNote(pe.getNote());
        m.setProjectId(Integer.parseInt(profile.getProjectId()));
        m.setServiceId(Integer.parseInt(profile.getServiceId()));
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

  private TimeEntry buildTimeEntry(Profile profile, LocalDate date, ProposalEntryModel pe) {
    TimeEntry te = new TimeEntry();
    te.setBillable(new TimeEntry.Billable());

    // mite-java JAXB model notes:
    //   ProjectId.setValue(long) — int is widened automatically
    //   ServiceId.setValue(int)
    //   Minutes.setValue(short) — explicit cast required
    //   DateAt.setValue(LocalDate) — not String

    TimeEntry.ProjectId p = new TimeEntry.ProjectId();
    p.setValue(Integer.parseInt(profile.getProjectId()));
    te.setProjectId(p);

    TimeEntry.ServiceId s = new TimeEntry.ServiceId();
    s.setValue(Integer.parseInt(profile.getServiceId()));
    te.setServiceId(s);

    TimeEntry.Minutes m = new TimeEntry.Minutes();
    m.setValue((short) pe.getMinutes());
    te.setMinutes(m);

    TimeEntry.DateAt d = new TimeEntry.DateAt();
    d.setValue(date);
    te.setDateAt(d);

    // note: setNote(Object) — String is accepted directly (per the JAXB schema)
    te.setNote(pe.getNote());

    return te;
  }
}
