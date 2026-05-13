package br.com.estudo.consorcio.service;

import br.com.estudo.consorcio.domain.dto.GrupoRequestDTO;
import br.com.estudo.consorcio.domain.dto.GrupoResponseDTO;
import br.com.estudo.consorcio.domain.model.Grupo;
import br.com.estudo.consorcio.domain.model.StatusGrupo;
import br.com.estudo.consorcio.domain.repository.GrupoRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GrupoServiceTest {

    @Mock
    private GrupoRepository repository;

    @InjectMocks
    private GrupoService service;

    @Test
    @DisplayName("Deve salvar um novo grupo garantindo o status EM_FORMACAO")
    void deveSalvarGrupoComSucesso() {
        // --- ARRANGE ---
        GrupoRequestDTO request = new GrupoRequestDTO("GRP-001", new BigDecimal("50000.00"), 60, new BigDecimal("15.00"));

        // Simula o salvamento e retorna a própria entidade que foi passada como argumento
        when(repository.save(any(Grupo.class))).thenAnswer(i -> {
            Grupo g = i.getArgument(0);
            g.setId(1L); // Simulamos o banco gerando o ID
            return g;
        });

        // --- ACT ---
        GrupoResponseDTO response = service.salvar(request);

        // --- ASSERT ---
        assertNotNull(response);
        assertEquals(1L, response.id());
        assertEquals("GRP-001", response.codigo());
        assertEquals(StatusGrupo.EM_FORMACAO, response.status(), "A regra de negócio exige que o grupo nasça EM_FORMACAO");

        verify(repository, times(1)).save(any(Grupo.class));
    }

    @Test
    @DisplayName("Deve inaugurar um grupo com sucesso")
    void deveInaugurarGrupoComSucesso() {
        // --- ARRANGE ---
        Long idGrupo = 1L;
        LocalDate dataInauguracao = LocalDate.now();

        Grupo grupo = new Grupo();
        grupo.setId(idGrupo);
        grupo.setStatus(StatusGrupo.EM_FORMACAO); // Status correto para inaugurar

        when(repository.findById(idGrupo)).thenReturn(Optional.of(grupo));
        when(repository.save(any(Grupo.class))).thenAnswer(i -> i.getArgument(0));

        // --- ACT ---
        GrupoResponseDTO response = service.inaugurar(idGrupo, dataInauguracao);

        // --- ASSERT ---
        assertEquals(StatusGrupo.EM_ANDAMENTO, response.status(), "O status deve mudar para EM_ANDAMENTO");
        assertEquals(dataInauguracao, response.dataInauguracao(), "A data de inauguração deve ser registrada");
        verify(repository, times(1)).save(grupo);
    }

    @Test
    @DisplayName("Deve lançar exceção ao tentar inaugurar um grupo que não existe")
    void deveLancarExcecaoAoInaugurarGrupoInexistente() {
        // --- ARRANGE ---
        Long idInexistente = 99L;
        when(repository.findById(idInexistente)).thenReturn(Optional.empty());

        // --- ACT & ASSERT ---
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            service.inaugurar(idInexistente, LocalDate.now());
        });

        assertEquals("Grupo não encontrado.", exception.getMessage());
        verify(repository, never()).save(any()); // Garante que não tentou salvar
    }

    @Test
    @DisplayName("Deve lançar exceção ao tentar inaugurar um grupo que já não está EM_FORMACAO")
    void deveLancarExcecaoAoInaugurarGrupoComStatusInvalido() {
        // --- ARRANGE ---
        Long idGrupo = 1L;
        Grupo grupoJaInaugurado = new Grupo();
        grupoJaInaugurado.setId(idGrupo);
        grupoJaInaugurado.setStatus(StatusGrupo.EM_ANDAMENTO); // Simula que já foi inaugurado

        when(repository.findById(idGrupo)).thenReturn(Optional.of(grupoJaInaugurado));

        // --- ACT & ASSERT ---
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            service.inaugurar(idGrupo, LocalDate.now());
        });

        assertEquals("Apenas grupos em formação podem ser inaugurados.", exception.getMessage());
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Deve listar todos os grupos e mapear para DTO")
    void deveListarTodosOsGrupos() {
        // --- ARRANGE ---
        Grupo g1 = new Grupo(); g1.setId(1L); g1.setCodigo("G-01");
        Grupo g2 = new Grupo(); g2.setId(2L); g2.setCodigo("G-02");

        when(repository.findAll()).thenReturn(List.of(g1, g2));

        // --- ACT ---
        List<GrupoResponseDTO> lista = service.listarTodos();

        // --- ASSERT ---
        assertNotNull(lista);
        assertEquals(2, lista.size());
        assertEquals("G-01", lista.get(0).codigo());
        assertEquals("G-02", lista.get(1).codigo());
        verify(repository, times(1)).findAll();
    }
}