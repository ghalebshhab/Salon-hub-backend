package com.salon.backend.Config;

import com.salon.backend.DTOs.ApiResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ApiExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ProbeController())
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    @Test
    void businessResponsesAreUntouched() throws Exception {
        mockMvc.perform(get("/probe/ok"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("fine"));
    }

    @Test
    void malformedJsonReturnsBadRequestEnvelope() throws Exception {
        assertEnvelope(mockMvc.perform(post("/probe/body")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{not json")), 400);
    }

    @Test
    void wrongParameterTypeReturnsBadRequestEnvelope() throws Exception {
        assertEnvelope(mockMvc.perform(get("/probe/number").param("value", "abc")), 400);
    }

    @Test
    void missingParameterReturnsBadRequestEnvelope() throws Exception {
        assertEnvelope(mockMvc.perform(get("/probe/number")), 400);
    }

    @Test
    void unsupportedMethodKeepsStatusAndAllowHeader() throws Exception {
        assertEnvelope(mockMvc.perform(delete("/probe/ok")), 405)
                .andExpect(header().string(HttpHeaders.ALLOW, "GET"));
    }

    @Test
    void unsupportedMediaTypeReturnsEnvelope() throws Exception {
        assertEnvelope(mockMvc.perform(post("/probe/body")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("text")), 415);
    }

    @Test
    void oversizedUploadReturnsPayloadTooLargeEnvelope() throws Exception {
        assertEnvelope(mockMvc.perform(get("/probe/too-large")), 413);
    }

    @Test
    void unexpectedFailureReturnsGenericEnvelopeWithoutInternalDetails() throws Exception {
        assertEnvelope(mockMvc.perform(get("/probe/boom")), 500)
                .andExpect(jsonPath("$.message").value(ApiExceptionHandler.INTERNAL_ERROR_MESSAGE));
    }

    @Test
    void securityExceptionsAreLeftForSpringSecurity() {
        assertThatThrownBy(() -> mockMvc.perform(get("/probe/denied")))
                .hasRootCauseInstanceOf(AccessDeniedException.class);
    }

    @Test
    void everyMappedStatusHasReadableText() {
        for (int code : new int[]{400, 404, 405, 406, 413, 415, 409, 500, 503}) {
            assertThat(ApiExceptionHandler.messageFor(org.springframework.http.HttpStatusCode.valueOf(code)))
                    .isNotBlank();
        }
    }

    private ResultActions assertEnvelope(ResultActions result, int expectedStatus) throws Exception {
        return result.andExpect(status().is(expectedStatus))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").isNotEmpty())
                .andExpect(jsonPath("$.data").value(nullValue()));
    }

    @RestController
    static class ProbeController {
        @GetMapping("/probe/ok")
        ApiResponse<Void> ok() {
            return ApiResponse.success("fine", null);
        }

        @PostMapping(value = "/probe/body", consumes = MediaType.APPLICATION_JSON_VALUE)
        ApiResponse<Map<String, Object>> body(@RequestBody Map<String, Object> body) {
            return ApiResponse.success("read", body);
        }

        @GetMapping("/probe/number")
        ApiResponse<Long> number(@RequestParam Long value) {
            return ApiResponse.success("number", value);
        }

        @GetMapping("/probe/too-large")
        ApiResponse<Void> tooLarge() {
            throw new MaxUploadSizeExceededException(1L);
        }

        @GetMapping("/probe/boom")
        ApiResponse<Void> boom() {
            throw new IllegalStateException("internal detail that must not leak");
        }

        @GetMapping("/probe/denied")
        ApiResponse<Void> denied() {
            throw new AccessDeniedException("denied");
        }
    }
}
