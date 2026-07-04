package com.tenpo.challenge.application.port.in;

import com.tenpo.challenge.domain.CalculationInput;
import com.tenpo.challenge.domain.CalculationResult;

public interface CalculateWithPercentageUseCase {

  CalculationResult calculate(CalculationInput input);
}
