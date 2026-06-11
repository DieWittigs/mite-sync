package org.twittig.mite.mitesync.web.model;

/**
 * One commit shown in the preview of a git-activity profile — the evidence behind the estimated
 * proposal entries.
 */
public class GitCommitModel {

  /** Local time of day of the commit, formatted HH:mm. */
  private String time;

  private String author;

  /** First line of the commit message. */
  private String subject;

  public GitCommitModel() {}

  public GitCommitModel(String time, String author, String subject) {
    this.time = time;
    this.author = author;
    this.subject = subject;
  }

  public String getTime() {
    return time;
  }

  public void setTime(String time) {
    this.time = time;
  }

  public String getAuthor() {
    return author;
  }

  public void setAuthor(String author) {
    this.author = author;
  }

  public String getSubject() {
    return subject;
  }

  public void setSubject(String subject) {
    this.subject = subject;
  }
}
