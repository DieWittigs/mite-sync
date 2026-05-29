package org.twittig.mite.mitesync.converter;

import io.seventytwo.oss.mite.model.TimeEntries;
import io.seventytwo.oss.mite.model.TimeEntry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

/**
 * A converter component that converts an instance of TimeEntries.TimeEntry to an instance of
 * TimeEntry. The conversion process includes mapping the project ID and service ID from the
 * configured properties, and copying relevant fields such as minutes, date, and note from the
 * source TimeEntry to the target TimeEntry.
 *
 * <p>This class uses configuration properties defined in the application properties file to set the
 * target project ID and target service ID.
 */
@Component
public class TimeEntryConverter implements Converter<TimeEntries.TimeEntry, TimeEntry> {

  @Value("${mite-sync.target.project-id}")
  private String targetProjectId;

  @Value("${mite-sync.target.service-id}")
  private String targetServiceId;

  @Override
  public TimeEntry convert(TimeEntries.TimeEntry sourceTimeEntry) {
    TimeEntry targetTimeEntry = new TimeEntry();

    targetTimeEntry.setBillable(new TimeEntry.Billable());

    // Setze Projekt-ID
    TimeEntry.ProjectId projectId = new TimeEntry.ProjectId();
    projectId.setValue(Integer.parseInt(targetProjectId));
    targetTimeEntry.setProjectId(projectId);

    // Setze Service-ID
    TimeEntry.ServiceId serviceId = new TimeEntry.ServiceId();
    serviceId.setValue(Integer.parseInt(targetServiceId));
    targetTimeEntry.setServiceId(serviceId);

    // Kopiere Minuten
    TimeEntry.Minutes minutes = new TimeEntry.Minutes();
    minutes.setValue(sourceTimeEntry.getMinutes().getValue());
    targetTimeEntry.setMinutes(minutes);

    // Kopiere Datum
    TimeEntry.DateAt dateAt = new TimeEntry.DateAt();
    dateAt.setValue(sourceTimeEntry.getDateAt().getValue());
    targetTimeEntry.setDateAt(dateAt);

    // Kopiere Notiz
    targetTimeEntry.setNote(sourceTimeEntry.getNote());

    return targetTimeEntry;
  }
}
