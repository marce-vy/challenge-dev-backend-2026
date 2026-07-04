package com.tenpo.challenge.application.port.out;

import com.tenpo.challenge.application.callhistory.CallHistoryPage;
import com.tenpo.challenge.application.callhistory.PaginationRequest;

public interface CallHistoryQueryPort {

  CallHistoryPage findPage(PaginationRequest request);
}
