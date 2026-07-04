package com.tenpo.challenge.infrastructure;

import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import reactor.core.publisher.Mono;

public class FailingPercentageExchangeFunction implements ExchangeFunction {

  private static final String RESPONSE_BODY = "{\"error\":\"provider unavailable\"}";

  private final AtomicInteger attempts = new AtomicInteger();

  public int attempts() {
    return attempts.get();
  }

  @Override
  public Mono<ClientResponse> exchange(ClientRequest request) {
    attempts.incrementAndGet();
    ClientResponse response =
        ClientResponse.create(HttpStatus.SERVICE_UNAVAILABLE)
            .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
            .body(RESPONSE_BODY)
            .build();
    return Mono.just(response);
  }
}
