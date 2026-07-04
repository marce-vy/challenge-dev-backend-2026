package com.tenpo.challenge.api.calculation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class CalculationE2ETest {

  @Container
  private static final PostgreSQLContainer<?> POSTGRES =
      new PostgreSQLContainer<>("postgres:16-alpine");

  @DynamicPropertySource
  static void postgresProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    registry.add("spring.datasource.username", POSTGRES::getUsername);
    registry.add("spring.datasource.password", POSTGRES::getPassword);
  }

  @Autowired private MockMvc mockMvc;
  @Autowired private JdbcTemplate jdbcTemplate;

  @BeforeEach
  void cleanCallHistory() {
    jdbcTemplate.update("DELETE FROM call_history");
  }

  @Test
  void calculatesAndRecordsTheFunctionalCallInHistory() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/calculations")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"num1\":100,\"num2\":50}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.num1").value(100))
        .andExpect(jsonPath("$.num2").value(50))
        .andExpect(jsonPath("$.sum").value(150))
        .andExpect(jsonPath("$.percentage").value(10))
        .andExpect(jsonPath("$.result").value(165));

    Map<String, Object> record = awaitHistoryRecord("/api/v1/calculations");

    assertThat(record.get("http_method")).isEqualTo("POST");
    assertThat(record.get("endpoint")).isEqualTo("/api/v1/calculations");
    assertThat(record.get("query_params")).isNull();
    assertThat(record.get("request_body")).isEqualTo("{\"num1\":100,\"num2\":50}");
    assertThat(record.get("response_body"))
        .isEqualTo("{\"num1\":100,\"num2\":50,\"sum\":150,\"percentage\":10,\"result\":165}");
    assertThat(record.get("error_body")).isNull();
    assertThat(record.get("http_status")).isEqualTo(200);
    assertThat(record.get("success")).isEqualTo(true);
    assertThat(record.get("duration_ms")).isNotNull();
    assertThat(record.get("client_ip")).isNotNull();
  }

  @Test
  void calculatesAndExposesTheFunctionalCallInHistoryEndpoint() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/calculations")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"num1\":100,\"num2\":50}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.result").value(165));

    awaitHistoryRecord("/api/v1/calculations");

    mockMvc
        .perform(get("/api/v1/call-history"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].httpMethod").value("POST"))
        .andExpect(jsonPath("$.content[0].endpoint").value("/api/v1/calculations"))
        .andExpect(jsonPath("$.content[0].queryParams").value(nullValue()))
        .andExpect(jsonPath("$.content[0].requestBody").value("{\"num1\":100,\"num2\":50}"))
        .andExpect(
            jsonPath("$.content[0].responseBody")
                .value("{\"num1\":100,\"num2\":50,\"sum\":150,\"percentage\":10,\"result\":165}"))
        .andExpect(jsonPath("$.content[0].errorBody").value(nullValue()))
        .andExpect(jsonPath("$.content[0].httpStatus").value(200))
        .andExpect(jsonPath("$.content[0].success").value(true))
        .andExpect(jsonPath("$.content[0].durationMs").isNumber())
        .andExpect(jsonPath("$.content[0].clientIp").isNotEmpty())
        .andExpect(jsonPath("$.page").value(0))
        .andExpect(jsonPath("$.size").value(20))
        .andExpect(jsonPath("$.totalElements").value(1))
        .andExpect(jsonPath("$.totalPages").value(1))
        .andExpect(jsonPath("$.hasNext").value(false))
        .andExpect(jsonPath("$.hasPrevious").value(false));
  }

  private Map<String, Object> awaitHistoryRecord(String endpoint) throws InterruptedException {
    long deadline = System.nanoTime() + Duration.ofSeconds(2).toNanos();
    while (System.nanoTime() < deadline) {
      Long count =
          jdbcTemplate.queryForObject(
              "SELECT COUNT(*) FROM call_history WHERE endpoint = ?", Long.class, endpoint);
      if (count != null && count > 0) {
        return jdbcTemplate.queryForMap(
            """
            SELECT http_method, endpoint, query_params, request_body, response_body,
                   error_body, http_status, success, duration_ms, client_ip
            FROM call_history
            WHERE endpoint = ?
            ORDER BY occurred_at DESC, id DESC
            LIMIT 1
            """,
            endpoint);
      }
      Thread.sleep(50L);
    }
    throw new IllegalStateException("call history record was not written within 2 seconds");
  }
}
