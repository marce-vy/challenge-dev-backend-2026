package com.tenpo.challenge.application.callhistory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class PaginationRequestTest {

  @Test
  void acceptsFirstPageAndMinimumSize() {
    PaginationRequest request = new PaginationRequest(0, 1);

    assertThat(request.page()).isZero();
    assertThat(request.size()).isEqualTo(1);
  }

  @Test
  void acceptsMaximumSize() {
    PaginationRequest request = new PaginationRequest(2, 100);

    assertThat(request.page()).isEqualTo(2);
    assertThat(request.size()).isEqualTo(100);
  }

  @Test
  void rejectsNegativePage() {
    assertThatThrownBy(() -> new PaginationRequest(-1, 20))
        .isInstanceOf(InvalidPaginationException.class)
        .hasMessage("page must be greater than or equal to 0");
  }

  @Test
  void rejectsSizeBelowMinimum() {
    assertThatThrownBy(() -> new PaginationRequest(0, 0))
        .isInstanceOf(InvalidPaginationException.class)
        .hasMessage("size must be between 1 and 100");
  }

  @Test
  void rejectsSizeAboveMaximum() {
    assertThatThrownBy(() -> new PaginationRequest(0, 101))
        .isInstanceOf(InvalidPaginationException.class)
        .hasMessage("size must be between 1 and 100");
  }
}
