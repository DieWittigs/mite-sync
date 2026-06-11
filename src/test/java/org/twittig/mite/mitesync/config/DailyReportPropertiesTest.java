package org.twittig.mite.mitesync.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.twittig.mite.mitesync.config.DailyReportProperties.Profile;
import org.twittig.mite.mitesync.config.DailyReportProperties.WorkflowType;

class DailyReportPropertiesTest {

  private final ApplicationContextRunner runner =
      new ApplicationContextRunner().withUserConfiguration(TestConfig.class);

  @Test
  void twoProfilesWithDifferentWorkflowTypesAndMiteInstancesCanCoexist() {
    runner
        .withPropertyValues(
            "daily-reports.default-profile=alpha",
            "daily-reports.profiles.alpha.workflow-type=calendar-devops",
            "daily-reports.profiles.alpha.mite-instance=source",
            "daily-reports.profiles.alpha.project-id=111",
            "daily-reports.profiles.alpha.rules.target-minutes=375",
            "daily-reports.profiles.beta.workflow-type=git-activity",
            "daily-reports.profiles.beta.mite-instance=target",
            "daily-reports.profiles.beta.project-id=222",
            "daily-reports.profiles.beta.rules.target-minutes=240")
        .run(
            context -> {
              DailyReportProperties props = context.getBean(DailyReportProperties.class);
              assertThat(props.getDefaultProfile()).isEqualTo("alpha");
              assertThat(props.getProfiles()).containsOnlyKeys("alpha", "beta");

              Profile alpha = props.getProfiles().get("alpha");
              assertThat(alpha.getWorkflowType()).isEqualTo(WorkflowType.CALENDAR_DEVOPS);
              assertThat(alpha.getMiteInstance()).isEqualTo("source");
              assertThat(alpha.getProjectId()).isEqualTo("111");
              assertThat(alpha.getRules().getTargetMinutes()).isEqualTo(375);

              Profile beta = props.getProfiles().get("beta");
              assertThat(beta.getWorkflowType()).isEqualTo(WorkflowType.GIT_ACTIVITY);
              assertThat(beta.getMiteInstance()).isEqualTo("target");
              assertThat(beta.getProjectId()).isEqualTo("222");
              assertThat(beta.getRules().getTargetMinutes()).isEqualTo(240);
            });
  }

  @Test
  void ruleDefaultsApplyWhenNotConfigured() {
    runner
        .withPropertyValues("daily-reports.profiles.alpha.project-id=111")
        .run(
            context -> {
              Profile alpha =
                  context.getBean(DailyReportProperties.class).getProfiles().get("alpha");
              assertThat(alpha.getWorkflowType()).isEqualTo(WorkflowType.CALENDAR_DEVOPS);
              assertThat(alpha.getMiteInstance()).isEqualTo("source");
              assertThat(alpha.getRules().getDailyEventSummary()).isEqualTo("Team Daily");
              assertThat(alpha.getRules().getDailyFixedMinutes()).isEqualTo(15);
              assertThat(alpha.getRules().getRoundingStepMinutes()).isEqualTo(15);
              assertThat(alpha.getRules().getTargetMinutes()).isEqualTo(375);
            });
  }

  @Test
  void gitActivitySettingsBindWithDefaults() {
    runner
        .withPropertyValues(
            "daily-reports.profiles.alpha.git.repositories[0]=/path/to/repo-a",
            "daily-reports.profiles.alpha.git.repositories[1]=/path/to/repo-b",
            "daily-reports.profiles.alpha.git.author=thomas")
        .run(
            context -> {
              var git = context.getBean(DailyReportProperties.class)
                  .getProfiles().get("alpha").getGit();
              assertThat(git.getRepositories())
                  .containsExactly("/path/to/repo-a", "/path/to/repo-b");
              assertThat(git.getAuthor()).isEqualTo("thomas");
              // defaults
              assertThat(git.getTicketPattern()).isEqualTo("^([A-Z]+-\\d+)");
              assertThat(git.getSessionGapMinutes()).isEqualTo(90);
              assertThat(git.getLeadInMinutes()).isEqualTo(30);
              assertThat(git.getFallbackTicket()).isEmpty();
            });
  }

  @Configuration
  @EnableConfigurationProperties(DailyReportProperties.class)
  static class TestConfig {}
}
