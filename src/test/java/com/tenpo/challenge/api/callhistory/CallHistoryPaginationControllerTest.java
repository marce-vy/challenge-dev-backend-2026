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
class CallHistoryPaginationControllerTest {

  @Autowired private WebTestClient webTestClient;

  @MockBean private GetCallHistoryUseCase useCase;

  @Test
  void forwardsExplicitPaginationAndRendersMetadata() {
    UUID entryId = UUID.fromString("9a5820f3-2b43-4f3f-b8ef-12a81d65c922");
    CallHistoryEntry entry =
        new CallHistoryEntry(
            entryId,
            Instant.parse("2026-07-01T22:10:30.123Z"),
            "GET",
            "/api/v1/calculations",
            "page=1&size=5",
            null,
            "{\"result\":11}",
            null,
            200,
            true,
            37L,
            "127.0.0.1");
    when(useCase.get(new PaginationRequest(1, 5)))
        .thenReturn(Mono.just(new CallHistoryPage(List.of(entry), 1, 5, 12, 3, true, true)));

    webTestClient
        .get()
        .uri(
            uriBuilder ->
                uriBuilder
                    .path("/api/v1/call-history")
                    .queryParam("page", 1)
                    .queryParam("size", 5)
                    .build())
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("$.content[0].id")
        .isEqualTo(entryId.toString())
        .jsonPath("$.content[0].queryParams")
        .isEqualTo("page=1&size=5")
        .jsonPath("$.page")
        .isEqualTo(1)
        .jsonPath("$.size")
        .isEqualTo(5)
        .jsonPath("$.totalElements")
        .isEqualTo(12)
        .jsonPath("$.totalPages")
        .isEqualTo(3)
        .jsonPath("$.hasNext")
        .isEqualTo(true)
        .jsonPath("$.hasPrevious")
        .isEqualTo(true);

    verify(useCase).get(new PaginationRequest(1, 5));
  }

  @Test
  void rendersEmptyPageWithStableShape() {
    when(useCase.get(new PaginationRequest(2, 5)))
        .thenReturn(Mono.just(new CallHistoryPage(List.of(), 2, 5, 0, 0, false, true)));

    webTestClient
        .get()
        .uri(
            uriBuilder ->
                uriBuilder
                    .path("/api/v1/call-history")
                    .queryParam("page", 2)
                    .queryParam("size", 5)
                    .build())
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("$.content")
        .isArray()
        .jsonPath("$.content")
        .isEmpty()
        .jsonPath("$.page")
        .isEqualTo(2)
        .jsonPath("$.size")
        .isEqualTo(5)
        .jsonPath("$.totalElements")
        .isEqualTo(0)
        .jsonPath("$.totalPages")
        .isEqualTo(0)
        .jsonPath("$.hasNext")
        .isEqualTo(false)
        .jsonPath("$.hasPrevious")
        .isEqualTo(true);

    verify(useCase).get(new PaginationRequest(2, 5));
  }
}
