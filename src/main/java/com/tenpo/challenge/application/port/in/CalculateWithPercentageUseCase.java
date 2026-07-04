package com.tenpo.challenge.application.port.in;

import com.tenpo.challenge.domain.CalculationInput;
import com.tenpo.challenge.domain.CalculationResult;
import reactor.core.publisher.Mono;

public interface CalculateWithPercentageUseCase {

  Mono<CalculationResult> calculate(CalculationInput input);
}
