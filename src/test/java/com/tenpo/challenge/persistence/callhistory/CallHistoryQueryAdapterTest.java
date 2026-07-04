package com.tenpo.challenge.persistence.callhistory;

import static org.assertj.core.api.Assertions.assertThat;

import com.tenpo.challenge.application.callhistory.CallHistoryPage;
import com.tenpo.challenge.application.callhistory.PaginationRequest;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@DataR2dbcTest
@ActiveProfiles("test")
@Import(CallHistoryQueryAdapter.class)
class CallHistoryQueryAdapterTest {

  @Container
  private static final PostgreSQLContainer<?> POSTGRES =
      new PostgreSQLContainer<>("postgres:16-alpine").withInitScript("schema.sql");

  @DynamicPropertySource
  static void r2dbcProperties(DynamicPropertyRegistry registry) {
    registry.add(
        "spring.r2dbc.url",
        () ->
            "r2dbc:postgresql://"
                + POSTGRES.getHost()
                + ":"
                + POSTGRES.getMappedPort(5432)
                + "/"
                + POSTGRES.getDatabaseName());
    registry.add("spring.r2dbc.username", POSTGRES::getUsername);
    registry.add("spring.r2dbc.password", POSTGRES::getPassword);
  }

  @Autowired private R2dbcEntityTemplate template;

  @Autowired private CallHistoryQueryAdapter adapter;

  @BeforeEach
  void setUp() {
    template.delete(CallHistoryEntity.class).all().block();
  }

  @Test
  void returnsEmptyPageWhenNoHistoryExists() {
    CallHistoryPage page = adapter.findPage(new PaginationRequest(0, 20)).block();

    assertThat(page.content()).isEmpty();
    assertThat(page.page()).isZero();
    assertThat(page.size()).isEqualTo(20);
    assertThat(page.totalElements()).isZero();
    assertThat(page.totalPages()).isZero();
    assertThat(page.hasNext()).isFalse();
    assertThat(page.hasPrevious()).isFalse();
  }

  @Test
  void returnsFirstPageNewestFirstWithStableMetadata() {
    UUID oldest = UUID.fromString("00000000-0000-0000-0000-000000000001");
    UUID newestTieLoser = UUID.fromString("00000000-0000-0000-0000-000000000002");
    UUID newestTieWinner = UUID.fromString("00000000-0000-0000-0000-000000000003");
    insertEntity(oldest, Instant.parse("2026-07-01T10:00:00Z"), "GET", "/oldest");
    insertEntity(newestTieLoser, Instant.parse("2026-07-01T11:00:00Z"), "POST", "/newest-a");
    insertEntity(newestTieWinner, Instant.parse("2026-07-01T11:00:00Z"), "POST", "/newest-b");

    CallHistoryPage page = adapter.findPage(new PaginationRequest(0, 2)).block();

    assertThat(page.content()).extracting("id").containsExactly(newestTieWinner, newestTieLoser);
    assertThat(page.page()).isZero();
    assertThat(page.size()).isEqualTo(2);
    assertThat(page.totalElements()).isEqualTo(3);
    assertThat(page.totalPages()).isEqualTo(2);
    assertThat(page.hasNext()).isTrue();
    assertThat(page.hasPrevious()).isFalse();
  }

  private void insertEntity(UUID id, Instant occurredAt, String method, String endpoint) {
    CallHistoryEntity entity = new CallHistoryEntity();
    entity.setId(id);
    entity.setOccurredAt(occurredAt);
    entity.setHttpMethod(method);
    entity.setEndpoint(endpoint);
    entity.setQueryParams(null);
    entity.setRequestBody(null);
    entity.setResponseBody("{}");
    entity.setErrorBody(null);
    entity.setHttpStatus(200);
    entity.setSuccess(true);
    entity.setDurationMs(10L);
    entity.setClientIp("127.0.0.1");
    template.insert(entity).block();
  }
}
