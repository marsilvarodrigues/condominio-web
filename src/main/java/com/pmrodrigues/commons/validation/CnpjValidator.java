package com.pmrodrigues.commons.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Validates a Brazilian CNPJ using the official check-digit algorithm. Accepts both formatted
 * ({@code XX.XXX.XXX/XXXX-XX}) and unformatted 14-digit strings. {@code null} and blank values are
 * skipped (delegate to {@code @NotBlank}/{@code @NotNull}).
 */
public class CnpjValidator implements ConstraintValidator<Cnpj, String> {

  private static final int[] FIRST_WEIGHTS = {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
  private static final int[] SECOND_WEIGHTS = {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};

  @Override
  public boolean isValid(String value, ConstraintValidatorContext context) {
    if (value == null || value.isBlank()) {
      return true;
    }

    String digits = value.replaceAll("[^\\d]", "");

    if (digits.length() != 14) {
      return false;
    }

    if (digits.chars().distinct().count() == 1) {
      return false;
    }

    return checkDigit(digits, FIRST_WEIGHTS) && checkDigit(digits, SECOND_WEIGHTS);
  }

  private boolean checkDigit(String digits, int[] weights) {
    int sum = 0;
    for (int i = 0; i < weights.length; i++) {
      sum += Character.getNumericValue(digits.charAt(i)) * weights[i];
    }
    int remainder = sum % 11;
    int expected = remainder < 2 ? 0 : 11 - remainder;
    return Character.getNumericValue(digits.charAt(weights.length)) == expected;
  }
}
