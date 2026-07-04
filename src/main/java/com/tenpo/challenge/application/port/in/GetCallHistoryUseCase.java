package com.tenpo.challenge.application.port.in;

import com.tenpo.challenge.application.callhistory.CallHistoryPage;
import com.tenpo.challenge.application.callhistory.PaginationRequest;
import reactor.core.publisher.Mono;

public interface GetCallHistoryUseCase {

  Mono<CallHistoryPage> get(PaginationRequest request);
}
