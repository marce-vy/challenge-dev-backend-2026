package com.tenpo.challenge.external.percentage;

import reactor.core.publisher.Mono;

@FunctionalInterface
public interface PercentageFetcher {

  Mono<PercentageResponse> fetch();
}
