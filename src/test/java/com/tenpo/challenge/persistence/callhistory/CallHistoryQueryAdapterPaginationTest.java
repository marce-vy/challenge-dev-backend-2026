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
class CallHistoryQueryAdapterPaginationTest {

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
  void returnsRequestedPageWithStableMetadata() {
    for (int i = 0; i < 12; i++) {
      insertEntity(i);
    }

    CallHistoryPage page = adapter.findPage(new PaginationRequest(1, 5)).block();

    assertThat(page.content())
        .extracting("endpoint")
        .containsExactly(
            "/api/v1/history/6",
            "/api/v1/history/5",
            "/api/v1/history/4",
            "/api/v1/history/3",
            "/api/v1/history/2");
    assertThat(page.page()).isEqualTo(1);
    assertThat(page.size()).isEqualTo(5);
    assertThat(page.totalElements()).isEqualTo(12);
    assertThat(page.totalPages()).isEqualTo(3);
    assertThat(page.hasNext()).isTrue();
    assertThat(page.hasPrevious()).isTrue();
  }

  @Test
  void returnsEmptyContentForPageBeyondExistingHistory() {
    insertEntity(0);

    CallHistoryPage page = adapter.findPage(new PaginationRequest(2, 5)).block();

    assertThat(page.content()).isEmpty();
    assertThat(page.page()).isEqualTo(2);
    assertThat(page.size()).isEqualTo(5);
    assertThat(page.totalElements()).isEqualTo(1);
    assertThat(page.totalPages()).isEqualTo(1);
    assertThat(page.hasNext()).isFalse();
    assertThat(page.hasPrevious()).isTrue();
  }

  private void insertEntity(int index) {
    CallHistoryEntity entity = new CallHistoryEntity();
    entity.setId(UUID.nameUUIDFromBytes(("history-" + index).getBytes()));
    entity.setOccurredAt(Instant.parse("2026-07-01T10:00:00Z").plusSeconds(index));
    entity.setHttpMethod("GET");
    entity.setEndpoint("/api/v1/history/" + index);
    entity.setQueryParams("page=" + index);
    entity.setRequestBody(null);
    entity.setResponseBody("{}");
    entity.setErrorBody(null);
    entity.setHttpStatus(200);
    entity.setSuccess(true);
    entity.setDurationMs(10L + index);
    entity.setClientIp("127.0.0.1");
    template.insert(entity).block();
  }
}
