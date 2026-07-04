package com.tenpo.challenge.application.service;

import com.tenpo.challenge.application.port.in.CalculateWithPercentageUseCase;
import com.tenpo.challenge.application.port.out.PercentageProviderPort;
import com.tenpo.challenge.domain.CalculationInput;
import com.tenpo.challenge.domain.CalculationResult;
import com.tenpo.challenge.domain.PercentageCalculator;
import java.math.BigDecimal;
import java.util.Objects;

public class CalculateWithPercentageService implements CalculateWithPercentageUseCase {

  private final PercentageProviderPort percentageProvider;
  private final PercentageCalculator calculator;

  public CalculateWithPercentageService(
      PercentageProviderPort percentageProvider, PercentageCalculator calculator) {
    this.percentageProvider = Objects.requireNonNull(percentageProvider);
    this.calculator = Objects.requireNonNull(calculator);
  }

  @Override
  public CalculationResult calculate(CalculationInput input) {
    Objects.requireNonNull(input, "input is required");

    BigDecimal percentage = percentageProvider.getPercentage();
    BigDecimal result = calculator.calculate(input, percentage);

    return new CalculationResult(input, input.sum(), percentage, result);
  }
}
