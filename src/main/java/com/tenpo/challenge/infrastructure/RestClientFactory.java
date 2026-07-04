package com.tenpo.challenge.infrastructure;

import org.springframework.http.client.ClientHttpRequestFactory;

public final class RestClientFactory {

  private RestClientFactory() {}

  public static org.springframework.web.client.RestClient springRestClient(
      String baseUrl, ClientHttpRequestFactory requestFactory) {
    return org.springframework.web.client.RestClient.builder()
        .baseUrl(baseUrl)
        .requestFactory(requestFactory)
        .build();
  }
}
