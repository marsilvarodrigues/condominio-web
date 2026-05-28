package com.pmrodrigues.security.controller;

import com.pmrodrigues.commons.service.MeterService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AuthExceptionHandlerTest {

    private static final String METRIC_NAME = "auth.errors";
    private static final String DESCRIPTION = "Authentication and authorization error occurrences";

    record ValidBody(@NotBlank String field) {}

    @RestController
    @RequestMapping("/test")
    static class StubController {

        RuntimeException toThrow;

        @GetMapping("/throw")
        void triggerException() {
            throw toThrow;
        }

        @PostMapping("/validate")
        void triggerValidation(@Valid @RequestBody ValidBody body) {}
    }

    @Mock MeterService meterService;

    StubController stubController = new StubController();
    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        var validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders
                .standaloneSetup(stubController)
                .setControllerAdvice(new AuthExceptionHandler(meterService))
                .setValidator(validator)
                .build();
    }

    @Test
    void handleBadCredentials_returns401WithInvalidCredentials() throws Exception {
        stubController.toThrow = new BadCredentialsException("bad");

        mockMvc.perform(get("/test/throw"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.data.error").value("invalid_credentials"))
                .andExpect(jsonPath("$.data.message").value("Invalid email or password"))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(meterService).incrementError(METRIC_NAME, "invalid_credentials", DESCRIPTION, 401);
    }

    @Test
    void handleDisabledException_returns401WithAccountDisabledMessage() throws Exception {
        stubController.toThrow = new DisabledException("disabled");

        mockMvc.perform(get("/test/throw"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.data.error").value("account_unavailable"))
                .andExpect(jsonPath("$.data.message").value("Account is disabled"));

        verify(meterService).incrementError(METRIC_NAME, "account_unavailable", DESCRIPTION, 401);
    }

    @Test
    void handleLockedException_returns401WithAccountLockedMessage() throws Exception {
        stubController.toThrow = new LockedException("locked");

        mockMvc.perform(get("/test/throw"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.data.error").value("account_unavailable"))
                .andExpect(jsonPath("$.data.message").value("Account is locked"));

        verify(meterService).incrementError(METRIC_NAME, "account_unavailable", DESCRIPTION, 401);
    }

    @Test
    void handleGenericAuthenticationException_returns401WithGenericMessage() throws Exception {
        stubController.toThrow = new AuthenticationException("generic") {};

        mockMvc.perform(get("/test/throw"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.data.error").value("authentication_failed"))
                .andExpect(jsonPath("$.data.message").value("Authentication failed"));

        verify(meterService).incrementError(METRIC_NAME, "authentication_failed", DESCRIPTION, 401);
    }

    @Test
    void handleResponseStatusException_returnsExceptionStatusAndReason() throws Exception {
        stubController.toThrow = new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");

        mockMvc.perform(get("/test/throw"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.data.error").value("error"))
                .andExpect(jsonPath("$.data.message").value("Access denied"))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(meterService).incrementError(METRIC_NAME, "response_status_error", DESCRIPTION, 403);
    }

    @Test
    void handleValidation_returns400WithFieldErrors() throws Exception {
        mockMvc.perform(post("/test/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"field\": \"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.error").value("validation_error"))
                .andExpect(jsonPath("$.data.message").value("Invalid request fields"))
                .andExpect(jsonPath("$.data.fields.field").exists())
                .andExpect(jsonPath("$.timestamp").exists());

        verify(meterService).incrementError(METRIC_NAME, "validation_error", DESCRIPTION, 400);
    }
}