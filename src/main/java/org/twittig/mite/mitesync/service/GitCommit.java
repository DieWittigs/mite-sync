package org.twittig.mite.mitesync.service;

import java.time.Instant;

/** One commit of the day, as read from a local repository by {@link GitActivityService}. */
public record GitCommit(Instant time, String message, String author) {

  /** First line of the commit message. */
  public String subjectLine() {
    if (message == null) {
      return "";
    }
    int newline = message.indexOf('\n');
    return (newline < 0 ? message : message.substring(0, newline)).strip();
  }
}
