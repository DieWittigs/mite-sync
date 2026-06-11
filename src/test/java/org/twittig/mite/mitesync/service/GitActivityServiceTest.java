package org.twittig.mite.mitesync.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.PersonIdent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.twittig.mite.mitesync.config.DailyReportProperties.GitActivity;

/** Tests against real (temporary) git repositories built with JGit. */
class GitActivityServiceTest {

  private static final LocalDate DAY = LocalDate.of(2026, 6, 10);
  private static final ZoneId ZONE = ZoneId.systemDefault();

  @TempDir Path tempDir;

  private GitActivityService service;
  private GitActivity config;

  @BeforeEach
  void setUp() {
    service = new GitActivityService();
    config = new GitActivity();
  }

  private static Instant at(LocalDate day, int hour, int minute) {
    return day.atTime(hour, minute).atZone(ZONE).toInstant();
  }

  private Git initRepo(String name) throws Exception {
    File dir = tempDir.resolve(name).toFile();
    Git git = Git.init().setDirectory(dir).call();
    config.getRepositories().add(dir.getAbsolutePath());
    return git;
  }

  private static void commit(Git git, String message, String author, String email, Instant time)
      throws Exception {
    PersonIdent ident = new PersonIdent(author, email, time, ZONE);
    git.commit()
        .setAllowEmpty(true)
        .setSign(false)
        .setAuthor(ident)
        .setCommitter(ident)
        .setMessage(message)
        .call();
  }

  @Test
  void returnsCommitsOfTheDay_sortedChronologically() throws Exception {
    try (Git git = initRepo("repo")) {
      commit(git, "VC-2: Second", "Dev", "dev@example.org", at(DAY, 14, 0));
      commit(git, "VC-1: First", "Dev", "dev@example.org", at(DAY, 9, 0));
    }

    List<GitCommit> result = service.getCommitsForDay(config, DAY);

    assertThat(result).hasSize(2);
    assertThat(result.get(0).subjectLine()).isEqualTo("VC-1: First");
    assertThat(result.get(1).subjectLine()).isEqualTo("VC-2: Second");
    assertThat(result.get(0).author()).isEqualTo("Dev");
  }

  @Test
  void commitsOfOtherDays_areExcluded() throws Exception {
    try (Git git = initRepo("repo")) {
      commit(git, "VC-1: Yesterday", "Dev", "dev@example.org", at(DAY.minusDays(1), 23, 50));
      commit(git, "VC-2: Today", "Dev", "dev@example.org", at(DAY, 0, 10));
      commit(git, "VC-3: Tomorrow", "Dev", "dev@example.org", at(DAY.plusDays(1), 0, 5));
    }

    List<GitCommit> result = service.getCommitsForDay(config, DAY);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).subjectLine()).isEqualTo("VC-2: Today");
  }

  @Test
  void authorFilter_excludesOtherCommitters() throws Exception {
    config.setAuthor("thomas");
    try (Git git = initRepo("repo")) {
      commit(git, "VC-1: Mine", "Thomas Dev", "thomas@example.org", at(DAY, 9, 0));
      commit(git, "VC-2: By name match", "THOMAS", "other@example.org", at(DAY, 10, 0));
      commit(git, "VC-3: By email match", "Someone", "thomas@example.org", at(DAY, 11, 0));
      commit(git, "VC-4: Not mine", "Colleague", "colleague@example.org", at(DAY, 12, 0));
    }

    List<GitCommit> result = service.getCommitsForDay(config, DAY);

    assertThat(result)
        .extracting(GitCommit::subjectLine)
        .containsExactly("VC-1: Mine", "VC-2: By name match", "VC-3: By email match");
  }

  @Test
  void commitsFromAllConfiguredRepositories_areMerged() throws Exception {
    try (Git git = initRepo("repo-a")) {
      commit(git, "VC-1: In repo A", "Dev", "dev@example.org", at(DAY, 10, 0));
    }
    try (Git git = initRepo("repo-b")) {
      commit(git, "VC-2: In repo B", "Dev", "dev@example.org", at(DAY, 9, 0));
    }

    List<GitCommit> result = service.getCommitsForDay(config, DAY);

    assertThat(result)
        .extracting(GitCommit::subjectLine)
        .containsExactly("VC-2: In repo B", "VC-1: In repo A");
  }

  @Test
  void commitsOnFeatureBranches_areFound() throws Exception {
    try (Git git = initRepo("repo")) {
      commit(git, "VC-1: On main", "Dev", "dev@example.org", at(DAY, 9, 0));
      git.checkout().setCreateBranch(true).setName("feature/VC-2").call();
      commit(git, "VC-2: On branch", "Dev", "dev@example.org", at(DAY, 10, 0));
    }

    List<GitCommit> result = service.getCommitsForDay(config, DAY);

    assertThat(result)
        .extracting(GitCommit::subjectLine)
        .contains("VC-1: On main", "VC-2: On branch");
  }

  @Test
  void brokenRepositoryPath_isSkippedWithoutError() throws Exception {
    config.getRepositories().add(tempDir.resolve("does-not-exist").toString());
    try (Git git = initRepo("repo")) {
      commit(git, "VC-1: Works", "Dev", "dev@example.org", at(DAY, 9, 0));
    }

    List<GitCommit> result = service.getCommitsForDay(config, DAY);

    assertThat(result).hasSize(1);
  }

  @Test
  void noRepositoriesConfigured_returnsEmptyList() {
    assertThat(service.getCommitsForDay(config, DAY)).isEmpty();
  }
}
