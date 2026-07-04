package com.tenpo.challenge.api.callhistory;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

@Schema(description = "Recorded functional HTTP call.")
public record CallHistoryResponse(
    @Schema(
            description = "Unique history entry identifier.",
            example = "2f75cfb0-8c11-4b8e-91b9-9dfdc21f90d7")
        UUID id,
    @Schema(
            description = "UTC timestamp when the call was recorded.",
            example = "2026-07-02T19:20:03.385224Z")
        Instant occurredAt,
    @Schema(description = "HTTP method used by the request.", example = "POST") String httpMethod,
    @Schema(description = "Request path without query string.", example = "/api/v1/calculations")
        String endpoint,
    @Schema(description = "Raw query string, when present.", example = "page=0&size=20")
        String queryParams,
    @Schema(
            description = "Captured request body, when present.",
            example = "{\"num1\":100,\"num2\":50}")
        String requestBody,
    @Schema(
            description = "Captured successful response body, when present.",
            example = "{\"num1\":100,\"num2\":50,\"sum\":150,\"percentage\":10,\"result\":165}")
        String responseBody,
    @Schema(
            description = "Captured error response body, when present.",
            example =
                "{\"status\":400,\"error\":\"Bad Request\",\"message\":\"Request body is invalid\"}")
        String errorBody,
    @Schema(description = "Final HTTP status returned to the client.", example = "200")
        int httpStatus,
    @Schema(
            description = "Whether the final HTTP status is in the 2xx or 3xx range.",
            example = "true")
        boolean success,
    @Schema(description = "Request handling duration in milliseconds.", example = "58")
        Long durationMs,
    @Schema(description = "Best-effort client IP address.", example = "192.168.65.1")
        String clientIp) {}
