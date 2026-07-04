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
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class CallHistoryRecordingIntegrationTest {

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

  @Test
  void recordsSuccessfulFunctionalCallWithoutChangingResponse() throws Exception {
    when(calculateWithPercentageUseCase.calculate(any(CalculationInput.class)))
        .thenReturn(
            new CalculationResult(
                new CalculationInput(new BigDecimal("100"), new BigDecimal("50")),
                new BigDecimal("150"),
                new BigDecimal("10"),
                new BigDecimal("165")));

    mockMvc
        .perform(
            post("/api/v1/calculations")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"num1\":100,\"num2\":50}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.sum").value(150))
        .andExpect(jsonPath("$.percentage").value(10))
        .andExpect(jsonPath("$.result").value(165));

    Map<String, Object> row = awaitRecord("/api/v1/calculations", 1);

    assertThat(row.get("http_method")).isEqualTo("POST");
    assertThat(row.get("query_params")).isNull();
    assertThat(row.get("request_body")).isEqualTo("{\"num1\":100,\"num2\":50}");
    assertThat(row.get("response_body"))
        .isEqualTo("{\"num1\":100,\"num2\":50,\"sum\":150,\"percentage\":10,\"result\":165}");
    assertThat(row.get("error_body")).isNull();
    assertThat(row.get("http_status")).isEqualTo(200);
    assertThat(row.get("success")).isEqualTo(true);
    assertThat(row.get("duration_ms")).isNotNull();
    assertThat(row.get("client_ip")).isNotNull();
  }

  private Map<String, Object> awaitRecord(String endpoint, int expectedCount) throws Exception {
    long deadline = System.nanoTime() + Duration.ofSeconds(2).toNanos();
    while (System.nanoTime() < deadline) {
      Long count =
          jdbcTemplate.queryForObject(
              "SELECT COUNT(*) FROM call_history WHERE endpoint = ?", Long.class, endpoint);
      if (count != null && count == expectedCount) {
        return jdbcTemplate.queryForMap(
            """
            SELECT id, occurred_at, http_method, endpoint, query_params, request_body,
                   response_body, error_body, http_status, success, duration_ms, client_ip
            FROM call_history
            WHERE endpoint = ?
            """,
            endpoint);
      }
      Thread.sleep(50L);
    }
    throw new IllegalStateException("call history record was not written within 2 seconds");
  }
}
