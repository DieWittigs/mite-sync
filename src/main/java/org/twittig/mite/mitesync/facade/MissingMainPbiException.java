package org.twittig.mite.mitesync.facade;

/**
 * Thrown when a calendar-devops preview is requested without a main PBI. Mapped to 400. (The
 * constraint lives here instead of a bean-validation annotation because git-activity profiles do
 * not use a main PBI.)
 */
public class MissingMainPbiException extends RuntimeException {

  public MissingMainPbiException() {
    super("mainPbiId is required for calendar-devops profiles");
  }
}
