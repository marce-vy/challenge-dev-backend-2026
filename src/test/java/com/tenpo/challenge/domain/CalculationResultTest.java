package com.tenpo.challenge.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class CalculationResultTest {

  @Test
  void exposesCalculationOutputValues() {
    CalculationResult result =
        new CalculationResult(
            new CalculationInput(new BigDecimal("100"), new BigDecimal("50")),
            new BigDecimal("150"),
            new BigDecimal("10"),
            new BigDecimal("165"));

    assertThat(result.num1()).isEqualByComparingTo("100");
    assertThat(result.num2()).isEqualByComparingTo("50");
    assertThat(result.sum()).isEqualByComparingTo("150");
    assertThat(result.percentage()).isEqualByComparingTo("10");
    assertThat(result.result()).isEqualByComparingTo("165");
  }
}
