package org.twittig.mite.mitesync.web.model;

import jakarta.validation.constraints.NotNull;

/**
 * PBI assignment: identifies the main work PBI for a day. Sent in the request body of
 * POST /daily-reports/{date}/preview.
 */
public class PbiAssignmentModel {

  @NotNull(message = "mainPbiId must not be null")
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
