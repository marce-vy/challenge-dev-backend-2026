package com.tenpo.challenge.api.callhistory;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class CallHistoryE2ETest {

  @Container
  private static final PostgreSQLContainer<?> POSTGRES =
      new PostgreSQLContainer<>("postgres:16-alpine");

  @DynamicPropertySource
  static void postgresProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    registry.add("spring.datasource.username", POSTGRES::getUsername);
    registry.add("spring.datasource.password", POSTGRES::getPassword);
  }

  @Autowired private TestRestTemplate restTemplate;
  @Autowired private JdbcTemplate jdbcTemplate;
  @Autowired private ObjectMapper objectMapper;

  @BeforeEach
  void cleanCallHistory() {
    jdbcTemplate.update("DELETE FROM call_history");
  }

  @Test
  void returnsPersistedCallHistoryFromTheDatabase() throws Exception {
    UUID id = UUID.fromString("11111111-1111-1111-1111-111111111111");
    jdbcTemplate.update(
        """
        INSERT INTO call_history (
          id, occurred_at, http_method, endpoint, query_params, request_body, response_body,
          error_body, http_status, success, duration_ms, client_ip
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """,
        id,
        Timestamp.from(Instant.parse("2026-07-01T22:15:30.123Z")),
        "POST",
        "/api/v1/calculations",
        null,
        "{\"num1\":100,\"num2\":50}",
        "{\"num1\":100,\"num2\":50,\"sum\":150,\"percentage\":10,\"result\":165}",
        null,
        200,
        true,
        42L,
        "127.0.0.1");

    ResponseEntity<String> response =
        restTemplate.getForEntity("/api/v1/call-history", String.class);
    String body = response.getBody();

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(body).isNotNull();

    JsonNode root = objectMapper.readTree(body);
    assertThat(root.path("page").asInt()).isEqualTo(0);
    assertThat(root.path("size").asInt()).isEqualTo(20);
    assertThat(root.path("totalElements").asLong()).isEqualTo(1L);
    assertThat(root.path("totalPages").asInt()).isEqualTo(1);
    assertThat(root.path("hasNext").asBoolean()).isFalse();
    assertThat(root.path("hasPrevious").asBoolean()).isFalse();
    assertThat(root.path("content").isArray()).isTrue();
    assertThat(root.path("content").size()).isEqualTo(1);
    assertThat(root.path("content").get(0).path("id").asText()).isEqualTo(id.toString());
    assertThat(root.path("content").get(0).path("httpMethod").asText()).isEqualTo("POST");
    assertThat(root.path("content").get(0).path("endpoint").asText())
        .isEqualTo("/api/v1/calculations");
    assertThat(root.path("content").get(0).path("requestBody").asText())
        .isEqualTo("{\"num1\":100,\"num2\":50}");
    assertThat(root.path("content").get(0).path("responseBody").asText())
        .isEqualTo("{\"num1\":100,\"num2\":50,\"sum\":150,\"percentage\":10,\"result\":165}");
    assertThat(root.path("content").get(0).path("httpStatus").asInt()).isEqualTo(200);
    assertThat(root.path("content").get(0).path("success").asBoolean()).isTrue();
    assertThat(root.path("content").get(0).path("durationMs").asLong()).isEqualTo(42L);
    assertThat(root.path("content").get(0).path("clientIp").asText()).isEqualTo("127.0.0.1");
  }
}
