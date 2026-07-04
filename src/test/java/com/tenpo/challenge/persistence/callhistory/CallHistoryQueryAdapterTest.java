package com.tenpo.challenge.persistence.callhistory;

import static org.assertj.core.api.Assertions.assertThat;

import com.tenpo.challenge.application.callhistory.CallHistoryPage;
import com.tenpo.challenge.application.callhistory.PaginationRequest;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(CallHistoryQueryAdapter.class)
class CallHistoryQueryAdapterTest {

  @Container
  private static final PostgreSQLContainer<?> POSTGRES =
      new PostgreSQLContainer<>("postgres:16-alpine");

  @DynamicPropertySource
  static void postgresProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    registry.add("spring.datasource.username", POSTGRES::getUsername);
    registry.add("spring.datasource.password", POSTGRES::getPassword);
  }

  @Autowired private JdbcTemplate jdbcTemplate;

  @Autowired private CallHistoryQueryAdapter adapter;

  @Test
  void returnsEmptyPageWhenNoHistoryExists() {
    CallHistoryPage page = adapter.findPage(new PaginationRequest(0, 20));

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
    insertHistory(oldest, Instant.parse("2026-07-01T10:00:00Z"), "GET", "/oldest");
    insertHistory(newestTieLoser, Instant.parse("2026-07-01T11:00:00Z"), "POST", "/newest-a");
    insertHistory(newestTieWinner, Instant.parse("2026-07-01T11:00:00Z"), "POST", "/newest-b");

    CallHistoryPage page = adapter.findPage(new PaginationRequest(0, 2));

    assertThat(page.content()).extracting("id").containsExactly(newestTieWinner, newestTieLoser);
    assertThat(page.page()).isZero();
    assertThat(page.size()).isEqualTo(2);
    assertThat(page.totalElements()).isEqualTo(3);
    assertThat(page.totalPages()).isEqualTo(2);
    assertThat(page.hasNext()).isTrue();
    assertThat(page.hasPrevious()).isFalse();
  }

  private void insertHistory(UUID id, Instant occurredAt, String method, String endpoint) {
    jdbcTemplate.update(
        """
        INSERT INTO call_history (
          id, occurred_at, http_method, endpoint, query_params, request_body, response_body,
          error_body, http_status, success, duration_ms, client_ip
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """,
        id,
        Timestamp.from(occurredAt),
        method,
        endpoint,
        null,
        null,
        "{}",
        null,
        200,
        true,
        10L,
        "127.0.0.1");
  }
}
