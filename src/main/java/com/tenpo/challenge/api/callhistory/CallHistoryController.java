package com.tenpo.challenge.api.callhistory;

import com.tenpo.challenge.application.callhistory.PaginationRequest;
import com.tenpo.challenge.application.port.in.GetCallHistoryUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@Tag(name = "Call History", description = "Paginated audit history for functional API calls.")
@RequestMapping(path = "/api/v1/call-history", produces = MediaType.APPLICATION_JSON_VALUE)
public class CallHistoryController {

  private final GetCallHistoryUseCase useCase;

  public CallHistoryController(GetCallHistoryUseCase useCase) {
    this.useCase = useCase;
  }

  @GetMapping
  @Operation(
      summary = "Get call history",
      description =
          "Returns functional API calls ordered by occurrence time descending. "
              + "Records are created asynchronously on a best-effort basis for successful, "
              + "failed, and rate-limited functional requests. "
              + "Technical endpoints such as Swagger UI and OpenAPI docs are excluded from history.")
  @ApiResponse(
      responseCode = "200",
      description = "Call history page returned.",
      headers = {
        @Header(name = "X-RateLimit-Limit", description = "Maximum requests allowed per minute."),
        @Header(
            name = "X-RateLimit-Remaining",
            description = "Remaining requests in the current window.")
      },
      content =
          @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              schema = @Schema(implementation = CallHistoryPageResponse.class)))
  @ApiResponse(
      responseCode = "400",
      description = "Invalid pagination parameters.",
      ref = "#/components/responses/BadRequest")
  @ApiResponse(
      responseCode = "429",
      description = "Rate limit exceeded.",
      ref = "#/components/responses/RateLimitExceeded")
  @ApiResponse(
      responseCode = "500",
      description = "Unexpected server error.",
      ref = "#/components/responses/UnexpectedServerError")
  public Mono<ResponseEntity<CallHistoryPageResponse>> getCallHistory(
      @Parameter(description = "Zero-based page index.", example = "0")
          @RequestParam(defaultValue = "0")
          int page,
      @Parameter(description = "Page size. Must be between 1 and 100.", example = "20")
          @RequestParam(defaultValue = "20")
          int size) {
    return useCase
        .get(new PaginationRequest(page, size))
        .map(result -> ResponseEntity.ok(CallHistoryWebMapper.toResponse(result)));
  }
}
