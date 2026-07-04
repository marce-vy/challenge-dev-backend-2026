package com.tenpo.challenge.api.calculation;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
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
class CalculationPercentageE2ETest {

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

  @BeforeEach
  void setUp() {
    webTestClient = webTestClient.mutate().build();
  }

  @Test
  void calculatesUsingTheConfiguredPercentageProviderTransport() {
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
  }
}
