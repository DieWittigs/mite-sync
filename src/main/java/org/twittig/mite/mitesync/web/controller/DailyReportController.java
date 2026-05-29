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
 * REST-Controller für die tägliche Buchungs-Routine.
 *
 * <p>Workflow:
 * <ol>
 *   <li>Client postet PBI-Zuordnung an /preview, bekommt einen Vorschlag mit allen Einträgen
 *   <li>Client editiert ggf. einzelne Einträge (Stunden, Notizen)
 *   <li>Client postet die finalen Einträge an /book — Mite-Buchungen werden erstellt
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
   * Erzeugt einen Tagesbericht inkl. Buchungsvorschlag. Datum im ISO-Format (YYYY-MM-DD) als
   * Path-Variable, PBI-Zuordnung im Body.
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
   * Bucht die im Body übergebenen Einträge an dem Datum. Die Einträge kommen typischerweise vom
   * /preview-Endpoint und können vorher manuell editiert worden sein.
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
