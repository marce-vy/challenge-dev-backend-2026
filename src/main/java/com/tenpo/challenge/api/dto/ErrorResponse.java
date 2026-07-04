package com.tenpo.challenge.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Standard error response.")
public record ErrorResponse(
    @Schema(description = "HTTP status code.", example = "400") int status,
    @Schema(description = "HTTP reason phrase.", example = "Bad Request") String error,
    @Schema(description = "Human-readable error message.", example = "Request body is invalid")
        String message) {}
