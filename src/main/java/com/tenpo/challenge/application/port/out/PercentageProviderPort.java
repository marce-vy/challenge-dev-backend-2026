package com.tenpo.challenge.application.port.out;

import java.math.BigDecimal;
import reactor.core.publisher.Mono;

public interface PercentageProviderPort {

  Mono<BigDecimal> getPercentage();
}
