package com.tenpo.challenge.persistence.callhistory;

import static org.assertj.core.api.Assertions.assertThat;

import com.tenpo.challenge.application.callhistory.RecordCallHistoryCommand;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Map;
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
@Import(CallHistoryCommandAdapter.class)
class CallHistoryCommandAdapterTest {

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

  @Autowired private CallHistoryCommandAdapter adapter;

  @Test
  void savesCommandIntoCallHistoryTable() {
    Instant occurredAt = Instant.parse("2026-07-01T22:15:30.123Z");
    RecordCallHistoryCommand command =
        new RecordCallHistoryCommand(
            occurredAt,
            "POST",
            "/api/v1/calculations",
            "a=1",
            "{\"num1\":5}",
            "{\"result\":11}",
            null,
            200,
            true,
            42L,
            "127.0.0.1");

    adapter.save(command);

    Map<String, Object> row =
        jdbcTemplate.queryForMap(
            """
            SELECT id, occurred_at, http_method, endpoint, query_params, request_body,
                   response_body, error_body, http_status, success, duration_ms, client_ip
            FROM call_history
            WHERE endpoint = ?
            """,
            "/api/v1/calculations");

    assertThat(row.get("id")).isNotNull();
    assertThat(((Timestamp) row.get("occurred_at")).toInstant()).isEqualTo(occurredAt);
    assertThat(row.get("http_method")).isEqualTo("POST");
    assertThat(row.get("query_params")).isEqualTo("a=1");
    assertThat(row.get("request_body")).isEqualTo("{\"num1\":5}");
    assertThat(row.get("response_body")).isEqualTo("{\"result\":11}");
    assertThat(row.get("error_body")).isNull();
    assertThat(row.get("http_status")).isEqualTo(200);
    assertThat(row.get("success")).isEqualTo(true);
    assertThat(row.get("duration_ms")).isEqualTo(42L);
    assertThat(row.get("client_ip")).isEqualTo("127.0.0.1");
  }
}
