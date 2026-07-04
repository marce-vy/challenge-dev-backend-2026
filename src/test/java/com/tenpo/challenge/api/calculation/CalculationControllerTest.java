package com.tenpo.challenge.api.calculation;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tenpo.challenge.application.exception.PercentageProviderUnavailableException;
import com.tenpo.challenge.application.port.in.CalculateWithPercentageUseCase;
import com.tenpo.challenge.domain.CalculationInput;
import com.tenpo.challenge.domain.CalculationResult;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CalculationController.class)
class CalculationControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockBean private CalculateWithPercentageUseCase useCase;

  @Test
  void calculatesWithPercentage() throws Exception {
    CalculationInput input = new CalculationInput(new BigDecimal("100"), new BigDecimal("50"));
    CalculationResult result =
        new CalculationResult(
            input, new BigDecimal("150"), new BigDecimal("10"), new BigDecimal("165"));
    when(useCase.calculate(any(CalculationInput.class))).thenReturn(result);

    mockMvc
        .perform(
            post("/api/v1/calculations")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"num1\":100,\"num2\":50}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.num1").value(100))
        .andExpect(jsonPath("$.num2").value(50))
        .andExpect(jsonPath("$.sum").value(150))
        .andExpect(jsonPath("$.percentage").value(10))
        .andExpect(jsonPath("$.result").value(165));

    verify(useCase).calculate(new CalculationInput(new BigDecimal("100"), new BigDecimal("50")));
  }

  @Test
  void rejectsMissingNum1() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/calculations")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"num2\":50}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.error").value("Bad Request"))
        .andExpect(jsonPath("$.message").value("num1 is required"));
  }

  @Test
  void rejectsMissingNum2() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/calculations")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"num1\":100}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.error").value("Bad Request"))
        .andExpect(jsonPath("$.message").value("num2 is required"));
  }

  @Test
  void rejectsNullNum1() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/calculations")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"num1\":null,\"num2\":50}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.error").value("Bad Request"))
        .andExpect(jsonPath("$.message").value("num1 is required"));
  }

  @Test
  void rejectsNullNum2() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/calculations")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"num1\":100,\"num2\":null}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.error").value("Bad Request"))
        .andExpect(jsonPath("$.message").value("num2 is required"));
  }

  @Test
  void rejectsNonNumericNum1() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/calculations")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"num1\":\"abc\",\"num2\":50}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.error").value("Bad Request"))
        .andExpect(jsonPath("$.message").value("Request body is invalid"));
  }

  @Test
  void rejectsZeroNum1() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/calculations")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"num1\":0,\"num2\":50}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.error").value("Bad Request"))
        .andExpect(jsonPath("$.message").value("num1 must be greater than zero"));
  }

  @Test
  void rejectsNegativeNum2() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/calculations")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"num1\":100,\"num2\":-1}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.error").value("Bad Request"))
        .andExpect(jsonPath("$.message").value("num2 must be greater than zero"));
  }

  @Test
  void rejectsMalformedJson() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/calculations")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"num1\":100,\"num2\":"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.error").value("Bad Request"))
        .andExpect(jsonPath("$.message").value("Request body is invalid"));
  }

  @Test
  void rejectsEmptyBody() throws Exception {
    mockMvc
        .perform(post("/api/v1/calculations").contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.error").value("Bad Request"))
        .andExpect(jsonPath("$.message").value("Request body is invalid"));
  }

  @Test
  void returnsServiceUnavailableWhenProviderIsExhausted() throws Exception {
    when(useCase.calculate(any(CalculationInput.class)))
        .thenThrow(
            new PercentageProviderUnavailableException("Percentage provider is unavailable", null));

    mockMvc
        .perform(
            post("/api/v1/calculations")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"num1\":100,\"num2\":50}"))
        .andExpect(status().isServiceUnavailable())
        .andExpect(jsonPath("$.status").value(503))
        .andExpect(jsonPath("$.error").value("Service Unavailable"))
        .andExpect(jsonPath("$.message").value("Percentage provider is unavailable"));
  }

  @Test
  void returnsInternalServerErrorForUnexpectedFailures() throws Exception {
    when(useCase.calculate(any(CalculationInput.class)))
        .thenThrow(new IllegalStateException("database password leaked"));

    mockMvc
        .perform(
            post("/api/v1/calculations")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"num1\":100,\"num2\":50}"))
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.status").value(500))
        .andExpect(jsonPath("$.error").value("Internal Server Error"))
        .andExpect(jsonPath("$.message").value("Unexpected server error"))
        .andExpect(jsonPath("$.trace").doesNotExist())
        .andExpect(jsonPath("$.exception").doesNotExist())
        .andExpect(jsonPath("$.errors").doesNotExist())
        .andExpect(content().string(not(containsString("database password leaked"))));
  }
}
