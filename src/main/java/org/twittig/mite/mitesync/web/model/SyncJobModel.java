package org.twittig.mite.mitesync.web.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.io.Serializable;
import org.twittig.mite.mitesync.web.annotation.ValidDateRange;

/**
 * Model representing a synchronization job.
 *
 * <p>This class contains information about a synchronization job such as the job name, a message,
 * success status, and the date range for the synchronization. It also includes validation
 * annotations to ensure that the data is correct.
 *
 * <p>The {@code SyncJobModel} class provides a nested {@code Builder} class to facilitate the
 * construction of {@code SyncJobModel} instances.
 *
 * <p>The {@code ValidDateRange} annotation ensures that the 'from' date is before the 'to' date.
 */
@ValidDateRange
public class SyncJobModel implements Serializable {

  @NotBlank(message = "Name must not be blank")
  private String name;

  private String message;
  private boolean success;

  @NotBlank(message = "'from' date must not be blank")
  @Pattern(
      regexp = "\\d{2}.\\d{2}.\\d{4}",
      message = "'from' date must use the format 'dd.MM.yyyy'")
  private String from;

  @NotBlank(message = "'to' date must not be blank")
  @Pattern(regexp = "\\d{2}.\\d{2}.\\d{4}", message = "'to' date must use the format 'dd.MM.yyyy'")
  private String to;

  public SyncJobModel() {}

  private SyncJobModel(Builder builder) {
    name = builder.name;
    message = builder.message;
    success = builder.success;
    from = builder.from;
    to = builder.to;
  }

  public static final class Builder {
    private String name;
    private String message;
    private boolean success;
    private String from;
    private String to;

    private Builder() {}

    public static Builder builder() {
      return new Builder();
    }

    public Builder withName(String val) {
      name = val;
      return this;
    }

    public Builder withMessage(String val) {
      message = val;
      return this;
    }

    public Builder withSuccess(boolean val) {
      success = val;
      return this;
    }

    public Builder withFrom(String val) {
      from = val;
      return this;
    }

    public Builder withTo(String val) {
      to = val;
      return this;
    }

    public SyncJobModel build() {
      return new SyncJobModel(this);
    }
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public boolean isSuccess() {
    return success;
  }

  public void setSuccess(boolean success) {
    this.success = success;
  }

  public String getFrom() {
    return from;
  }

  public void setFrom(String from) {
    this.from = from;
  }

  public String getTo() {
    return to;
  }

  public void setTo(String to) {
    this.to = to;
  }
}
