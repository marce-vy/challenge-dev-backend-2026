package com.tenpo.challenge.application.port.out;

import com.tenpo.challenge.application.callhistory.CallHistoryPage;
import com.tenpo.challenge.application.callhistory.PaginationRequest;
import reactor.core.publisher.Mono;

public interface CallHistoryQueryPort {

  Mono<CallHistoryPage> findPage(PaginationRequest request);
}
