package br.com.estudo.consorcio.security;

import br.com.estudo.consorcio.config.SecurityConfigurations;
import br.com.estudo.consorcio.config.SecurityFilter;
import br.com.estudo.consorcio.controller.AutenticacaoController;
import br.com.estudo.consorcio.domain.repository.UsuarioRepository;
import br.com.estudo.consorcio.service.MfaService;
import br.com.estudo.consorcio.service.SecurityAuditService;
import br.com.estudo.consorcio.service.TokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;

@WebMvcTest(AutenticacaoController.class)
@AutoConfigureMockMvc
@Import({SecurityConfigurations.class})
class CsrfSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthenticationManager authenticationManager;

    @MockitoBean
    private MfaService mfaService;

    @MockitoBean
    private SecurityFilter securityFilter;

    @MockitoBean
    private IntrusionDetectionFilter intrusionDetectionFilter;

    @MockitoBean
    private IntrusionDetectionService intrusionDetectionService;

    @MockitoBean
    private TokenService tokenService;

    @MockitoBean
    private SecurityAuditService securityAuditService;

    @MockitoBean
    private UsuarioRepository usuarioRepository;

    @BeforeEach
    void setUp() throws Exception {
        doAnswer(invocation -> {
            HttpServletRequest req = invocation.getArgument(0);
            HttpServletResponse res = invocation.getArgument(1);
            FilterChain chain = invocation.getArgument(2);
            chain.doFilter(req, res);
            return null;
        }).when(securityFilter).doFilter(any(), any(), any());

        doAnswer(invocation -> {
            HttpServletRequest req = invocation.getArgument(0);
            HttpServletResponse res = invocation.getArgument(1);
            FilterChain chain = invocation.getArgument(2);
            chain.doFilter(req, res);
            return null;
        }).when(intrusionDetectionFilter).doFilter(any(), any(), any());
    }

    @Test
    @DisplayName("CA-01: Deve emitir cookie XSRF-TOKEN em requisições GET para consumo do SPA")
    void deveEmitirCookieXsrfTokenEmRequisicaoGet() throws Exception {
        mockMvc.perform(get("/api/login/me"))
                .andExpect(cookie().exists("XSRF-TOKEN"));
    }
}