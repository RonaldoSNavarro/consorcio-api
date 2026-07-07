package br.com.estudo.consorcio.service;

import br.com.estudo.consorcio.domain.dto.ContemplacaoRequestDTO;
import br.com.estudo.consorcio.domain.dto.ContemplacaoResponseDTO;
import br.com.estudo.consorcio.domain.dto.CotaResponseDTO;
import br.com.estudo.consorcio.domain.model.*;
import br.com.estudo.consorcio.domain.repository.AssembleiaRepository;
import br.com.estudo.consorcio.domain.repository.ContemplacaoRepository;
import br.com.estudo.consorcio.domain.repository.CotaRepository;
import br.com.estudo.consorcio.domain.repository.ParcelaRepository;
import br.com.estudo.consorcio.domain.repository.LanceRepository;
import br.com.estudo.consorcio.exception.RegraDeNegocioException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContemplacaoServiceTest {

    @Mock
    private ContemplacaoRepository contemplacaoRepository;

    @Mock
    private AssembleiaRepository assembleiaRepository;

    @Mock
    private CotaRepository cotaRepository;

    @Mock
    private ParcelaRepository parcelaRepository;

    @Mock
    private MovimentoFinanceiroService movimentoService;

    @Mock
    private CotaService cotaService;

    @Mock
    private HistoricoConsorciadoService historicoService;

    @Mock
    private ContabilidadeService contabilidadeService;

    @Mock
    private LanceRepository lanceRepository;

    @Mock
    private br.com.estudo.consorcio.domain.mapper.CotaMapper cotaMapper;

    @org.mockito.Spy
    private br.com.estudo.consorcio.domain.mapper.ContemplacaoMapper mapper = org.mapstruct.factory.Mappers.getMapper(br.com.estudo.consorcio.domain.mapper.ContemplacaoMapper.class);

    @Mock
    private br.com.estudo.consorcio.domain.repository.AlertaComplianceRepository alertaComplianceRepository;

    @InjectMocks
    private ContemplacaoService service;

    @Test
    @DisplayName("Deve barrar e lançar exceção quando o lance embutido for maior que 30% do crédito do grupo")
    void deveBarrarLanceEmbutidoAcimaDeTrintaPorcento() {
        // --- ARRANGE ---
        Long idCota = 1L;
        Long idAssembleia = 2L;

        // 1. Criamos um grupo com crédito de 100 mil
        Grupo grupo = new Grupo();
        grupo.setId(10L);
        grupo.setValorCredito(new BigDecimal("100000.00"));
        grupo.setPercentualLanceEmbutidoMaximo(new BigDecimal("0.30"));

        // 2. Vinculamos a assembleia ao grupo
        Assembleia assembleia = new Assembleia();
        assembleia.setId(idAssembleia);
        assembleia.setGrupo(grupo);
        assembleia.setDataAssembleia(LocalDate.now());

        // 3. Vinculamos a cota ao mesmo grupo
        Cota cota = new Cota();
        cota.setId(idCota);
        cota.setGrupo(grupo);
        cota.setStatus(StatusCota.ATIVA);

        // 4. Simulamos um request maldoso: Lance embutido de 35 mil (35%)
        BigDecimal lanceExcessivo = new BigDecimal("35000.00");
        ContemplacaoRequestDTO requestMalicioso = new ContemplacaoRequestDTO(
                idCota,
                idAssembleia,
                TipoContemplacao.LANCE_FIXO,
                lanceExcessivo,
                true // Lance Embutido = TRUE
        );

        // 5. Ensinamos o Mockito a devolver nossos objetos quando procurados
        when(assembleiaRepository.findById(idAssembleia)).thenReturn(Optional.of(assembleia));
        when(cotaRepository.findById(idCota)).thenReturn(Optional.of(cota));
        when(parcelaRepository.existsByCotaIdAndStatusAndDataVencimentoBefore(eq(idCota), eq(StatusParcela.PENDENTE), any())).thenReturn(false);

        // --- ACT & ASSERT ---
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            service.registrar(requestMalicioso);
        });

        // Verificamos se a mensagem de erro é a correta do BCB
        assertTrue(exception.getMessage().contains("não pode ultrapassar 30% do crédito"));

        // O MAIS IMPORTANTE: Garantimos que o banco de dados NUNCA foi chamado para salvar isso
        verify(contemplacaoRepository, never()).save(any());
        verify(parcelaRepository, never()).somarFundoComumPorGrupoEStatus(anyLong(), any());
    }

    @Test
    @DisplayName("Deve barrar contemplação se o saldo do Fundo Comum for insuficiente")
    void deveBarrarContemplacaoPorSaldoInsuficiente() {
        // --- ARRANGE ---
        Long idCota = 1L;
        Long idAssembleia = 2L;
        Long idGrupo = 10L;

        // 1. Grupo com crédito de 50.000
        Grupo grupo = new Grupo();
        grupo.setId(idGrupo);
        grupo.setValorCredito(new BigDecimal("50000.00"));

        Assembleia assembleia = new Assembleia();
        assembleia.setId(idAssembleia);
        assembleia.setGrupo(grupo);
        assembleia.setDataAssembleia(LocalDate.now());

        Cota cota = new Cota();
        cota.setId(idCota);
        cota.setGrupo(grupo);
        cota.setStatus(StatusCota.ATIVA);

        // Pedido de contemplação normal (Sorteio, sem lance)
        ContemplacaoRequestDTO request = new ContemplacaoRequestDTO(
                idCota,
                idAssembleia,
                TipoContemplacao.SORTEIO,
                null,
                false
        );

        when(assembleiaRepository.findById(idAssembleia)).thenReturn(Optional.of(assembleia));
        when(cotaRepository.findById(idCota)).thenReturn(Optional.of(cota));
        when(parcelaRepository.existsByCotaIdAndStatusAndDataVencimentoBefore(eq(idCota), eq(StatusParcela.PENDENTE), any())).thenReturn(false);

        // A MÁGICA ACONTECE AQUI: Simulamos que o grupo só tem 10.000 em caixa (insuficiente para os 50k)
        when(contabilidadeService.calcularSaldoConta(eq(grupo), anyString()))
                .thenReturn(new BigDecimal("10000.00"));

        // --- ACT & ASSERT ---
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            service.registrar(request);
        });

        // Verifica se a mensagem avisa sobre o saldo insuficiente
        assertTrue(exception.getMessage().contains("Saldo insuficiente no Fundo Comum"));

        // Garante que a contemplação não foi salva
        verify(contemplacaoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve realizar contemplação com sucesso se todas as regras forem cumpridas")
    void deveContemplarComSucesso() {
        // --- ARRANGE ---
        Long idCota = 1L;
        Long idAssembleia = 2L;
        Long idGrupo = 10L;

        Grupo grupo = new Grupo();
        grupo.setId(idGrupo);
        grupo.setValorCredito(new BigDecimal("50000.00"));

        Assembleia assembleia = new Assembleia();
        assembleia.setId(idAssembleia);
        assembleia.setGrupo(grupo);
        assembleia.setDataAssembleia(LocalDate.now());

        Cota cota = new Cota();
        cota.setId(idCota);
        cota.setGrupo(grupo);
        cota.setStatus(StatusCota.ATIVA);

        ContemplacaoRequestDTO request = new ContemplacaoRequestDTO(
                idCota, idAssembleia, TipoContemplacao.SORTEIO, null, false
        );

        when(assembleiaRepository.findById(idAssembleia)).thenReturn(Optional.of(assembleia));
        when(cotaRepository.findById(idCota)).thenReturn(Optional.of(cota));
        when(parcelaRepository.existsByCotaIdAndStatusAndDataVencimentoBefore(eq(idCota), eq(StatusParcela.PENDENTE), any())).thenReturn(false);

        // Simulamos que o grupo tem dinheiro de sobra (100.000 em caixa)
        when(contabilidadeService.calcularSaldoConta(eq(grupo), anyString()))
                .thenReturn(new BigDecimal("100000.00"));

        // Ensina o mock a devolver o próprio objeto ao guardar
        when(contemplacaoRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);

        doAnswer(i -> {
            Cota c = i.getArgument(0);
            c.setStatus(StatusCota.CONTEMPLADA);
            return null;
        }).when(cotaService).registrarTransicaoVersao(any(Cota.class), any(StatusCota.class), anyString());

        // --- ACT ---
        var resultado = service.registrar(request);

        // --- ASSERT ---
        assertNotNull(resultado);
        assertEquals(new BigDecimal("50000.00"), resultado.valorCreditoLiberado());
        assertEquals(StatusCota.CONTEMPLADA, cota.getStatus()); // Verifica se a cota mudou de estado
        verify(contemplacaoRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("Deve barrar contemplação se a cota possuir parcelas vencidas em atraso")
    void deveBarrarContemplacaoPorInadimplencia() {
        // --- ARRANGE ---
        Long idCota = 1L;
        Long idAssembleia = 2L;
        Long idGrupo = 10L;

        Grupo grupo = new Grupo();
        grupo.setId(idGrupo);
        grupo.setValorCredito(new BigDecimal("50000.00"));

        Assembleia assembleia = new Assembleia();
        assembleia.setId(idAssembleia);
        assembleia.setGrupo(grupo);
        assembleia.setDataAssembleia(LocalDate.now());

        Cota cota = new Cota();
        cota.setId(idCota);
        cota.setGrupo(grupo);
        cota.setStatus(StatusCota.ATIVA);

        ContemplacaoRequestDTO request = new ContemplacaoRequestDTO(
                idCota, idAssembleia, TipoContemplacao.SORTEIO, null, false
        );

        when(assembleiaRepository.findById(idAssembleia)).thenReturn(Optional.of(assembleia));
        when(cotaRepository.findById(idCota)).thenReturn(Optional.of(cota));
        // MÁGICA: Cota está com parcelas atrasadas!
        when(parcelaRepository.existsByCotaIdAndStatusAndDataVencimentoBefore(eq(idCota), eq(StatusParcela.PENDENTE), eq(assembleia.getDataAssembleia())))
                .thenReturn(true);

        // --- ACT & ASSERT ---
        RegraDeNegocioException exception = assertThrows(RegraDeNegocioException.class, () -> {
            service.registrar(request);
        });

        assertEquals("Não é possível contemplar a cota: existem parcelas em atraso.", exception.getMessage());
        verify(contemplacaoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Cenário 1: Classificação e Contemplação Inicial por Lance Livre")
    void deveContemplarLanceLivreComStatusPendente() {
        // --- ARRANGE ---
        Long idCota = 1L;
        Long idAssembleia = 2L;

        Grupo grupo = new Grupo();
        grupo.setId(10L);
        grupo.setValorCredito(new BigDecimal("100000.00"));
        grupo.setPercentualLanceEmbutidoMaximo(new BigDecimal("0.30"));

        Assembleia assembleia = new Assembleia();
        assembleia.setId(idAssembleia);
        assembleia.setGrupo(grupo);
        assembleia.setDataAssembleia(LocalDate.now());

        Cota cota = new Cota();
        cota.setId(idCota);
        cota.setGrupo(grupo);
        cota.setStatus(StatusCota.ATIVA);

        ContemplacaoRequestDTO request = new ContemplacaoRequestDTO(
                idCota, idAssembleia, TipoContemplacao.LANCE_LIVRE, new BigDecimal("20000.00"), false
        );

        when(assembleiaRepository.findById(idAssembleia)).thenReturn(Optional.of(assembleia));
        when(cotaRepository.findById(idCota)).thenReturn(Optional.of(cota));
        when(parcelaRepository.existsByCotaIdAndStatusAndDataVencimentoBefore(eq(idCota), eq(StatusParcela.PENDENTE), any())).thenReturn(false);
        when(contabilidadeService.calcularSaldoConta(eq(grupo), anyString())).thenReturn(new BigDecimal("100000.00"));
        when(contemplacaoRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);

        // --- ACT ---
        ContemplacaoResponseDTO response = service.registrar(request);

        // --- ASSERT ---
        assertNotNull(response);
        assertEquals(new BigDecimal("100000.00"), response.valorCreditoLiberado());
        verify(cotaService, times(1)).registrarTransicaoVersao(cota, StatusCota.PENDENTE_INTEGRALIZACAO, "Cota contemplada via Lance Livre - Aguardando Integralização do Lance");
        verify(contabilidadeService, never()).registrarBaixa(any(), any(), any(), eq(ContabilidadeService.CONTA_FUNDO_COMUM), eq(ContabilidadeService.CONTA_CREDITOS_LIBERAR), any(), any(), any());
    }

    @Test
    @DisplayName("Cenário 2: Confirmação e Compensação Bancária do Lance (Integralização)")
    void deveConfirmarPagamentoLanceComSucesso() {
        // --- ARRANGE ---
        Long idLance = 5L;
        Long idCota = 1L;

        Grupo grupo = new Grupo();
        grupo.setId(10L);
        grupo.setValorCredito(new BigDecimal("100000.00"));

        Cota cota = new Cota();
        cota.setId(idCota);
        cota.setNumeroCota(44);
        cota.setGrupo(grupo);
        cota.setStatus(StatusCota.PENDENTE_INTEGRALIZACAO);

        Cliente cliente = new Cliente();
        cliente.setId(3L);
        cota.setCliente(cliente);

        Lance lance = new Lance();
        lance.setId(idLance);
        lance.setCota(cota);
        lance.setStatusApuracao(StatusApuracaoLance.VENCEDOR);
        lance.setValorOferta(new BigDecimal("20000.00"));

        Contemplacao contemplacao = new Contemplacao();
        contemplacao.setCota(cota);
        contemplacao.setValorCreditoLiberado(new BigDecimal("100000.00"));

        when(lanceRepository.findById(idLance)).thenReturn(Optional.of(lance));
        when(contemplacaoRepository.findTopByCotaIdOrderByDataContemplacaoDesc(idCota)).thenReturn(Optional.of(contemplacao));
        when(cotaMapper.toResponse(any(Cota.class))).thenReturn(new CotaResponseDTO(idCota, 44, 3L, 10L, StatusCota.AGUARDANDO_ANALISE, 0));

        // --- ACT ---
        CotaResponseDTO response = service.confirmarPagamentoLance(idLance);

        // --- ASSERT ---
        assertNotNull(response);
        verify(cotaService, times(1)).registrarTransicaoVersao(cota, StatusCota.AGUARDANDO_ANALISE, "Integralização do lance efetuada - Cota aguardando análise de crédito");
        
        // Verifica se fez os dois lançamentos contábeis no Ledger
        verify(contabilidadeService, times(1)).registrarBaixa(grupo, cota, null, ContabilidadeService.CONTA_CAIXA, ContabilidadeService.CONTA_FUNDO_COMUM, new BigDecimal("20000.00"), LocalDate.now(), "Integralização física de lance livre - Cota 44");
        verify(contabilidadeService, times(1)).registrarBaixa(grupo, cota, null, ContabilidadeService.CONTA_FUNDO_COMUM, ContabilidadeService.CONTA_CREDITOS_LIBERAR, new BigDecimal("100000.00"), LocalDate.now(), "Trânsito de crédito contemplado pós-integralização - Cota 44");
    }

    @Test
    @DisplayName("Cenário 3: Cancelamento de Lance por Expiração do Prazo (Inadimplência de Integralização)")
    void deveCancelarContemplacaoPorAtrasoComSucesso() {
        // --- ARRANGE ---
        Long idContemplacao = 7L;
        Long idCota = 1L;
        Long idAssembleia = 2L;

        Grupo grupo = new Grupo();
        grupo.setId(10L);
        grupo.setValorCredito(new BigDecimal("100000.00"));

        Assembleia assembleia = new Assembleia();
        assembleia.setId(idAssembleia);

        Cota cota = new Cota();
        cota.setId(idCota);
        cota.setGrupo(grupo);
        cota.setStatus(StatusCota.PENDENTE_INTEGRALIZACAO);

        Cliente cliente = new Cliente();
        cliente.setId(3L);
        cota.setCliente(cliente);

        Contemplacao contemplacao = new Contemplacao();
        contemplacao.setId(idContemplacao);
        contemplacao.setCota(cota);
        contemplacao.setAssembleia(assembleia);

        Lance lance = new Lance();
        lance.setCota(cota);
        lance.setStatusApuracao(StatusApuracaoLance.VENCEDOR);

        when(contemplacaoRepository.findById(idContemplacao)).thenReturn(Optional.of(contemplacao));
        when(lanceRepository.findByCotaIdAndAssembleiaId(idCota, idAssembleia)).thenReturn(Optional.of(lance));

        // --- ACT ---
        service.cancelarContemplacaoPorAtraso(idContemplacao);

        // --- ASSERT ---
        verify(cotaService, times(1)).registrarTransicaoVersao(cota, StatusCota.ATIVA, "Contemplação cancelada por atraso na integralização do lance.");
        assertEquals(StatusApuracaoLance.INVALIDO, lance.getStatusApuracao());
        verify(contemplacaoRepository, times(1)).delete(contemplacao);
    }

    @Test
    @DisplayName("Deve barrar e lançar exceção ao tentar registrar contemplação para cliente com alertas restritivos de compliance")
    void deveBarrarContemplacaoComAlertaRestritivo() {
        // --- ARRANGE ---
        Long idCota = 1L;
        Long idAssembleia = 2L;

        Grupo grupo = new Grupo();
        grupo.setId(10L);

        Assembleia assembleia = new Assembleia();
        assembleia.setId(idAssembleia);
        assembleia.setGrupo(grupo);
        assembleia.setDataAssembleia(LocalDate.now());

        Cliente cliente = new Cliente();
        cliente.setId(5L);

        Cota cota = new Cota();
        cota.setId(idCota);
        cota.setGrupo(grupo);
        cota.setStatus(StatusCota.ATIVA);
        cota.setCliente(cliente);

        ContemplacaoRequestDTO request = new ContemplacaoRequestDTO(
                idCota,
                idAssembleia,
                TipoContemplacao.SORTEIO,
                null,
                false
        );

        when(assembleiaRepository.findById(idAssembleia)).thenReturn(Optional.of(assembleia));
        when(cotaRepository.findById(idCota)).thenReturn(Optional.of(cota));
        when(alertaComplianceRepository.existsByClienteIdAndStatusIn(
                eq(5L),
                anyList()
        )).thenReturn(true);

        // --- ACT & ASSERT ---
        RegraDeNegocioException exception = assertThrows(RegraDeNegocioException.class, () -> service.registrar(request));
        assertTrue(exception.getMessage().contains("Contemplação bloqueada por Compliance/PLD"));
        verify(contemplacaoRepository, never()).save(any());
    }
}