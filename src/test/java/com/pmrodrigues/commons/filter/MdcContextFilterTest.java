package com.pmrodrigues.commons.filter;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MdcContextFilterTest {

    @Mock FilterChain chain;

    MdcContextFilter filter = new MdcContextFilter();

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    // ── correlation_id ────────────────────────────────────────────────────

    @Test
    void correlationId_whenHeaderPresent_usesHeaderValue() throws Exception {
        var req = new MockHttpServletRequest("GET", "/api/test");
        req.addHeader(MdcContextFilter.CORRELATION_ID_HEADER, "abc-123");
        var res = new MockHttpServletResponse();

        filter.doFilterInternal(req, res, chain);

        assertThat(res.getHeader(MdcContextFilter.CORRELATION_ID_HEADER)).isEqualTo("abc-123");
    }

    @Test
    void correlationId_whenHeaderAbsent_generatesUuid() throws Exception {
        var req = new MockHttpServletRequest("GET", "/api/test");
        var res = new MockHttpServletResponse();

        filter.doFilterInternal(req, res, chain);

        String cid = res.getHeader(MdcContextFilter.CORRELATION_ID_HEADER);
        assertThat(cid).isNotBlank();
        // must be a valid UUID
        assertThat(java.util.UUID.fromString(cid)).isNotNull();
    }

    // ── request_id ────────────────────────────────────────────────────────

    @Test
    void requestId_whenHeaderPresent_populatesMdc() throws Exception {
        var req = new MockHttpServletRequest("GET", "/api/test");
        req.addHeader(MdcContextFilter.REQUEST_ID_HEADER, "550e8400-e29b-41d4-a716-446655440000");
        var res = new MockHttpServletResponse();

        // capture MDC value during chain execution
        var captured = new String[1];
        filter.doFilterInternal(req, res, (rq, rs) -> captured[0] = MDC.get("request_id"));

        assertThat(captured[0]).isEqualTo("550e8400-e29b-41d4-a716-446655440000");
    }

    @Test
    void requestId_whenHeaderAbsent_notInMdc() throws Exception {
        var req = new MockHttpServletRequest("GET", "/api/test");
        var res = new MockHttpServletResponse();

        var captured = new String[1];
        filter.doFilterInternal(req, res, (rq, rs) -> captured[0] = MDC.get("request_id"));

        assertThat(captured[0]).isNull();
    }

    // ── idempotency_id ────────────────────────────────────────────────────

    @Test
    void idempotencyId_whenHeaderPresent_populatesMdc() throws Exception {
        var req = new MockHttpServletRequest("POST", "/api/orders");
        req.addHeader(MdcContextFilter.IDEMPOTENCY_KEY_HEADER, "idem-key-42");
        var res = new MockHttpServletResponse();

        var captured = new String[1];
        filter.doFilterInternal(req, res, (rq, rs) -> captured[0] = MDC.get("idempotency_id"));

        assertThat(captured[0]).isEqualTo("idem-key-42");
    }

    @Test
    void idempotencyId_whenHeaderAbsent_notInMdc() throws Exception {
        var req = new MockHttpServletRequest("POST", "/api/orders");
        var res = new MockHttpServletResponse();

        var captured = new String[1];
        filter.doFilterInternal(req, res, (rq, rs) -> captured[0] = MDC.get("idempotency_id"));

        assertThat(captured[0]).isNull();
    }

    // ── http_method / http_url / query_params ────────────────────────────

    @Test
    void httpMethodAndUrl_populatedInMdc() throws Exception {
        var req = new MockHttpServletRequest("DELETE", "/api/items/1");
        var res = new MockHttpServletResponse();

        var method = new String[1];
        var url    = new String[1];
        filter.doFilterInternal(req, res, (rq, rs) -> {
            method[0] = MDC.get("http_method");
            url[0]    = MDC.get("http_url");
        });

        assertThat(method[0]).isEqualTo("DELETE");
        assertThat(url[0]).isEqualTo("/api/items/1");
    }

    @Test
    void queryParams_whenPresent_populatedInMdc() throws Exception {
        var req = new MockHttpServletRequest("GET", "/api/items");
        req.setQueryString("page=0&size=20");
        var res = new MockHttpServletResponse();

        var captured = new String[1];
        filter.doFilterInternal(req, res, (rq, rs) -> captured[0] = MDC.get("query_params"));

        assertThat(captured[0]).isEqualTo("page=0&size=20");
    }

    @Test
    void queryParams_whenAbsent_notInMdc() throws Exception {
        var req = new MockHttpServletRequest("GET", "/api/items");
        var res = new MockHttpServletResponse();

        var captured = new String[1];
        filter.doFilterInternal(req, res, (rq, rs) -> captured[0] = MDC.get("query_params"));

        assertThat(captured[0]).isNull();
    }

    // ── MDC cleared after response ────────────────────────────────────────

    @Test
    void mdc_clearedAfterFilterChainCompletes() throws Exception {
        var req = new MockHttpServletRequest("GET", "/api/test");
        req.addHeader(MdcContextFilter.CORRELATION_ID_HEADER, "cid-xyz");
        var res = new MockHttpServletResponse();

        filter.doFilterInternal(req, res, chain);

        assertThat(MDC.get("correlation_id")).isNull();
        assertThat(MDC.get("request_id")).isNull();
    }

    // ── filter always runs (shouldNotFilter = false) ──────────────────────

    @Test
    void filterChain_delegatedForEveryRequest() throws Exception {
        var req = new MockHttpServletRequest("GET", "/api/anything");
        var res = new MockHttpServletResponse();

        filter.doFilterInternal(req, res, chain);

        verify(chain).doFilter(req, res);
    }
}