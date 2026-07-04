package com.tenpo.challenge.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tenpo.challenge.api.dto.ErrorResponse;
import com.tenpo.challenge.application.callhistory.InvalidPaginationException;
import com.tenpo.challenge.application.exception.PercentageProviderUnavailableException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

class GlobalExceptionHandlerTest {

  private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

  @Test
  void handleValidationReturns400WithFirstFieldErrorMessage() throws Exception {
    BindingResult bindingResult = mock(BindingResult.class);
    FieldError fieldError = new FieldError("request", "num1", "num1 is required");
    when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));

    MethodArgumentNotValidException ex =
        new MethodArgumentNotValidException(mock(MethodParameter.class), bindingResult);

    ResponseEntity<ErrorResponse> response = handler.handleValidation(ex);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().status()).isEqualTo(400);
    assertThat(response.getBody().error()).isEqualTo("Bad Request");
    assertThat(response.getBody().message()).isEqualTo("num1 is required");
  }

  @Test
  void handleValidationReturnsGenericMessageWhenNoFieldErrors() throws Exception {
    BindingResult bindingResult = mock(BindingResult.class);
    when(bindingResult.getFieldErrors()).thenReturn(List.of());

    MethodArgumentNotValidException ex =
        new MethodArgumentNotValidException(mock(MethodParameter.class), bindingResult);

    ResponseEntity<ErrorResponse> response = handler.handleValidation(ex);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().message()).isEqualTo("Request body is invalid");
  }

  @Test
  void handleNotReadableReturns400() {
    HttpMessageNotReadableException ex = new HttpMessageNotReadableException("malformed");

    ResponseEntity<ErrorResponse> response = handler.handleNotReadable(ex);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().status()).isEqualTo(400);
    assertThat(response.getBody().error()).isEqualTo("Bad Request");
    assertThat(response.getBody().message()).isEqualTo("Request body is invalid");
  }

  @Test
  void handleInvalidPaginationReturns400WithExceptionMessage() {
    InvalidPaginationException ex =
        new InvalidPaginationException("size must be between 1 and 100");

    ResponseEntity<ErrorResponse> response = handler.handleInvalidPagination(ex);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().message()).isEqualTo("size must be between 1 and 100");
  }

  @Test
  void handleTypeMismatchReturns400() {
    MethodArgumentTypeMismatchException ex =
        new MethodArgumentTypeMismatchException("abc", Integer.class, "page", null, null);

    ResponseEntity<ErrorResponse> response = handler.handleTypeMismatch(ex);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().status()).isEqualTo(400);
    assertThat(response.getBody().message()).isEqualTo("Request parameter is invalid");
  }

  @Test
  void handleProviderUnavailableReturns503() {
    PercentageProviderUnavailableException ex =
        new PercentageProviderUnavailableException("Percentage provider is unavailable", null);

    ResponseEntity<ErrorResponse> response = handler.handleProviderUnavailable(ex);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().status()).isEqualTo(503);
    assertThat(response.getBody().error()).isEqualTo("Service Unavailable");
    assertThat(response.getBody().message()).isEqualTo("Percentage provider is unavailable");
  }

  @Test
  void handleUnexpectedReturns500WithoutInternalDetails() {
    Exception ex = new IllegalStateException("database password leaked");

    ResponseEntity<ErrorResponse> response = handler.handleUnexpected(ex);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().status()).isEqualTo(500);
    assertThat(response.getBody().error()).isEqualTo("Internal Server Error");
    assertThat(response.getBody().message()).isEqualTo("Unexpected server error");
  }
}
