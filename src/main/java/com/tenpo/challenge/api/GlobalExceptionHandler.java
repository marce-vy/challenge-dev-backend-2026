package com.tenpo.challenge.api;

import com.tenpo.challenge.api.dto.ErrorResponse;
import com.tenpo.challenge.application.PercentageProviderUnavailableException;
import com.tenpo.challenge.application.callhistory.InvalidPaginationException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ServerWebInputException;

@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @ExceptionHandler(WebExchangeBindException.class)
  public ResponseEntity<ErrorResponse> handleValidation(WebExchangeBindException ex) {
    var fieldErrors = ex.getBindingResult().getFieldErrors();
    String message =
        fieldErrors.isEmpty() ? "Request body is invalid" : fieldErrors.get(0).getDefaultMessage();
    return errorResponse(HttpStatus.BAD_REQUEST, message);
  }

  @ExceptionHandler(ServerWebInputException.class)
  public ResponseEntity<ErrorResponse> handleServerWebInput(ServerWebInputException ex) {
    String message = "Request body is invalid";
    var methodParameter = ex.getMethodParameter();
    if (methodParameter != null && !methodParameter.hasParameterAnnotation(RequestBody.class)) {
      message = "Request parameter is invalid";
    }
    return errorResponse(HttpStatus.BAD_REQUEST, message);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
    List<FieldError> fieldErrors = ex.getBindingResult().getFieldErrors();
    String message =
        fieldErrors.isEmpty() ? "Request body is invalid" : fieldErrors.get(0).getDefaultMessage();
    return errorResponse(HttpStatus.BAD_REQUEST, message);
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ErrorResponse> handleNotReadable(HttpMessageNotReadableException ex) {
    return errorResponse(HttpStatus.BAD_REQUEST, "Request body is invalid");
  }

  @ExceptionHandler(InvalidPaginationException.class)
  public ResponseEntity<ErrorResponse> handleInvalidPagination(InvalidPaginationException ex) {
    return errorResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
  }

  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
    return errorResponse(HttpStatus.BAD_REQUEST, "Request parameter is invalid");
  }

  @ExceptionHandler(PercentageProviderUnavailableException.class)
  public ResponseEntity<ErrorResponse> handleProviderUnavailable(
      PercentageProviderUnavailableException ex) {
    return errorResponse(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage());
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex) {
    LOGGER.error("Unexpected API error", ex);
    return errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected server error");
  }

  private ResponseEntity<ErrorResponse> errorResponse(HttpStatus status, String message) {
    return ResponseEntity.status(status)
        .body(new ErrorResponse(status.value(), status.getReasonPhrase(), message));
  }
}
