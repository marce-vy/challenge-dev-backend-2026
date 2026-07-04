package com.tenpo.challenge.api.calculation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tenpo.challenge.application.PercentageProviderUnavailableException;
import com.tenpo.challenge.application.port.in.CalculateWithPercentageUseCase;
import com.tenpo.challenge.domain.CalculationInput;
import com.tenpo.challenge.domain.CalculationResult;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

@WebFluxTest(CalculationController.class)
class CalculationControllerTest {

  @Autowired private WebTestClient webTestClient;

  @MockBean private CalculateWithPercentageUseCase useCase;

  @Test
  void calculatesWithPercentage() {
    CalculationInput input = new CalculationInput(new BigDecimal("100"), new BigDecimal("50"));
    CalculationResult result =
        new CalculationResult(
            input, new BigDecimal("150"), new BigDecimal("10"), new BigDecimal("165"));
    when(useCase.calculate(any(CalculationInput.class))).thenReturn(Mono.just(result));

    webTestClient
        .post()
        .uri("/api/v1/calculations")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue("{\"num1\":100,\"num2\":50}")
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("$.num1")
        .isEqualTo(100)
        .jsonPath("$.num2")
        .isEqualTo(50)
        .jsonPath("$.sum")
        .isEqualTo(150)
        .jsonPath("$.percentage")
        .isEqualTo(10)
        .jsonPath("$.result")
        .isEqualTo(165);

    verify(useCase).calculate(new CalculationInput(new BigDecimal("100"), new BigDecimal("50")));
  }

  @Test
  void rejectsMissingNum1() {
    webTestClient
        .post()
        .uri("/api/v1/calculations")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue("{\"num2\":50}")
        .exchange()
        .expectStatus()
        .isBadRequest()
        .expectBody()
        .jsonPath("$.status")
        .isEqualTo(400)
        .jsonPath("$.error")
        .isEqualTo("Bad Request")
        .jsonPath("$.message")
        .isEqualTo("num1 is required");
  }

  @Test
  void rejectsMissingNum2() {
    webTestClient
        .post()
        .uri("/api/v1/calculations")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue("{\"num1\":100}")
        .exchange()
        .expectStatus()
        .isBadRequest()
        .expectBody()
        .jsonPath("$.status")
        .isEqualTo(400)
        .jsonPath("$.error")
        .isEqualTo("Bad Request")
        .jsonPath("$.message")
        .isEqualTo("num2 is required");
  }

  @Test
  void rejectsNullNum1() {
    webTestClient
        .post()
        .uri("/api/v1/calculations")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue("{\"num1\":null,\"num2\":50}")
        .exchange()
        .expectStatus()
        .isBadRequest()
        .expectBody()
        .jsonPath("$.status")
        .isEqualTo(400)
        .jsonPath("$.error")
        .isEqualTo("Bad Request")
        .jsonPath("$.message")
        .isEqualTo("num1 is required");
  }

  @Test
  void rejectsNullNum2() {
    webTestClient
        .post()
        .uri("/api/v1/calculations")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue("{\"num1\":100,\"num2\":null}")
        .exchange()
        .expectStatus()
        .isBadRequest()
        .expectBody()
        .jsonPath("$.status")
        .isEqualTo(400)
        .jsonPath("$.error")
        .isEqualTo("Bad Request")
        .jsonPath("$.message")
        .isEqualTo("num2 is required");
  }

  @Test
  void rejectsNonNumericNum1() {
    webTestClient
        .post()
        .uri("/api/v1/calculations")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue("{\"num1\":\"abc\",\"num2\":50}")
        .exchange()
        .expectStatus()
        .isBadRequest()
        .expectBody()
        .jsonPath("$.status")
        .isEqualTo(400)
        .jsonPath("$.error")
        .isEqualTo("Bad Request")
        .jsonPath("$.message")
        .isEqualTo("Request body is invalid");
  }

  @Test
  void rejectsZeroNum1() {
    webTestClient
        .post()
        .uri("/api/v1/calculations")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue("{\"num1\":0,\"num2\":50}")
        .exchange()
        .expectStatus()
        .isBadRequest()
        .expectBody()
        .jsonPath("$.status")
        .isEqualTo(400)
        .jsonPath("$.error")
        .isEqualTo("Bad Request")
        .jsonPath("$.message")
        .isEqualTo("num1 must be greater than zero");
  }

  @Test
  void rejectsNegativeNum2() {
    webTestClient
        .post()
        .uri("/api/v1/calculations")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue("{\"num1\":100,\"num2\":-1}")
        .exchange()
        .expectStatus()
        .isBadRequest()
        .expectBody()
        .jsonPath("$.status")
        .isEqualTo(400)
        .jsonPath("$.error")
        .isEqualTo("Bad Request")
        .jsonPath("$.message")
        .isEqualTo("num2 must be greater than zero");
  }

  @Test
  void rejectsMalformedJson() {
    webTestClient
        .post()
        .uri("/api/v1/calculations")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue("{\"num1\":100,\"num2\":")
        .exchange()
        .expectStatus()
        .isBadRequest()
        .expectBody()
        .jsonPath("$.status")
        .isEqualTo(400)
        .jsonPath("$.error")
        .isEqualTo("Bad Request")
        .jsonPath("$.message")
        .isEqualTo("Request body is invalid");
  }

  @Test
  void rejectsEmptyBody() {
    webTestClient
        .post()
        .uri("/api/v1/calculations")
        .contentType(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus()
        .isBadRequest()
        .expectBody()
        .jsonPath("$.status")
        .isEqualTo(400)
        .jsonPath("$.error")
        .isEqualTo("Bad Request")
        .jsonPath("$.message")
        .isEqualTo("Request body is invalid");
  }

  @Test
  void returnsServiceUnavailableWhenProviderIsExhausted() {
    when(useCase.calculate(any(CalculationInput.class)))
        .thenReturn(
            Mono.error(
                new PercentageProviderUnavailableException(
                    "Percentage provider is unavailable", null)));

    webTestClient
        .post()
        .uri("/api/v1/calculations")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue("{\"num1\":100,\"num2\":50}")
        .exchange()
        .expectStatus()
        .is5xxServerError()
        .expectBody()
        .jsonPath("$.status")
        .isEqualTo(503)
        .jsonPath("$.error")
        .isEqualTo("Service Unavailable")
        .jsonPath("$.message")
        .isEqualTo("Percentage provider is unavailable");
  }

  @Test
  void returnsInternalServerErrorForUnexpectedFailures() {
    when(useCase.calculate(any(CalculationInput.class)))
        .thenReturn(Mono.error(new IllegalStateException("database password leaked")));

    webTestClient
        .post()
        .uri("/api/v1/calculations")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue("{\"num1\":100,\"num2\":50}")
        .exchange()
        .expectStatus()
        .is5xxServerError()
        .expectBody()
        .jsonPath("$.status")
        .isEqualTo(500)
        .jsonPath("$.error")
        .isEqualTo("Internal Server Error")
        .jsonPath("$.message")
        .isEqualTo("Unexpected server error")
        .jsonPath("$.trace")
        .doesNotExist()
        .jsonPath("$.exception")
        .doesNotExist()
        .jsonPath("$.errors")
        .doesNotExist()
        .consumeWith(
            result -> {
              String body = new String(result.getResponseBody());
              assertThat(body).doesNotContain("database password leaked");
            });
  }
}
