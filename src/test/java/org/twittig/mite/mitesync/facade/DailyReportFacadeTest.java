package org.twittig.mite.mitesync.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.twittig.mite.mitesync.config.DailyReportProperties.Profile;
import org.twittig.mite.mitesync.config.DailyReportProperties.WorkflowType;
import org.twittig.mite.mitesync.config.ProfileRegistry;
import org.twittig.mite.mitesync.config.UnknownProfileException;
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

@ExtendWith(MockitoExtension.class)
class DailyReportFacadeTest {

  @Mock private ProfileRegistry profileRegistry;
  @Mock private GoogleCalendarService googleCalendarService;
  @Mock private AzureDevOpsService azureDevOpsService;
  @Mock private MiteBookingService miteBookingService;
  @Mock private BookingProposalService bookingProposalService;
  @InjectMocks private DailyReportFacade facade;

  private Profile profile;

  @BeforeEach
  void setUp() {
    profile = new Profile();
    profile.setWorkflowType(WorkflowType.CALENDAR_DEVOPS);
    profile.getRules().setRoundingStepMinutes(15);
  }

  private void givenDefaultProfile() {
    when(profileRegistry.defaultProfileKey()).thenReturn("default");
    when(profileRegistry.resolve("default")).thenReturn(profile);
  }

  @Test
  void preview_populatesAllFields() {
    givenDefaultProfile();
    LocalDate date = LocalDate.of(2024, 3, 15);
    PbiAssignmentModel pbi = new PbiAssignmentModel();
    pbi.setMainPbiId(12345);

    CalendarEventModel event = new CalendarEventModel();
    event.setSummary("Standup");
    event.setMinutes(15);

    MiteEntryModel booked = new MiteEntryModel(1L, 60, "Previous", 11L, 22L);
    WorkItemModel changed = new WorkItemModel();
    WorkItemModel open = new WorkItemModel();
    ProposalEntryModel proposal = new ProposalEntryModel(375, "Dev work", "main-pbi-fill", 12345, "My PBI");

    when(googleCalendarService.getEventsForDay(date, 15)).thenReturn(List.of(event));
    when(miteBookingService.getEntriesForDate(profile, date)).thenReturn(List.of(booked));
    when(azureDevOpsService.getWorkItemsChangedByMeOnDate(date)).thenReturn(List.of(changed));
    when(azureDevOpsService.getOpenWorkItemsAssignedToMe()).thenReturn(List.of(open));
    when(bookingProposalService.buildProposal(eq(profile), any(), any(), any(), eq(pbi)))
        .thenReturn(List.of(proposal));

    DailyReportModel result = facade.preview(date, pbi);

    assertThat(result.getDate()).isEqualTo(date);
    assertThat(result.getDayOfWeek()).isNotBlank();
    assertThat(result.getCalendarEvents()).containsExactly(event);
    assertThat(result.getAlreadyBookedInMite()).containsExactly(booked);
    assertThat(result.getDevOpsActivityOnDate()).containsExactly(changed);
    assertThat(result.getOpenWorkItems()).containsExactly(open);
    assertThat(result.getProposal()).containsExactly(proposal);
    assertThat(result.getProposalTotalMinutes()).isEqualTo(375);
  }

  @Test
  void preview_passesProfileRoundingStepToCalendarService() {
    givenDefaultProfile();
    profile.getRules().setRoundingStepMinutes(30);
    LocalDate date = LocalDate.of(2024, 3, 15);
    PbiAssignmentModel pbi = new PbiAssignmentModel();
    pbi.setMainPbiId(12345);

    when(googleCalendarService.getEventsForDay(date, 30)).thenReturn(List.of());
    when(miteBookingService.getEntriesForDate(profile, date)).thenReturn(List.of());
    when(azureDevOpsService.getWorkItemsChangedByMeOnDate(date)).thenReturn(List.of());
    when(azureDevOpsService.getOpenWorkItemsAssignedToMe()).thenReturn(List.of());
    when(bookingProposalService.buildProposal(any(), any(), any(), any(), any())).thenReturn(List.of());

    facade.preview(date, pbi);

    verify(googleCalendarService).getEventsForDay(date, 30);
  }

  @Test
  void preview_emptyProposal_totalMinutesIsZero() {
    givenDefaultProfile();
    LocalDate date = LocalDate.of(2024, 3, 15);
    PbiAssignmentModel pbi = new PbiAssignmentModel();
    pbi.setMainPbiId(12345);

    when(googleCalendarService.getEventsForDay(any(), anyInt())).thenReturn(List.of());
    when(miteBookingService.getEntriesForDate(eq(profile), any())).thenReturn(List.of());
    when(azureDevOpsService.getWorkItemsChangedByMeOnDate(any())).thenReturn(List.of());
    when(azureDevOpsService.getOpenWorkItemsAssignedToMe()).thenReturn(List.of());
    when(bookingProposalService.buildProposal(any(), any(), any(), any(), any())).thenReturn(List.of());

    DailyReportModel result = facade.preview(date, pbi);

    assertThat(result.getProposalTotalMinutes()).isEqualTo(0);
  }

  @Test
  void preview_resolvesTheRequestedProfile() {
    when(profileRegistry.resolve("alpha")).thenReturn(profile);
    LocalDate date = LocalDate.of(2024, 3, 15);
    PbiAssignmentModel pbi = new PbiAssignmentModel();
    pbi.setMainPbiId(12345);

    when(googleCalendarService.getEventsForDay(any(), anyInt())).thenReturn(List.of());
    when(miteBookingService.getEntriesForDate(eq(profile), any())).thenReturn(List.of());
    when(azureDevOpsService.getWorkItemsChangedByMeOnDate(any())).thenReturn(List.of());
    when(azureDevOpsService.getOpenWorkItemsAssignedToMe()).thenReturn(List.of());
    when(bookingProposalService.buildProposal(any(), any(), any(), any(), any())).thenReturn(List.of());

    facade.preview("alpha", date, pbi);

    verify(profileRegistry).resolve("alpha");
  }

  @Test
  void preview_unknownProfile_propagatesException() {
    when(profileRegistry.resolve("nope")).thenThrow(new UnknownProfileException("nope"));

    assertThatThrownBy(() -> facade.preview("nope", LocalDate.now(), new PbiAssignmentModel()))
        .isInstanceOf(UnknownProfileException.class);
    verifyNoInteractions(googleCalendarService, azureDevOpsService, miteBookingService);
  }

  @Test
  void preview_gitActivityWorkflow_throwsUnsupported() {
    profile.setWorkflowType(WorkflowType.GIT_ACTIVITY);
    when(profileRegistry.resolve("git")).thenReturn(profile);

    assertThatThrownBy(() -> facade.preview("git", LocalDate.now(), new PbiAssignmentModel()))
        .isInstanceOf(UnsupportedWorkflowException.class)
        .hasMessageContaining("GIT_ACTIVITY");
    verifyNoInteractions(googleCalendarService, azureDevOpsService, miteBookingService);
  }

  @Test
  void book_delegatesToMiteBookingServiceWithDefaultProfile() {
    givenDefaultProfile();
    LocalDate date = LocalDate.of(2024, 3, 15);
    List<ProposalEntryModel> entries = List.of(
        new ProposalEntryModel(90, "Work", "main-pbi-fill", 1, "PBI"));
    BookingResultModel expected = new BookingResultModel();
    expected.setDate(date);

    when(miteBookingService.book(profile, date, entries)).thenReturn(expected);

    BookingResultModel result = facade.book(date, entries);

    assertThat(result).isSameAs(expected);
    verify(miteBookingService).book(profile, date, entries);
  }

  @Test
  void book_resolvesTheRequestedProfile() {
    when(profileRegistry.resolve("alpha")).thenReturn(profile);
    LocalDate date = LocalDate.of(2024, 3, 15);
    List<ProposalEntryModel> entries = List.of(
        new ProposalEntryModel(90, "Work", "main-pbi-fill", 1, "PBI"));
    BookingResultModel expected = new BookingResultModel();

    when(miteBookingService.book(profile, date, entries)).thenReturn(expected);

    BookingResultModel result = facade.book("alpha", date, entries);

    assertThat(result).isSameAs(expected);
  }
}
