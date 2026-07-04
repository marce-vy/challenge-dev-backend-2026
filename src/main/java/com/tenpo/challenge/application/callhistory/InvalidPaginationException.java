package com.tenpo.challenge.application.callhistory;

public class InvalidPaginationException extends RuntimeException {

  public InvalidPaginationException(String message) {
    super(message);
  }
}
