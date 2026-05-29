package org.twittig.mite.mitesync.web.annotation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Custom annotation for validating that a date range is valid.
 *
 * <p>This annotation is applied at the class level and ensures that the 'from' date is before the
 * 'to' date within the annotated class. The actual validation logic is implemented in the {@code
 * DateRangeValidator} class.
 *
 * <p>The default validation message is "Datum 'from' muss vor 'to' liegen".
 *
 * <p>Attributes: - message: Customizable validation error message. - groups: Allows specification
 * of validation groups, to which this constraint belongs. - payload: Can be used to attach custom
 * payload objects to a constraint.
 */
@Documented
@Constraint(validatedBy = DateRangeValidator.class)
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidDateRange {
  String message() default "Datum 'from' muss vor 'to' liegen";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};
}
