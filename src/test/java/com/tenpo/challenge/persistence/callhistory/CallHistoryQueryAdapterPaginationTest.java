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
class CallHistoryQueryAdapterPaginationTest {

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
  void returnsRequestedPageWithStableMetadata() {
    for (int i = 0; i < 12; i++) {
      insertHistory(i);
    }

    CallHistoryPage page = adapter.findPage(new PaginationRequest(1, 5));

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
    insertHistory(0);

    CallHistoryPage page = adapter.findPage(new PaginationRequest(2, 5));

    assertThat(page.content()).isEmpty();
    assertThat(page.page()).isEqualTo(2);
    assertThat(page.size()).isEqualTo(5);
    assertThat(page.totalElements()).isEqualTo(1);
    assertThat(page.totalPages()).isEqualTo(1);
    assertThat(page.hasNext()).isFalse();
    assertThat(page.hasPrevious()).isTrue();
  }

  private void insertHistory(int index) {
    jdbcTemplate.update(
        """
        INSERT INTO call_history (
          id, occurred_at, http_method, endpoint, query_params, request_body, response_body,
          error_body, http_status, success, duration_ms, client_ip
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """,
        UUID.nameUUIDFromBytes(("history-" + index).getBytes()),
        Timestamp.from(Instant.parse("2026-07-01T10:00:00Z").plusSeconds(index)),
        "GET",
        "/api/v1/history/" + index,
        "page=" + index,
        null,
        "{}",
        null,
        200,
        true,
        10L + index,
        "127.0.0.1");
  }
}
