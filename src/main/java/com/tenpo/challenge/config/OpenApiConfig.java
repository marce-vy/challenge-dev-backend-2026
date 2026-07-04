package com.tenpo.challenge.config;

import com.tenpo.challenge.api.dto.ErrorResponse;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.servers.Server;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.headers.Header;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import java.util.Map;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;

@Configuration
@OpenAPIDefinition(
    info =
        @Info(
            title = "Tenpo Backend Challenge API",
            version = "1.0.0",
            description =
                "REST API for percentage calculations, per-IP rate limiting, "
                    + "and asynchronous call-history auditing. Functional requests are "
                    + "recorded on a best-effort basis after response handling, including "
                    + "successful, failed, and rate-limited calls.",
            contact = @Contact(name = "Tenpo Backend Challenge")),
    servers = @Server(url = "http://localhost:8080", description = "Local Docker Compose server"))
public class OpenApiConfig {

  @Bean
  public OpenAPI tenpoChallengeOpenApi() {
    return new OpenAPI()
        .components(
            new Components()
                .addResponses("BadRequest", errorResponse("Invalid request."))
                .addResponses(
                    "ProviderUnavailable", errorResponse("Percentage provider unavailable."))
                .addResponses("UnexpectedServerError", errorResponse("Unexpected server error."))
                .addResponses(
                    "RateLimitExceeded",
                    errorResponse("Rate limit exceeded.").headers(rateLimitHeaders())));
  }

  @Bean
  public OpenApiCustomizer errorSchemaCustomizer() {
    return openApi -> openApi.getComponents().addSchemas("ErrorResponse", errorSchema());
  }

  private Map<String, Header> rateLimitHeaders() {
    return Map.of(
        "X-RateLimit-Limit",
        new Header().description("Maximum requests allowed per minute."),
        "X-RateLimit-Remaining",
        new Header().description("Remaining requests in the current window."),
        HttpHeaders.RETRY_AFTER,
        new Header().description("Seconds until the request can be retried."));
  }

  private Schema<?> errorSchema() {
    return new Schema<>()
        .type("object")
        .addProperty(
            "status", new Schema<>().type("integer").description("HTTP status code.").example(400))
        .addProperty(
            "error",
            new Schema<>().type("string").description("HTTP reason phrase.").example("Bad Request"))
        .addProperty(
            "message",
            new Schema<>()
                .type("string")
                .description("Human-readable error message.")
                .example("Request body is invalid"));
  }

  private ApiResponse errorResponse(String description) {
    Schema<ErrorResponse> errorSchema =
        new Schema<ErrorResponse>().$ref("#/components/schemas/ErrorResponse");
    return new ApiResponse()
        .description(description)
        .content(
            new Content()
                .addMediaType(
                    org.springframework.http.MediaType.APPLICATION_JSON_VALUE,
                    new MediaType().schema(errorSchema)));
  }
}
