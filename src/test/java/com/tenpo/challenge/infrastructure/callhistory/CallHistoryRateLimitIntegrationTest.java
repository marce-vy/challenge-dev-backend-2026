package com.tenpo.challenge.infrastructure.callhistory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tenpo.challenge.application.port.in.CalculateWithPercentageUseCase;
import com.tenpo.challenge.application.port.in.GetCallHistoryUseCase;
import com.tenpo.challenge.domain.CalculationInput;
import com.tenpo.challenge.domain.CalculationResult;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@TestPropertySource(properties = {"rate-limit.capacity=1", "rate-limit.refill-tokens=1"})
class CallHistoryRateLimitIntegrationTest {

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

  @MockBean private GetCallHistoryUseCase getCallHistoryUseCase;

  @MockBean private CalculateWithPercentageUseCase calculateWithPercentageUseCase;

  @BeforeEach
  void stubCalculation() {
    when(calculateWithPercentageUseCase.calculate(any(CalculationInput.class)))
        .thenReturn(
            new CalculationResult(
                new CalculationInput(new BigDecimal("100"), new BigDecimal("50")),
                new BigDecimal("150"),
                new BigDecimal("10"),
                new BigDecimal("165")));
  }

  @Test
  void recordsRateLimitedRequestWithSuccessFalse() throws Exception {
    jdbcTemplate.update("DELETE FROM call_history WHERE endpoint = '/api/v1/calculations'");

    mockMvc
        .perform(
            post("/api/v1/calculations")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"num1\":100,\"num2\":50}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.result").value(165));

    mockMvc
        .perform(
            post("/api/v1/calculations")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"num1\":100,\"num2\":50}"))
        .andExpect(status().isTooManyRequests());

    Map<String, Object> successRecord = awaitRecord("/api/v1/calculations", true, 2);
    assertThat(successRecord).isNotNull();

    Map<String, Object> rateLimitedRecord = awaitRecord("/api/v1/calculations", false, 2);
    assertThat(rateLimitedRecord).isNotNull();
    assertThat(rateLimitedRecord.get("success")).isEqualTo(false);
    assertThat(rateLimitedRecord.get("http_status")).isEqualTo(429);
    assertThat(rateLimitedRecord.get("error_body")).isNotNull();
    assertThat(rateLimitedRecord.get("response_body")).isNull();
  }

  private Map<String, Object> awaitRecord(String endpoint, boolean success, int expectedCount)
      throws Exception {
    long deadline = System.nanoTime() + Duration.ofSeconds(2).toNanos();
    while (System.nanoTime() < deadline) {
      Long count =
          jdbcTemplate.queryForObject(
              "SELECT COUNT(*) FROM call_history WHERE endpoint = ?", Long.class, endpoint);
      if (count != null && count >= expectedCount) {
        var rows =
            jdbcTemplate.queryForList(
                """
                SELECT id, occurred_at, http_method, endpoint, query_params, request_body,
                       response_body, error_body, http_status, success, duration_ms, client_ip
                FROM call_history
                WHERE endpoint = ? AND success = ?
                """,
                endpoint,
                success);
        if (!rows.isEmpty()) {
          return rows.getFirst();
        }
      }
      Thread.sleep(50L);
    }
    throw new IllegalStateException(
        "call history record with success=" + success + " was not written within 2 seconds");
  }
}
