package com.tenpo.challenge.application.service;

import com.tenpo.challenge.application.port.in.CalculateWithPercentageUseCase;
import com.tenpo.challenge.application.port.out.PercentageProviderPort;
import com.tenpo.challenge.domain.CalculationInput;
import com.tenpo.challenge.domain.CalculationResult;
import com.tenpo.challenge.domain.PercentageCalculator;
import java.util.Objects;
import reactor.core.publisher.Mono;

public class CalculateWithPercentageService implements CalculateWithPercentageUseCase {

  private final PercentageProviderPort percentageProvider;
  private final PercentageCalculator calculator;

  public CalculateWithPercentageService(
      PercentageProviderPort percentageProvider, PercentageCalculator calculator) {
    this.percentageProvider = Objects.requireNonNull(percentageProvider);
    this.calculator = Objects.requireNonNull(calculator);
  }

  @Override
  public Mono<CalculationResult> calculate(CalculationInput input) {
    Objects.requireNonNull(input, "input is required");

    return percentageProvider
        .getPercentage()
        .map(
            percentage ->
                new CalculationResult(
                    input, input.sum(), percentage, calculator.calculate(input, percentage)));
  }
}
