package org.twittig.mite.mitesync.config;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration of the daily-report workflows. Each profile describes one project: which workflow
 * type composes the proposal, which Mite instance is read from and booked into, and the booking
 * rules.
 *
 * <p>Bound from {@code daily-reports.*} in {@code application.yml}; all values can be overridden
 * via environment variables (Spring relaxed binding, e.g. {@code
 * DAILY_REPORTS_PROFILES_DEFAULT_RULES_TARGET_MINUTES}).
 */
@ConfigurationProperties(prefix = "daily-reports")
public class DailyReportProperties {

  /** Profile key used by the legacy endpoints without a project path segment. */
  private String defaultProfile = "default";

  private Map<String, Profile> profiles = new LinkedHashMap<>();

  public String getDefaultProfile() {
    return defaultProfile;
  }

  public void setDefaultProfile(String defaultProfile) {
    this.defaultProfile = defaultProfile;
  }

  public Map<String, Profile> getProfiles() {
    return profiles;
  }

  public void setProfiles(Map<String, Profile> profiles) {
    this.profiles = profiles;
  }

  /** Which sources compose the booking proposal of a profile. */
  public enum WorkflowType {
    /** Meetings from Google Calendar + work items from Azure DevOps + fill-up onto a main PBI. */
    CALENDAR_DEVOPS,
    /** Proposal derived purely from local git history (issues #2/#3, not implemented yet). */
    GIT_ACTIVITY
  }

  /** One project profile. */
  public static class Profile {

    private WorkflowType workflowType = WorkflowType.CALENDAR_DEVOPS;

    /** Key of the Mite instance this profile reads from and books into ("source" or "target"). */
    private String miteInstance = "source";

    /** Mite project id used for reading already-booked entries and for created time entries. */
    private String projectId;

    /** Mite service id for created time entries. */
    private String serviceId;

    /** Collector PBI for meeting notes (calendar events are booked under this number). */
    private String meetingCollectorPbi;

    private Rules rules = new Rules();

    public WorkflowType getWorkflowType() {
      return workflowType;
    }

    public void setWorkflowType(WorkflowType workflowType) {
      this.workflowType = workflowType;
    }

    public String getMiteInstance() {
      return miteInstance;
    }

    public void setMiteInstance(String miteInstance) {
      this.miteInstance = miteInstance;
    }

    public String getProjectId() {
      return projectId;
    }

    public void setProjectId(String projectId) {
      this.projectId = projectId;
    }

    public String getServiceId() {
      return serviceId;
    }

    public void setServiceId(String serviceId) {
      this.serviceId = serviceId;
    }

    public String getMeetingCollectorPbi() {
      return meetingCollectorPbi;
    }

    public void setMeetingCollectorPbi(String meetingCollectorPbi) {
      this.meetingCollectorPbi = meetingCollectorPbi;
    }

    public Rules getRules() {
      return rules;
    }

    public void setRules(Rules rules) {
      this.rules = rules;
    }
  }

  /** Booking rules of a profile. */
  public static class Rules {

    /** The daily is always booked at {@link #dailyFixedMinutes}, regardless of duration. */
    private String dailyEventSummary = "Team Daily";

    private int dailyFixedMinutes = 15;

    /** Other meetings are rounded up to the next step. */
    private int roundingStepMinutes = 15;

    /** Daily target when the request does not override it. 375 min = 6.25 h. */
    private int targetMinutes = 375;

    public String getDailyEventSummary() {
      return dailyEventSummary;
    }

    public void setDailyEventSummary(String dailyEventSummary) {
      this.dailyEventSummary = dailyEventSummary;
    }

    public int getDailyFixedMinutes() {
      return dailyFixedMinutes;
    }

    public void setDailyFixedMinutes(int dailyFixedMinutes) {
      this.dailyFixedMinutes = dailyFixedMinutes;
    }

    public int getRoundingStepMinutes() {
      return roundingStepMinutes;
    }

    public void setRoundingStepMinutes(int roundingStepMinutes) {
      this.roundingStepMinutes = roundingStepMinutes;
    }

    public int getTargetMinutes() {
      return targetMinutes;
    }

    public void setTargetMinutes(int targetMinutes) {
      this.targetMinutes = targetMinutes;
    }
  }
}
