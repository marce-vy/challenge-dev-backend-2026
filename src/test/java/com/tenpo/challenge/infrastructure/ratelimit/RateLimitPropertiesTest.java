package com.tenpo.challenge.infrastructure.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tenpo.challenge.application.ratelimit.RateLimitPolicy;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.bind.validation.ValidationBindHandler;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

class RateLimitPropertiesTest {

  @Test
  void bindsConfiguredPolicyValues() {
    RateLimitProperties properties =
        Binder.get(
                new MockEnvironment()
                    .withProperty("rate-limit.capacity", "3")
                    .withProperty("rate-limit.refill-tokens", "3")
                    .withProperty("rate-limit.refill-period", "1m"))
            .bindOrCreate("rate-limit", Bindable.of(RateLimitProperties.class));

    assertThat(properties.capacity()).isEqualTo(3);
    assertThat(properties.refillTokens()).isEqualTo(3);
    assertThat(properties.refillPeriod()).isEqualTo(Duration.ofMinutes(1));

    RateLimitPolicy policy = properties.toPolicy();

    assertThat(policy.capacity()).isEqualTo(3);
    assertThat(policy.refillTokens()).isEqualTo(3);
    assertThat(policy.refillPeriod()).isEqualTo(Duration.ofMinutes(1));
  }

  @Test
  void rejectsInvalidValuesDuringBindingValidation() {
    MockEnvironment environment =
        new MockEnvironment()
            .withProperty("rate-limit.capacity", "0")
            .withProperty("rate-limit.refill-tokens", "-1")
            .withProperty("rate-limit.refill-period", "0s");

    assertThatThrownBy(
            () ->
                Binder.get(environment)
                    .bind(
                        "rate-limit",
                        Bindable.of(RateLimitProperties.class),
                        new ValidationBindHandler(validator())))
        .hasMessageContaining("rate-limit");
  }

  private LocalValidatorFactoryBean validator() {
    LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
    validator.afterPropertiesSet();
    return validator;
  }
}
