package br.com.estudo.consorcio.service;

import br.com.estudo.consorcio.domain.dto.GrupoFinanceiroResponseDTO;
import br.com.estudo.consorcio.domain.dto.GrupoRequestDTO;
import br.com.estudo.consorcio.domain.dto.GrupoResponseDTO;
import br.com.estudo.consorcio.domain.model.Grupo;
import br.com.estudo.consorcio.domain.model.Parcela;
import br.com.estudo.consorcio.domain.model.StatusGrupo;
import br.com.estudo.consorcio.domain.model.StatusParcela;
import br.com.estudo.consorcio.domain.repository.ContemplacaoRepository;
import br.com.estudo.consorcio.domain.repository.GrupoRepository;
import br.com.estudo.consorcio.domain.repository.ParcelaRepository;
import br.com.estudo.consorcio.exception.RegraDeNegocioException;
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

    @Mock
    private ParcelaRepository parcelaRepository;

    @Mock
    private ContemplacaoRepository contemplacaoRepository;

    @org.mockito.Spy
    private br.com.estudo.consorcio.domain.mapper.GrupoMapper mapper = org.mapstruct.factory.Mappers.getMapper(br.com.estudo.consorcio.domain.mapper.GrupoMapper.class);

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

    @Test
    @DisplayName("Deve reajustar grupo com sucesso recalculando o valorFundoComum das parcelas pendentes e atrasadas")
    void deveReajustarGrupoComSucesso() {
        // --- ARRANGE ---
        Long grupoId = 1L;
        BigDecimal antigoValorCredito = new BigDecimal("100000.00");
        BigDecimal novoValorCredito = new BigDecimal("110000.00");

        Grupo grupo = new Grupo();
        grupo.setId(grupoId);
        grupo.setValorCredito(antigoValorCredito);
        grupo.setStatus(StatusGrupo.EM_ANDAMENTO);

        Parcela p1 = new Parcela();
        p1.setStatus(StatusParcela.PENDENTE);
        p1.setValorFundoComum(new BigDecimal("1000.00"));

        Parcela p2 = new Parcela();
        p2.setStatus(StatusParcela.ATRASADA);
        p2.setValorFundoComum(new BigDecimal("1000.00"));

        List<Parcela> parcelas = List.of(p1, p2);

        when(repository.findById(grupoId)).thenReturn(Optional.of(grupo));
        when(repository.save(any(Grupo.class))).thenAnswer(i -> i.getArgument(0));
        when(parcelaRepository.findByCotaGrupoIdAndStatusIn(eq(grupoId), anyList())).thenReturn(parcelas);

        // --- ACT ---
        GrupoResponseDTO response = service.reajustarGrupo(grupoId, novoValorCredito);

        // --- ASSERT ---
        assertNotNull(response);
        assertEquals(novoValorCredito, grupo.getValorCredito());
        // Fator de reajuste: 110000 / 100000 = 1.10
        // Novo fundo comum: 1000 * 1.10 = 1100.00
        assertEquals(new BigDecimal("1100.00"), p1.getValorFundoComum());
        assertEquals(new BigDecimal("1100.00"), p2.getValorFundoComum());

        verify(repository, times(1)).save(grupo);
        verify(parcelaRepository, times(1)).saveAll(parcelas);
    }

    @Test
    @DisplayName("Deve lançar exceção ao reajustar grupo inexistente")
    void deveLancarExcecaoAoReajustarGrupoInexistente() {
        Long grupoId = 99L;
        when(repository.findById(grupoId)).thenReturn(Optional.empty());

        assertThrows(RegraDeNegocioException.class, () -> service.reajustarGrupo(grupoId, new BigDecimal("120000.00")));
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Deve lançar exceção ao reajustar grupo encerrado")
    void deveLancarExcecaoAoReajustarGrupoEncerrado() {
        Long grupoId = 1L;
        Grupo grupo = new Grupo();
        grupo.setId(grupoId);
        grupo.setStatus(StatusGrupo.ENCERRADO);

        when(repository.findById(grupoId)).thenReturn(Optional.of(grupo));

        assertThrows(RegraDeNegocioException.class, () -> service.reajustarGrupo(grupoId, new BigDecimal("120000.00")));
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Deve retornar o grupo sem reajustar se o valor for idêntico")
    void deveRetornarGrupoSemReajusteSeValorForIdentico() {
        Long grupoId = 1L;
        BigDecimal valor = new BigDecimal("100000.00");
        Grupo grupo = new Grupo();
        grupo.setId(grupoId);
        grupo.setValorCredito(valor);
        grupo.setStatus(StatusGrupo.EM_ANDAMENTO);

        when(repository.findById(grupoId)).thenReturn(Optional.of(grupo));

        var response = service.reajustarGrupo(grupoId, valor);

        assertNotNull(response);
        verify(repository, never()).save(any());
        verify(parcelaRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("Deve obter relatório financeiro consolidando parcelas pagas e contemplações")
    void deveObterRelatorioFinanceiroComSucesso() {
        // --- ARRANGE ---
        Long grupoId = 1L;
        Grupo grupo = new Grupo();
        grupo.setId(grupoId);
        grupo.setCodigo("GRP-001");

        when(repository.findById(grupoId)).thenReturn(Optional.of(grupo));
        when(parcelaRepository.somarFundoComumPorGrupoEStatus(grupoId, StatusParcela.PAGA)).thenReturn(new BigDecimal("50000.00"));
        when(parcelaRepository.somarTaxaAdministracaoPorGrupoEStatus(grupoId, StatusParcela.PAGA)).thenReturn(new BigDecimal("7500.00"));
        when(parcelaRepository.somarFundoReservaPorGrupoEStatus(grupoId, StatusParcela.PAGA)).thenReturn(new BigDecimal("2500.00"));
        when(contemplacaoRepository.somarCreditosLiberadosPorGrupo(grupoId)).thenReturn(new BigDecimal("40000.00"));

        // --- ACT ---
        GrupoFinanceiroResponseDTO relatorio = service.obterRelatorioFinanceiro(grupoId);

        // --- ASSERT ---
        assertNotNull(relatorio);
        assertEquals(grupoId, relatorio.grupoId());
        assertEquals("GRP-001", relatorio.codigoGrupo());
        assertEquals(new BigDecimal("50000.00"), relatorio.totalFundoComumArrecadado());
        assertEquals(new BigDecimal("7500.00"), relatorio.totalTaxaAdministracaoArrecadada());
        assertEquals(new BigDecimal("2500.00"), relatorio.totalFundoReservaArrecadado());
        assertEquals(new BigDecimal("40000.00"), relatorio.totalCreditosLiberados());
        assertEquals(new BigDecimal("10000.00"), relatorio.saldoDisponivelFundoComum()); // 50000 - 40000 = 10000
        assertEquals(new BigDecimal("2500.00"), relatorio.saldoDisponivelFundoReserva());
    }

    @Test
    @DisplayName("Deve encerrar grupo com sucesso quando não há parcelas em aberto")
    void deveEncerrarGrupoComSucesso() {
        // --- ARRANGE ---
        Long grupoId = 1L;
        Grupo grupo = new Grupo();
        grupo.setId(grupoId);
        grupo.setStatus(StatusGrupo.EM_ANDAMENTO);

        when(repository.findById(grupoId)).thenReturn(Optional.of(grupo));
        when(parcelaRepository.countByCotaGrupoIdAndStatusIn(eq(grupoId), anyList())).thenReturn(0L);
        when(repository.save(any(Grupo.class))).thenAnswer(i -> i.getArgument(0));

        // --- ACT ---
        GrupoResponseDTO response = service.encerrarGrupo(grupoId);

        // --- ASSERT ---
        assertNotNull(response);
        assertEquals(StatusGrupo.ENCERRADO, response.status());
        verify(repository, times(1)).save(grupo);
    }

    @Test
    @DisplayName("Deve lançar exceção ao tentar encerrar grupo com parcelas em aberto")
    void deveLancarExcecaoAoEncerrarGrupoComParcelasEmAberto() {
        // --- ARRANGE ---
        Long grupoId = 1L;
        Grupo grupo = new Grupo();
        grupo.setId(grupoId);
        grupo.setStatus(StatusGrupo.EM_ANDAMENTO);

        when(repository.findById(grupoId)).thenReturn(Optional.of(grupo));
        // Simulamos que existem 2 parcelas em aberto
        when(parcelaRepository.countByCotaGrupoIdAndStatusIn(eq(grupoId), anyList())).thenReturn(2L);

        // --- ACT & ASSERT ---
        assertThrows(RegraDeNegocioException.class, () -> service.encerrarGrupo(grupoId));
        verify(repository, never()).save(any());
    }
}