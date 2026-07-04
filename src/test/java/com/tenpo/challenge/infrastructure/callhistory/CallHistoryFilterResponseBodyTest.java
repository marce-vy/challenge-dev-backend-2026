package com.tenpo.challenge.infrastructure.callhistory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class CallHistoryFilterResponseBodyTest {

  @Test
  void preservesTheOriginalResponseBodyAfterRecording() throws Exception {
    CallHistoryRecorder recorder = Mockito.mock(CallHistoryRecorder.class);
    CallHistoryFilter filter = new CallHistoryFilter(recorder, (addr, forwarded) -> addr);
    MockHttpServletRequest request =
        CallHistoryFilterTestSupport.postCalculationRequest("{\"num1\":100,\"num2\":50}");
    MockHttpServletResponse response =
        CallHistoryFilterTestSupport.doFilterWithJsonResponse(
            filter, request, "{\"num1\":100,\"num2\":50,\"sum\":150}", 200);

    assertThat(response.getContentAsString()).isEqualTo("{\"num1\":100,\"num2\":50,\"sum\":150}");
  }

  @Test
  void preservesSuccessfulResponseWhenRecorderThrows() throws Exception {
    CallHistoryRecorder recorder = mock(CallHistoryRecorder.class);
    doThrow(new RuntimeException("persistence unavailable"))
        .when(recorder)
        .record(org.mockito.ArgumentMatchers.any());
    CallHistoryFilter filter = new CallHistoryFilter(recorder, (addr, forwarded) -> addr);
    MockHttpServletRequest request =
        CallHistoryFilterTestSupport.postCalculationRequest("{\"num1\":100,\"num2\":50}");
    MockHttpServletResponse response =
        CallHistoryFilterTestSupport.doFilterWithJsonResponse(
            filter, request, "{\"result\":165}", 200);

    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(response.getContentAsString()).isEqualTo("{\"result\":165}");
  }

  @Test
  void preservesErrorResponseWhenRecorderThrows() throws Exception {
    CallHistoryRecorder recorder = mock(CallHistoryRecorder.class);
    doThrow(new RuntimeException("persistence unavailable"))
        .when(recorder)
        .record(org.mockito.ArgumentMatchers.any());
    CallHistoryFilter filter = new CallHistoryFilter(recorder, (addr, forwarded) -> addr);
    MockHttpServletRequest request =
        CallHistoryFilterTestSupport.postCalculationRequest("{\"num1\":100}");
    MockHttpServletResponse response =
        CallHistoryFilterTestSupport.doFilterWithJsonResponse(
            filter, request, "{\"status\":400,\"error\":\"Bad Request\"}", 400);

    assertThat(response.getStatus()).isEqualTo(400);
    assertThat(response.getContentAsString())
        .isEqualTo("{\"status\":400,\"error\":\"Bad Request\"}");
  }
}
