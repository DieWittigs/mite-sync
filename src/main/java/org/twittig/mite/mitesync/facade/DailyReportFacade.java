package org.twittig.mite.mitesync.facade;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.twittig.mite.mitesync.service.AzureDevOpsService;
import org.twittig.mite.mitesync.service.BookingProposalService;
import org.twittig.mite.mitesync.service.GoogleCalendarService;
import org.twittig.mite.mitesync.service.MiteBookingService;
import org.twittig.mite.mitesync.web.model.BookingResultModel;
import org.twittig.mite.mitesync.web.model.CalendarEventModel;
import org.twittig.mite.mitesync.web.model.DailyReportModel;
import org.twittig.mite.mitesync.web.model.MiteEntryModel;
import org.twittig.mite.mitesync.web.model.PbiAssignmentModel;
import org.twittig.mite.mitesync.web.model.ProposalEntryModel;
import org.twittig.mite.mitesync.web.model.WorkItemModel;

/**
 * Orchestrates the daily-report workflow: reads Calendar + DevOps + Mite, builds a proposal.
 *
 * <p>The actual booking lives in a separate method to allow a manual review pause between
 * preview and book.
 */
@Component
public class DailyReportFacade {

  @Value("${daily-reports.rules.rounding-step-minutes}")
  private int roundingStepMinutes;

  private final GoogleCalendarService googleCalendarService;
  private final AzureDevOpsService azureDevOpsService;
  private final MiteBookingService miteBookingService;
  private final BookingProposalService bookingProposalService;

  public DailyReportFacade(
      GoogleCalendarService googleCalendarService,
      AzureDevOpsService azureDevOpsService,
      MiteBookingService miteBookingService,
      BookingProposalService bookingProposalService) {
    this.googleCalendarService = googleCalendarService;
    this.azureDevOpsService = azureDevOpsService;
    this.miteBookingService = miteBookingService;
    this.bookingProposalService = bookingProposalService;
  }

  /** Builds the full daily report including the booking proposal. */
  public DailyReportModel preview(LocalDate date, PbiAssignmentModel pbiAssignment) {
    List<CalendarEventModel> events =
        googleCalendarService.getEventsForDay(date, roundingStepMinutes);
    List<MiteEntryModel> alreadyBooked = miteBookingService.getEntriesForDate(date);
    List<WorkItemModel> changedToday = azureDevOpsService.getWorkItemsChangedByMeOnDate(date);
    List<WorkItemModel> openItems = azureDevOpsService.getOpenWorkItemsAssignedToMe();

    List<ProposalEntryModel> proposal =
        bookingProposalService.buildProposal(events, alreadyBooked, openItems, pbiAssignment);

    DailyReportModel m = new DailyReportModel();
    m.setDate(date);
    m.setDayOfWeek(date.getDayOfWeek().getDisplayName(java.time.format.TextStyle.SHORT, Locale.ENGLISH));
    m.setCalendarEvents(events);
    m.setAlreadyBookedInMite(alreadyBooked);
    m.setDevOpsActivityOnDate(changedToday);
    m.setOpenWorkItems(openItems);
    m.setProposal(proposal);
    m.setProposalTotalMinutes(proposal.stream().mapToInt(ProposalEntryModel::getMinutes).sum());
    return m;
  }

  /** Books the supplied entries into the source Mite. */
  public BookingResultModel book(LocalDate date, List<ProposalEntryModel> entries) {
    return miteBookingService.book(date, entries);
  }
}
