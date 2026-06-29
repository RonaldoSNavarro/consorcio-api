package br.com.estudo.consorcio.controller;

import br.com.estudo.consorcio.config.SecurityConfigurations;
import br.com.estudo.consorcio.config.SecurityFilter;
import br.com.estudo.consorcio.domain.dto.CotaReembolsoResponseDTO;
import br.com.estudo.consorcio.domain.dto.CotaResponseDTO;
import br.com.estudo.consorcio.domain.model.StatusCota;
import br.com.estudo.consorcio.domain.model.Usuario;
import br.com.estudo.consorcio.domain.repository.UsuarioRepository;
import br.com.estudo.consorcio.service.ContemplacaoService;
import br.com.estudo.consorcio.service.CotaService;
import br.com.estudo.consorcio.service.ParcelaService;
import br.com.estudo.consorcio.service.TokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {ContemplacaoController.class, CotaController.class})
@AutoConfigureMockMvc
@Import({SecurityConfigurations.class, SecurityFilter.class})
class EndpointsSecurityControllerTest {

    @org.springframework.boot.test.context.TestConfiguration
    static class TestConfig {
        @org.springframework.context.annotation.Bean
        public org.springframework.boot.web.servlet.FilterRegistrationBean<SecurityFilter> registration(SecurityFilter filter) {
            org.springframework.boot.web.servlet.FilterRegistrationBean<SecurityFilter> registration = new org.springframework.boot.web.servlet.FilterRegistrationBean<>(filter);
            registration.setEnabled(false);
            return registration;
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SecurityFilter filterUnderTest;

    @MockitoBean
    private ContemplacaoService contemplacaoService;

    @MockitoBean
    private CotaService cotaService;

    @MockitoBean
    private ParcelaService parcelaService;

    @MockitoBean
    private br.com.estudo.consorcio.security.IntrusionDetectionService intrusionDetectionService;

    @MockitoBean
    private TokenService tokenService;

    @MockitoBean
    private UsuarioRepository usuarioRepository;

    @BeforeEach
    void setUpSecurityMocks() {
        // Mock token subject retrieval
        when(tokenService.getSubject("token-admin")).thenReturn("admin");
        when(tokenService.getSubject("token-gestor")).thenReturn("gestor");
        when(tokenService.getSubject("token-consorciado")).thenReturn("consorciado");

        // Mock users with different roles
        Usuario adminUser = new Usuario("admin", "senha");
        adminUser.setRole("ADMIN");

        Usuario gestorUser = new Usuario("gestor", "senha");
        gestorUser.setRole("GESTOR");

        Usuario consorciadoUser = new Usuario("consorciado", "senha");
        consorciadoUser.setRole("CONSORCIADO");

        when(usuarioRepository.findByLogin("admin")).thenReturn(adminUser);
        when(usuarioRepository.findByLogin("gestor")).thenReturn(gestorUser);
        when(usuarioRepository.findByLogin("consorciado")).thenReturn(consorciadoUser);
    }

    @Test
    void testMockTokenService() {
        org.junit.jupiter.api.Assertions.assertEquals("admin", tokenService.getSubject("token-admin"));
    }

    @Test
    void testFilterIntegration() throws Exception {
        org.springframework.mock.web.MockHttpServletRequest request = new org.springframework.mock.web.MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer token-admin");
        org.springframework.mock.web.MockHttpServletResponse response = new org.springframework.mock.web.MockHttpServletResponse();
        org.springframework.mock.web.MockFilterChain filterChain = new org.springframework.mock.web.MockFilterChain();

        filterUnderTest.doFilter(request, response, filterChain);

        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        org.junit.jupiter.api.Assertions.assertNotNull(auth, "Authentication should not be null!");
        org.junit.jupiter.api.Assertions.assertEquals("admin", auth.getName());
        System.out.println("DEBUG AUTHORITIES: " + auth.getAuthorities());
        org.junit.jupiter.api.Assertions.assertTrue(
            auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")),
            "Authorities should contain ROLE_ADMIN!"
        );
    }

    @Test
    @DisplayName("Confirmar Integralização - Autorizado para ADMIN")
    void confirmarIntegralizacaoDeveRetornarOkParaAdmin() throws Exception {
        CotaResponseDTO dto = new CotaResponseDTO(1L, 44, 2L, 3L, StatusCota.AGUARDANDO_ANALISE, 0);
        when(contemplacaoService.confirmarPagamentoLance(anyLong())).thenReturn(dto);

        mockMvc.perform(post("/api/contemplacoes/lances/1/integralizar")
                        .header("Authorization", "Bearer token-admin")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Confirmar Integralização - Autorizado para GESTOR")
    void confirmarIntegralizacaoDeveRetornarOkParaGestor() throws Exception {
        CotaResponseDTO dto = new CotaResponseDTO(1L, 44, 2L, 3L, StatusCota.AGUARDANDO_ANALISE, 0);
        when(contemplacaoService.confirmarPagamentoLance(anyLong())).thenReturn(dto);

        mockMvc.perform(post("/api/contemplacoes/lances/1/integralizar")
                        .header("Authorization", "Bearer token-gestor")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Confirmar Integralização - Negado para CONSORCIADO")
    void confirmarIntegralizacaoDeveRetornarForbiddenParaConsorciado() throws Exception {
        mockMvc.perform(post("/api/contemplacoes/lances/1/integralizar")
                        .header("Authorization", "Bearer token-consorciado")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Reembolsar Cota - Autorizado para ADMIN")
    void reembolsarCotaDeveRetornarOkParaAdmin() throws Exception {
        CotaReembolsoResponseDTO dto = new CotaReembolsoResponseDTO(1L, 44, new BigDecimal("2000.00"), new BigDecimal("200.00"), new BigDecimal("1800.00"), true);
        when(cotaService.reembolsarCota(anyLong())).thenReturn(dto);

        mockMvc.perform(post("/api/cotas/1/reembolsar")
                        .header("Authorization", "Bearer token-admin")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Reembolsar Cota - Autorizado para GESTOR")
    void reembolsarCotaDeveRetornarOkParaGestor() throws Exception {
        CotaReembolsoResponseDTO dto = new CotaReembolsoResponseDTO(1L, 44, new BigDecimal("2000.00"), new BigDecimal("200.00"), new BigDecimal("1800.00"), true);
        when(cotaService.reembolsarCota(anyLong())).thenReturn(dto);

        mockMvc.perform(post("/api/cotas/1/reembolsar")
                        .header("Authorization", "Bearer token-gestor")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Reembolsar Cota - Negado para CONSORCIADO")
    void reembolsarCotaDeveRetornarForbiddenParaConsorciado() throws Exception {
        mockMvc.perform(post("/api/cotas/1/reembolsar")
                        .header("Authorization", "Bearer token-consorciado")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }
}
