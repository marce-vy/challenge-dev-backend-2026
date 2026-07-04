package com.tenpo.challenge.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class PercentageCalculatorTest {

  private final PercentageCalculator calculator = new PercentageCalculator();

  @Test
  void calculatesIntegerLikeDecimals() {
    CalculationInput input = new CalculationInput(new BigDecimal("100"), new BigDecimal("50"));

    BigDecimal result = calculator.calculate(input, new BigDecimal("10"));

    assertThat(result).isEqualByComparingTo("165");
  }

  @Test
  void calculatesDecimalInputsUsingBigDecimalPrecision() {
    CalculationInput input = new CalculationInput(new BigDecimal("10.50"), new BigDecimal("2.25"));

    BigDecimal result = calculator.calculate(input, new BigDecimal("10"));

    assertThat(input.sum()).isEqualByComparingTo("12.75");
    assertThat(result).isEqualByComparingTo("14.025");
  }
}
