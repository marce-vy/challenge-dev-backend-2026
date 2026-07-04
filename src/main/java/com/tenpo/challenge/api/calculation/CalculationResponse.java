package com.tenpo.challenge.api.calculation;

import com.tenpo.challenge.domain.CalculationResult;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

@Schema(description = "Calculation result with the applied percentage.")
public record CalculationResponse(
    @Schema(description = "First input number.", example = "100") BigDecimal num1,
    @Schema(description = "Second input number.", example = "50") BigDecimal num2,
    @Schema(description = "Plain sum of num1 and num2.", example = "150") BigDecimal sum,
    @Schema(description = "Percentage obtained from the configured provider.", example = "10")
        BigDecimal percentage,
    @Schema(description = "Final result after applying the percentage.", example = "165")
        BigDecimal result) {

  static CalculationResponse from(CalculationResult result) {
    return new CalculationResponse(
        result.num1(), result.num2(), result.sum(), result.percentage(), result.result());
  }
}
