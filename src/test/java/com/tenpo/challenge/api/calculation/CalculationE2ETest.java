package com.tenpo.challenge.api.calculation;

import static org.assertj.core.api.Assertions.assertThat;

import com.tenpo.challenge.persistence.callhistory.CallHistoryEntity;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.data.relational.core.query.Query;
import org.springframework.http.MediaType;
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
class CalculationE2ETest {

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
  void calculatesAndRecordsTheFunctionalCallInHistory() {
    webTestClient
        .post()
        .uri("/api/v1/calculations")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue("{\"num1\":100,\"num2\":50}")
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("$.num1")
        .isEqualTo(100)
        .jsonPath("$.num2")
        .isEqualTo(50)
        .jsonPath("$.sum")
        .isEqualTo(150)
        .jsonPath("$.percentage")
        .isEqualTo(10)
        .jsonPath("$.result")
        .isEqualTo(165);

    CallHistoryEntity record = awaitHistoryRecord("/api/v1/calculations");

    assertThat(record.httpMethod()).isEqualTo("POST");
    assertThat(record.endpoint()).isEqualTo("/api/v1/calculations");
    assertThat(record.queryParams()).isNull();
    assertThat(record.requestBody()).isEqualTo("{\"num1\":100,\"num2\":50}");
    assertThat(record.responseBody())
        .isEqualTo("{\"num1\":100,\"num2\":50,\"sum\":150,\"percentage\":10,\"result\":165}");
    assertThat(record.errorBody()).isNull();
    assertThat(record.httpStatus()).isEqualTo(200);
    assertThat(record.success()).isTrue();
    assertThat(record.durationMs()).isNotNull();
    assertThat(record.clientIp()).isNotNull();
  }

  @Test
  void calculatesAndExposesTheFunctionalCallInHistoryEndpoint() {
    webTestClient
        .post()
        .uri("/api/v1/calculations")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue("{\"num1\":100,\"num2\":50}")
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("$.result")
        .isEqualTo(165);

    awaitHistoryRecord("/api/v1/calculations");

    webTestClient
        .get()
        .uri("/api/v1/call-history")
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("$.content[0].httpMethod")
        .isEqualTo("POST")
        .jsonPath("$.content[0].endpoint")
        .isEqualTo("/api/v1/calculations")
        .jsonPath("$.content[0].queryParams").doesNotExist()
        .jsonPath("$.content[0].requestBody")
        .isEqualTo("{\"num1\":100,\"num2\":50}")
        .jsonPath("$.content[0].responseBody")
        .isEqualTo("{\"num1\":100,\"num2\":50,\"sum\":150,\"percentage\":10,\"result\":165}")
        .jsonPath("$.content[0].errorBody").doesNotExist()
        .jsonPath("$.content[0].httpStatus")
        .isEqualTo(200)
        .jsonPath("$.content[0].success")
        .isEqualTo(true)
        .jsonPath("$.content[0].durationMs").isNumber()
        .jsonPath("$.content[0].clientIp").isNotEmpty()
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
  }

  private CallHistoryEntity awaitHistoryRecord(String endpoint) {
    long deadline = System.nanoTime() + Duration.ofSeconds(3).toNanos();
    Query query = Query.query(Criteria.where("endpoint").is(endpoint));
    while (System.nanoTime() < deadline) {
      Long count = template.count(query, CallHistoryEntity.class).block();
      if (count != null && count > 0) {
        return template.select(CallHistoryEntity.class).matching(query).one().block();
      }
      try {
        Thread.sleep(100L);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new IllegalStateException("Interrupted while waiting for record", e);
      }
    }
    throw new IllegalStateException("call history record was not written within 3 seconds");
  }
}
