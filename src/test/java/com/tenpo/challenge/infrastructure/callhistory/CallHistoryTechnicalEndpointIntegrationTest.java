package com.tenpo.challenge.infrastructure.callhistory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import com.tenpo.challenge.application.port.in.CalculateWithPercentageUseCase;
import com.tenpo.challenge.application.port.in.GetCallHistoryUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
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
class CallHistoryTechnicalEndpointIntegrationTest {

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
  void doesNotRecordFaviconRequests() throws Exception {
    mockMvc.perform(get("/favicon.ico"));

    Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM call_history", Long.class);
    assertThat(count).isZero();
  }

  @Test
  void doesNotRecordV3ApiDocsRequests() throws Exception {
    mockMvc.perform(get("/v3/api-docs/swagger-config"));

    Long count =
        jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM call_history WHERE endpoint = '/v3/api-docs/swagger-config'",
            Long.class);
    assertThat(count).isZero();
  }
}
