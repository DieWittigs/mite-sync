package org.twittig.mite.mitesync.config;

/** Thrown when a request references a project profile that is not configured. Mapped to 404. */
public class UnknownProfileException extends RuntimeException {

  public UnknownProfileException(String profileKey) {
    super("Unknown project profile '" + profileKey + "'");
  }
}
