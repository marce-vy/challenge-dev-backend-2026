package com.tenpo.challenge.application.port.out;

import reactor.core.publisher.Mono;

public interface RetryStrategy {

  <T> Mono<T> apply(Mono<T> source);
}
