package com.tenpo.challenge.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.tenpo.challenge.application.callhistory.CallHistoryPage;
import com.tenpo.challenge.application.callhistory.PaginationRequest;
import com.tenpo.challenge.application.port.out.CallHistoryQueryPort;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class GetCallHistoryServiceTest {

  @Test
  void delegatesValidatedPaginationToQueryPort() {
    AtomicReference<PaginationRequest> capturedRequest = new AtomicReference<>();
    CallHistoryPage expectedPage = new CallHistoryPage(List.of(), 1, 5, 0, 0, false, true);
    CallHistoryQueryPort queryPort =
        request -> {
          capturedRequest.set(request);
          return expectedPage;
        };
    GetCallHistoryService useCase = new GetCallHistoryService(queryPort);

    CallHistoryPage result = useCase.get(new PaginationRequest(1, 5));

    assertThat(result).isSameAs(expectedPage);
    assertThat(capturedRequest.get()).isEqualTo(new PaginationRequest(1, 5));
  }
}
