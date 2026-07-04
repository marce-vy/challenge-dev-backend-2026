package com.tenpo.challenge.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.tenpo.challenge.application.port.out.PercentageProviderPort;
import com.tenpo.challenge.domain.CalculationInput;
import com.tenpo.challenge.domain.CalculationResult;
import com.tenpo.challenge.domain.PercentageCalculator;
import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class CalculateWithPercentageServiceTest {

  @Test
  void obtainsPercentageFromProviderAndCalculatesResult() {
    AtomicInteger providerCalls = new AtomicInteger();
    PercentageProviderPort percentageProvider =
        () -> {
          providerCalls.incrementAndGet();
          return new BigDecimal("10");
        };
    CalculateWithPercentageService useCase =
        new CalculateWithPercentageService(percentageProvider, new PercentageCalculator());

    CalculationResult result =
        useCase.calculate(new CalculationInput(new BigDecimal("100"), new BigDecimal("50")));

    assertThat(providerCalls).hasValue(1);
    assertThat(result.num1()).isEqualByComparingTo("100");
    assertThat(result.num2()).isEqualByComparingTo("50");
    assertThat(result.sum()).isEqualByComparingTo("150");
    assertThat(result.percentage()).isEqualByComparingTo("10");
    assertThat(result.result()).isEqualByComparingTo("165");
  }
}
