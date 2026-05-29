package org.twittig.mite.mitesync.web.model;

import java.time.LocalTime;

/**
 * Repräsentiert ein Calendar-Event auf einem bestimmten Tag, gefiltert auf für Mite relevante
 * Felder. Wird vom GET /daily-reports/{date}-Endpoint zurückgegeben.
 */
public class CalendarEventModel {

  private String summary;
  private LocalTime startTime;
  private LocalTime endTime;
  private int minutes;
  private int roundedMinutes; // nach Aufrundungs-Regel
  private String responseStatus; // accepted | declined | needsAction | tentative
  private boolean skipped; // true wenn in skip-summaries Liste

  public CalendarEventModel() {}

  public String getSummary() {
    return summary;
  }

  public void setSummary(String summary) {
    this.summary = summary;
  }

  public LocalTime getStartTime() {
    return startTime;
  }

  public void setStartTime(LocalTime startTime) {
    this.startTime = startTime;
  }

  public LocalTime getEndTime() {
    return endTime;
  }

  public void setEndTime(LocalTime endTime) {
    this.endTime = endTime;
  }

  public int getMinutes() {
    return minutes;
  }

  public void setMinutes(int minutes) {
    this.minutes = minutes;
  }

  public int getRoundedMinutes() {
    return roundedMinutes;
  }

  public void setRoundedMinutes(int roundedMinutes) {
    this.roundedMinutes = roundedMinutes;
  }

  public String getResponseStatus() {
    return responseStatus;
  }

  public void setResponseStatus(String responseStatus) {
    this.responseStatus = responseStatus;
  }

  public boolean isSkipped() {
    return skipped;
  }

  public void setSkipped(boolean skipped) {
    this.skipped = skipped;
  }
}
