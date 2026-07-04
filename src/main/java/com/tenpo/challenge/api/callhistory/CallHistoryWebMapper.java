package com.tenpo.challenge.api.callhistory;

import com.tenpo.challenge.application.callhistory.CallHistoryEntry;
import com.tenpo.challenge.application.callhistory.CallHistoryPage;

final class CallHistoryWebMapper {

  private CallHistoryWebMapper() {}

  static CallHistoryPageResponse toResponse(CallHistoryPage page) {
    return new CallHistoryPageResponse(
        page.content().stream().map(CallHistoryWebMapper::toResponse).toList(),
        page.page(),
        page.size(),
        page.totalElements(),
        page.totalPages(),
        page.hasNext(),
        page.hasPrevious());
  }

  private static CallHistoryResponse toResponse(CallHistoryEntry entry) {
    return new CallHistoryResponse(
        entry.id(),
        entry.occurredAt(),
        entry.httpMethod(),
        entry.endpoint(),
        entry.queryParams(),
        entry.requestBody(),
        entry.responseBody(),
        entry.errorBody(),
        entry.httpStatus(),
        entry.success(),
        entry.durationMs(),
        entry.clientIp());
  }
}
