package com.tenpo.challenge.api.callhistory;

import static org.assertj.core.api.Assertions.assertThat;

import com.tenpo.challenge.persistence.callhistory.CallHistoryEntity;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class CallHistoryE2ETest {

  @Container
  private static final PostgreSQLContainer<?> POSTGRES =
      new PostgreSQLContainer<>("postgres:16-alpine").withInitScript("schema.sql");

  @DynamicPropertySource
  static void databaseProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    registry.add("spring.datasource.username", POSTGRES::getUsername);
    registry.add("spring.datasource.password", POSTGRES::getPassword);
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

  @Autowired private WebTestClient webTestClient;
  @Autowired private R2dbcEntityTemplate template;

  @BeforeEach
  void setUp() {
    webTestClient = webTestClient.mutate().build();
    template.delete(CallHistoryEntity.class).all().block();
  }

  @Test
  void returnsPersistedCallHistoryFromTheDatabase() {
    UUID id = UUID.fromString("11111111-1111-1111-1111-111111111111");
    CallHistoryEntity entity = new CallHistoryEntity();
    entity.setId(id);
    entity.setOccurredAt(Instant.parse("2026-07-01T22:15:30.123Z"));
    entity.setHttpMethod("POST");
    entity.setEndpoint("/api/v1/calculations");
    entity.setQueryParams(null);
    entity.setRequestBody("{\"num1\":100,\"num2\":50}");
    entity.setResponseBody(
        "{\"num1\":100,\"num2\":50,\"sum\":150,\"percentage\":10,\"result\":165}");
    entity.setErrorBody(null);
    entity.setHttpStatus(200);
    entity.setSuccess(true);
    entity.setDurationMs(42L);
    entity.setClientIp("127.0.0.1");
    template.insert(entity).block();

    webTestClient
        .get()
        .uri("/api/v1/call-history")
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
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
        .isEqualTo(false)
        .jsonPath("$.content[0].id")
        .isEqualTo(id.toString())
        .jsonPath("$.content[0].httpMethod")
        .isEqualTo("POST")
        .jsonPath("$.content[0].endpoint")
        .isEqualTo("/api/v1/calculations")
        .jsonPath("$.content[0].requestBody")
        .isEqualTo("{\"num1\":100,\"num2\":50}")
        .jsonPath("$.content[0].responseBody")
        .isEqualTo("{\"num1\":100,\"num2\":50,\"sum\":150,\"percentage\":10,\"result\":165}")
        .jsonPath("$.content[0].httpStatus")
        .isEqualTo(200)
        .jsonPath("$.content[0].success")
        .isEqualTo(true)
        .jsonPath("$.content[0].durationMs")
        .isEqualTo(42)
        .jsonPath("$.content[0].clientIp")
        .isEqualTo("127.0.0.1");
  }
}
