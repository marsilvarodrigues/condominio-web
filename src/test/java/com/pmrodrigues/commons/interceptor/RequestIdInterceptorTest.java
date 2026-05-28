package com.pmrodrigues.commons.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

class RequestIdInterceptorTest {

    ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json().build();
    RequestIdInterceptor interceptor;

    @BeforeEach
    void setUp() {
        interceptor = new RequestIdInterceptor(objectMapper);
    }

    @Test
    void preHandle_withValidUUID_setsAttributeAndReturnsTrue() throws Exception {
        var requestId = UUID.randomUUID();
        var request   = new MockHttpServletRequest();
        request.addHeader(RequestIdInterceptor.REQUEST_ID_HEADER, requestId.toString());
        var response = new MockHttpServletResponse();

        boolean result = interceptor.preHandle(request, response, new Object());

        assertThat(result).isTrue();
        assertThat(request.getAttribute(RequestIdInterceptor.REQUEST_ID_ATTRIBUTE))
                .isEqualTo(requestId);
    }

    @Test
    void preHandle_withMissingHeader_returnsFalseAnd400() throws Exception {
        var request  = new MockHttpServletRequest();
        var response = new MockHttpServletResponse();

        boolean result = interceptor.preHandle(request, response, new Object());

        assertThat(result).isFalse();
        assertThat(response.getStatus()).isEqualTo(BAD_REQUEST.value());
        assertThat(response.getContentType()).isEqualTo(MediaType.APPLICATION_JSON_VALUE);

        var body = objectMapper.readTree(response.getContentAsString());
        assertThat(body.at("/data/error").asText()).isEqualTo("missing_request_id");
    }

    @Test
    void preHandle_withBlankHeader_returnsFalseAnd400() throws Exception {
        var request = new MockHttpServletRequest();
        request.addHeader(RequestIdInterceptor.REQUEST_ID_HEADER, "   ");
        var response = new MockHttpServletResponse();

        boolean result = interceptor.preHandle(request, response, new Object());

        assertThat(result).isFalse();
        assertThat(response.getStatus()).isEqualTo(BAD_REQUEST.value());

        var body = objectMapper.readTree(response.getContentAsString());
        assertThat(body.at("/data/error").asText()).isEqualTo("missing_request_id");
    }

    @Test
    void preHandle_withNonUUIDHeader_returnsFalseAnd400() throws Exception {
        var request = new MockHttpServletRequest();
        request.addHeader(RequestIdInterceptor.REQUEST_ID_HEADER, "not-a-uuid");
        var response = new MockHttpServletResponse();

        boolean result = interceptor.preHandle(request, response, new Object());

        assertThat(result).isFalse();
        assertThat(response.getStatus()).isEqualTo(BAD_REQUEST.value());

        var body = objectMapper.readTree(response.getContentAsString());
        assertThat(body.at("/data/error").asText()).isEqualTo("invalid_request_id");
    }
}