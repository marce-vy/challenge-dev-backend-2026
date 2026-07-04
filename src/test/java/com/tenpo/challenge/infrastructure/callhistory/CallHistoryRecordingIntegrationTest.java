package com.tenpo.challenge.infrastructure.callhistory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.tenpo.challenge.application.port.in.CalculateWithPercentageUseCase;
import com.tenpo.challenge.application.port.in.GetCallHistoryUseCase;
import com.tenpo.challenge.domain.CalculationInput;
import com.tenpo.challenge.domain.CalculationResult;
import com.tenpo.challenge.persistence.callhistory.CallHistoryEntity;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.data.relational.core.query.Query;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.core.publisher.Mono;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class CallHistoryRecordingIntegrationTest {

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

  @Autowired private TestRestTemplate restTemplate;

  @Autowired private R2dbcEntityTemplate template;

  @MockBean private GetCallHistoryUseCase getCallHistoryUseCase;

  @MockBean private CalculateWithPercentageUseCase calculateWithPercentageUseCase;

  @Test
  void recordsSuccessfulFunctionalCallWithoutChangingResponse() throws Exception {
    when(calculateWithPercentageUseCase.calculate(any(CalculationInput.class)))
        .thenReturn(
            Mono.just(
                new CalculationResult(
                    new CalculationInput(new BigDecimal("100"), new BigDecimal("50")),
                    new BigDecimal("150"),
                    new BigDecimal("10"),
                    new BigDecimal("165"))));

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    HttpEntity<String> request = new HttpEntity<>("{\"num1\":100,\"num2\":50}", headers);

    ResponseEntity<Map> response =
        restTemplate.postForEntity("/api/v1/calculations", request, Map.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).containsEntry("sum", 150);
    assertThat(response.getBody()).containsEntry("percentage", 10);
    assertThat(response.getBody()).containsEntry("result", 165);

    CallHistoryEntity row = awaitRecord("/api/v1/calculations", 1);

    assertThat(row.httpMethod()).isEqualTo("POST");
    assertThat(row.queryParams()).isNull();
    assertThat(row.requestBody()).isEqualTo("{\"num1\":100,\"num2\":50}");
    assertThat(row.responseBody())
        .isEqualTo("{\"num1\":100,\"num2\":50,\"sum\":150,\"percentage\":10,\"result\":165}");
    assertThat(row.errorBody()).isNull();
    assertThat(row.httpStatus()).isEqualTo(200);
    assertThat(row.success()).isTrue();
    assertThat(row.durationMs()).isNotNull();
    assertThat(row.clientIp()).isNotNull();
  }

  private CallHistoryEntity awaitRecord(String endpoint, int expectedCount) {
    long deadline = System.nanoTime() + Duration.ofSeconds(3).toNanos();
    Query query = Query.query(Criteria.where("endpoint").is(endpoint));
    while (System.nanoTime() < deadline) {
      Long count = template.count(query, CallHistoryEntity.class).block();
      if (count != null && count == expectedCount) {
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
