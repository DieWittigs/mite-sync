package org.twittig.mite.mitesync.web.annotation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import org.twittig.mite.mitesync.web.model.SyncJobModel;

/**
 * Validates that the 'from' date is before the 'to' date in a {@link SyncJobModel}.
 *
 * <p>The {@code DateRangeValidator} class implements the {@link ConstraintValidator} interface to
 * provide custom validation logic for the {@link ValidDateRange} annotation. It checks that the
 * 'from' date precedes the 'to' date using a specified date format.
 *
 * <p>The date format expected is "dd.MM.yyyy". The validation will pass if either date is null, as
 * it assumes other constraints (like @NotBlank) will handle null checks.
 *
 * <p>The validation will also fail if the dates are in an invalid format or if parsing errors
 * occur.
 */
public class DateRangeValidator implements ConstraintValidator<ValidDateRange, SyncJobModel> {

  private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");

  @Override
  public boolean isValid(
      SyncJobModel syncJobModel, ConstraintValidatorContext constraintValidatorContext) {

    if (syncJobModel.getFrom() == null || syncJobModel.getTo() == null) {
      return true; // @NotBlank already covers this
    }

    try {
      LocalDate from = LocalDate.parse(syncJobModel.getFrom(), formatter);
      LocalDate to = LocalDate.parse(syncJobModel.getTo(), formatter);
      // Same-day syncs are allowed (e.g. a daily sync covering only one day).
      return !from.isAfter(to);
    } catch (Exception e) {
      return false; // invalid date
    }
  }
}
