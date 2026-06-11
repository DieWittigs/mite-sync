package org.twittig.mite.mitesync.facade;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;
import org.twittig.mite.mitesync.config.DailyReportProperties.Profile;
import org.twittig.mite.mitesync.config.DailyReportProperties.WorkflowType;
import org.twittig.mite.mitesync.config.ProfileRegistry;
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
 * Orchestrates the daily-report workflow of the requested project profile: reads the profile's
 * sources, builds a proposal and books it.
 *
 * <p>The actual booking lives in a separate method to allow a manual review pause between
 * preview and book.
 */
@Component
public class DailyReportFacade {

  private final ProfileRegistry profileRegistry;
  private final GoogleCalendarService googleCalendarService;
  private final AzureDevOpsService azureDevOpsService;
  private final MiteBookingService miteBookingService;
  private final BookingProposalService bookingProposalService;

  public DailyReportFacade(
      ProfileRegistry profileRegistry,
      GoogleCalendarService googleCalendarService,
      AzureDevOpsService azureDevOpsService,
      MiteBookingService miteBookingService,
      BookingProposalService bookingProposalService) {
    this.profileRegistry = profileRegistry;
    this.googleCalendarService = googleCalendarService;
    this.azureDevOpsService = azureDevOpsService;
    this.miteBookingService = miteBookingService;
    this.bookingProposalService = bookingProposalService;
  }

  /** Builds the daily report for the default profile (legacy endpoints without a project). */
  public DailyReportModel preview(LocalDate date, PbiAssignmentModel pbiAssignment) {
    return preview(profileRegistry.defaultProfileKey(), date, pbiAssignment);
  }

  /** Builds the full daily report including the booking proposal for the given profile. */
  public DailyReportModel preview(String profileKey, LocalDate date, PbiAssignmentModel pbiAssignment) {
    Profile profile = profileRegistry.resolve(profileKey);
    if (profile.getWorkflowType() != WorkflowType.CALENDAR_DEVOPS) {
      throw new UnsupportedWorkflowException(profile.getWorkflowType());
    }

    List<CalendarEventModel> events =
        googleCalendarService.getEventsForDay(date, profile.getRules().getRoundingStepMinutes());
    List<MiteEntryModel> alreadyBooked = miteBookingService.getEntriesForDate(profile, date);
    List<WorkItemModel> changedToday = azureDevOpsService.getWorkItemsChangedByMeOnDate(date);
    List<WorkItemModel> openItems = azureDevOpsService.getOpenWorkItemsAssignedToMe();

    List<ProposalEntryModel> proposal =
        bookingProposalService.buildProposal(profile, events, alreadyBooked, openItems, pbiAssignment);

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

  /** Books the supplied entries via the default profile (legacy endpoints without a project). */
  public BookingResultModel book(LocalDate date, List<ProposalEntryModel> entries) {
    return book(profileRegistry.defaultProfileKey(), date, entries);
  }

  /** Books the supplied entries into the Mite instance of the given profile. */
  public BookingResultModel book(String profileKey, LocalDate date, List<ProposalEntryModel> entries) {
    Profile profile = profileRegistry.resolve(profileKey);
    return miteBookingService.book(profile, date, entries);
  }
}
