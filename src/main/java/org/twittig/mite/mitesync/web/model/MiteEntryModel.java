package org.twittig.mite.mitesync.web.model;

/** Schlanke Repräsentation eines existierenden Mite-Time-Entries (zum Anzeigen). */
public class MiteEntryModel {

  private long miteId;
  private int minutes;
  private String note;
  private long projectId;
  private long serviceId;

  public MiteEntryModel() {}

  public MiteEntryModel(long miteId, int minutes, String note, long projectId, long serviceId) {
    this.miteId = miteId;
    this.minutes = minutes;
    this.note = note;
    this.projectId = projectId;
    this.serviceId = serviceId;
  }

  public long getMiteId() {
    return miteId;
  }

  public void setMiteId(long miteId) {
    this.miteId = miteId;
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

  public long getProjectId() {
    return projectId;
  }

  public void setProjectId(long projectId) {
    this.projectId = projectId;
  }

  public long getServiceId() {
    return serviceId;
  }

  public void setServiceId(long serviceId) {
    this.serviceId = serviceId;
  }
}
