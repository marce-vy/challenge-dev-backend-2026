package com.tenpo.challenge.api.callhistory;

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
class CallHistoryPaginationControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockBean private GetCallHistoryUseCase useCase;

  @Test
  void forwardsExplicitPaginationAndRendersMetadata() throws Exception {
    UUID entryId = UUID.fromString("9a5820f3-2b43-4f3f-b8ef-12a81d65c922");
    CallHistoryEntry entry =
        new CallHistoryEntry(
            entryId,
            Instant.parse("2026-07-01T22:10:30.123Z"),
            "GET",
            "/api/v1/calculations",
            "page=1&size=5",
            null,
            "{\"result\":11}",
            null,
            200,
            true,
            37L,
            "127.0.0.1");
    when(useCase.get(new PaginationRequest(1, 5)))
        .thenReturn(new CallHistoryPage(List.of(entry), 1, 5, 12, 3, true, true));

    mockMvc
        .perform(get("/api/v1/call-history").param("page", "1").param("size", "5"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].id").value(entryId.toString()))
        .andExpect(jsonPath("$.content[0].queryParams").value("page=1&size=5"))
        .andExpect(jsonPath("$.page").value(1))
        .andExpect(jsonPath("$.size").value(5))
        .andExpect(jsonPath("$.totalElements").value(12))
        .andExpect(jsonPath("$.totalPages").value(3))
        .andExpect(jsonPath("$.hasNext").value(true))
        .andExpect(jsonPath("$.hasPrevious").value(true));

    verify(useCase).get(new PaginationRequest(1, 5));
  }

  @Test
  void rendersEmptyPageWithStableShape() throws Exception {
    when(useCase.get(new PaginationRequest(2, 5)))
        .thenReturn(new CallHistoryPage(List.of(), 2, 5, 0, 0, false, true));

    mockMvc
        .perform(get("/api/v1/call-history").param("page", "2").param("size", "5"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isArray())
        .andExpect(jsonPath("$.content").isEmpty())
        .andExpect(jsonPath("$.page").value(2))
        .andExpect(jsonPath("$.size").value(5))
        .andExpect(jsonPath("$.totalElements").value(0))
        .andExpect(jsonPath("$.totalPages").value(0))
        .andExpect(jsonPath("$.hasNext").value(false))
        .andExpect(jsonPath("$.hasPrevious").value(true));

    verify(useCase).get(new PaginationRequest(2, 5));
  }
}
