package br.com.estudo.consorcio.service;

import br.com.estudo.consorcio.domain.dto.AssembleiaRequestDTO;
import br.com.estudo.consorcio.domain.dto.AssembleiaResponseDTO;
import br.com.estudo.consorcio.domain.model.Assembleia;
import br.com.estudo.consorcio.domain.model.Grupo;
import br.com.estudo.consorcio.domain.model.TipoAssembleia;
import br.com.estudo.consorcio.domain.repository.AssembleiaRepository;
import br.com.estudo.consorcio.domain.repository.GrupoRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AssembleiaServiceTest {

    @Mock
    private AssembleiaRepository assembleiaRepository;

    @Mock
    private GrupoRepository grupoRepository;

    @InjectMocks
    private AssembleiaService service;

    @Test
    @DisplayName("Deve agendar assembleia com sucesso usando o tipo informado")
    void deveSalvarAssembleiaComTipoInformado() {
        // --- ARRANGE ---
        Long idGrupo = 1L;
        LocalDate data = LocalDate.now().plusDays(30);
        AssembleiaRequestDTO request = new AssembleiaRequestDTO(data, TipoAssembleia.EXTRAORDINARIA, idGrupo);

        Grupo grupo = new Grupo();
        grupo.setId(idGrupo);

        when(grupoRepository.findById(idGrupo)).thenReturn(Optional.of(grupo));
        when(assembleiaRepository.save(any(Assembleia.class))).thenAnswer(i -> {
            Assembleia a = i.getArgument(0);
            a.setId(10L);
            return a;
        });

        // --- ACT ---
        AssembleiaResponseDTO response = service.salvar(request);

        // --- ASSERT ---
        assertNotNull(response);
        assertEquals(TipoAssembleia.EXTRAORDINARIA, response.tipo());
        assertEquals(idGrupo, response.grupoId());
        verify(assembleiaRepository, times(1)).save(any(Assembleia.class));
    }

    @Test
    @DisplayName("Deve assumir tipo ORDINARIA quando o tipo não for informado no DTO")
    void deveAssumirTipoPadraoQuandoNull() {
        // --- ARRANGE ---
        Long idGrupo = 1L;
        // Request com tipo NULL
        AssembleiaRequestDTO request = new AssembleiaRequestDTO(LocalDate.now(), null, idGrupo);

        Grupo grupo = new Grupo();
        grupo.setId(idGrupo);

        when(grupoRepository.findById(idGrupo)).thenReturn(Optional.of(grupo));
        when(assembleiaRepository.save(any(Assembleia.class))).thenAnswer(i -> i.getArgument(0));

        // --- ACT ---
        AssembleiaResponseDTO response = service.salvar(request);

        // --- ASSERT ---
        assertEquals(TipoAssembleia.ORDINARIA, response.tipo(), "A regra de negócio deve aplicar ORDINARIA como padrão");
    }

    @Test
    @DisplayName("Deve lançar exceção ao tentar agendar assembleia para grupo inexistente")
    void deveLancarExcecaoGrupoNaoEncontrado() {
        // --- ARRANGE ---
        AssembleiaRequestDTO request = new AssembleiaRequestDTO(LocalDate.now(), TipoAssembleia.ORDINARIA, 99L);
        when(grupoRepository.findById(99L)).thenReturn(Optional.empty());

        // --- ACT & ASSERT ---
        RuntimeException exception = assertThrows(RuntimeException.class, () -> service.salvar(request));

        assertEquals("Grupo não encontrado para agendar assembleia.", exception.getMessage());
        verify(assembleiaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve listar assembleias de um grupo corretamente")
    void deveListarAssembleiasPorGrupo() {
        // --- ARRANGE ---
        Long idGrupo = 1L;
        Grupo grupo = new Grupo(); grupo.setId(idGrupo);

        Assembleia a1 = new Assembleia(); a1.setId(1L); a1.setGrupo(grupo); a1.setTipo(TipoAssembleia.ORDINARIA);
        Assembleia a2 = new Assembleia(); a2.setId(2L); a2.setGrupo(grupo); a2.setTipo(TipoAssembleia.EXTRAORDINARIA);

        when(assembleiaRepository.findByGrupoId(idGrupo)).thenReturn(List.of(a1, a2));

        // --- ACT ---
        List<AssembleiaResponseDTO> resultado = service.listarPorGrupo(idGrupo);

        // --- ASSERT ---
        assertEquals(2, resultado.size());
        assertEquals(TipoAssembleia.ORDINARIA, resultado.get(0).tipo());
        assertEquals(TipoAssembleia.EXTRAORDINARIA, resultado.get(1).tipo());
        verify(assembleiaRepository, times(1)).findByGrupoId(idGrupo);
    }
}