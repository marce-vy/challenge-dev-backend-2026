package com.tenpo.challenge.persistence.callhistory;

import static org.assertj.core.api.Assertions.assertThat;

import com.tenpo.challenge.application.callhistory.RecordCallHistoryCommand;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.data.relational.core.query.Query;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@DataR2dbcTest
@ActiveProfiles("test")
@Import(CallHistoryCommandAdapter.class)
class CallHistoryCommandAdapterTest {

  @Container
  private static final PostgreSQLContainer<?> POSTGRES =
      new PostgreSQLContainer<>("postgres:16-alpine").withInitScript("schema.sql");

  @DynamicPropertySource
  static void r2dbcProperties(DynamicPropertyRegistry registry) {
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

  @Autowired private R2dbcEntityTemplate template;

  @Autowired private CallHistoryCommandAdapter adapter;

  @BeforeEach
  void setUp() {
    template.delete(CallHistoryEntity.class).all().block();
  }

  @Test
  void savesCommandIntoCallHistoryTable() {
    Instant occurredAt = Instant.parse("2026-07-01T22:15:30.123Z");
    RecordCallHistoryCommand command =
        new RecordCallHistoryCommand(
            occurredAt,
            "POST",
            "/api/v1/calculations",
            "a=1",
            "{\"num1\":5}",
            "{\"result\":11}",
            null,
            200,
            true,
            42L,
            "127.0.0.1");

    adapter.save(command).block();

    CallHistoryEntity saved =
        template
            .select(CallHistoryEntity.class)
            .matching(Query.query(Criteria.where("endpoint").is("/api/v1/calculations")))
            .one()
            .block();

    assertThat(saved).isNotNull();
    assertThat(saved.id()).isNotNull();
    assertThat(saved.occurredAt()).isEqualTo(occurredAt);
    assertThat(saved.httpMethod()).isEqualTo("POST");
    assertThat(saved.queryParams()).isEqualTo("a=1");
    assertThat(saved.requestBody()).isEqualTo("{\"num1\":5}");
    assertThat(saved.responseBody()).isEqualTo("{\"result\":11}");
    assertThat(saved.errorBody()).isNull();
    assertThat(saved.httpStatus()).isEqualTo(200);
    assertThat(saved.success()).isTrue();
    assertThat(saved.durationMs()).isEqualTo(42L);
    assertThat(saved.clientIp()).isEqualTo("127.0.0.1");
  }
}
