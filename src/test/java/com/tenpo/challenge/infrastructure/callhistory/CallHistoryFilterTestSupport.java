package com.tenpo.challenge.infrastructure.callhistory;

import java.nio.charset.StandardCharsets;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

final class CallHistoryFilterTestSupport {

  private CallHistoryFilterTestSupport() {}

  static MockHttpServletRequest postCalculationRequest(String body) {
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/calculations");
    request.setContent(body.getBytes(StandardCharsets.UTF_8));
    request.setCharacterEncoding(StandardCharsets.UTF_8.name());
    return request;
  }

  static MockHttpServletResponse doFilterWithJsonResponse(
      CallHistoryFilter filter, MockHttpServletRequest request, String responseBody, int status)
      throws Exception {
    MockHttpServletResponse response = new MockHttpServletResponse();
    filter.doFilter(
        request,
        response,
        (servletRequest, servletResponse) -> {
          servletRequest.getInputStream().readAllBytes();
          jakarta.servlet.http.HttpServletResponse httpResponse =
              (jakarta.servlet.http.HttpServletResponse) servletResponse;
          httpResponse.setCharacterEncoding(StandardCharsets.UTF_8.name());
          httpResponse.getWriter().write(responseBody);
          httpResponse.setStatus(status);
        });
    return response;
  }
}
