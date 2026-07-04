package com.tenpo.challenge.api.callhistory;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tenpo.challenge.application.callhistory.CallHistoryEntry;
import com.tenpo.challenge.application.callhistory.CallHistoryPage;
import com.tenpo.challenge.application.callhistory.PaginationRequest;
import com.tenpo.challenge.application.port.in.GetCallHistoryUseCase;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

@WebFluxTest(CallHistoryController.class)
class CallHistoryControllerTest {

  @Autowired private WebTestClient webTestClient;

  @MockBean private GetCallHistoryUseCase useCase;

  @Test
  void returnsDefaultFirstPage() {
    UUID entryId = UUID.fromString("9a5820f3-2b43-4f3f-b8ef-12a81d65c921");
    CallHistoryEntry entry =
        new CallHistoryEntry(
            entryId,
            Instant.parse("2026-07-01T22:15:30.123Z"),
            "POST",
            "/api/v1/calculations",
            null,
            "{\"num1\":5,\"num2\":5}",
            "{\"result\":11}",
            null,
            200,
            true,
            42L,
            "127.0.0.1");
    when(useCase.get(new PaginationRequest(0, 20)))
        .thenReturn(Mono.just(new CallHistoryPage(List.of(entry), 0, 20, 1, 1, false, false)));

    webTestClient
        .get()
        .uri("/api/v1/call-history")
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("$.content[0].id")
        .isEqualTo(entryId.toString())
        .jsonPath("$.content[0].occurredAt")
        .isEqualTo("2026-07-01T22:15:30.123Z")
        .jsonPath("$.content[0].httpMethod")
        .isEqualTo("POST")
        .jsonPath("$.content[0].endpoint")
        .isEqualTo("/api/v1/calculations")
        .jsonPath("$.content[0].queryParams")
        .isEmpty()
        .jsonPath("$.content[0].requestBody")
        .isEqualTo("{\"num1\":5,\"num2\":5}")
        .jsonPath("$.content[0].responseBody")
        .isEqualTo("{\"result\":11}")
        .jsonPath("$.content[0].errorBody")
        .isEmpty()
        .jsonPath("$.content[0].httpStatus")
        .isEqualTo(200)
        .jsonPath("$.content[0].success")
        .isEqualTo(true)
        .jsonPath("$.content[0].durationMs")
        .isEqualTo(42)
        .jsonPath("$.content[0].clientIp")
        .isEqualTo("127.0.0.1")
        .jsonPath("$.page")
        .isEqualTo(0)
        .jsonPath("$.size")
        .isEqualTo(20)
        .jsonPath("$.totalElements")
        .isEqualTo(1)
        .jsonPath("$.totalPages")
        .isEqualTo(1)
        .jsonPath("$.hasNext")
        .isEqualTo(false)
        .jsonPath("$.hasPrevious")
        .isEqualTo(false);

    verify(useCase).get(new PaginationRequest(0, 20));
  }
}
