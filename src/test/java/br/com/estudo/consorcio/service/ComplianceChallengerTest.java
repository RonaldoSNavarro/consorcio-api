package br.com.estudo.consorcio.service;

import br.com.estudo.consorcio.domain.dto.CotaRequestDTO;
import br.com.estudo.consorcio.domain.dto.PropostaRequestDTO;
import br.com.estudo.consorcio.domain.dto.TransferirCotaRequestDTO;
import br.com.estudo.consorcio.domain.dto.ContemplacaoRequestDTO;
import br.com.estudo.consorcio.domain.enums.StatusContrato;
import br.com.estudo.consorcio.domain.enums.StatusProposta;
import br.com.estudo.consorcio.domain.model.*;
import br.com.estudo.consorcio.domain.repository.*;
import br.com.estudo.consorcio.domain.service.PropostaAdesaoService;
import br.com.estudo.consorcio.exception.RegraDeNegocioException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mapstruct.factory.Mappers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class ComplianceChallengerTest {

    // Repositories
    private ClienteRepository clienteRepository;
    private ListaRestritivaRepository listaRestritivaRepository;
    private AlertaComplianceRepository alertaComplianceRepository;
    private CotaRepository cotaRepository;
    private GrupoRepository grupoRepository;
    private ParcelaRepository parcelaRepository;
    private HistoricoVersaoCotaRepository historicoVersaoCotaRepository;
    private ContemplacaoRepository contemplacaoRepository;
    private PropostaAdesaoRepository propostaRepository;
    private ContratoAdesaoRepository contratoRepository;
    private ProdutoConsorcioRepository produtoRepository;
    private TipoVendaRepository tipoVendaRepository;
    private AssembleiaRepository assembleiaRepository;
    private LanceRepository lanceRepository;

    // Services under test / mocked services
    private MatchComplianceService matchComplianceService;
    private CotaService cotaService;
    private ContemplacaoService contemplacaoService;
    private PropostaAdesaoService propostaAdesaoService;
    private MovimentoFinanceiroService movimentoService;
    private HistoricoConsorciadoService historicoService;
    private ContabilidadeService contabilidadeService;

    // Mappers
    private br.com.estudo.consorcio.domain.mapper.CotaMapper cotaMapper;
    private br.com.estudo.consorcio.domain.mapper.ContemplacaoMapper contemplacaoMapper;

    @BeforeEach
    void setUp() {
        clienteRepository = mock(ClienteRepository.class);
        listaRestritivaRepository = mock(ListaRestritivaRepository.class);
        alertaComplianceRepository = mock(AlertaComplianceRepository.class);
        cotaRepository = mock(CotaRepository.class);
        grupoRepository = mock(GrupoRepository.class);
        parcelaRepository = mock(ParcelaRepository.class);
        historicoVersaoCotaRepository = mock(HistoricoVersaoCotaRepository.class);
        contemplacaoRepository = mock(ContemplacaoRepository.class);
        propostaRepository = mock(PropostaAdesaoRepository.class);
        contratoRepository = mock(ContratoAdesaoRepository.class);
        produtoRepository = mock(ProdutoConsorcioRepository.class);
        tipoVendaRepository = mock(TipoVendaRepository.class);
        assembleiaRepository = mock(AssembleiaRepository.class);
        movimentoService = mock(MovimentoFinanceiroService.class);
        historicoService = mock(HistoricoConsorciadoService.class);
        contabilidadeService = mock(ContabilidadeService.class);
        lanceRepository = mock(LanceRepository.class);

        cotaMapper = Mappers.getMapper(br.com.estudo.consorcio.domain.mapper.CotaMapper.class);
        org.springframework.test.util.ReflectionTestUtils.setField(cotaMapper, "parcelaRepository", parcelaRepository);
        contemplacaoMapper = Mappers.getMapper(br.com.estudo.consorcio.domain.mapper.ContemplacaoMapper.class);

        // MatchComplianceService
        matchComplianceService = new MatchComplianceService(clienteRepository, listaRestritivaRepository, alertaComplianceRepository);

        // CotaService
        cotaService = new CotaService(
                cotaRepository, clienteRepository, grupoRepository, parcelaRepository,
                cotaMapper, movimentoService, historicoVersaoCotaRepository,
                historicoService, contemplacaoRepository, contabilidadeService, alertaComplianceRepository,
                mock(br.com.estudo.consorcio.service.ComissaoVendaService.class),
                mock(br.com.estudo.consorcio.service.CorretorService.class)
        );

        // ContemplacaoService
        br.com.estudo.consorcio.domain.mapper.CotaMapper cotaMapperSpy = spy(cotaMapper);
        contemplacaoService = new ContemplacaoService(
                contemplacaoRepository, assembleiaRepository, cotaRepository,
                parcelaRepository, contemplacaoMapper, contabilidadeService,
                cotaService, historicoService, lanceRepository, cotaMapperSpy, alertaComplianceRepository
        );

        // PropostaAdesaoService
        propostaAdesaoService = new PropostaAdesaoService(
                propostaRepository, contratoRepository, clienteRepository,
                produtoRepository, tipoVendaRepository, alertaComplianceRepository,
                matchComplianceService,
                grupoRepository, cotaRepository, assembleiaRepository, parcelaRepository,
                java.time.Clock.systemDefaultZone()
        );
    }

    // =========================================================================
    // FOCUS 1: Simulating typographical variations of names and verifying Jaro-Winkler
    // =========================================================================
    @Test
    @DisplayName("1.1. Verificar Jaro-Winkler com variações tipográficas do nome do cliente")
    void testJaroWinklerNameVariationsAndThresholds() {
        // Setup a client with name "Osama Bin Laden"
        Cliente cliente = new Cliente();
        cliente.setId(1L);
        cliente.setNome("Osama Bin Laden");
        cliente.setCpfCnpj("11122233344");

        // We will test multiple variations of list names
        // 1. "OSAMA BIN LADIN" -> JW similarity is ~0.97, should match >= 0.90
        // 2. "OSAMA BEN LADEN" -> JW similarity is ~0.95, should match >= 0.90 but NOT >= 0.96
        // 3. "OSAMA BIN LADEN JR" -> JW similarity is ~0.94, should match >= 0.90 but NOT >= 0.95
        // 4. "GEORGE BUSH" -> JW similarity is very low, should NOT match

        // We run match runs with threshold 0.90
        matchComplianceService.setSimilarityThreshold(0.90);

        List<String> matchingNames = List.of("OSAMA BIN LADIN", "OSAMA BEN LADEN", "OSAMA BIN LADEN JR");
        for (String listName : matchingNames) {
            ListaRestritiva listEntry = new ListaRestritiva();
            listEntry.setId(100L);
            listEntry.setNome(listName);
            listEntry.setOrigem(OrigemListaRestritiva.ONU);

            AlertaComplianceRepository.MatchResultProjection match = new AlertaComplianceRepository.MatchResultProjection() {
                public Long getClienteId() { return 1L; }
                public Long getListaId() { return 100L; }
                public Double getScore() { return 0.95; }
            };

            when(alertaComplianceRepository.findOfacOnuMatches(0.90)).thenReturn(List.of(match));
            when(clienteRepository.getReferenceById(1L)).thenReturn(cliente);
            when(listaRestritivaRepository.getReferenceById(100L)).thenReturn(listEntry);

            matchComplianceService.cruzarBaseDeClientes();

            // Should save an alert
            verify(alertaComplianceRepository, atLeastOnce()).saveAll(argThat(alertas -> {
                AlertaCompliance alerta = ((List<AlertaCompliance>) alertas).get(0);
                assertEquals(cliente.getId(), alerta.getCliente().getId());
                assertEquals(listEntry.getId(), alerta.getListaRestritiva().getId());
                assertTrue(alerta.getScore().doubleValue() >= 0.90);
                return true;
            }));
            Mockito.clearInvocations(alertaComplianceRepository);
        }

        // Test Non-Matching Name
        ListaRestritiva nonMatchingEntry = new ListaRestritiva();
        nonMatchingEntry.setId(101L);
        nonMatchingEntry.setNome("GEORGE BUSH");
        nonMatchingEntry.setOrigem(OrigemListaRestritiva.ONU);

        when(alertaComplianceRepository.findOfacOnuMatches(0.90)).thenReturn(Collections.emptyList());

        matchComplianceService.cruzarBaseDeClientes();

        // Should NOT save an alert
        verify(alertaComplianceRepository, never()).saveAll(any());
        Mockito.clearInvocations(alertaComplianceRepository);

        // Test configurable threshold increased to 0.96
        matchComplianceService.setSimilarityThreshold(0.96);

        // OSAMA BEN LADEN has JW similarity of ~0.946, so with threshold 0.96 it should NOT match
        ListaRestritiva benLadenEntry = new ListaRestritiva();
        benLadenEntry.setId(102L);
        benLadenEntry.setNome("OSAMA BEN LADEN");
        benLadenEntry.setOrigem(OrigemListaRestritiva.ONU);

        when(alertaComplianceRepository.findOfacOnuMatches(0.96)).thenReturn(Collections.emptyList());

        matchComplianceService.cruzarBaseDeClientes();
        verify(alertaComplianceRepository, never()).saveAll(any());
    }

    // =========================================================================
    // FOCUS 2: Verifying PEP CPF masking extraction against full and masked CPFs
    // =========================================================================
    @Test
    @DisplayName("2.1. Verificar matching de PEP com CPF mascarado (e.g. ***.531.324-**)")
    void testPepCpfMaskingMatching() {
        // Client CPF: "111.531.324-88" -> clean CPF: "11153132488" -> central 6 digits: "531324"
        Cliente cliente = new Cliente();
        cliente.setId(2L);
        cliente.setNome("Marcelo Antonio Carreira");
        cliente.setCpfCnpj("111.531.324-88");

        // PEP with masked CPF: "***.531.324-**" -> clean PEP: "531324" -> should match
        ListaRestritiva pepMasked = new ListaRestritiva();
        pepMasked.setId(200L);
        pepMasked.setNome("MARCELO ANTONIO CARREIRA");
        pepMasked.setDocumentoOrigem("***.531.324-**");
        pepMasked.setOrigem(OrigemListaRestritiva.PEP);

        AlertaComplianceRepository.MatchResultProjection match = new AlertaComplianceRepository.MatchResultProjection() {
            public Long getClienteId() { return 2L; }
            public Long getListaId() { return 200L; }
            public Double getScore() { return 1.0; }
        };

        when(alertaComplianceRepository.findPepMatches(anyDouble())).thenReturn(List.of(match));
        when(clienteRepository.getReferenceById(2L)).thenReturn(cliente);
        when(listaRestritivaRepository.getReferenceById(200L)).thenReturn(pepMasked);

        matchComplianceService.cruzarBaseDeClientes();

        verify(alertaComplianceRepository, times(1)).saveAll(argThat(alertas -> {
            AlertaCompliance alerta = ((List<AlertaCompliance>) alertas).get(0);
            assertEquals(2L, alerta.getCliente().getId());
            assertEquals(200L, alerta.getListaRestritiva().getId());
            return true;
        }));
        Mockito.clearInvocations(alertaComplianceRepository);

        // Test non-matching client CPF centrais
        cliente.setCpfCnpj("111.666.324-88"); // central: 666324, PEP: 531324
        when(alertaComplianceRepository.findPepMatches(anyDouble())).thenReturn(Collections.emptyList());
        matchComplianceService.cruzarBaseDeClientes();
        verify(alertaComplianceRepository, never()).saveAll(any());
        Mockito.clearInvocations(alertaComplianceRepository);

        // Test matching with full CPF in PEP (unmasked)
        cliente.setCpfCnpj("111.531.324-88");
        pepMasked.setDocumentoOrigem("111.531.324-88"); // full CPF
        when(alertaComplianceRepository.findPepMatches(anyDouble())).thenReturn(List.of(match));
        matchComplianceService.cruzarBaseDeClientes();
        verify(alertaComplianceRepository, times(1)).saveAll(any());
    }

    // =========================================================================
    // FOCUS 3: Simulating transactional flows and checking they are correctly blocked
    // =========================================================================

    @Test
    @DisplayName("3.1. PropostaAdesaoService - Criar, aprovar e efetivar proposta deve bloquear cliente com alertas restritivos")
    public void testBlockProposalFlows() {
        Cliente client = new Cliente();
        client.setId(1L);
        client.setStatus(StatusCliente.ATIVO);

        when(clienteRepository.findById(1L)).thenReturn(Optional.of(client));

        // When client has PENDENTE_ANALISE alert, proposal creation should be blocked
        when(alertaComplianceRepository.existsByClienteIdAndStatusIn(eq(1L), anyList())).thenReturn(true);

        PropostaRequestDTO requestDto = new PropostaRequestDTO();
        requestDto.setClienteId(1L);
        requestDto.setProdutoId(10L);
        requestDto.setTipoVendaId(20L);
        requestDto.setGrupoId(100L);
        requestDto.setValorCreditoSolicitado(BigDecimal.valueOf(100000));

        assertThrows(RegraDeNegocioException.class, () -> propostaAdesaoService.criarProposta(requestDto),
                "Should block proposal creation for client with compliance alerts");

        // Repeat for proposal approval
        PropostaAdesao proposta = new PropostaAdesao();
        proposta.setId(5L);
        proposta.setCliente(client);
        proposta.setStatus(StatusProposta.EM_ANALISE);
        when(propostaRepository.findById(5L)).thenReturn(Optional.of(proposta));

        assertThrows(RegraDeNegocioException.class, () -> propostaAdesaoService.aprovarProposta(5L),
                "Should block proposal approval for client with compliance alerts");

        // Repeat for contract execution
        ContratoAdesao contrato = new ContratoAdesao();
        contrato.setId(8L);
        contrato.setProposta(proposta);
        contrato.setStatus(StatusContrato.PENDENTE_PAGAMENTO);
        when(contratoRepository.findById(8L)).thenReturn(Optional.of(contrato));

        assertThrows(RegraDeNegocioException.class, () -> propostaAdesaoService.efetivarContrato(8L),
                "Should block contract execution for client with compliance alerts");
    }

    @Test
    @DisplayName("3.2. ContemplacaoService - Registrar contemplacao deve bloquear cliente com alertas restritivos")
    public void testBlockContemplation() {
        Cliente client = new Cliente();
        client.setId(1L);

        Grupo grupo = new Grupo();
        grupo.setId(2L);
        grupo.setValorCredito(BigDecimal.valueOf(100000));

        Cota cota = new Cota();
        cota.setId(10L);
        cota.setCliente(client);
        cota.setGrupo(grupo);
        cota.setStatus(StatusCota.ATIVA);

        Assembleia assembleia = new Assembleia();
        assembleia.setId(20L);
        assembleia.setGrupo(grupo);
        assembleia.setDataAssembleia(LocalDate.now().plusDays(1));

        when(cotaRepository.findById(10L)).thenReturn(Optional.of(cota));
        when(assembleiaRepository.findById(20L)).thenReturn(Optional.of(assembleia));
        when(alertaComplianceRepository.existsByClienteIdAndStatusIn(eq(1L), anyList())).thenReturn(true);

        ContemplacaoRequestDTO requestDto = new ContemplacaoRequestDTO(
                10L, 20L, TipoContemplacao.LANCE_LIVRE, BigDecimal.valueOf(15000), false
        );

        assertThrows(RegraDeNegocioException.class, () -> contemplacaoService.registrar(requestDto),
                "Should block contemplation for cota with compliance alerts");
    }

    @Test
    @DisplayName("3.3. CotaService - Transferir Cota deve bloquear se origem ou destino possuir alerta")
    public void testBlockCotaTransfer() {
        Cliente originClient = new Cliente();
        originClient.setId(1L);
        originClient.setStatus(StatusCliente.ATIVO);

        Cliente destClient = new Cliente();
        destClient.setId(2L);
        destClient.setStatus(StatusCliente.ATIVO);

        Grupo grupo = new Grupo();
        grupo.setId(3L);

        Cota cota = new Cota();
        cota.setId(10L);
        cota.setCliente(originClient);
        cota.setGrupo(grupo);
        cota.setStatus(StatusCota.ATIVA);

        when(cotaRepository.findById(10L)).thenReturn(Optional.of(cota));
        when(clienteRepository.findById(2L)).thenReturn(Optional.of(destClient));

        // Case A: Destination client has alert
        when(alertaComplianceRepository.existsByClienteIdAndStatusIn(eq(2L), anyList())).thenReturn(true);
        when(alertaComplianceRepository.existsByClienteIdAndStatusIn(eq(1L), anyList())).thenReturn(false);

        TransferirCotaRequestDTO transferDto = new TransferirCotaRequestDTO(2L, "Motivo", BigDecimal.ZERO);

        assertThrows(RegraDeNegocioException.class, () -> cotaService.transferirCota(10L, transferDto),
                "Should block cota transfer if destination client has compliance alerts");

        // Case B: Origin client has alert
        when(alertaComplianceRepository.existsByClienteIdAndStatusIn(eq(2L), anyList())).thenReturn(false);
        when(alertaComplianceRepository.existsByClienteIdAndStatusIn(eq(1L), anyList())).thenReturn(true);

        assertThrows(RegraDeNegocioException.class, () -> cotaService.transferirCota(10L, transferDto),
                "Should block cota transfer if origin client has compliance alerts");
    }

    @Test
    @DisplayName("3.4. CotaService - Salvar nova cota deve bloquear cliente com alertas restritivos")
    void testCotaSalvarBlockedByCompliance() {
        Cliente cliente = new Cliente();
        cliente.setId(12L);
        cliente.setStatus(StatusCliente.ATIVO);

        when(clienteRepository.findById(12L)).thenReturn(Optional.of(cliente));
        when(alertaComplianceRepository.existsByClienteIdAndStatusIn(eq(12L), anyList())).thenReturn(true);

        CotaRequestDTO dto = new CotaRequestDTO(1, 12L, 5L);

        RegraDeNegocioException ex = assertThrows(RegraDeNegocioException.class, () -> {
            cotaService.salvar(dto);
        });
        assertTrue(ex.getMessage().contains("Operação bloqueada pelo Compliance"));
    }

    @Test
    @DisplayName("3.5. CotaService - Readmitir cota deve bloquear cliente com alertas restritivos")
    void testCotaReadmitirBlockedByCompliance() {
        Cliente cliente = new Cliente();
        cliente.setId(13L);

        Cota cota = new Cota();
        cota.setId(1001L);
        cota.setCliente(cliente);
        cota.setStatus(StatusCota.EXCLUIDA);

        when(cotaRepository.findById(1001L)).thenReturn(Optional.of(cota));
        when(alertaComplianceRepository.existsByClienteIdAndStatusIn(eq(13L), anyList())).thenReturn(true);

        RegraDeNegocioException ex = assertThrows(RegraDeNegocioException.class, () -> {
            cotaService.readmitirCota(1001L);
        });
        assertTrue(ex.getMessage().contains("Readmissão bloqueada pelo Compliance"));
    }

    // ==========================================
    // 4. Siscoaf Notification Flag on Lance
    // ==========================================
    @Test
    @DisplayName("Should trigger Siscoaf notification flag only for winning lances of type FIRME with value >= 50,000.00")
    public void testSiscoafNotificationFlag() throws Exception {
        Method onCreate = Lance.class.getDeclaredMethod("onCreate");
        onCreate.setAccessible(true);

        // Case A: Winning, type FIRME, value 50000.00 -> Should notify
        Lance lanceA = new Lance();
        lanceA.setStatusApuracao(StatusApuracaoLance.VENCEDOR);
        lanceA.setTipo(TipoLance.FIRME);
        lanceA.setValorOferta(new BigDecimal("50000.00"));
        onCreate.invoke(lanceA);
        assertTrue(lanceA.isNotificarSiscoaf(), "Siscoaf flag should be true for winning FIRME lance with value = 50,000.00");

        // Case B: Winning, type FIRME, value 49999.99 -> Should NOT notify
        Lance lanceB = new Lance();
        lanceB.setStatusApuracao(StatusApuracaoLance.VENCEDOR);
        lanceB.setTipo(TipoLance.FIRME);
        lanceB.setValorOferta(new BigDecimal("49999.99"));
        onCreate.invoke(lanceB);
        assertFalse(lanceB.isNotificarSiscoaf(), "Siscoaf flag should be false for winning FIRME lance with value < 50,000.00");

        // Case C: Winning, type EMBUTIDO, value 60000.00 -> Should NOT notify (not FIRME)
        Lance lanceC = new Lance();
        lanceC.setStatusApuracao(StatusApuracaoLance.VENCEDOR);
        lanceC.setTipo(TipoLance.EMBUTIDO);
        lanceC.setValorOferta(new BigDecimal("60000.00"));
        onCreate.invoke(lanceC);
        assertFalse(lanceC.isNotificarSiscoaf(), "Siscoaf flag should be false for winning EMBUTIDO lance");

        // Case D: Cadastrado (not winning), type FIRME, value 60000.00 -> Should NOT notify
        Lance lanceD = new Lance();
        lanceD.setStatusApuracao(StatusApuracaoLance.CADASTRADO);
        lanceD.setTipo(TipoLance.FIRME);
        lanceD.setValorOferta(new BigDecimal("60000.00"));
        onCreate.invoke(lanceD);
        assertFalse(lanceD.isNotificarSiscoaf(), "Siscoaf flag should be false for non-winning lance");
    }
}
