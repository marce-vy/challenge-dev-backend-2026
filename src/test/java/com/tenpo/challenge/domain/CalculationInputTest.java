package com.tenpo.challenge.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class CalculationInputTest {

  @Test
  void exposesOperandsAndSum() {
    CalculationInput input = new CalculationInput(new BigDecimal("100"), new BigDecimal("50"));

    assertThat(input.num1()).isEqualByComparingTo("100");
    assertThat(input.num2()).isEqualByComparingTo("50");
    assertThat(input.sum()).isEqualByComparingTo("150");
  }

  @Test
  void rejectsNullOperands() {
    assertThatNullPointerException()
        .isThrownBy(() -> new CalculationInput(null, BigDecimal.ONE))
        .withMessage("num1 is required");
    assertThatNullPointerException()
        .isThrownBy(() -> new CalculationInput(BigDecimal.ONE, null))
        .withMessage("num2 is required");
  }
}
