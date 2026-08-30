package br.com.estudo.consorcio.controller;

import br.com.estudo.consorcio.domain.dto.GrupoRequestDTO;
import br.com.estudo.consorcio.domain.dto.GrupoResponseDTO;
import br.com.estudo.consorcio.domain.enums.CategoriaBem;
import br.com.estudo.consorcio.domain.model.IndiceReajuste;
import br.com.estudo.consorcio.domain.model.StatusGrupo;
import br.com.estudo.consorcio.service.GrupoService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(GrupoController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({br.com.estudo.consorcio.config.SecurityConfigurations.class})
@WithMockUser(authorities = {"MANAGE_GRUPOS", "VIEW_GRUPOS"})
class GrupoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private ObjectMapper objectMapper = new ObjectMapper().registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

    @MockitoBean
    private GrupoService service;

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
    @DisplayName("Deve cadastrar grupo com sucesso")
    void deveCadastrarGrupo() throws Exception {
        GrupoRequestDTO req = new GrupoRequestDTO(
                "GRP-100", new BigDecimal("150000.00"), 60, new BigDecimal("15.00"),
                CategoriaBem.VEICULO_AUTOMOTOR, IndiceReajuste.INCC, 1, 100, null, null
        );
        GrupoResponseDTO resp = new GrupoResponseDTO(
                1L, "GRP-100", new BigDecimal("150000.00"), 60, new BigDecimal("15.00"),
                StatusGrupo.EM_FORMACAO, LocalDate.now(), null,
                CategoriaBem.VEICULO_AUTOMOTOR, IndiceReajuste.INCC, 1, 100, null, null
        );

        when(service.salvar(any(GrupoRequestDTO.class))).thenReturn(resp);

        mockMvc.perform(post("/api/grupos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.codigoGrupo").value("GRP-100"));
    }

    @Test
    @DisplayName("Deve listar grupos paginados")
    void deveListarGrupos() throws Exception {
        GrupoResponseDTO resp = new GrupoResponseDTO(
                1L, "GRP-100", new BigDecimal("150000.00"), 60, new BigDecimal("15.00"),
                StatusGrupo.EM_FORMACAO, LocalDate.now(), null,
                CategoriaBem.VEICULO_AUTOMOTOR, IndiceReajuste.INCC, 1, 100, null, null
        );

        when(service.listarTodos(any())).thenReturn(new PageImpl<>(List.of(resp), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/grupos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].codigoGrupo").value("GRP-100"));
    }

    @Test
    @DisplayName("Deve inaugurar grupo com sucesso")
    void deveInaugurarGrupo() throws Exception {
        LocalDate data = LocalDate.now().plusDays(15);
        GrupoResponseDTO resp = new GrupoResponseDTO(
                1L, "GRP-100", new BigDecimal("150000.00"), 60, new BigDecimal("15.00"),
                StatusGrupo.EM_ANDAMENTO, LocalDate.now(), data,
                CategoriaBem.VEICULO_AUTOMOTOR, IndiceReajuste.INCC, 1, 100, null, null
        );

        when(service.inaugurar(eq(1L), eq(data))).thenReturn(resp);

        mockMvc.perform(put("/api/grupos/1/inaugurar")
                .param("dataAssembleia", data.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EM_ANDAMENTO"));
    }
}