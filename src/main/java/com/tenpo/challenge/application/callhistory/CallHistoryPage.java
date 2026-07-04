package com.tenpo.challenge.application.callhistory;

import java.util.List;

public record CallHistoryPage(
    List<CallHistoryEntry> content,
    int page,
    int size,
    long totalElements,
    int totalPages,
    boolean hasNext,
    boolean hasPrevious) {

  public CallHistoryPage {
    content = List.copyOf(content);
    if (page < 0) {
      throw new IllegalArgumentException("page must be greater than or equal to 0");
    }
    if (size < 1 || size > PaginationRequest.MAX_SIZE) {
      throw new IllegalArgumentException("size must be between 1 and 100");
    }
    if (totalElements < 0) {
      throw new IllegalArgumentException("totalElements must be greater than or equal to 0");
    }
    if (totalPages < 0) {
      throw new IllegalArgumentException("totalPages must be greater than or equal to 0");
    }
  }
}
