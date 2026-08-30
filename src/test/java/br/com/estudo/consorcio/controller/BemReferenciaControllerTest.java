package br.com.estudo.consorcio.controller;

import br.com.estudo.consorcio.domain.dto.BemReferenciaResponseDTO;
import br.com.estudo.consorcio.service.BemReferenciaService;
import br.com.estudo.consorcio.service.FipeService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BemReferenciaController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({br.com.estudo.consorcio.config.SecurityConfigurations.class})
@WithMockUser(authorities = {"VIEW_GRUPOS", "MANAGE_GRUPOS"})
class BemReferenciaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BemReferenciaService service;

    @MockitoBean
    private FipeService fipeService;

    @MockitoBean
    private br.com.estudo.consorcio.security.IntrusionDetectionService intrusionDetectionService;

    @MockitoBean
    private br.com.estudo.consorcio.config.SecurityFilter securityFilter;

    @MockitoBean
    private br.com.estudo.consorcio.security.IntrusionDetectionFilter intrusionDetectionFilter;

    @MockitoBean
    private br.com.estudo.consorcio.service.TokenService tokenService;

    @MockitoBean
    private br.com.estudo.consorcio.service.SecurityAuditService securityAuditService;

    @Test
    @DisplayName("Deve listar todos os bens ativos")
    void deveListarTodosAtivos() throws Exception {
        BemReferenciaResponseDTO dto = new BemReferenciaResponseDTO(
                1L, 1L, "Automóveis", "VEICULO_AUTOMOTOR", "INCC",
                "Honda Civic", new BigDecimal("180000.00"), LocalDate.now(), "004380-0", true
        );

        when(service.listarTodosAtivos()).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/bens-referencia/todos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].descricao").value("Honda Civic"));
    }

    @Test
    @DisplayName("Deve obter bem por ID")
    void deveObterPorId() throws Exception {
        BemReferenciaResponseDTO dto = new BemReferenciaResponseDTO(
                1L, 1L, "Automóveis", "VEICULO_AUTOMOTOR", "INCC",
                "Honda Civic", new BigDecimal("180000.00"), LocalDate.now(), "004380-0", true
        );

        when(service.obterPorId(1L)).thenReturn(dto);

        mockMvc.perform(get("/api/bens-referencia/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.descricao").value("Honda Civic"));
    }
}