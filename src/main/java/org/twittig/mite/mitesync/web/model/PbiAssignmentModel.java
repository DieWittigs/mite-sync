package org.twittig.mite.mitesync.web.model;

/**
 * PBI assignment: identifies the main work PBI for a day. Sent in the request body of
 * POST /daily-reports/{project}/{date}/preview.
 */
public class PbiAssignmentModel {

  /**
   * Required for calendar-devops profiles (enforced in the facade — git-activity profiles do not
   * use a main PBI).
   */
  private Integer mainPbiId;

  /** Daily target in hours. When null, the service falls back to the default (6.25h). */
  private Double targetHours;

  public PbiAssignmentModel() {}

  public Integer getMainPbiId() {
    return mainPbiId;
  }

  public void setMainPbiId(Integer mainPbiId) {
    this.mainPbiId = mainPbiId;
  }

  public Double getTargetHours() {
    return targetHours;
  }

  public void setTargetHours(Double targetHours) {
    this.targetHours = targetHours;
  }
}
