package org.twittig.mite.mitesync.web.controller;

import jakarta.validation.Valid;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.twittig.mite.mitesync.facade.MiteSyncFacade;
import org.twittig.mite.mitesync.web.model.SyncJobModel;

/**
 * Controller for handling synchronization jobs.
 *
 * <p>This class provides endpoints to start synchronization jobs for time entries using the Mite
 * API. It interacts with the MiteSyncFacade to perform the sync operations and logs the progress.
 */
@RestController
@RequestMapping("/sync-jobs")
public class MiteSyncController {

  private static final Logger log = LogManager.getLogger(MiteSyncController.class);
  private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");

  private final MiteSyncFacade miteSyncFacade;

  public MiteSyncController(MiteSyncFacade miteSyncFacade) {
    this.miteSyncFacade = miteSyncFacade;
  }

  /**
   * Handles a POST request to initiate a synchronization job.
   *
   * <p>This method logs the start and end of a synchronization job, performs the synchronization
   * using the MiteSyncFacade, and returns a response entity containing the outcome of the
   * synchronization.
   *
   * @param syncJobModel the synchronization job model containing the name, date range, and other
   *     details of the job
   * @return a response entity containing a SyncJobModel with the result of the synchronization
   */
  @PostMapping
  public ResponseEntity<SyncJobModel> getSyncJobs(@RequestBody @Valid SyncJobModel syncJobModel) {

    log.info(
        "Start SyncJob: {} with date from: {} to {}",
        syncJobModel.getName(),
        syncJobModel.getFrom(),
        syncJobModel.getTo());

    miteSyncFacade.sync(
        LocalDate.parse(syncJobModel.getFrom(), formatter),
        LocalDate.parse(syncJobModel.getTo(), formatter));

    log.info(
        "End SyncJob: {} with date from: {} to {}",
        syncJobModel.getName(),
        syncJobModel.getFrom(),
        syncJobModel.getTo());

    return ResponseEntity.ok(
        SyncJobModel.Builder.builder()
            .withName(syncJobModel.getName())
            .withMessage(
                "SyncJob von Source-Mite zu Target-Mite erfolgreich")
            .withSuccess(true)
            .withFrom(syncJobModel.getFrom())
            .withTo(syncJobModel.getTo())
            .build());
  }
}
