package com.tenpo.challenge.infrastructure.callhistory;

import static org.assertj.core.api.Assertions.assertThat;

import com.tenpo.challenge.application.port.in.CalculateWithPercentageUseCase;
import com.tenpo.challenge.application.port.in.GetCallHistoryUseCase;
import com.tenpo.challenge.persistence.callhistory.CallHistoryEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.data.relational.core.query.Query;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class CallHistoryTechnicalEndpointIntegrationTest {

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

  @MockBean private GetCallHistoryUseCase getCallHistoryUseCase;

  @MockBean private CalculateWithPercentageUseCase calculateWithPercentageUseCase;

  @Test
  void doesNotRecordFaviconRequests() {
    webTestClient.get().uri("/favicon.ico").exchange().expectStatus().is5xxServerError();

    Long count = template.count(Query.empty(), CallHistoryEntity.class).block();
    assertThat(count).isZero();
  }

  @Test
  void doesNotRecordV3ApiDocsRequests() {
    webTestClient.get().uri("/v3/api-docs/swagger-config").exchange().expectStatus().isOk();

    Long count =
        template
            .count(
                Query.query(Criteria.where("endpoint").is("/v3/api-docs/swagger-config")),
                CallHistoryEntity.class)
            .block();
    assertThat(count).isZero();
  }
}
