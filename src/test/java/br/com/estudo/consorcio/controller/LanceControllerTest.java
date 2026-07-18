package br.com.estudo.consorcio.controller;

import br.com.estudo.consorcio.domain.dto.LanceRequestDTO;
import br.com.estudo.consorcio.domain.dto.LanceResponseDTO;
import br.com.estudo.consorcio.domain.model.ModalidadeLance;
import br.com.estudo.consorcio.domain.model.StatusApuracaoLance;
import br.com.estudo.consorcio.domain.model.TipoLance;
import br.com.estudo.consorcio.service.LanceService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LanceController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({br.com.estudo.consorcio.config.SecurityConfigurations.class})
class LanceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private ObjectMapper objectMapper = new ObjectMapper().registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

    @MockitoBean
    private LanceService lanceService;

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

    @MockitoBean
    private br.com.estudo.consorcio.service.ViaCepService viaCepService;

    @MockitoBean
    private br.com.estudo.consorcio.service.HistoricoConsorciadoService historicoService;

    @Test
    @DisplayName("Deve devolver 201 Created e o JSON do lance ao cadastrar com sucesso")
    void deveRetornar201AoCadastrarLance() throws Exception {
        // Arrange
        LanceRequestDTO request = new LanceRequestDTO(3L, 2L, TipoLance.FIRME, new BigDecimal("15000.00"), ModalidadeLance.LIVRE);
        
        LanceResponseDTO response = new LanceResponseDTO(
                1L, 
                3L, 
                2L, 
                TipoLance.FIRME, 
                new BigDecimal("15000.00"), 
                LocalDateTime.now(), 
                StatusApuracaoLance.CADASTRADO, 
                ModalidadeLance.LIVRE
        );

        when(lanceService.registrarLance(any(LanceRequestDTO.class))).thenReturn(response);
        String jsonRequest = objectMapper.writeValueAsString(request);

        // Act & Assert
        mockMvc.perform(post("/api/lances")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.cotaId").value(3L))
                .andExpect(jsonPath("$.assembleiaId").value(2L))
                .andExpect(jsonPath("$.valorOferta").value(15000.00))
                .andExpect(jsonPath("$.modalidade").value("LIVRE"));
    }
}
