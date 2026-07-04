package com.tenpo.challenge.api.callhistory;

import static org.assertj.core.api.Assertions.assertThat;

import com.tenpo.challenge.api.ratelimit.ClientIpResolver;
import com.tenpo.challenge.application.callhistory.RecordCallHistoryCommand;
import com.tenpo.challenge.application.port.out.CallHistoryRecorder;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

class CallHistoryWebFilterTest {

  private static final ClientIpResolver FIXED_IP_RESOLVER = request -> "10.0.0.1";

  @Test
  void skipsExcludedSwaggerPath() {
    List<RecordCallHistoryCommand> recorded = new CopyOnWriteArrayList<>();
    CallHistoryRecorder recorder =
        cmd -> {
          recorded.add(cmd);
          return Mono.empty();
        };
    WebFilterChain chain = exchange -> exchange.getResponse().setComplete();
    CallHistoryWebFilter filter = new CallHistoryWebFilter(recorder, FIXED_IP_RESOLVER);

    MockServerWebExchange exchange =
        MockServerWebExchange.from(MockServerHttpRequest.get("/swagger-ui/index.html"));

    filter.filter(exchange, chain).block();

    assertThat(recorded).isEmpty();
  }

  @Test
  void skipsExcludedActuatorPath() {
    List<RecordCallHistoryCommand> recorded = new CopyOnWriteArrayList<>();
    CallHistoryRecorder recorder =
        cmd -> {
          recorded.add(cmd);
          return Mono.empty();
        };
    WebFilterChain chain = exchange -> exchange.getResponse().setComplete();
    CallHistoryWebFilter filter = new CallHistoryWebFilter(recorder, FIXED_IP_RESOLVER);

    MockServerWebExchange exchange =
        MockServerWebExchange.from(MockServerHttpRequest.get("/actuator/health"));

    filter.filter(exchange, chain).block();

    assertThat(recorded).isEmpty();
  }

  @Test
  void skipsExcludedV3ApiDocsPath() {
    List<RecordCallHistoryCommand> recorded = new CopyOnWriteArrayList<>();
    CallHistoryRecorder recorder =
        cmd -> {
          recorded.add(cmd);
          return Mono.empty();
        };
    WebFilterChain chain = exchange -> exchange.getResponse().setComplete();
    CallHistoryWebFilter filter = new CallHistoryWebFilter(recorder, FIXED_IP_RESOLVER);

    MockServerWebExchange exchange =
        MockServerWebExchange.from(MockServerHttpRequest.get("/v3/api-docs/swagger-config"));

    filter.filter(exchange, chain).block();

    assertThat(recorded).isEmpty();
  }

  @Test
  void skipsExcludedCallHistoryPath() {
    List<RecordCallHistoryCommand> recorded = new CopyOnWriteArrayList<>();
    CallHistoryRecorder recorder =
        cmd -> {
          recorded.add(cmd);
          return Mono.empty();
        };
    WebFilterChain chain = exchange -> exchange.getResponse().setComplete();
    CallHistoryWebFilter filter = new CallHistoryWebFilter(recorder, FIXED_IP_RESOLVER);

    MockServerWebExchange exchange =
        MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/call-history"));

    filter.filter(exchange, chain).block();

    assertThat(recorded).isEmpty();
  }

  @Test
  void recordsSuccessfulPostRequest() {
    List<RecordCallHistoryCommand> recorded = new CopyOnWriteArrayList<>();
    CallHistoryRecorder recorder =
        cmd -> {
          recorded.add(cmd);
          return Mono.empty();
        };
    WebFilterChain chain =
        exchange -> {
          exchange.getResponse().setStatusCode(HttpStatus.OK);
          return exchange
              .getRequest()
              .getBody()
              .then(
                  exchange
                      .getResponse()
                      .writeWith(
                          Mono.just(
                              exchange
                                  .getResponse()
                                  .bufferFactory()
                                  .wrap("{\"result\":11}".getBytes()))));
        };
    CallHistoryWebFilter filter = new CallHistoryWebFilter(recorder, FIXED_IP_RESOLVER);

    MockServerWebExchange exchange =
        MockServerWebExchange.from(
            MockServerHttpRequest.post("/api/v1/calculations")
                .header("X-Forwarded-For", "1.2.3.4")
                .body("{\"num1\":5,\"num2\":6}"));

    filter.filter(exchange, chain).block();

    assertThat(recorded).hasSize(1);
    RecordCallHistoryCommand cmd = recorded.get(0);
    assertThat(cmd.httpMethod()).isEqualTo("POST");
    assertThat(cmd.endpoint()).isEqualTo("/api/v1/calculations");
    assertThat(cmd.httpStatus()).isEqualTo(200);
    assertThat(cmd.success()).isTrue();
    assertThat(cmd.responseBody()).isEqualTo("{\"result\":11}");
    assertThat(cmd.errorBody()).isNull();
    assertThat(cmd.requestBody()).isEqualTo("{\"num1\":5,\"num2\":6}");
    assertThat(cmd.clientIp()).isEqualTo("10.0.0.1");
    assertThat(cmd.occurredAt()).isNotNull();
    assertThat(cmd.durationMs()).isNotNull().isNotNegative();
  }

  @Test
  void recordsFailedRequestWithErrorBody() {
    List<RecordCallHistoryCommand> recorded = new CopyOnWriteArrayList<>();
    CallHistoryRecorder recorder =
        cmd -> {
          recorded.add(cmd);
          return Mono.empty();
        };
    WebFilterChain chain =
        exchange -> {
          exchange.getResponse().setStatusCode(HttpStatus.BAD_REQUEST);
          return exchange
              .getResponse()
              .writeWith(
                  Mono.just(
                      exchange
                          .getResponse()
                          .bufferFactory()
                          .wrap("{\"status\":400,\"message\":\"bad\"}".getBytes())));
        };
    CallHistoryWebFilter filter = new CallHistoryWebFilter(recorder, FIXED_IP_RESOLVER);

    MockServerWebExchange exchange =
        MockServerWebExchange.from(
            MockServerHttpRequest.post("/api/v1/calculations").body("{\"num1\":-1}"));

    filter.filter(exchange, chain).block();

    assertThat(recorded).hasSize(1);
    RecordCallHistoryCommand cmd = recorded.get(0);
    assertThat(cmd.httpStatus()).isEqualTo(400);
    assertThat(cmd.success()).isFalse();
    assertThat(cmd.responseBody()).isNull();
    assertThat(cmd.errorBody()).isEqualTo("{\"status\":400,\"message\":\"bad\"}");
  }

  @Test
  void recordingFailureDoesNotBreakResponse() {
    CallHistoryRecorder recorder = cmd -> Mono.error(new RuntimeException("db down"));
    WebFilterChain chain =
        exchange -> {
          exchange.getResponse().setStatusCode(HttpStatus.OK);
          return exchange.getResponse().setComplete();
        };
    CallHistoryWebFilter filter = new CallHistoryWebFilter(recorder, FIXED_IP_RESOLVER);

    MockServerWebExchange exchange =
        MockServerWebExchange.from(
            MockServerHttpRequest.get("/api/v1/call-history?page=0&size=10"));

    filter.filter(exchange, chain).block();

    assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.OK);
  }

  @Test
  void resolvesClientIpFromForwardedHeader() {
    List<String> resolvedRequests = new CopyOnWriteArrayList<>();
    ClientIpResolver trackingResolver =
        request -> {
          resolvedRequests.add(
              request.getHeaders().getFirst("X-Forwarded-For"));
          return request.getHeaders().getFirst("X-Forwarded-For") != null
              ? request.getHeaders().getFirst("X-Forwarded-For")
              : "unknown";
        };
    List<RecordCallHistoryCommand> recorded = new CopyOnWriteArrayList<>();
    CallHistoryRecorder recorder =
        cmd -> {
          recorded.add(cmd);
          return Mono.empty();
        };
    WebFilterChain chain =
        exchange -> {
          exchange.getResponse().setStatusCode(HttpStatus.OK);
          return exchange.getResponse().setComplete();
        };
    CallHistoryWebFilter filter = new CallHistoryWebFilter(recorder, trackingResolver);

    MockServerWebExchange exchange =
        MockServerWebExchange.from(
            MockServerHttpRequest.get("/api/v1/calculations")
                .header("X-Forwarded-For", "203.0.113.1"));

    filter.filter(exchange, chain).block();

    assertThat(recorded).hasSize(1);
    assertThat(recorded.get(0).clientIp()).isEqualTo("203.0.113.1");
    assertThat(resolvedRequests).contains("203.0.113.1");
  }

  @Test
  void recordsGetRequestWithoutBody() {
    List<RecordCallHistoryCommand> recorded = new CopyOnWriteArrayList<>();
    CallHistoryRecorder recorder =
        cmd -> {
          recorded.add(cmd);
          return Mono.empty();
        };
    WebFilterChain chain =
        exchange -> {
          exchange.getResponse().setStatusCode(HttpStatus.OK);
          return exchange.getResponse().setComplete();
        };
    CallHistoryWebFilter filter = new CallHistoryWebFilter(recorder, FIXED_IP_RESOLVER);

    MockServerWebExchange exchange =
        MockServerWebExchange.from(
            MockServerHttpRequest.get("/api/v1/calculations?page=0&size=20"));

    filter.filter(exchange, chain).block();

    assertThat(recorded).hasSize(1);
    RecordCallHistoryCommand cmd = recorded.get(0);
    assertThat(cmd.httpMethod()).isEqualTo("GET");
    assertThat(cmd.endpoint()).isEqualTo("/api/v1/calculations");
    assertThat(cmd.queryParams()).isEqualTo("page=0&size=20");
    assertThat(cmd.requestBody()).isNull();
  }

  @Test
  void recordsDurationInMillis() {
    List<RecordCallHistoryCommand> recorded = new CopyOnWriteArrayList<>();
    CallHistoryRecorder recorder =
        cmd -> {
          recorded.add(cmd);
          return Mono.empty();
        };
    WebFilterChain chain =
        exchange -> {
          exchange.getResponse().setStatusCode(HttpStatus.OK);
          return exchange.getResponse().setComplete();
        };
    CallHistoryWebFilter filter = new CallHistoryWebFilter(recorder, FIXED_IP_RESOLVER);

    MockServerWebExchange exchange =
        MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/calculations"));

    filter.filter(exchange, chain).block();

    assertThat(recorded).hasSize(1);
    assertThat(recorded.get(0).durationMs()).isNotNull().isNotNegative();
  }
}
