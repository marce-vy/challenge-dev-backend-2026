package com.tenpo.challenge.api.calculation;

import com.tenpo.challenge.domain.CalculationInput;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

@Schema(description = "Numbers used by the calculation endpoint.")
public record CalculationRequest(
    @Schema(
            description = "First positive number.",
            example = "100",
            requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "num1 is required") @Positive(message = "num1 must be greater than zero") BigDecimal num1,
    @Schema(
            description = "Second positive number.",
            example = "50",
            requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "num2 is required") @Positive(message = "num2 must be greater than zero") BigDecimal num2) {

  CalculationInput toInput() {
    return new CalculationInput(num1, num2);
  }
}
