package br.com.estudo.consorcio.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IntrusionDetectionFilterTest {

    @Mock
    private IntrusionDetectionService intrusionDetectionService;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private IntrusionDetectionFilter filter;

    @Test
    @DisplayName("Deve permitir requisição normal sem marcar sessão como suspeita")
    void devePermitirRequisicaoNormal() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("192.168.1.10");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(intrusionDetectionService.isSuspicious("192.168.1.10")).thenReturn(false);

        filter.doFilterInternal(request, response, filterChain);

        assertNull(request.getAttribute("suspicious_session"));
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Deve marcar sessão como suspeita quando o IP for classificado como intrusor")
    void deveMarcarSessaoSuspeitaQuandoIpIntrusor() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("203.0.113.5");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(intrusionDetectionService.isSuspicious("203.0.113.5")).thenReturn(true);

        filter.doFilterInternal(request, response, filterChain);

        assertEquals(true, request.getAttribute("suspicious_session"));
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Deve extrair IP prioritariamente do cabeçalho X-Forwarded-For quando presente")
    void deveExtrairIpDoHeaderXForwardedFor() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.1");
        request.addHeader("X-Forwarded-For", "198.51.100.25, 10.0.0.1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(intrusionDetectionService.isSuspicious("198.51.100.25")).thenReturn(true);

        filter.doFilterInternal(request, response, filterChain);

        assertEquals(true, request.getAttribute("suspicious_session"));
        verify(intrusionDetectionService).isSuspicious("198.51.100.25");
        verify(filterChain).doFilter(request, response);
    }
}