package com.tenpo.challenge.api.callhistory;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tenpo.challenge.application.callhistory.CallHistoryEntry;
import com.tenpo.challenge.application.callhistory.CallHistoryPage;
import com.tenpo.challenge.application.callhistory.PaginationRequest;
import com.tenpo.challenge.application.port.in.GetCallHistoryUseCase;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CallHistoryController.class)
class CallHistoryControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockBean private GetCallHistoryUseCase useCase;

  @Test
  void returnsDefaultFirstPage() throws Exception {
    UUID entryId = UUID.fromString("9a5820f3-2b43-4f3f-b8ef-12a81d65c921");
    CallHistoryEntry entry =
        new CallHistoryEntry(
            entryId,
            Instant.parse("2026-07-01T22:15:30.123Z"),
            "POST",
            "/api/v1/calculations",
            null,
            "{\"num1\":5,\"num2\":5}",
            "{\"result\":11}",
            null,
            200,
            true,
            42L,
            "127.0.0.1");
    when(useCase.get(new PaginationRequest(0, 20)))
        .thenReturn(new CallHistoryPage(List.of(entry), 0, 20, 1, 1, false, false));

    mockMvc
        .perform(get("/api/v1/call-history"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].id").value(entryId.toString()))
        .andExpect(jsonPath("$.content[0].occurredAt").value("2026-07-01T22:15:30.123Z"))
        .andExpect(jsonPath("$.content[0].httpMethod").value("POST"))
        .andExpect(jsonPath("$.content[0].endpoint").value("/api/v1/calculations"))
        .andExpect(jsonPath("$.content[0].queryParams").value(nullValue()))
        .andExpect(jsonPath("$.content[0].requestBody").value("{\"num1\":5,\"num2\":5}"))
        .andExpect(jsonPath("$.content[0].responseBody").value("{\"result\":11}"))
        .andExpect(jsonPath("$.content[0].errorBody").value(nullValue()))
        .andExpect(jsonPath("$.content[0].httpStatus").value(200))
        .andExpect(jsonPath("$.content[0].success").value(true))
        .andExpect(jsonPath("$.content[0].durationMs").value(42))
        .andExpect(jsonPath("$.content[0].clientIp").value("127.0.0.1"))
        .andExpect(jsonPath("$.page").value(0))
        .andExpect(jsonPath("$.size").value(20))
        .andExpect(jsonPath("$.totalElements").value(1))
        .andExpect(jsonPath("$.totalPages").value(1))
        .andExpect(jsonPath("$.hasNext").value(false))
        .andExpect(jsonPath("$.hasPrevious").value(false));

    verify(useCase).get(new PaginationRequest(0, 20));
  }
}
