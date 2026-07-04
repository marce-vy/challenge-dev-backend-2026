package com.tenpo.challenge.api.callhistory;

import com.tenpo.challenge.api.ratelimit.ClientIpResolver;
import com.tenpo.challenge.application.callhistory.RecordCallHistoryCommand;
import com.tenpo.challenge.application.port.out.CallHistoryRecorder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Order(Ordered.HIGHEST_PRECEDENCE)
public class CallHistoryWebFilter implements WebFilter {

  private static final Logger log = LoggerFactory.getLogger(CallHistoryWebFilter.class);

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

  public CallHistoryWebFilter(CallHistoryRecorder recorder, ClientIpResolver clientIpResolver) {
    this.recorder = Objects.requireNonNull(recorder, "recorder is required");
    this.clientIpResolver =
        Objects.requireNonNull(clientIpResolver, "clientIpResolver is required");
  }

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
    if (shouldNotFilter(exchange.getRequest())) {
      return chain.filter(exchange);
    }

    Instant occurredAt = Instant.now();
    long startedAtNanos = System.nanoTime();

    CachingRequestDecorator requestDecorator = new CachingRequestDecorator(exchange.getRequest());
    CachingResponseDecorator responseDecorator =
        new CachingResponseDecorator(exchange.getResponse());

    ServerWebExchange decorated =
        exchange.mutate().request(requestDecorator).response(responseDecorator).build();

    return chain
        .filter(decorated)
        .doFinally(
            signalType -> {
              try {
                RecordCallHistoryCommand command =
                    toCommand(
                        decorated.getRequest(),
                        responseDecorator,
                        requestDecorator.getCachedBody(),
                        occurredAt,
                        startedAtNanos);
                recorder
                    .record(command)
                    .subscribe(null, ex -> log.warn("Best-effort history recording failed", ex));
              } catch (RuntimeException ex) {
                log.warn("Best-effort history recording failed", ex);
              }
            });
  }

  private boolean shouldNotFilter(ServerHttpRequest request) {
    String path = request.getURI().getPath();
    return EXCLUDED_PATHS.stream().anyMatch(pattern -> matcher.match(pattern, path));
  }

  private RecordCallHistoryCommand toCommand(
      ServerHttpRequest request,
      CachingResponseDecorator responseDecorator,
      byte[] cachedRequestBody,
      Instant occurredAt,
      long startedAtNanos) {
    int status =
        responseDecorator.getStatusCode() != null ? responseDecorator.getStatusCode().value() : 0;
    boolean success = status >= 200 && status < 400;

    String requestBody = truncate(bytesToString(cachedRequestBody));
    byte[] responseBytes = responseDecorator.getCachedBody();
    String responseBody = bytesToString(responseBytes);
    String errorBody = success ? null : responseBody;

    String clientIp = clientIpResolver.resolve(request);

    return new RecordCallHistoryCommand(
        occurredAt,
        request.getMethod().name(),
        request.getURI().getPath(),
        request.getURI().getRawQuery(),
        requestBody,
        success ? truncate(responseBody) : null,
        errorBody != null ? truncate(errorBody) : null,
        status,
        success,
        TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAtNanos),
        clientIp);
  }

  private static String bytesToString(byte[] bytes) {
    if (bytes == null || bytes.length == 0) {
      return null;
    }
    return new String(bytes, StandardCharsets.UTF_8);
  }

  private static String truncate(String body) {
    if (body == null || body.length() <= MAX_BODY_LENGTH) {
      return body;
    }
    return body.substring(0, MAX_BODY_LENGTH) + "...[truncated]";
  }

  static class CachingRequestDecorator extends ServerHttpRequestDecorator {

    private final AtomicReference<byte[]> cachedBody = new AtomicReference<>();

    CachingRequestDecorator(ServerHttpRequest delegate) {
      super(delegate);
    }

    byte[] getCachedBody() {
      return cachedBody.get();
    }

    @Override
    public Flux<DataBuffer> getBody() {
      return DataBufferUtils.join(super.getBody())
          .doOnNext(
              buffer -> {
                byte[] bytes = new byte[buffer.readableByteCount()];
                buffer.read(bytes);
                cachedBody.set(bytes);
                DataBufferUtils.release(buffer);
              })
          .flatMapMany(
              buffer -> {
                DataBufferFactory factory = new DefaultDataBufferFactory();
                byte[] bytes = cachedBody.get();
                if (bytes != null) {
                  return Flux.just(factory.wrap(bytes));
                }
                return Flux.empty();
              });
    }
  }

  static class CachingResponseDecorator extends ServerHttpResponseDecorator {

    private final AtomicReference<byte[]> cachedBody = new AtomicReference<>();

    CachingResponseDecorator(ServerHttpResponse delegate) {
      super(delegate);
    }

    byte[] getCachedBody() {
      return cachedBody.get();
    }

    @Override
    public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
      return DataBufferUtils.join(body)
          .doOnNext(
              buffer -> {
                byte[] bytes = new byte[buffer.readableByteCount()];
                buffer.read(bytes);
                cachedBody.set(bytes);
              })
          .flatMap(
              buffer -> {
                DataBufferFactory factory = new DefaultDataBufferFactory();
                byte[] bytes = cachedBody.get();
                if (bytes != null) {
                  return super.writeWith(Mono.just(factory.wrap(bytes)));
                }
                return super.writeWith(Mono.empty());
              });
    }
  }
}
