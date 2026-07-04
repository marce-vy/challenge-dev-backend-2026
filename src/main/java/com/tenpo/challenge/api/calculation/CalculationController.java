package com.tenpo.challenge.api.calculation;

import com.tenpo.challenge.application.port.in.CalculateWithPercentageUseCase;
import com.tenpo.challenge.domain.CalculationResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(
    name = "Calculations",
    description = "Calculation operations using the configured percentage provider.")
@RequestMapping(path = "/api/v1/calculations", produces = MediaType.APPLICATION_JSON_VALUE)
public class CalculationController {

  private final CalculateWithPercentageUseCase useCase;

  public CalculationController(CalculateWithPercentageUseCase useCase) {
    this.useCase = useCase;
  }

  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
  @Operation(
      summary = "Calculate with percentage",
      description =
          "Receives two positive numbers, adds them, obtains the configured percentage, "
              + "and returns the final result. Functional calls are recorded asynchronously.")
  @ApiResponse(
      responseCode = "200",
      description = "Calculation completed.",
      headers = {
        @Header(name = "X-RateLimit-Limit", description = "Maximum requests allowed per minute."),
        @Header(
            name = "X-RateLimit-Remaining",
            description = "Remaining requests in the current window.")
      },
      content =
          @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              schema = @Schema(implementation = CalculationResponse.class)))
  @ApiResponse(
      responseCode = "400",
      description = "Invalid request body or validation error.",
      ref = "#/components/responses/BadRequest")
  @ApiResponse(
      responseCode = "429",
      description = "Rate limit exceeded.",
      ref = "#/components/responses/RateLimitExceeded")
  @ApiResponse(
      responseCode = "503",
      description = "Percentage provider unavailable after retry attempts.",
      ref = "#/components/responses/ProviderUnavailable")
  @ApiResponse(
      responseCode = "500",
      description = "Unexpected server error.",
      ref = "#/components/responses/UnexpectedServerError")
  public CalculationResponse calculate(@Valid @RequestBody CalculationRequest request) {
    CalculationResult result = useCase.calculate(request.toInput());
    return CalculationResponse.from(result);
  }
}
