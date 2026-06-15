package com.pmrodrigues.commons.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.pmrodrigues.commons.interceptor.RequestIdInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@ExtendWith(MockitoExtension.class)
class AuditRevisionListenerTest {

    private final AuditRevisionListener listener = new AuditRevisionListener();

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        RequestContextHolder.resetRequestAttributes();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void newRevision_comUsuarioAutenticado_preencheUsername() {
        var auth = new UsernamePasswordAuthenticationToken("admin@test.com", null, java.util.List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);

        var rev = new CustomRevisionEntity();
        listener.newRevision(rev);

        assertThat(rev.getUsername()).isEqualTo("admin@test.com");
    }

    @Test
    void newRevision_semAutenticacao_naoPreencheUsername() {
        var rev = new CustomRevisionEntity();
        listener.newRevision(rev);

        assertThat(rev.getUsername()).isNull();
    }

    @Test
    void newRevision_comRequestId_preencheRequestId() {
        var request = new MockHttpServletRequest();
        request.addHeader(RequestIdInterceptor.REQUEST_ID_HEADER, "req-abc-123");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        var rev = new CustomRevisionEntity();
        listener.newRevision(rev);

        assertThat(rev.getRequestId()).isEqualTo("req-abc-123");
    }

    @Test
    void newRevision_semRequest_naoPreencheRequestId() {
        var rev = new CustomRevisionEntity();
        listener.newRevision(rev);

        assertThat(rev.getRequestId()).isNull();
    }

    @Test
    void newRevision_usuarioAnonimo_naoPreencheUsername() {
        var auth = new UsernamePasswordAuthenticationToken("anonymousUser", null, java.util.List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);

        var rev = new CustomRevisionEntity();
        listener.newRevision(rev);

        assertThat(rev.getUsername()).isNull();
    }
}
