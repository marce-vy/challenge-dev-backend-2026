package com.tenpo.challenge.api.callhistory;

import static org.mockito.Mockito.verifyNoInteractions;

import com.tenpo.challenge.application.port.in.GetCallHistoryUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.reactive.server.WebTestClient;

@WebFluxTest(CallHistoryController.class)
class CallHistoryPaginationValidationTest {

  @Autowired private WebTestClient webTestClient;

  @MockBean private GetCallHistoryUseCase useCase;

  @Test
  void rejectsNegativePageBeforeUseCaseIsReached() {
    webTestClient
        .get()
        .uri("/api/v1/call-history?page=-1")
        .exchange()
        .expectStatus()
        .isBadRequest()
        .expectBody()
        .jsonPath("$.status")
        .isEqualTo(400)
        .jsonPath("$.error")
        .isEqualTo("Bad Request")
        .jsonPath("$.message")
        .isEqualTo("page must be greater than or equal to 0");

    verifyNoInteractions(useCase);
  }

  @Test
  void rejectsSizeBelowMinimumBeforeUseCaseIsReached() {
    webTestClient
        .get()
        .uri("/api/v1/call-history?size=0")
        .exchange()
        .expectStatus()
        .isBadRequest()
        .expectBody()
        .jsonPath("$.status")
        .isEqualTo(400)
        .jsonPath("$.error")
        .isEqualTo("Bad Request")
        .jsonPath("$.message")
        .isEqualTo("size must be between 1 and 100");

    verifyNoInteractions(useCase);
  }

  @Test
  void rejectsSizeAboveMaximumBeforeUseCaseIsReached() {
    webTestClient
        .get()
        .uri("/api/v1/call-history?size=101")
        .exchange()
        .expectStatus()
        .isBadRequest()
        .expectBody()
        .jsonPath("$.status")
        .isEqualTo(400)
        .jsonPath("$.error")
        .isEqualTo("Bad Request")
        .jsonPath("$.message")
        .isEqualTo("size must be between 1 and 100");

    verifyNoInteractions(useCase);
  }

  @Test
  void rejectsNonNumericPageBeforeUseCaseIsReached() {
    webTestClient
        .get()
        .uri("/api/v1/call-history?page=abc")
        .exchange()
        .expectStatus()
        .isBadRequest()
        .expectBody()
        .jsonPath("$.status")
        .isEqualTo(400)
        .jsonPath("$.error")
        .isEqualTo("Bad Request")
        .jsonPath("$.message")
        .isEqualTo("Request parameter is invalid");

    verifyNoInteractions(useCase);
  }

  @Test
  void rejectsNonNumericSizeBeforeUseCaseIsReached() {
    webTestClient
        .get()
        .uri("/api/v1/call-history?size=abc")
        .exchange()
        .expectStatus()
        .isBadRequest()
        .expectBody()
        .jsonPath("$.status")
        .isEqualTo(400)
        .jsonPath("$.error")
        .isEqualTo("Bad Request")
        .jsonPath("$.message")
        .isEqualTo("Request parameter is invalid");

    verifyNoInteractions(useCase);
  }
}
