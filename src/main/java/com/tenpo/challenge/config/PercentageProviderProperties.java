package com.tenpo.challenge.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "percentage-provider")
public record PercentageProviderProperties(Http http, RetryProperties retry) {

  public record Http(String baseUrl, String path) {}
}
