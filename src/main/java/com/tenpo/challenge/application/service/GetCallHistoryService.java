package com.tenpo.challenge.application.service;

import com.tenpo.challenge.application.callhistory.CallHistoryPage;
import com.tenpo.challenge.application.callhistory.PaginationRequest;
import com.tenpo.challenge.application.port.in.GetCallHistoryUseCase;
import com.tenpo.challenge.application.port.out.CallHistoryQueryPort;
import java.util.Objects;

public class GetCallHistoryService implements GetCallHistoryUseCase {

  private final CallHistoryQueryPort queryPort;

  public GetCallHistoryService(CallHistoryQueryPort queryPort) {
    this.queryPort = Objects.requireNonNull(queryPort);
  }

  @Override
  public CallHistoryPage get(PaginationRequest request) {
    return queryPort.findPage(Objects.requireNonNull(request, "request is required"));
  }
}
