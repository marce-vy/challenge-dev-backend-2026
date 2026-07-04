package com.tenpo.challenge.api.callhistory;

import com.tenpo.challenge.api.ratelimit.ClientIpResolver;
import com.tenpo.challenge.application.callhistory.RecordCallHistoryCommand;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

public class CallHistoryFilter extends OncePerRequestFilter implements Ordered {

  private static final Logger log = LoggerFactory.getLogger(CallHistoryFilter.class);

  private static final int MAX_BODY_LENGTH = 4096;

  private static final List<String> EXCLUDED_PATHS =
      List.of(
          "/swagger-ui",
          "/swagger-ui/**",
          "/v3/api-docs",
          "/v3/api-docs/**",
          "/api/v1/call-history",
          "/actuator",
          "/actuator/**",
          "/favicon.ico");

  private final CallHistoryRecorder recorder;
  private final ClientIpResolver clientIpResolver;
  private final AntPathMatcher matcher = new AntPathMatcher();

  public CallHistoryFilter(CallHistoryRecorder recorder, ClientIpResolver clientIpResolver) {
    this.recorder = Objects.requireNonNull(recorder, "recorder is required");
    this.clientIpResolver = Objects.requireNonNull(clientIpResolver, "clientIpResolver is required");
  }

  @Override
  public int getOrder() {
    return Ordered.HIGHEST_PRECEDENCE;
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    String path = request.getRequestURI();
    return EXCLUDED_PATHS.stream().anyMatch(pattern -> matcher.match(pattern, path));
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(request);
    ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);
    Instant occurredAt = Instant.now();
    long startedAtNanos = System.nanoTime();
    try {
      filterChain.doFilter(wrappedRequest, wrappedResponse);
    } finally {
      try {
        recorder.record(toCommand(wrappedRequest, wrappedResponse, occurredAt, startedAtNanos));
      } catch (RuntimeException ex) {
        log.warn("Best-effort history recording failed", ex);
      } finally {
        wrappedResponse.copyBodyToResponse();
      }
    }
  }

  private RecordCallHistoryCommand toCommand(
      ContentCachingRequestWrapper request,
      ContentCachingResponseWrapper response,
      Instant occurredAt,
      long startedAtNanos) {
    int status = response.getStatus();
    boolean success = status >= 200 && status < 400;
    String body = bodyToString(response.getContentAsByteArray(), response.getCharacterEncoding());
    String requestBody =
        bodyToString(request.getContentAsByteArray(), request.getCharacterEncoding());

    return new RecordCallHistoryCommand(
        occurredAt,
        request.getMethod(),
        request.getRequestURI(),
        request.getQueryString(),
        truncate(requestBody),
        success ? truncate(body) : null,
        success ? null : truncate(body),
        status,
        success,
        TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAtNanos),
        clientIpResolver.resolve(request));
  }

  private String bodyToString(byte[] body, String encoding) {
    if (body == null || body.length == 0) {
      return null;
    }
    Charset charset = encoding == null ? StandardCharsets.UTF_8 : Charset.forName(encoding);
    return new String(body, charset);
  }

  private static String truncate(String body) {
    if (body == null || body.length() <= MAX_BODY_LENGTH) {
      return body;
    }
    return body.substring(0, MAX_BODY_LENGTH) + "...[truncated]";
  }
}
