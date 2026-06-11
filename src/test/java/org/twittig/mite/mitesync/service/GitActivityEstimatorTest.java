package org.twittig.mite.mitesync.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.twittig.mite.mitesync.config.DailyReportProperties.GitActivity;
import org.twittig.mite.mitesync.web.model.ProposalEntryModel;

class GitActivityEstimatorTest {

  private static final Instant T0 = Instant.parse("2026-06-10T09:00:00Z");

  private GitActivityEstimator estimator;
  private GitActivity config;

  @BeforeEach
  void setUp() {
    estimator = new GitActivityEstimator();
    config = new GitActivity(); // defaults: gap 90, lead-in 30, pattern ^([A-Z]+-\d+)
  }

  private static GitCommit commit(int minutesAfterT0, String message) {
    return new GitCommit(T0.plusSeconds(minutesAfterT0 * 60L), message, "Dev");
  }

  private List<ProposalEntryModel> estimate(GitCommit... commits) {
    return estimator.estimate(List.of(commits), config, 15);
  }

  // -------- Basics --------

  @Test
  void noCommits_returnsEmptyProposal() {
    assertThat(estimator.estimate(List.of(), config, 15)).isEmpty();
    assertThat(estimator.estimate(null, config, 15)).isEmpty();
  }

  @Test
  void singleCommit_countsLeadInMinutes() {
    List<ProposalEntryModel> result = estimate(commit(0, "VC-1: Fix the thing"));

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getMinutes()).isEqualTo(30); // lead-in only
    assertThat(result.get(0).getNote()).isEqualTo("#VC-1 Fix the thing");
    assertThat(result.get(0).getSource()).isEqualTo("git");
  }

  @Test
  void sessionSpansFirstToLastCommit_plusLeadIn() {
    List<ProposalEntryModel> result =
        estimate(commit(0, "VC-1: Start"), commit(60, "VC-1: Finish"));

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getMinutes()).isEqualTo(90); // 60 span + 30 lead-in
  }

  @Test
  void noteUsesSubjectOfLatestCommit() {
    List<ProposalEntryModel> result =
        estimate(commit(0, "VC-1: Start"), commit(60, "VC-1: Finish"));

    assertThat(result.get(0).getNote()).isEqualTo("#VC-1 Finish");
  }

  // -------- Sessions --------

  @Test
  void gapLargerThanSessionGap_splitsIntoTwoSessions() {
    // 4 h gap → two single-commit sessions of 30 min lead-in each
    List<ProposalEntryModel> result =
        estimate(commit(0, "VC-1: Morning"), commit(240, "VC-1: Afternoon"));

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getMinutes()).isEqualTo(60); // 30 + 30
  }

  @Test
  void gapWithinSessionGap_staysOneSession() {
    // 89 min gap (≤ 90) → one session: 89 + 30 = 119 → rounded up to 120
    List<ProposalEntryModel> result =
        estimate(commit(0, "VC-1: Start"), commit(89, "VC-1: End"));

    assertThat(result.get(0).getMinutes()).isEqualTo(120);
  }

  // -------- Distribution across tickets --------

  @Test
  void sessionMinutesAreDistributedProportionallyToCommitCount() {
    // Session 09:00–10:00 + 30 lead-in = 90 min; VC-1 has 2 of 3 commits, VC-2 one
    List<ProposalEntryModel> result =
        estimate(
            commit(0, "VC-1: Part one"),
            commit(30, "VC-2: Other work"),
            commit(60, "VC-1: Part two"));

    assertThat(byNotePrefix(result, "#VC-1").getMinutes()).isEqualTo(60); // 90 * 2/3
    assertThat(byNotePrefix(result, "#VC-2").getMinutes()).isEqualTo(30); // 90 * 1/3
  }

  @Test
  void perTicketTotalsAreRoundedUpToTheStep() {
    // Session 09:00–09:50 + 30 = 80 min; VC-1: 53.3 → 60, VC-2: 26.7 → 30
    List<ProposalEntryModel> result =
        estimate(
            commit(0, "VC-1: Part one"),
            commit(25, "VC-2: Other work"),
            commit(50, "VC-1: Part two"));

    assertThat(byNotePrefix(result, "#VC-1").getMinutes()).isEqualTo(60);
    assertThat(byNotePrefix(result, "#VC-2").getMinutes()).isEqualTo(30);
  }

  @Test
  void ticketMinutesAccumulateAcrossSessions() {
    // Two sessions (gap 240): each 30 min lead-in for the same ticket → 60 total
    List<ProposalEntryModel> result =
        estimate(
            commit(0, "VC-1: Morning"),
            commit(240, "VC-1: Afternoon"),
            commit(250, "VC-2: Quick fix"));

    // Session 2: span 10 + 30 = 40; VC-1 1/2 → 20, VC-2 1/2 → 20
    assertThat(byNotePrefix(result, "#VC-1").getMinutes()).isEqualTo(60); // 30 + 20 → ceil 60
    assertThat(byNotePrefix(result, "#VC-2").getMinutes()).isEqualTo(30); // 20 → ceil 30
  }

  // -------- Ticket extraction --------

  @Test
  void commitWithoutTicket_usesFallbackTicket() {
    config.setFallbackTicket("MISC");

    List<ProposalEntryModel> result = estimate(commit(0, "Refactor build setup"));

    assertThat(result.get(0).getNote()).isEqualTo("#MISC Refactor build setup");
  }

  @Test
  void commitWithoutTicket_blankFallback_noteHasNoTicketPrefix() {
    List<ProposalEntryModel> result = estimate(commit(0, "Refactor build setup"));

    assertThat(result.get(0).getNote()).isEqualTo("Refactor build setup");
  }

  @Test
  void customTicketPattern_isApplied() {
    config.setTicketPattern("^#(\\d+)");

    List<ProposalEntryModel> result = estimate(commit(0, "#4711 Fix encoding"));

    assertThat(result.get(0).getNote()).isEqualTo("#4711 Fix encoding");
  }

  @Test
  void subjectIsFirstLineOfMultilineMessage() {
    List<ProposalEntryModel> result =
        estimate(commit(0, "VC-9: Subject line\n\nLong body\nwith details"));

    assertThat(result.get(0).getNote()).isEqualTo("#VC-9 Subject line");
  }

  @Test
  void messageThatIsOnlyATicketId_getsPlaceholderSubject() {
    List<ProposalEntryModel> result = estimate(commit(0, "VC-9"));

    assertThat(result.get(0).getNote()).isEqualTo("#VC-9 (no subject)");
  }

  private static ProposalEntryModel byNotePrefix(List<ProposalEntryModel> entries, String prefix) {
    return entries.stream()
        .filter(e -> e.getNote() != null && e.getNote().startsWith(prefix + " "))
        .findFirst()
        .orElseThrow(() -> new AssertionError("No entry with note prefix: " + prefix));
  }
}
