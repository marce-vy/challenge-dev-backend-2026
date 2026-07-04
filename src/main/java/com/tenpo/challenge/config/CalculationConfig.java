package com.tenpo.challenge.config;

import com.tenpo.challenge.application.port.in.CalculateWithPercentageUseCase;
import com.tenpo.challenge.application.port.out.PercentageProviderPort;
import com.tenpo.challenge.application.service.CalculateWithPercentageService;
import com.tenpo.challenge.domain.PercentageCalculator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CalculationConfig {

  @Bean
  public PercentageCalculator percentageCalculator() {
    return new PercentageCalculator();
  }

  @Bean
  public CalculateWithPercentageUseCase calculateWithPercentageUseCase(
      PercentageProviderPort percentageProvider, PercentageCalculator percentageCalculator) {
    return new CalculateWithPercentageService(percentageProvider, percentageCalculator);
  }
}
