package br.com.estudo.consorcio.controller;

import br.com.estudo.consorcio.domain.dto.AnaliseCreditoRequestDTO;
import br.com.estudo.consorcio.domain.dto.AnaliseCreditoResponseDTO;
import br.com.estudo.consorcio.domain.model.StatusAnalise;
import br.com.estudo.consorcio.service.AnaliseCreditoService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AnaliseCreditoController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({br.com.estudo.consorcio.config.SecurityConfigurations.class})
@WithMockUser(authorities = {"MANAGE_COTAS"})
class AnaliseCreditoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private ObjectMapper objectMapper = new ObjectMapper().registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

    @MockitoBean
    private AnaliseCreditoService service;

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
    @DisplayName("Deve avaliar análise de crédito com sucesso")
    void deveAvaliarAnalise() throws Exception {
        AnaliseCreditoRequestDTO req = new AnaliseCreditoRequestDTO(
                10L, new BigDecimal("15000.00"), true, "Imóvel caucionado"
        );
        AnaliseCreditoResponseDTO resp = new AnaliseCreditoResponseDTO(
                1L, 10L, new BigDecimal("15000.00"), true, StatusAnalise.APROVADA,
                LocalDate.now(), "Imóvel caucionado aprovado"
        );

        when(service.avaliarAnalise(any(AnaliseCreditoRequestDTO.class))).thenReturn(resp);

        mockMvc.perform(post("/api/analises-credito/avaliar")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("APROVADA"));
    }
}