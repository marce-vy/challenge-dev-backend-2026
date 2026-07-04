package com.tenpo.challenge;

import com.tenpo.challenge.application.port.in.GetCallHistoryUseCase;
import com.tenpo.challenge.persistence.callhistory.CallHistoryRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest(
    classes = TenpoChallengeApplication.class,
    properties = {
      "spring.autoconfigure.exclude="
          + "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,"
          + "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration,"
          + "org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration"
    })
class TenpoChallengeApplicationTests {

  @MockBean private GetCallHistoryUseCase getCallHistoryUseCase;
  @MockBean private CallHistoryRepository callHistoryRepository;

  @Test
  void contextLoads() {}
}
