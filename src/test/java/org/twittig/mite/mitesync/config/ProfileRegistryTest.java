package org.twittig.mite.mitesync.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.twittig.mite.mitesync.config.DailyReportProperties.Profile;
import org.twittig.mite.mitesync.config.DailyReportProperties.WorkflowType;

class ProfileRegistryTest {

  private ProfileRegistry registry;
  private Profile alpha;

  @BeforeEach
  void setUp() {
    DailyReportProperties properties = new DailyReportProperties();
    properties.setDefaultProfile("alpha");
    alpha = new Profile();
    alpha.setWorkflowType(WorkflowType.CALENDAR_DEVOPS);
    properties.getProfiles().put("alpha", alpha);
    registry = new ProfileRegistry(properties);
  }

  @Test
  void resolve_returnsConfiguredProfile() {
    assertThat(registry.resolve("alpha")).isSameAs(alpha);
  }

  @Test
  void resolve_unknownKey_throws() {
    assertThatThrownBy(() -> registry.resolve("nope"))
        .isInstanceOf(UnknownProfileException.class)
        .hasMessageContaining("nope");
  }

  @Test
  void defaultProfileKey_returnsConfiguredKey() {
    assertThat(registry.defaultProfileKey()).isEqualTo("alpha");
  }
}
