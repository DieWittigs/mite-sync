package org.twittig.mite.mitesync.service;

import io.seventytwo.oss.mite.MiteClient;
import io.seventytwo.oss.mite.TimeEntriesRequest;
import io.seventytwo.oss.mite.model.TimeEntries;
import io.seventytwo.oss.mite.model.TimeEntry;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Service;

/**
 * A service class to handle synchronization of time entries between projects using the Mite API.
 */
@Service
public class MiteSyncService {

  private static final DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE;

  private final ConversionService conversionService;

  public MiteSyncService(ConversionService conversionService) {
    this.conversionService = conversionService;
  }

  /**
   * Retrieves a list of time entries for a specified project within a date range using a Mite
   * client.
   *
   * @param projectId the ID of the project for which to retrieve time entries
   * @param from the start date of the range to retrieve time entries from (inclusive)
   * @param to the end date of the range to retrieve time entries to (inclusive)
   * @param client the Mite client used to perform the request
   * @return a list of time entries for the specified project within the given date range
   */
  public List<TimeEntries.TimeEntry> getTimeEntries(
      String projectId, LocalDate from, LocalDate to, MiteClient client) {

    return client
        .getTimeEntries(
            new TimeEntriesRequest.Builder()
                .projectId(projectId)
                .from(from.format(formatter))
                .to(to.format(formatter))
                .build())
        .getTimeEntry();
  }

  /**
   * Deletes a list of time entries using the provided Mite client.
   *
   * @param timeEntries the list of time entries to delete
   * @param client the Mite client used to perform the deletion
   */
  public void deleteTimeEntries(List<TimeEntries.TimeEntry> timeEntries, MiteClient client) {
    timeEntries.forEach(timeEntry -> client.deleteTimeEntry(timeEntry.getId().getValue()));
  }

  /**
   * Creates new time entries using the provided Mite client.
   *
   * @param timeEntries the list of time entries to create
   * @param client the Mite client used to perform the creation
   */
  public void createTimeEntries(List<TimeEntries.TimeEntry> timeEntries, MiteClient client) {
    timeEntries.stream()
        .map(timeEntry -> conversionService.convert(timeEntry, TimeEntry.class))
        .forEach(client::createTimeEntry);
  }
}
