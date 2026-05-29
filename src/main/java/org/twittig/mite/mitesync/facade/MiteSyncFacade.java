package org.twittig.mite.mitesync.facade;

import io.seventytwo.oss.mite.MiteClient;
import java.time.LocalDate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.twittig.mite.mitesync.service.MiteSyncService;

/**
 * This class is a facade for synchronizing time entries between source and target projects using
 * the Mite API.
 */
@Component
public class MiteSyncFacade {

  @Value("${mite-sync.source.project-id}")
  private String sourceProjectId;

  @Value("${mite-sync.target.project-id}")
  private String targetProjectId;

  private final MiteClient sourceMiteClient;
  private final MiteClient targetMiteClient;
  private final MiteSyncService miteSyncService;

  public MiteSyncFacade(
      MiteClient sourceMiteClient, MiteClient targetMiteClient, MiteSyncService miteSyncService) {
    this.sourceMiteClient = sourceMiteClient;
    this.targetMiteClient = targetMiteClient;
    this.miteSyncService = miteSyncService;
  }

  /**
   * Synchronizes time entries between the source project and the target project within the
   * specified date range.
   *
   * @param from the start date of the range to sync (inclusive)
   * @param to the end date of the range to sync (inclusive)
   */
  public void sync(LocalDate from, LocalDate to) {

    // Delete all time entries for the target project
    miteSyncService.deleteTimeEntries(
        miteSyncService.getTimeEntries(targetProjectId, from, to, targetMiteClient),
        targetMiteClient);

    // Create all time entries for the target project
    miteSyncService.createTimeEntries(
        miteSyncService.getTimeEntries(sourceProjectId, from, to, sourceMiteClient),
        targetMiteClient);
  }
}
