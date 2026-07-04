package com.tenpo.challenge.api.callhistory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.tenpo.challenge.application.port.out.CallHistoryRecorder;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class CallHistoryFilterExclusionTest {

  static Stream<Arguments> excludedPaths() {
    return Stream.of(
        Arguments.of("/swagger-ui/index.html"),
        Arguments.of("/swagger-ui/swagger-initializer.js"),
        Arguments.of("/v3/api-docs"),
        Arguments.of("/v3/api-docs/swagger-config"),
        Arguments.of("/api/v1/call-history"),
        Arguments.of("/actuator/health"),
        Arguments.of("/favicon.ico"));
  }

  @ParameterizedTest
  @MethodSource("excludedPaths")
  void skipsRecordingForExcludedPaths(String path) throws Exception {
    CallHistoryRecorder recorder = mock(CallHistoryRecorder.class);
    CallHistoryFilter filter = new CallHistoryFilter(recorder, request -> request.getRemoteAddr());
    MockHttpServletRequest request = new MockHttpServletRequest("GET", path);
    request.setCharacterEncoding(StandardCharsets.UTF_8.name());
    MockHttpServletResponse response = new MockHttpServletResponse();

    FilterChain chain =
        (servletRequest, servletResponse) -> {
          HttpServletResponse httpResponse = (HttpServletResponse) servletResponse;
          httpResponse.setStatus(200);
        };
    filter.doFilter(request, response, chain);

    verify(recorder, never()).record(org.mockito.ArgumentMatchers.any());
    assertThat(response.getStatus()).isEqualTo(200);
  }
}
