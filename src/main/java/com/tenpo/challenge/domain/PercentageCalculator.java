package com.tenpo.challenge.domain;

import java.math.BigDecimal;
import java.util.Objects;

public class PercentageCalculator {

  private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

  public BigDecimal calculate(CalculationInput input, BigDecimal percentage) {
    Objects.requireNonNull(input, "input is required");
    Objects.requireNonNull(percentage, "percentage is required");

    BigDecimal sum = input.sum();
    return sum.add(sum.multiply(percentage).divide(ONE_HUNDRED));
  }
}
