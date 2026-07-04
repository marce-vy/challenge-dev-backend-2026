package com.tenpo.challenge.domain;

import java.math.BigDecimal;
import java.util.Objects;

public record CalculationResult(
    CalculationInput input, BigDecimal sum, BigDecimal percentage, BigDecimal result) {

  public CalculationResult {
    Objects.requireNonNull(input, "input is required");
    Objects.requireNonNull(sum, "sum is required");
    Objects.requireNonNull(percentage, "percentage is required");
    Objects.requireNonNull(result, "result is required");
  }

  public BigDecimal num1() {
    return input.num1();
  }

  public BigDecimal num2() {
    return input.num2();
  }
}
