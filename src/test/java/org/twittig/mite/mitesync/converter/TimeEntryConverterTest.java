package org.twittig.mite.mitesync.converter;

import static org.assertj.core.api.Assertions.assertThat;

import io.seventytwo.oss.mite.model.TimeEntries;
import io.seventytwo.oss.mite.model.TimeEntry;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class TimeEntryConverterTest {

  private TimeEntryConverter converter;

  @BeforeEach
  void setUp() {
    converter = new TimeEntryConverter();
    ReflectionTestUtils.setField(converter, "targetProjectId", "12345");
    ReflectionTestUtils.setField(converter, "targetServiceId", "67890");
  }

  @Test
  void convert_setsTargetProjectAndServiceId() {
    TimeEntries.TimeEntry source = buildSource((short) 60, LocalDate.of(2024, 3, 15), "Note");

    TimeEntry result = converter.convert(source);

    assertThat(result.getProjectId().getValue()).isEqualTo(12345);
    assertThat(result.getServiceId().getValue()).isEqualTo(67890);
  }

  @Test
  void convert_copiesMinutes() {
    TimeEntries.TimeEntry source = buildSource((short) 90, LocalDate.of(2024, 3, 15), "Note");

    TimeEntry result = converter.convert(source);

    assertThat(result.getMinutes().getValue()).isEqualTo((short) 90);
  }

  @Test
  void convert_copiesDateAt() {
    LocalDate date = LocalDate.of(2024, 6, 1);
    TimeEntries.TimeEntry source = buildSource((short) 30, date, "Note");

    TimeEntry result = converter.convert(source);

    assertThat(result.getDateAt().getValue()).isEqualTo(date);
  }

  @Test
  void convert_copiesNote() {
    TimeEntries.TimeEntry source = buildSource((short) 45, LocalDate.of(2024, 3, 15), "Sprint review");

    TimeEntry result = converter.convert(source);

    assertThat(result.getNote()).isEqualTo("Sprint review");
  }

  @Test
  void convert_nullNote_remainsNull() {
    TimeEntries.TimeEntry source = buildSource((short) 30, LocalDate.of(2024, 3, 15), null);

    TimeEntry result = converter.convert(source);

    assertThat(result.getNote()).isNull();
  }

  @Test
  void convert_setsBillable() {
    TimeEntries.TimeEntry source = buildSource((short) 30, LocalDate.of(2024, 3, 15), "Note");

    TimeEntry result = converter.convert(source);

    assertThat(result.getBillable()).isNotNull();
  }

  private TimeEntries.TimeEntry buildSource(short minutes, LocalDate date, String note) {
    TimeEntries.TimeEntry entry = new TimeEntries.TimeEntry();

    TimeEntries.TimeEntry.Minutes m = new TimeEntries.TimeEntry.Minutes();
    m.setValue(minutes);
    entry.setMinutes(m);

    TimeEntries.TimeEntry.DateAt d = new TimeEntries.TimeEntry.DateAt();
    d.setValue(date);
    entry.setDateAt(d);

    entry.setNote(note);
    return entry;
  }
}
