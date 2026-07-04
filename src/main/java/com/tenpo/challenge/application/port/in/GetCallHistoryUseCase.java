package com.tenpo.challenge.application.port.in;

import com.tenpo.challenge.application.callhistory.CallHistoryPage;
import com.tenpo.challenge.application.callhistory.PaginationRequest;

public interface GetCallHistoryUseCase {

  CallHistoryPage get(PaginationRequest request);
}
