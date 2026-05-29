package org.twittig.mite.mitesync.web.model;

import java.time.LocalDate;
import java.util.List;

/** Antwort von POST /daily-reports/{date}/book: was wurde erfolgreich gebucht und was nicht. */
public class BookingResultModel {

  private LocalDate date;
  private List<MiteEntryModel> created;
  private List<FailedEntry> failed;
  private int totalMinutesCreated;

  public static class FailedEntry {
    private int minutes;
    private String note;
    private String error;

    public FailedEntry() {}

    public FailedEntry(int minutes, String note, String error) {
      this.minutes = minutes;
      this.note = note;
      this.error = error;
    }

    public int getMinutes() {
      return minutes;
    }

    public void setMinutes(int minutes) {
      this.minutes = minutes;
    }

    public String getNote() {
      return note;
    }

    public void setNote(String note) {
      this.note = note;
    }

    public String getError() {
      return error;
    }

    public void setError(String error) {
      this.error = error;
    }
  }

  public LocalDate getDate() {
    return date;
  }

  public void setDate(LocalDate date) {
    this.date = date;
  }

  public List<MiteEntryModel> getCreated() {
    return created;
  }

  public void setCreated(List<MiteEntryModel> created) {
    this.created = created;
  }

  public List<FailedEntry> getFailed() {
    return failed;
  }

  public void setFailed(List<FailedEntry> failed) {
    this.failed = failed;
  }

  public int getTotalMinutesCreated() {
    return totalMinutesCreated;
  }

  public void setTotalMinutesCreated(int totalMinutesCreated) {
    this.totalMinutesCreated = totalMinutesCreated;
  }
}
