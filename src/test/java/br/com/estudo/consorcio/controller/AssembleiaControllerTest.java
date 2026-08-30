package br.com.estudo.consorcio.controller;

import br.com.estudo.consorcio.domain.dto.AssembleiaRequestDTO;
import br.com.estudo.consorcio.domain.dto.AssembleiaResponseDTO;
import br.com.estudo.consorcio.domain.model.StatusAssembleia;
import br.com.estudo.consorcio.domain.model.TipoAssembleia;
import br.com.estudo.consorcio.service.AssembleiaService;
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

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AssembleiaController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({br.com.estudo.consorcio.config.SecurityConfigurations.class})
@WithMockUser(authorities = {"MANAGE_GRUPOS", "VIEW_GRUPOS"})
class AssembleiaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private ObjectMapper objectMapper = new ObjectMapper().registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

    @MockitoBean
    private AssembleiaService service;

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
    @DisplayName("Deve agendar assembleia com sucesso")
    void deveAgendarAssembleia() throws Exception {
        AssembleiaRequestDTO req = new AssembleiaRequestDTO(LocalDate.now().plusDays(10), TipoAssembleia.ORDINARIA, 1L);
        AssembleiaResponseDTO resp = new AssembleiaResponseDTO(1L, LocalDate.now().plusDays(10), TipoAssembleia.ORDINARIA, 1L, StatusAssembleia.AGENDADA, null, null, null, null, null, null);

        when(service.salvar(any(AssembleiaRequestDTO.class))).thenReturn(resp);

        mockMvc.perform(post("/api/assembleias")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("AGENDADA"));
    }

    @Test
    @DisplayName("Deve listar assembleias por grupo")
    void deveListarPorGrupo() throws Exception {
        AssembleiaResponseDTO resp = new AssembleiaResponseDTO(1L, LocalDate.now(), TipoAssembleia.ORDINARIA, 10L, StatusAssembleia.REALIZADA, null, null, null, null, null, null);
        when(service.listarPorGrupo(10L)).thenReturn(List.of(resp));

        mockMvc.perform(get("/api/assembleias/grupo/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].grupoId").value(10));
    }

    @Test
    @DisplayName("Deve abrir captação de lances da assembleia")
    void deveAbrirCaptacao() throws Exception {
        mockMvc.perform(post("/api/assembleias/1/abrir-captacao"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensagem").exists());

        verify(service).abrirCaptacao(1L);
    }

    @Test
    @DisplayName("Deve fechar captação de lances da assembleia")
    void deveFecharCaptacao() throws Exception {
        mockMvc.perform(post("/api/assembleias/1/fechar-captacao"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensagem").exists());

        verify(service).fecharCaptacao(1L);
    }
}