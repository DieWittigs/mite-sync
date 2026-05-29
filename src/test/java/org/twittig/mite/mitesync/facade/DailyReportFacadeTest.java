package org.twittig.mite.mitesync.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
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

  @Mock private GoogleCalendarService googleCalendarService;
  @Mock private AzureDevOpsService azureDevOpsService;
  @Mock private MiteBookingService miteBookingService;
  @Mock private BookingProposalService bookingProposalService;
  @InjectMocks private DailyReportFacade facade;

  @BeforeEach
  void setUp() {
    ReflectionTestUtils.setField(facade, "roundingStepMinutes", 15);
  }

  @Test
  void preview_populatesAllFields() {
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
    when(miteBookingService.getEntriesForDate(date)).thenReturn(List.of(booked));
    when(azureDevOpsService.getWorkItemsChangedByMeOnDate(date)).thenReturn(List.of(changed));
    when(azureDevOpsService.getOpenWorkItemsAssignedToMe()).thenReturn(List.of(open));
    when(bookingProposalService.buildProposal(any(), any(), any(), eq(pbi))).thenReturn(List.of(proposal));

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
  void preview_passesRoundingStepToCalendarService() {
    LocalDate date = LocalDate.of(2024, 3, 15);
    PbiAssignmentModel pbi = new PbiAssignmentModel();
    pbi.setMainPbiId(12345);

    when(googleCalendarService.getEventsForDay(date, 15)).thenReturn(List.of());
    when(miteBookingService.getEntriesForDate(date)).thenReturn(List.of());
    when(azureDevOpsService.getWorkItemsChangedByMeOnDate(date)).thenReturn(List.of());
    when(azureDevOpsService.getOpenWorkItemsAssignedToMe()).thenReturn(List.of());
    when(bookingProposalService.buildProposal(any(), any(), any(), any())).thenReturn(List.of());

    facade.preview(date, pbi);

    verify(googleCalendarService).getEventsForDay(date, 15);
  }

  @Test
  void preview_emptyProposal_totalMinutesIsZero() {
    LocalDate date = LocalDate.of(2024, 3, 15);
    PbiAssignmentModel pbi = new PbiAssignmentModel();
    pbi.setMainPbiId(12345);

    when(googleCalendarService.getEventsForDay(any(), anyInt())).thenReturn(List.of());
    when(miteBookingService.getEntriesForDate(any())).thenReturn(List.of());
    when(azureDevOpsService.getWorkItemsChangedByMeOnDate(any())).thenReturn(List.of());
    when(azureDevOpsService.getOpenWorkItemsAssignedToMe()).thenReturn(List.of());
    when(bookingProposalService.buildProposal(any(), any(), any(), any())).thenReturn(List.of());

    DailyReportModel result = facade.preview(date, pbi);

    assertThat(result.getProposalTotalMinutes()).isEqualTo(0);
  }

  @Test
  void book_delegatesToMiteBookingService() {
    LocalDate date = LocalDate.of(2024, 3, 15);
    List<ProposalEntryModel> entries = List.of(
        new ProposalEntryModel(90, "Work", "main-pbi-fill", 1, "PBI"));
    BookingResultModel expected = new BookingResultModel();
    expected.setDate(date);

    when(miteBookingService.book(date, entries)).thenReturn(expected);

    BookingResultModel result = facade.book(date, entries);

    assertThat(result).isSameAs(expected);
    verify(miteBookingService).book(date, entries);
  }
}
