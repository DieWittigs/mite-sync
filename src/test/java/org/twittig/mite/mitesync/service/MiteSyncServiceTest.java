package org.twittig.mite.mitesync.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.convert.ConversionService;

@ExtendWith(MockitoExtension.class)
class MiteSyncServiceTest {

  @Mock private ConversionService conversionService;
  @InjectMocks private MiteSyncService service;

  private MiteClient client;

  @BeforeEach
  void setUp() {
    client = mock(MiteClient.class);
  }

  @Test
  void getTimeEntries_returnsEntriesFromClient() {
    LocalDate from = LocalDate.of(2024, 1, 1);
    LocalDate to = LocalDate.of(2024, 1, 31);
    TimeEntries.TimeEntry entry = new TimeEntries.TimeEntry();
    TimeEntries response = new TimeEntries();
    response.getTimeEntry().add(entry);

    when(client.getTimeEntries(any(TimeEntriesRequest.class))).thenReturn(response);

    List<TimeEntries.TimeEntry> result = service.getTimeEntries("111", from, to, client);

    assertThat(result).containsExactly(entry);
    verify(client).getTimeEntries(any(TimeEntriesRequest.class));
  }

  @Test
  void getTimeEntries_emptyResult() {
    LocalDate from = LocalDate.of(2024, 1, 1);
    LocalDate to = LocalDate.of(2024, 1, 31);
    when(client.getTimeEntries(any(TimeEntriesRequest.class))).thenReturn(new TimeEntries());

    List<TimeEntries.TimeEntry> result = service.getTimeEntries("111", from, to, client);

    assertThat(result).isEmpty();
  }

  @Test
  void deleteTimeEntries_callsDeleteForEachEntry() {
    TimeEntries.TimeEntry e1 = entryWithId(101L);
    TimeEntries.TimeEntry e2 = entryWithId(202L);

    service.deleteTimeEntries(List.of(e1, e2), client);

    verify(client).deleteTimeEntry(101L);
    verify(client).deleteTimeEntry(202L);
  }

  @Test
  void deleteTimeEntries_emptyList_noDeletion() {
    service.deleteTimeEntries(List.of(), client);

    verify(client, never()).deleteTimeEntry(any(Long.class));
  }

  @Test
  void createTimeEntries_convertsAndCreatesEachEntry() {
    TimeEntries.TimeEntry source = new TimeEntries.TimeEntry();
    TimeEntry converted = new TimeEntry();

    when(conversionService.convert(source, TimeEntry.class)).thenReturn(converted);

    service.createTimeEntries(List.of(source), client);

    verify(conversionService).convert(source, TimeEntry.class);
    verify(client).createTimeEntry(converted);
  }

  @Test
  void createTimeEntries_emptyList_noCreation() {
    service.createTimeEntries(List.of(), client);

    verify(client, never()).createTimeEntry(any());
  }

  private TimeEntries.TimeEntry entryWithId(long id) {
    TimeEntries.TimeEntry entry = new TimeEntries.TimeEntry();
    TimeEntries.TimeEntry.Id idObj = new TimeEntries.TimeEntry.Id();
    idObj.setValue(id);
    entry.setId(idObj);
    return entry;
  }
}
