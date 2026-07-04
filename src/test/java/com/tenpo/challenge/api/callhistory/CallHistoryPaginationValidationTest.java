package com.tenpo.challenge.api.callhistory;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tenpo.challenge.application.port.in.GetCallHistoryUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CallHistoryController.class)
class CallHistoryPaginationValidationTest {

  @Autowired private MockMvc mockMvc;

  @MockBean private GetCallHistoryUseCase useCase;

  @Test
  void rejectsNegativePageBeforeUseCaseIsReached() throws Exception {
    mockMvc
        .perform(get("/api/v1/call-history").param("page", "-1"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.error").value("Bad Request"))
        .andExpect(jsonPath("$.message").value("page must be greater than or equal to 0"));

    verifyNoInteractions(useCase);
  }

  @Test
  void rejectsSizeBelowMinimumBeforeUseCaseIsReached() throws Exception {
    mockMvc
        .perform(get("/api/v1/call-history").param("size", "0"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.error").value("Bad Request"))
        .andExpect(jsonPath("$.message").value("size must be between 1 and 100"));

    verifyNoInteractions(useCase);
  }

  @Test
  void rejectsSizeAboveMaximumBeforeUseCaseIsReached() throws Exception {
    mockMvc
        .perform(get("/api/v1/call-history").param("size", "101"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.error").value("Bad Request"))
        .andExpect(jsonPath("$.message").value("size must be between 1 and 100"));

    verifyNoInteractions(useCase);
  }

  @Test
  void rejectsNonNumericPageBeforeUseCaseIsReached() throws Exception {
    mockMvc
        .perform(get("/api/v1/call-history").param("page", "abc"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.error").value("Bad Request"))
        .andExpect(jsonPath("$.message").value("Request parameter is invalid"));

    verifyNoInteractions(useCase);
  }

  @Test
  void rejectsNonNumericSizeBeforeUseCaseIsReached() throws Exception {
    mockMvc
        .perform(get("/api/v1/call-history").param("size", "abc"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.error").value("Bad Request"))
        .andExpect(jsonPath("$.message").value("Request parameter is invalid"));

    verifyNoInteractions(useCase);
  }
}
