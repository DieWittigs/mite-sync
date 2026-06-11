package org.twittig.mite.mitesync.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.seventytwo.oss.mite.MiteClient;
import io.seventytwo.oss.mite.TimeEntriesRequest;
import io.seventytwo.oss.mite.model.TimeEntries;
import io.seventytwo.oss.mite.model.TimeEntry;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.twittig.mite.mitesync.config.DailyReportProperties.Profile;
import org.twittig.mite.mitesync.config.MiteClientRegistry;
import org.twittig.mite.mitesync.web.model.BookingResultModel;
import org.twittig.mite.mitesync.web.model.MiteEntryModel;
import org.twittig.mite.mitesync.web.model.ProposalEntryModel;

@ExtendWith(MockitoExtension.class)
class MiteBookingServiceTest {

  @Mock private MiteClient sourceMiteClient;
  @Mock private MiteClient targetMiteClient;

  private MiteBookingService service;
  private Profile profile;

  @BeforeEach
  void setUp() {
    service = new MiteBookingService(new MiteClientRegistry(sourceMiteClient, targetMiteClient));
    profile = profile("source", "11111", "22222");
  }

  private static Profile profile(String miteInstance, String projectId, String serviceId) {
    Profile p = new Profile();
    p.setMiteInstance(miteInstance);
    p.setProjectId(projectId);
    p.setServiceId(serviceId);
    return p;
  }

  @Test
  void getEntriesForDate_returnsMappedEntries() {
    LocalDate date = LocalDate.of(2024, 3, 15);
    TimeEntries.TimeEntry jaxbEntry = buildJaxbEntry(999L, (short) 90, "My note", 11111L, 22222L);
    TimeEntries response = new TimeEntries();
    response.getTimeEntry().add(jaxbEntry);

    when(sourceMiteClient.getTimeEntries(any(TimeEntriesRequest.class))).thenReturn(response);

    List<MiteEntryModel> result = service.getEntriesForDate(profile, date);

    assertThat(result).hasSize(1);
    MiteEntryModel m = result.get(0);
    assertThat(m.getMiteId()).isEqualTo(999L);
    assertThat(m.getMinutes()).isEqualTo(90);
    assertThat(m.getNote()).isEqualTo("My note");
    assertThat(m.getProjectId()).isEqualTo(11111L);
    assertThat(m.getServiceId()).isEqualTo(22222L);
  }

  @Test
  void getEntriesForDate_nullNote_mapsToNull() {
    LocalDate date = LocalDate.of(2024, 3, 15);
    TimeEntries.TimeEntry jaxbEntry = buildJaxbEntry(1L, (short) 30, null, 11111L, 22222L);
    TimeEntries response = new TimeEntries();
    response.getTimeEntry().add(jaxbEntry);

    when(sourceMiteClient.getTimeEntries(any(TimeEntriesRequest.class))).thenReturn(response);

    List<MiteEntryModel> result = service.getEntriesForDate(profile, date);

    assertThat(result.get(0).getNote()).isNull();
  }

  @Test
  void getEntriesForDate_empty() {
    LocalDate date = LocalDate.of(2024, 3, 15);
    when(sourceMiteClient.getTimeEntries(any(TimeEntriesRequest.class))).thenReturn(new TimeEntries());

    assertThat(service.getEntriesForDate(profile, date)).isEmpty();
  }

  @Test
  void getEntriesForDate_usesTheProfilesMiteInstance() {
    LocalDate date = LocalDate.of(2024, 3, 15);
    Profile targetProfile = profile("target", "33333", "44444");
    when(targetMiteClient.getTimeEntries(any(TimeEntriesRequest.class))).thenReturn(new TimeEntries());

    service.getEntriesForDate(targetProfile, date);

    verify(targetMiteClient).getTimeEntries(any(TimeEntriesRequest.class));
    verifyNoInteractions(sourceMiteClient);
  }

  @Test
  void book_successfulBooking_returnsMiteEntry() {
    LocalDate date = LocalDate.of(2024, 3, 15);
    ProposalEntryModel pe = new ProposalEntryModel(90, "Sprint work", "main-pbi-fill", 12345, "My PBI");

    TimeEntry created = new TimeEntry();
    TimeEntry.Id idObj = new TimeEntry.Id();
    idObj.setValue(987654L);
    created.setId(idObj);

    when(sourceMiteClient.createTimeEntry(any(TimeEntry.class))).thenReturn(created);

    BookingResultModel result = service.book(profile, date, List.of(pe));

    assertThat(result.getDate()).isEqualTo(date);
    assertThat(result.getCreated()).hasSize(1);
    assertThat(result.getCreated().get(0).getMiteId()).isEqualTo(987654L);
    assertThat(result.getCreated().get(0).getMinutes()).isEqualTo(90);
    assertThat(result.getCreated().get(0).getNote()).isEqualTo("Sprint work");
    assertThat(result.getFailed()).isEmpty();
    assertThat(result.getTotalMinutesCreated()).isEqualTo(90);
  }

  @Test
  void book_usesTheProfilesMiteInstance() {
    LocalDate date = LocalDate.of(2024, 3, 15);
    Profile targetProfile = profile("target", "33333", "44444");
    ProposalEntryModel pe = new ProposalEntryModel(60, "Work", "main-pbi-fill", null, null);

    when(targetMiteClient.createTimeEntry(any(TimeEntry.class))).thenReturn(new TimeEntry());

    BookingResultModel result = service.book(targetProfile, date, List.of(pe));

    assertThat(result.getCreated()).hasSize(1);
    assertThat(result.getCreated().get(0).getProjectId()).isEqualTo(33333L);
    assertThat(result.getCreated().get(0).getServiceId()).isEqualTo(44444L);
    verify(targetMiteClient).createTimeEntry(any(TimeEntry.class));
    verifyNoInteractions(sourceMiteClient);
  }

  @Test
  void book_multipleEntries_sumsMinutes() {
    LocalDate date = LocalDate.of(2024, 3, 15);
    ProposalEntryModel pe1 = new ProposalEntryModel(60, "Entry 1", "calendar", null, null);
    ProposalEntryModel pe2 = new ProposalEntryModel(120, "Entry 2", "main-pbi-fill", null, null);

    when(sourceMiteClient.createTimeEntry(any(TimeEntry.class))).thenReturn(new TimeEntry());

    BookingResultModel result = service.book(profile, date, List.of(pe1, pe2));

    assertThat(result.getTotalMinutesCreated()).isEqualTo(180);
    assertThat(result.getCreated()).hasSize(2);
  }

  @Test
  void book_clientThrows_failedEntryCollected() {
    LocalDate date = LocalDate.of(2024, 3, 15);
    ProposalEntryModel pe = new ProposalEntryModel(60, "Fail entry", "calendar", null, null);

    when(sourceMiteClient.createTimeEntry(any(TimeEntry.class)))
        .thenThrow(new RuntimeException("API error"));

    BookingResultModel result = service.book(profile, date, List.of(pe));

    assertThat(result.getCreated()).isEmpty();
    assertThat(result.getFailed()).hasSize(1);
    assertThat(result.getFailed().get(0).getNote()).isEqualTo("Fail entry");
    assertThat(result.getFailed().get(0).getMinutes()).isEqualTo(60);
    assertThat(result.getTotalMinutesCreated()).isEqualTo(0);
  }

  @Test
  void book_mixedSuccessAndFailure_bothCollected() {
    LocalDate date = LocalDate.of(2024, 3, 15);
    ProposalEntryModel ok = new ProposalEntryModel(90, "OK entry", "main-pbi-fill", null, null);
    ProposalEntryModel fail = new ProposalEntryModel(30, "Fail entry", "calendar", null, null);

    when(sourceMiteClient.createTimeEntry(any(TimeEntry.class)))
        .thenReturn(new TimeEntry())
        .thenThrow(new RuntimeException("Network error"));

    BookingResultModel result = service.book(profile, date, List.of(ok, fail));

    assertThat(result.getCreated()).hasSize(1);
    assertThat(result.getFailed()).hasSize(1);
    assertThat(result.getTotalMinutesCreated()).isEqualTo(90);
  }

  private TimeEntries.TimeEntry buildJaxbEntry(long id, short minutes, String note,
      long projectId, long serviceId) {
    TimeEntries.TimeEntry entry = new TimeEntries.TimeEntry();

    TimeEntries.TimeEntry.Id idObj = new TimeEntries.TimeEntry.Id();
    idObj.setValue(id);
    entry.setId(idObj);

    TimeEntries.TimeEntry.Minutes m = new TimeEntries.TimeEntry.Minutes();
    m.setValue(minutes);
    entry.setMinutes(m);

    entry.setNote(note);

    TimeEntries.TimeEntry.ProjectId pid = new TimeEntries.TimeEntry.ProjectId();
    pid.setValue(projectId);
    entry.setProjectId(pid);

    TimeEntries.TimeEntry.ServiceId sid = new TimeEntries.TimeEntry.ServiceId();
    sid.setValue(serviceId);
    entry.setServiceId(sid);

    return entry;
  }
}
