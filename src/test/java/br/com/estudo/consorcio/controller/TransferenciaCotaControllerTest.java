package br.com.estudo.consorcio.controller;

import br.com.estudo.consorcio.domain.dto.TransferenciaCotaRequestDTO;
import br.com.estudo.consorcio.domain.dto.TransferenciaCotaResponseDTO;
import br.com.estudo.consorcio.domain.enums.StatusTransferencia;
import br.com.estudo.consorcio.service.TransferenciaCotaService;
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
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TransferenciaCotaController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({br.com.estudo.consorcio.config.SecurityConfigurations.class})
@WithMockUser(authorities = {"MANAGE_COTAS", "VIEW_COTAS"})
class TransferenciaCotaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private ObjectMapper objectMapper = new ObjectMapper().registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

    @MockitoBean
    private TransferenciaCotaService service;

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
    @DisplayName("Deve solicitar transferência com sucesso")
    void deveSolicitarTransferencia() throws Exception {
        TransferenciaCotaRequestDTO req = new TransferenciaCotaRequestDTO(100L, 2L, new BigDecimal("3000.00"), "Transferência");
        TransferenciaCotaResponseDTO resp = new TransferenciaCotaResponseDTO(
                1L, 100L, "GRP-01", 25, 1L, "Carlos", "11122233344",
                2L, "Ana", "99988877766", LocalDateTime.now(), null,
                new BigDecimal("3000.00"), StatusTransferencia.EM_ANALISE_CREDITO, null, "Transferência"
        );

        when(service.solicitarTransferencia(any(TransferenciaCotaRequestDTO.class))).thenReturn(resp);

        mockMvc.perform(post("/api/transferencias")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("EM_ANALISE_CREDITO"));
    }

    @Test
    @DisplayName("Deve efetivar transferência com sucesso")
    void deveEfetivarTransferencia() throws Exception {
        TransferenciaCotaResponseDTO resp = new TransferenciaCotaResponseDTO(
                1L, 100L, "GRP-01", 25, 1L, "Carlos", "11122233344",
                2L, "Ana", "99988877766", LocalDateTime.now(), LocalDateTime.now(),
                new BigDecimal("3000.00"), StatusTransferencia.EFETIVADA, null, "Transferência"
        );

        when(service.efetivarTransferencia(1L)).thenReturn(resp);

        mockMvc.perform(put("/api/transferencias/1/efetivar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EFETIVADA"));
    }
}