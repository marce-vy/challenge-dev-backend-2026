package com.tenpo.challenge.domain;

import java.math.BigDecimal;
import java.util.Objects;

public record CalculationInput(BigDecimal num1, BigDecimal num2) {

  public CalculationInput {
    Objects.requireNonNull(num1, "num1 is required");
    Objects.requireNonNull(num2, "num2 is required");
  }

  public BigDecimal sum() {
    return num1.add(num2);
  }
}
