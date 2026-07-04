package com.tenpo.challenge.infrastructure.callhistory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.tenpo.challenge.api.ratelimit.ClientIpResolver;
import com.tenpo.challenge.application.callhistory.RecordCallHistoryCommand;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class CallHistoryFilterTest {

  @Test
  void capturesSuccessfulFunctionalRequestAndResponse() throws Exception {
    AtomicReference<RecordCallHistoryCommand> captured = new AtomicReference<>();
    CallHistoryRecorder recorder = mock(CallHistoryRecorder.class);
    ClientIpResolver ipResolver = request -> request.getRemoteAddr();
    CallHistoryFilter filter = new CallHistoryFilter(recorder, ipResolver);
    MockHttpServletRequest request =
        CallHistoryFilterTestSupport.postCalculationRequest("{\"num1\":100,\"num2\":50}");
    request.setQueryString("source=test");
    request.setRemoteAddr("127.0.0.1");
    MockHttpServletResponse response =
        CallHistoryFilterTestSupport.doFilterWithJsonResponse(
            filter, request, "{\"result\":165}", 200);

    ArgumentCaptor<RecordCallHistoryCommand> captor =
        ArgumentCaptor.forClass(RecordCallHistoryCommand.class);
    verify(recorder).record(captor.capture());
    captured.set(captor.getValue());

    assertThat(response.getContentAsString()).isEqualTo("{\"result\":165}");
    assertThat(captured.get()).isNotNull();
    assertThat(captured.get().httpMethod()).isEqualTo("POST");
    assertThat(captured.get().endpoint()).isEqualTo("/api/v1/calculations");
    assertThat(captured.get().queryParams()).isEqualTo("source=test");
    assertThat(captured.get().requestBody()).isEqualTo("{\"num1\":100,\"num2\":50}");
    assertThat(captured.get().responseBody()).isEqualTo("{\"result\":165}");
    assertThat(captured.get().errorBody()).isNull();
    assertThat(captured.get().httpStatus()).isEqualTo(200);
    assertThat(captured.get().success()).isTrue();
    assertThat(captured.get().clientIp()).isEqualTo("127.0.0.1");
  }

  @Test
  void recordsFailureWithoutBreakingTheResponseBody() throws Exception {
    AtomicReference<RecordCallHistoryCommand> captured = new AtomicReference<>();
    CallHistoryRecorder recorder = mock(CallHistoryRecorder.class);
    ClientIpResolver ipResolver = request -> request.getRemoteAddr();
    CallHistoryFilter filter = new CallHistoryFilter(recorder, ipResolver);
    MockHttpServletRequest request =
        CallHistoryFilterTestSupport.postCalculationRequest("{\"num1\":100}");
    MockHttpServletResponse response =
        CallHistoryFilterTestSupport.doFilterWithJsonResponse(
            filter, request, "{\"status\":400,\"error\":\"Bad Request\"}", 400);

    ArgumentCaptor<RecordCallHistoryCommand> captor =
        ArgumentCaptor.forClass(RecordCallHistoryCommand.class);
    verify(recorder).record(captor.capture());
    captured.set(captor.getValue());

    assertThat(response.getContentAsString())
        .isEqualTo("{\"status\":400,\"error\":\"Bad Request\"}");
    assertThat(captured.get()).isNotNull();
    assertThat(captured.get().responseBody()).isNull();
    assertThat(captured.get().errorBody()).isEqualTo("{\"status\":400,\"error\":\"Bad Request\"}");
    assertThat(captured.get().success()).isFalse();
  }
}
