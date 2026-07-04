package com.tenpo.challenge.infrastructure;

import com.tenpo.challenge.external.percentage.PercentageFetcher;
import com.tenpo.challenge.external.percentage.PercentageResponse;
import io.github.resilience4j.retry.Retry;
import java.util.Objects;
import java.util.function.Supplier;
import org.springframework.web.client.RestClient;

public class RetryingPercentageFetcher implements PercentageFetcher {

  private final Supplier<PercentageResponse> supplier;

  public RetryingPercentageFetcher(RestClient restClient, Retry retry, String percentagePath) {
    Objects.requireNonNull(restClient, "restClient is required");
    Objects.requireNonNull(retry, "retry is required");
    Objects.requireNonNull(percentagePath, "percentagePath is required");
    this.supplier =
        Retry.decorateSupplier(
            retry,
            () -> restClient.get().uri(percentagePath).retrieve().body(PercentageResponse.class));
  }

  @Override
  public PercentageResponse fetch() {
    return supplier.get();
  }
}
