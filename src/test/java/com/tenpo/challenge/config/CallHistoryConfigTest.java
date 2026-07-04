package com.tenpo.challenge.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.tenpo.challenge.application.port.out.CallHistoryPersistencePort;
import com.tenpo.challenge.application.port.out.CallHistoryQueryPort;
import com.tenpo.challenge.persistence.callhistory.CallHistoryCommandAdapter;
import com.tenpo.challenge.persistence.callhistory.CallHistoryQueryAdapter;
import com.tenpo.challenge.persistence.callhistory.CallHistoryRepository;
import org.junit.jupiter.api.Test;

class CallHistoryConfigTest {

  @Test
  void queryPortWiresQueryAdapter() {
    CallHistoryQueryPort port =
        new CallHistoryConfig().callHistoryQueryPort(mock(CallHistoryRepository.class));

    assertThat(port).isInstanceOf(CallHistoryQueryAdapter.class);
  }

  @Test
  void persistencePortWiresCommandAdapter() {
    CallHistoryPersistencePort port =
        new CallHistoryConfig().callHistoryPersistencePort(mock(CallHistoryRepository.class));

    assertThat(port).isInstanceOf(CallHistoryCommandAdapter.class);
  }
}
