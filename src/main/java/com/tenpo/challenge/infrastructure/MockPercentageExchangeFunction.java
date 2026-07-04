package com.tenpo.challenge.infrastructure;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import reactor.core.publisher.Mono;

public class MockPercentageExchangeFunction implements ExchangeFunction {

  @Override
  public Mono<ClientResponse> exchange(
      org.springframework.web.reactive.function.client.ClientRequest request) {
    ClientResponse response =
        ClientResponse.create(HttpStatus.OK)
            .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
            .body("{\"percentage\":10}")
            .build();
    return Mono.just(response);
  }
}
