package org.twittig.mite.mitesync.web.model;

import jakarta.validation.constraints.NotNull;

/**
 * PBI-Zuordnung: definiert die Hauptarbeit-PBI für einen Tag und optional zusätzliche PBIs für die
 * Verteilung der Dev-Stunden. Wird im Request-Body von POST /daily-reports/{date}/preview
 * mitgegeben.
 */
public class PbiAssignmentModel {

  @NotNull(message = "mainPbiId darf nicht leer sein")
  private Integer mainPbiId;

  /** Ziel-Stunden für den Tag, in Stunden. Wenn null, default 6.25h (kann von Service überschrieben werden). */
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
