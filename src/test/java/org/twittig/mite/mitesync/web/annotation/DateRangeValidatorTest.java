package org.twittig.mite.mitesync.web.annotation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.twittig.mite.mitesync.web.model.SyncJobModel;

class DateRangeValidatorTest {

  private DateRangeValidator validator;
  private ConstraintValidatorContext ctx;

  @BeforeEach
  void setUp() {
    validator = new DateRangeValidator();
    ctx = mock(ConstraintValidatorContext.class);
  }

  @Test
  void valid_fromBeforeTo() {
    assertThat(validator.isValid(model("01.01.2024", "31.01.2024"), ctx)).isTrue();
  }

  @Test
  void valid_fromEqualsTo() {
    assertThat(validator.isValid(model("15.01.2024", "15.01.2024"), ctx)).isTrue();
  }

  @Test
  void invalid_fromAfterTo() {
    assertThat(validator.isValid(model("31.01.2024", "01.01.2024"), ctx)).isFalse();
  }

  @Test
  void valid_nullFrom_delegatesToNotBlank() {
    assertThat(validator.isValid(model(null, "31.01.2024"), ctx)).isTrue();
  }

  @Test
  void valid_nullTo_delegatesToNotBlank() {
    assertThat(validator.isValid(model("01.01.2024", null), ctx)).isTrue();
  }

  @Test
  void valid_bothNull_delegatesToNotBlank() {
    assertThat(validator.isValid(model(null, null), ctx)).isTrue();
  }

  @Test
  void invalid_unparsableDate() {
    assertThat(validator.isValid(model("not-a-date", "31.01.2024"), ctx)).isFalse();
  }

  @Test
  void valid_yearBoundary() {
    assertThat(validator.isValid(model("31.12.2023", "01.01.2024"), ctx)).isTrue();
  }

  private SyncJobModel model(String from, String to) {
    SyncJobModel m = new SyncJobModel();
    m.setFrom(from);
    m.setTo(to);
    return m;
  }
}
