package com.tenpo.challenge.api.callhistory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.tenpo.challenge.application.callhistory.RecordCallHistoryCommand;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class CallHistoryFilterErrorTest {

  @Test
  void capturesServerErrorWithErrorBody() throws Exception {
    CallHistoryRecorder recorder = mock(CallHistoryRecorder.class);
    CallHistoryFilter filter = new CallHistoryFilter(recorder, request -> request.getRemoteAddr());
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/calculations");
    request.setCharacterEncoding(StandardCharsets.UTF_8.name());
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilter(
        request,
        response,
        (servletRequest, servletResponse) -> {
          servletRequest.getInputStream().readAllBytes();
          jakarta.servlet.http.HttpServletResponse httpResponse =
              (jakarta.servlet.http.HttpServletResponse) servletResponse;
          httpResponse.setCharacterEncoding(StandardCharsets.UTF_8.name());
          httpResponse.getWriter().write("{\"status\":500,\"error\":\"Internal Server Error\"}");
          httpResponse.setStatus(500);
        });

    ArgumentCaptor<RecordCallHistoryCommand> captor =
        ArgumentCaptor.forClass(RecordCallHistoryCommand.class);
    verify(recorder).record(captor.capture());

    assertThat(response.getContentAsString())
        .isEqualTo("{\"status\":500,\"error\":\"Internal Server Error\"}");
    assertThat(captor.getValue().responseBody()).isNull();
    assertThat(captor.getValue().errorBody())
        .isEqualTo("{\"status\":500,\"error\":\"Internal Server Error\"}");
    assertThat(captor.getValue().httpStatus()).isEqualTo(500);
    assertThat(captor.getValue().success()).isFalse();
  }

  @Test
  void classifiesRedirectAsSuccess() throws Exception {
    CallHistoryRecorder recorder = mock(CallHistoryRecorder.class);
    CallHistoryFilter filter = new CallHistoryFilter(recorder, request -> request.getRemoteAddr());
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/redirect");
    request.setCharacterEncoding(StandardCharsets.UTF_8.name());
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilter(
        request,
        response,
        (servletRequest, servletResponse) -> {
          servletRequest.getInputStream().readAllBytes();
          jakarta.servlet.http.HttpServletResponse httpResponse =
              (jakarta.servlet.http.HttpServletResponse) servletResponse;
          httpResponse.setCharacterEncoding(StandardCharsets.UTF_8.name());
          httpResponse.getWriter().write("moved");
          httpResponse.setStatus(302);
        });

    ArgumentCaptor<RecordCallHistoryCommand> captor =
        ArgumentCaptor.forClass(RecordCallHistoryCommand.class);
    verify(recorder).record(captor.capture());

    assertThat(captor.getValue().success()).isTrue();
    assertThat(captor.getValue().responseBody()).isEqualTo("moved");
    assertThat(captor.getValue().errorBody()).isNull();
    assertThat(captor.getValue().httpStatus()).isEqualTo(302);
  }

  @Test
  void capturesHttpStatusWithNoResponseBodyAsSuccess() throws Exception {
    CallHistoryRecorder recorder = mock(CallHistoryRecorder.class);
    CallHistoryFilter filter = new CallHistoryFilter(recorder, request -> request.getRemoteAddr());
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/calculations");
    request.setCharacterEncoding(StandardCharsets.UTF_8.name());
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilter(
        request,
        response,
        (servletRequest, servletResponse) -> {
          servletRequest.getInputStream().readAllBytes();
          jakarta.servlet.http.HttpServletResponse httpResponse =
              (jakarta.servlet.http.HttpServletResponse) servletResponse;
          httpResponse.setStatus(204);
        });

    ArgumentCaptor<RecordCallHistoryCommand> captor =
        ArgumentCaptor.forClass(RecordCallHistoryCommand.class);
    verify(recorder).record(captor.capture());

    assertThat(captor.getValue().httpStatus()).isEqualTo(204);
    assertThat(captor.getValue().success()).isTrue();
    assertThat(captor.getValue().responseBody()).isNull();
    assertThat(captor.getValue().errorBody()).isNull();
  }
}
