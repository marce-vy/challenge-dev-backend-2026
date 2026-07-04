package com.tenpo.challenge.external.percentage;

import com.tenpo.challenge.application.exception.PercentageProviderUnavailableException;
import com.tenpo.challenge.application.port.out.PercentageProviderPort;
import java.math.BigDecimal;
import java.util.Objects;

public class ExternalPercentageProvider implements PercentageProviderPort {

  private static final String UNAVAILABLE_MESSAGE = "Percentage provider is unavailable";

  private final PercentageFetcher fetcher;

  public ExternalPercentageProvider(PercentageFetcher fetcher) {
    this.fetcher = Objects.requireNonNull(fetcher, "fetcher is required");
  }

  @Override
  public BigDecimal getPercentage() {
    try {
      PercentageResponse response = fetcher.fetch();
      if (response == null || response.percentage() == null) {
        throw new IllegalStateException("Percentage provider returned an empty response");
      }
      return response.percentage();
    } catch (RuntimeException exception) {
      throw new PercentageProviderUnavailableException(UNAVAILABLE_MESSAGE, exception);
    }
  }
}
