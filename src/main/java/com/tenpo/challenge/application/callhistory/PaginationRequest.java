package com.tenpo.challenge.application.callhistory;

public record PaginationRequest(int page, int size) {

  public static final int DEFAULT_PAGE = 0;
  public static final int DEFAULT_SIZE = 20;
  public static final int MAX_SIZE = 100;

  public PaginationRequest {
    if (page < 0) {
      throw new InvalidPaginationException("page must be greater than or equal to 0");
    }
    if (size < 1 || size > MAX_SIZE) {
      throw new InvalidPaginationException("size must be between 1 and 100");
    }
  }
}
