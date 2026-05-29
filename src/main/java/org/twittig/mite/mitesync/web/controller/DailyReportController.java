package org.twittig.mite.mitesync.web.controller;

import jakarta.validation.Valid;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.twittig.mite.mitesync.facade.DailyReportFacade;
import org.twittig.mite.mitesync.web.model.BookingRequestModel;
import org.twittig.mite.mitesync.web.model.BookingResultModel;
import org.twittig.mite.mitesync.web.model.DailyReportModel;
import org.twittig.mite.mitesync.web.model.PbiAssignmentModel;

/**
 * REST controller for the daily booking routine.
 *
 * <p>Workflow:
 * <ol>
 *   <li>The client posts the PBI assignment to /preview and receives a proposal with all entries.
 *   <li>The client optionally edits individual entries (minutes, notes).
 *   <li>The client posts the final entries to /book — Mite entries are created.
 * </ol>
 */
@RestController
@RequestMapping("/daily-reports")
public class DailyReportController {

  private static final Logger log = LogManager.getLogger(DailyReportController.class);
  private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE;

  private final DailyReportFacade facade;

  public DailyReportController(DailyReportFacade facade) {
    this.facade = facade;
  }

  /**
   * Builds a daily report including the booking proposal. The date is supplied as an ISO path
   * variable (YYYY-MM-DD); the PBI assignment is in the request body.
   */
  @PostMapping("/{date}/preview")
  public ResponseEntity<DailyReportModel> preview(
      @PathVariable("date") String dateStr, @RequestBody @Valid PbiAssignmentModel pbiAssignment) {
    LocalDate date = LocalDate.parse(dateStr, ISO);
    log.info("Daily-report preview requested for {} (mainPbi={})", date, pbiAssignment.getMainPbiId());
    DailyReportModel report = facade.preview(date, pbiAssignment);
    return ResponseEntity.ok(report);
  }

  /**
   * Books the entries from the request body for the given date. Entries typically come from the
   * /preview endpoint and may have been edited manually by the user.
   */
  @PostMapping("/{date}/book")
  public ResponseEntity<BookingResultModel> book(
      @PathVariable("date") String dateStr, @RequestBody @Valid BookingRequestModel request) {
    LocalDate date = LocalDate.parse(dateStr, ISO);
    log.info("Daily-report book requested for {} ({} entries)", date, request.getEntries().size());
    BookingResultModel result = facade.book(date, request.getEntries());
    log.info(
        "Daily-report booking done: {} created, {} failed",
        result.getCreated().size(),
        result.getFailed().size());
    return ResponseEntity.ok(result);
  }
}
