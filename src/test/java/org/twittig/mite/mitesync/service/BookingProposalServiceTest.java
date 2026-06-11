package org.twittig.mite.mitesync.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.twittig.mite.mitesync.config.DailyReportProperties.Profile;
import org.twittig.mite.mitesync.web.model.CalendarEventModel;
import org.twittig.mite.mitesync.web.model.MiteEntryModel;
import org.twittig.mite.mitesync.web.model.PbiAssignmentModel;
import org.twittig.mite.mitesync.web.model.ProposalEntryModel;
import org.twittig.mite.mitesync.web.model.WorkItemModel;

class BookingProposalServiceTest {

    private BookingProposalService service;
    private Profile profile;

    @BeforeEach
    void setUp() {
        service = new BookingProposalService();
        profile = new Profile();
        profile.setMeetingCollectorPbi("1234");
        profile.getRules().setDailyEventSummary("Team Daily");
        profile.getRules().setDailyFixedMinutes(15);
        profile.getRules().setRoundingStepMinutes(15);
        profile.getRules().setTargetMinutes(375);
    }

    // -------- Daily special handling --------

    @Test
    void daily_event_always_uses_fixed_minutes_regardless_of_duration() {
        CalendarEventModel daily = event("Team Daily", "accepted", false, 90, 90);

        List<ProposalEntryModel> result = buildWith(List.of(daily), List.of(), List.of(), pbi(123, null));

        ProposalEntryModel entry = findByNoteContaining(result, "Team Daily");
        assertThat(entry.getMinutes()).isEqualTo(15);
    }

    @Test
    void daily_comparison_is_case_insensitive() {
        CalendarEventModel daily = event("team daily", "accepted", false, 90, 90);

        List<ProposalEntryModel> result = buildWith(List.of(daily), List.of(), List.of(), pbi(123, null));

        ProposalEntryModel entry = findByNoteContaining(result, "team daily");
        assertThat(entry.getMinutes()).isEqualTo(15);
    }

    @Test
    void non_daily_meeting_uses_rounded_minutes_from_model() {
        CalendarEventModel meeting = event("Sprint Review", "accepted", false, 50, 60);

        List<ProposalEntryModel> result = buildWith(List.of(meeting), List.of(), List.of(), pbi(123, null));

        ProposalEntryModel entry = findByNoteContaining(result, "Sprint Review");
        assertThat(entry.getMinutes()).isEqualTo(60);
    }

    // -------- Skip & response status --------

    @Test
    void skipped_event_is_excluded() {
        CalendarEventModel skipped = event("Blocker", "accepted", true, 60, 60);

        List<ProposalEntryModel> result = buildWith(List.of(skipped), List.of(), List.of(), pbi(123, null));

        assertThat(result).noneMatch(e -> e.getNote() != null && e.getNote().contains("Blocker"));
    }

    @Test
    void declined_event_is_excluded() {
        CalendarEventModel declined = event("Backlog Refinement", "declined", false, 60, 60);

        List<ProposalEntryModel> result = buildWith(List.of(declined), List.of(), List.of(), pbi(123, null));

        assertThat(result).noneMatch(e -> e.getNote() != null && e.getNote().contains("Backlog Refinement"));
    }

    @Test
    void needsAction_event_is_included() {
        CalendarEventModel needsAction = event("Team Meeting", "needsAction", false, 30, 30);

        List<ProposalEntryModel> result = buildWith(List.of(needsAction), List.of(), List.of(), pbi(123, null));

        assertThat(result).anyMatch(e -> e.getNote() != null && e.getNote().contains("Team Meeting"));
    }

    @Test
    void tentative_event_is_included() {
        CalendarEventModel tentative = event("Workshop", "tentative", false, 120, 120);

        List<ProposalEntryModel> result = buildWith(List.of(tentative), List.of(), List.of(), pbi(123, null));

        assertThat(result).anyMatch(e -> e.getNote() != null && e.getNote().contains("Workshop"));
    }

    @Test
    void event_with_zero_rounded_minutes_is_excluded() {
        CalendarEventModel zero = event("Quick Note", "accepted", false, 0, 0);

        List<ProposalEntryModel> result = buildWith(List.of(zero), List.of(), List.of(), pbi(123, null));

        assertThat(result).noneMatch(e -> e.getNote() != null && e.getNote().contains("Quick Note"));
    }

    // -------- Duplicate check --------

    @Test
    void already_booked_calendar_event_is_not_duplicated() {
        CalendarEventModel event = event("Team Meeting", "accepted", false, 30, 30);
        MiteEntryModel booked = booked(1L, 30, "#595030 Team Meeting");

        List<ProposalEntryModel> result = buildWith(List.of(event), List.of(booked), List.of(), pbi(123, null));

        assertThat(result).noneMatch(e -> "#595030 Team Meeting".equals(e.getNote()));
    }

    @Test
    void duplicate_check_is_case_insensitive() {
        CalendarEventModel event = event("Team Meeting", "accepted", false, 30, 30);
        // Mite entry uses different casing
        MiteEntryModel booked = booked(1L, 30, "#595030 TEAM MEETING");

        List<ProposalEntryModel> result = buildWith(List.of(event), List.of(booked), List.of(), pbi(123, null));

        assertThat(result).noneMatch(e -> e.getNote() != null
                && e.getNote().equalsIgnoreCase("#595030 Team Meeting"));
    }

    @Test
    void different_meeting_is_not_blocked_by_existing_entry() {
        CalendarEventModel event = event("Sprint Review", "accepted", false, 60, 60);
        MiteEntryModel booked = booked(1L, 30, "#595030 Team Meeting"); // different note

        List<ProposalEntryModel> result = buildWith(List.of(event), List.of(booked), List.of(), pbi(123, null));

        assertThat(result).anyMatch(e -> e.getNote() != null && e.getNote().contains("Sprint Review"));
    }

    // -------- Dev fill / remaining time --------

    @Test
    void full_day_with_no_meetings_fills_main_pbi_with_default_target() {
        WorkItemModel workItem = workItem(1001, "Feature XY");

        List<ProposalEntryModel> result = buildWith(List.of(), List.of(), List.of(workItem), pbi(1001, null));

        ProposalEntryModel devEntry = findDevFill(result);
        assertThat(devEntry.getMinutes()).isEqualTo(375); // default 6.25 h
        assertThat(devEntry.getNote()).isEqualTo("#1001 Feature XY");
        assertThat(devEntry.getPbiId()).isEqualTo(1001);
    }

    @Test
    void meeting_minutes_reduce_dev_fill() {
        CalendarEventModel meeting = event("Sprint Review", "accepted", false, 60, 60);
        WorkItemModel workItem = workItem(1001, "Feature XY");

        List<ProposalEntryModel> result = buildWith(List.of(meeting), List.of(), List.of(workItem), pbi(1001, null));

        assertThat(findDevFill(result).getMinutes()).isEqualTo(315); // 375 - 60
    }

    @Test
    void already_booked_minutes_reduce_dev_fill() {
        MiteEntryModel booked = booked(1L, 120, "#1001 Feature XY");
        WorkItemModel workItem = workItem(1001, "Feature XY");

        List<ProposalEntryModel> result = buildWith(List.of(), List.of(booked), List.of(workItem), pbi(1001, null));

        assertThat(findDevFill(result).getMinutes()).isEqualTo(255); // 375 - 120
    }

    @Test
    void no_dev_entry_when_day_is_already_full() {
        MiteEntryModel booked = booked(1L, 375, "#1001 Feature XY");

        List<ProposalEntryModel> result = buildWith(List.of(), List.of(booked), List.of(), pbi(1001, null));

        assertThat(result).noneMatch(e -> "main-pbi-fill".equals(e.getSource()));
    }

    @Test
    void no_dev_entry_when_meetings_exceed_target() {
        CalendarEventModel m1 = event("Meeting A", "accepted", false, 300, 300);
        CalendarEventModel m2 = event("Meeting B", "accepted", false, 120, 120);

        List<ProposalEntryModel> result = buildWith(List.of(m1, m2), List.of(), List.of(), pbi(1001, null));

        assertThat(result).noneMatch(e -> "main-pbi-fill".equals(e.getSource()));
    }

    @Test
    void dev_fill_is_rounded_down_to_15min_boundary() {
        // Meeting 20 min → remaining = 375 − 20 = 355 → roundDown(355) = 345
        CalendarEventModel meeting = event("Quick Sync", "accepted", false, 20, 20);
        WorkItemModel workItem = workItem(1001, "Feature XY");

        List<ProposalEntryModel> result = buildWith(List.of(meeting), List.of(), List.of(workItem), pbi(1001, null));

        assertThat(findDevFill(result).getMinutes()).isEqualTo(345);
    }

    @Test
    void dev_fill_edge_case_14min_remaining_produces_zero_and_no_entry() {
        // Meeting 361 min → remaining = 375 − 361 = 14 → roundDown(14) = 0 → no entry
        CalendarEventModel meeting = event("Long Workshop", "accepted", false, 361, 361);

        List<ProposalEntryModel> result = buildWith(List.of(meeting), List.of(), List.of(), pbi(1001, null));

        assertThat(result).noneMatch(e -> "main-pbi-fill".equals(e.getSource()));
    }

    @Test
    void target_hours_override_sets_custom_day_target() {
        WorkItemModel workItem = workItem(1001, "Feature XY");

        List<ProposalEntryModel> result = buildWith(List.of(), List.of(), List.of(workItem), pbi(1001, 4.0));

        assertThat(findDevFill(result).getMinutes()).isEqualTo(240); // 4 h = 240 min
    }

    @Test
    void target_hours_fractional_rounds_correctly() {
        // 6.25 h = 375 min — same as the default when explicitly overridden
        WorkItemModel workItem = workItem(1001, "Feature XY");

        List<ProposalEntryModel> result = buildWith(List.of(), List.of(), List.of(workItem), pbi(1001, 6.25));

        assertThat(findDevFill(result).getMinutes()).isEqualTo(375);
    }

    @Test
    void unknown_pbi_id_uses_fallback_title() {
        List<ProposalEntryModel> result = buildWith(List.of(), List.of(), List.of(), pbi(9999, null));

        ProposalEntryModel devEntry = findDevFill(result);
        assertThat(devEntry.getNote()).isEqualTo("#9999 (unknown title)");
    }

    @Test
    void meeting_note_uses_meeting_collector_pbi_prefix() {
        CalendarEventModel meeting = event("Sprint Backlog Refinement", "accepted", false, 60, 60);

        List<ProposalEntryModel> result = buildWith(List.of(meeting), List.of(), List.of(), pbi(123, null));

        ProposalEntryModel entry = findByNoteContaining(result, "Backlog Refinement");
        assertThat(entry.getNote()).isEqualTo("#1234 Sprint Backlog Refinement");
        assertThat(entry.getSource()).isEqualTo("calendar");
    }

    // -------- buildGitProposal (git-activity workflow) --------

    @Test
    void gitProposal_passesEstimatedEntriesThrough() {
        ProposalEntryModel estimated = gitEntry(60, "#VC-1 Fix the thing");

        List<ProposalEntryModel> result =
                service.buildGitProposal(profile, List.of(estimated), List.of(), null);

        assertThat(result).containsExactly(estimated);
    }

    @Test
    void gitProposal_alreadyBookedEntry_isDroppedCaseInsensitive() {
        ProposalEntryModel estimated = gitEntry(60, "#VC-1 Fix the thing");
        MiteEntryModel alreadyBooked = booked(1L, 60, "  #vc-1 FIX THE THING ");

        List<ProposalEntryModel> result =
                service.buildGitProposal(profile, List.of(estimated), List.of(alreadyBooked), null);

        assertThat(result).isEmpty();
    }

    @Test
    void gitProposal_noFillUpByDefault() {
        ProposalEntryModel estimated = gitEntry(60, "#VC-1 Fix the thing");

        List<ProposalEntryModel> result =
                service.buildGitProposal(profile, List.of(estimated), List.of(), null);

        assertThat(result).noneMatch(e -> "git-fill".equals(e.getSource()));
    }

    @Test
    void gitProposal_fillUpTicketConfigured_fillsToTarget() {
        profile.getGit().setFillUpTicket("VC-99");
        // target 375 − estimated 60 − booked 120 = 195 → rounded down to 195 (already step-aligned)
        ProposalEntryModel estimated = gitEntry(60, "#VC-1 Fix the thing");
        MiteEntryModel alreadyBooked = booked(1L, 120, "#VC-2 Other");

        List<ProposalEntryModel> result =
                service.buildGitProposal(profile, List.of(estimated), List.of(alreadyBooked), null);

        ProposalEntryModel fill = result.stream()
                .filter(e -> "git-fill".equals(e.getSource()))
                .findFirst().orElseThrow();
        assertThat(fill.getMinutes()).isEqualTo(195);
        assertThat(fill.getNote()).isEqualTo("#VC-99 Development");
    }

    @Test
    void gitProposal_fillUp_respectsTargetHoursOverride() {
        profile.getGit().setFillUpTicket("VC-99");
        ProposalEntryModel estimated = gitEntry(60, "#VC-1 Fix the thing");

        List<ProposalEntryModel> result =
                service.buildGitProposal(profile, List.of(estimated), List.of(), 2.0);

        ProposalEntryModel fill = result.stream()
                .filter(e -> "git-fill".equals(e.getSource()))
                .findFirst().orElseThrow();
        assertThat(fill.getMinutes()).isEqualTo(60); // 120 − 60
    }

    @Test
    void gitProposal_fillUp_remainingIsRoundedDownToStep() {
        profile.getGit().setFillUpTicket("VC-99");
        // target 375 − estimated 70 = 305 → rounded down to 300
        ProposalEntryModel estimated = gitEntry(70, "#VC-1 Fix the thing");

        List<ProposalEntryModel> result =
                service.buildGitProposal(profile, List.of(estimated), List.of(), null);

        ProposalEntryModel fill = result.stream()
                .filter(e -> "git-fill".equals(e.getSource()))
                .findFirst().orElseThrow();
        assertThat(fill.getMinutes()).isEqualTo(300);
    }

    @Test
    void gitProposal_fillUp_noEntryWhenTargetAlreadyReached() {
        profile.getGit().setFillUpTicket("VC-99");
        ProposalEntryModel estimated = gitEntry(60, "#VC-1 Fix the thing");
        MiteEntryModel alreadyBooked = booked(1L, 375, "#VC-2 Other");

        List<ProposalEntryModel> result =
                service.buildGitProposal(profile, List.of(estimated), List.of(alreadyBooked), null);

        assertThat(result).noneMatch(e -> "git-fill".equals(e.getSource()));
    }

    private ProposalEntryModel gitEntry(int minutes, String note) {
        return new ProposalEntryModel(minutes, note, "git", null, null);
    }

    // -------- Helpers --------

    private List<ProposalEntryModel> buildWith(
            List<CalendarEventModel> events,
            List<MiteEntryModel> booked,
            List<WorkItemModel> workItems,
            PbiAssignmentModel pbi) {
        return service.buildProposal(profile, events, booked, workItems, pbi);
    }

    private ProposalEntryModel findDevFill(List<ProposalEntryModel> result) {
        return result.stream()
                .filter(e -> "main-pbi-fill".equals(e.getSource()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No main-pbi-fill entry in proposal"));
    }

    private ProposalEntryModel findByNoteContaining(List<ProposalEntryModel> result, String fragment) {
        return result.stream()
                .filter(e -> e.getNote() != null && e.getNote().contains(fragment))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No entry with note fragment: " + fragment));
    }

    private CalendarEventModel event(String summary, String responseStatus, boolean skipped,
                                      int minutes, int roundedMinutes) {
        CalendarEventModel e = new CalendarEventModel();
        e.setSummary(summary);
        e.setResponseStatus(responseStatus);
        e.setSkipped(skipped);
        e.setMinutes(minutes);
        e.setRoundedMinutes(roundedMinutes);
        return e;
    }

    private MiteEntryModel booked(long id, int minutes, String note) {
        return new MiteEntryModel(id, minutes, note, 0L, 0L);
    }

    private WorkItemModel workItem(int id, String title) {
        WorkItemModel w = new WorkItemModel();
        w.setId(id);
        w.setTitle(title);
        return w;
    }

    private PbiAssignmentModel pbi(Integer mainPbiId, Double targetHours) {
        PbiAssignmentModel p = new PbiAssignmentModel();
        p.setMainPbiId(mainPbiId);
        p.setTargetHours(targetHours);
        return p;
    }
}
