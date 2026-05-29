package org.twittig.mite.mitesync.facade;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.seventytwo.oss.mite.MiteClient;
import io.seventytwo.oss.mite.model.TimeEntries;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.twittig.mite.mitesync.service.MiteSyncService;

@ExtendWith(MockitoExtension.class)
class MiteSyncFacadeTest {

  @Mock private MiteClient sourceMiteClient;
  @Mock private MiteClient targetMiteClient;
  @Mock private MiteSyncService miteSyncService;

  private MiteSyncFacade facade;

  @BeforeEach
  void setUp() {
    // Explicit construction to guarantee correct injection of two same-type MiteClient params.
    facade = new MiteSyncFacade(sourceMiteClient, targetMiteClient, miteSyncService);
    ReflectionTestUtils.setField(facade, "sourceProjectId", "111");
    ReflectionTestUtils.setField(facade, "targetProjectId", "222");
  }

  @Test
  void sync_deletesTargetThenCreatesFromSource() {
    LocalDate from = LocalDate.of(2024, 1, 1);
    LocalDate to = LocalDate.of(2024, 1, 31);
    List<TimeEntries.TimeEntry> sourceEntries = List.of(new TimeEntries.TimeEntry());
    List<TimeEntries.TimeEntry> targetEntries = List.of(new TimeEntries.TimeEntry());

    when(miteSyncService.getTimeEntries("222", from, to, targetMiteClient)).thenReturn(targetEntries);
    when(miteSyncService.getTimeEntries("111", from, to, sourceMiteClient)).thenReturn(sourceEntries);

    facade.sync(from, to);

    verify(miteSyncService).deleteTimeEntries(targetEntries, targetMiteClient);
    verify(miteSyncService).createTimeEntries(sourceEntries, targetMiteClient);
  }

  @Test
  void sync_emptyEntries_stillCallsDeleteAndCreate() {
    LocalDate from = LocalDate.of(2024, 1, 1);
    LocalDate to = LocalDate.of(2024, 1, 31);
    List<TimeEntries.TimeEntry> empty = List.of();

    when(miteSyncService.getTimeEntries("222", from, to, targetMiteClient)).thenReturn(empty);
    when(miteSyncService.getTimeEntries("111", from, to, sourceMiteClient)).thenReturn(empty);

    facade.sync(from, to);

    verify(miteSyncService).deleteTimeEntries(empty, targetMiteClient);
    verify(miteSyncService).createTimeEntries(empty, targetMiteClient);
  }

  @Test
  void sync_singleDay_works() {
    LocalDate date = LocalDate.of(2024, 5, 1);
    List<TimeEntries.TimeEntry> entries = List.of(new TimeEntries.TimeEntry());

    when(miteSyncService.getTimeEntries("222", date, date, targetMiteClient)).thenReturn(List.of());
    when(miteSyncService.getTimeEntries("111", date, date, sourceMiteClient)).thenReturn(entries);

    facade.sync(date, date);

    verify(miteSyncService).createTimeEntries(entries, targetMiteClient);
  }
}
