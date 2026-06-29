package br.com.estudo.consorcio.service;

import br.com.estudo.consorcio.domain.dto.CotaReembolsoResponseDTO;
import br.com.estudo.consorcio.domain.dto.CotaRequestDTO;
import br.com.estudo.consorcio.domain.dto.CotaResponseDTO;
import br.com.estudo.consorcio.domain.model.*;
import br.com.estudo.consorcio.domain.repository.ClienteRepository;
import br.com.estudo.consorcio.domain.repository.CotaRepository;
import br.com.estudo.consorcio.domain.repository.GrupoRepository;
import br.com.estudo.consorcio.domain.repository.ParcelaRepository;
import br.com.estudo.consorcio.domain.repository.ContemplacaoRepository;
import br.com.estudo.consorcio.exception.ClienteInativoException;
import br.com.estudo.consorcio.exception.RegraDeNegocioException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CotaServiceTest {

    @Mock
    private CotaRepository cotaRepository;
    @Mock
    private ClienteRepository clienteRepository;
    @Mock
    private GrupoRepository grupoRepository;
    @Mock
    private ParcelaRepository parcelaRepository;
    @Mock
    private MovimentoFinanceiroService movimentoService;
    @Mock
    private br.com.estudo.consorcio.domain.repository.HistoricoVersaoCotaRepository historicoVersaoCotaRepository;
    @Mock
    private HistoricoConsorciadoService historicoService;
    @Mock
    private ContemplacaoRepository contemplacaoRepository;
    @Mock
    private ContabilidadeService contabilidadeService;

    @Mock
    private br.com.estudo.consorcio.domain.repository.AlertaComplianceRepository alertaComplianceRepository;

    @org.mockito.Spy
    private br.com.estudo.consorcio.domain.mapper.CotaMapper mapper = org.mapstruct.factory.Mappers.getMapper(br.com.estudo.consorcio.domain.mapper.CotaMapper.class);

    @InjectMocks
    private CotaService service;

    @Test
    @DisplayName("Deve lançar exceção ao tentar salvar cota para um cliente inexistente")
    void deveLancarExcecaoClienteNaoEncontrado() {
        // --- ARRANGE ---
        CotaRequestDTO request = new CotaRequestDTO(15, 99L, 1L); // Cliente 99 não existe

        when(clienteRepository.findById(99L)).thenReturn(Optional.empty());

        // --- ACT & ASSERT ---
        RuntimeException exception = assertThrows(RuntimeException.class, () -> service.salvar(request));

        assertEquals("Cliente não encontrado.", exception.getMessage());
        verify(cotaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve salvar cota com sucesso e garantir o status ATIVA")
    void deveSalvarCotaComSucesso() {
        // --- ARRANGE ---
        CotaRequestDTO request = new CotaRequestDTO(15, 1L, 2L);

        Cliente cliente = new Cliente();
        cliente.setId(1L);
        cliente.setStatus(StatusCliente.ATIVO);
        Grupo grupo = new Grupo(); grupo.setId(2L);

        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
        when(grupoRepository.findById(2L)).thenReturn(Optional.of(grupo));

        // Simula o repositório devolvendo a cota salva
        when(cotaRepository.save(any(Cota.class))).thenAnswer(i -> i.getArguments()[0]);

        // --- ACT ---
        var response = service.salvar(request);

        // --- ASSERT ---
        assertNotNull(response);
        assertEquals(15, response.numeroCota());
        assertEquals(StatusCota.ATIVA, response.status(), "A cota deve nascer obrigatoriamente ATIVA");
        verify(cotaRepository, times(1)).save(any(Cota.class));
    }

    @Test
    @DisplayName("Deve lançar exceção ao tentar salvar cota para um grupo inexistente")
    void deveLancarExcecaoGrupoNaoEncontrado() {
        // --- ARRANGE ---
        CotaRequestDTO request = new CotaRequestDTO(15, 1L, 99L);

        Cliente cliente = new Cliente();
        cliente.setId(1L);
        cliente.setStatus(StatusCliente.ATIVO);

        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
        when(grupoRepository.findById(99L)).thenReturn(Optional.empty());

        // --- ACT & ASSERT ---
        RuntimeException exception = assertThrows(RuntimeException.class, () -> service.salvar(request));

        assertEquals("Grupo não encontrado.", exception.getMessage());
        verify(cotaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve lançar exceção ao tentar salvar cota para um cliente inativo")
    void deveLancarExcecaoAoSalvarCotaParaClienteInativo() {
        // --- ARRANGE ---
        CotaRequestDTO request = new CotaRequestDTO(15, 1L, 2L);

        Cliente cliente = new Cliente();
        cliente.setId(1L);
        cliente.setStatus(StatusCliente.INATIVO);

        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));

        // --- ACT & ASSERT ---
        assertThrows(ClienteInativoException.class, () -> service.salvar(request));

        verify(cotaRepository, never()).save(any());
        verify(grupoRepository, never()).findById(any());
    }

    @Test
    @DisplayName("Deve listar todas as cotas mapeadas para DTO")
    void deveListarTodasAsCotas() {
        // --- ARRANGE ---
        Cliente cliente = new Cliente(); cliente.setId(1L);
        Grupo grupo = new Grupo(); grupo.setId(2L);

        Cota cota1 = new Cota(); cota1.setId(100L); cota1.setNumeroCota(10); cota1.setCliente(cliente); cota1.setGrupo(grupo); cota1.setStatus(StatusCota.ATIVA);
        Cota cota2 = new Cota(); cota2.setId(101L); cota2.setNumeroCota(20); cota2.setCliente(cliente); cota2.setGrupo(grupo); cota2.setStatus(StatusCota.ATIVA);

        when(cotaRepository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(cota1, cota2)));

        // --- ACT ---
        Page<CotaResponseDTO> resultado = service.listarTodas(PageRequest.of(0, 10));

        // --- ASSERT ---
        assertNotNull(resultado);
        assertEquals(2, resultado.getContent().size());
        assertEquals(100L, resultado.getContent().get(0).id());
        assertEquals(10, resultado.getContent().get(0).numeroCota());
        assertEquals(1L, resultado.getContent().get(0).clienteId());

        verify(cotaRepository, times(1)).findAll(any(Pageable.class));
    }

    @Test
    @DisplayName("Deve listar cotas filtradas por ID do Cliente")
    void deveListarCotasPorCliente() {
        // --- ARRANGE ---
        Long idClientePesquisado = 5L;
        Cliente cliente = new Cliente(); cliente.setId(idClientePesquisado);
        Grupo grupo = new Grupo(); grupo.setId(2L);

        Cota cota = new Cota(); cota.setId(100L); cota.setCliente(cliente); cota.setGrupo(grupo);

        when(clienteRepository.findById(idClientePesquisado)).thenReturn(Optional.of(cliente));
        when(cotaRepository.findByClienteId(eq(idClientePesquisado), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(cota)));

        // --- ACT ---
        Page<CotaResponseDTO> resultado = service.listarPorCliente(idClientePesquisado, PageRequest.of(0, 10));

        // --- ASSERT ---
        assertEquals(1, resultado.getContent().size());
        assertEquals(idClientePesquisado, resultado.getContent().get(0).clienteId());
        verify(cotaRepository, times(1)).findByClienteId(eq(idClientePesquisado), any(Pageable.class));
    }

    @Test
    @DisplayName("Deve listar cotas filtradas por ID do Grupo")
    void deveListarCotasPorGrupo() {
        // --- ARRANGE ---
        Long idGrupoPesquisado = 10L;
        Cliente cliente = new Cliente(); cliente.setId(1L);
        Grupo grupo = new Grupo(); grupo.setId(idGrupoPesquisado);

        Cota cota = new Cota(); cota.setId(100L); cota.setCliente(cliente); cota.setGrupo(grupo);

        when(cotaRepository.findByGrupoId(eq(idGrupoPesquisado), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(cota)));

        // --- ACT ---
        Page<CotaResponseDTO> resultado = service.listarPorGrupo(idGrupoPesquisado, PageRequest.of(0, 10));

        // --- ASSERT ---
        assertEquals(1, resultado.getContent().size());
        assertEquals(idGrupoPesquisado, resultado.getContent().get(0).grupoId());
        verify(cotaRepository, times(1)).findByGrupoId(eq(idGrupoPesquisado), any(Pageable.class));
    }

    @Test
    @DisplayName("Deve retornar uma lista vazia caso não encontre cotas")
    void deveRetornarListaVaziaQuandoNaoHouverCotas() {
        // --- ARRANGE ---
        when(cotaRepository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));

        // --- ACT ---
        Page<CotaResponseDTO> resultado = service.listarTodas(PageRequest.of(0, 10));

        // --- ASSERT ---
        assertNotNull(resultado);
        assertTrue(resultado.isEmpty(), "A lista deve retornar vazia, não nula");
    }

    @Test
    @DisplayName("Deve cancelar cota com sucesso e deletar parcelas pendentes")
    void deveCancelarCotaComSucesso() {
        // --- ARRANGE ---
        Long cotaId = 1L;
        Cota cota = new Cota();
        cota.setId(cotaId);
        cota.setStatus(StatusCota.ATIVA);

        Grupo grupo = new Grupo();
        grupo.setId(10L);
        grupo.setValorCredito(new BigDecimal("100000.00"));
        cota.setGrupo(grupo);

        Cliente cliente = new Cliente();
        cliente.setId(5L);
        cota.setCliente(cliente);

        Parcela p1 = new Parcela();
        p1.setId(10L);
        p1.setStatus(StatusParcela.PENDENTE);

        Parcela p2 = new Parcela();
        p2.setId(11L);
        p2.setStatus(StatusParcela.PAGA);

        when(cotaRepository.findById(cotaId)).thenReturn(Optional.of(cota));
        when(cotaRepository.save(any(Cota.class))).thenAnswer(i -> i.getArgument(0));
        when(parcelaRepository.findByCotaId(cotaId)).thenReturn(List.of(p1, p2));

        // --- ACT ---
        CotaResponseDTO response = service.cancelarCota(cotaId);

        // --- ASSERT ---
        assertNotNull(response);
        assertEquals(StatusCota.CANCELADA, response.status());
        verify(cotaRepository, times(1)).save(cota);
        
        // Garante que deletou apenas a pendente (p1) usando ArgumentCaptor
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Parcela>> captor = ArgumentCaptor.forClass(List.class);
        verify(parcelaRepository, times(1)).deleteAll(captor.capture());
        List<Parcela> deletadas = captor.getValue();
        assertEquals(1, deletadas.size());
        assertTrue(deletadas.contains(p1));
    }

    @Test
    @DisplayName("Deve lançar exceção ao tentar cancelar cota já cancelada")
    void deveLancarExcecaoAoCancelarCotaJaCancelada() {
        Long cotaId = 1L;
        Cota cota = new Cota();
        cota.setId(cotaId);
        cota.setStatus(StatusCota.CANCELADA);

        when(cotaRepository.findById(cotaId)).thenReturn(Optional.of(cota));

        assertThrows(RegraDeNegocioException.class, () -> service.cancelarCota(cotaId));
        verify(cotaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve reembolsar cota cancelada com 10% de cláusula penal")
    void deveReembolsarCotaCanceladaComSucesso() {
        // --- ARRANGE ---
        Long cotaId = 1L;
        Cota cota = new Cota();
        cota.setId(cotaId);
        cota.setNumeroCota(44);
        cota.setStatus(StatusCota.CANCELADA);
        cota.setReembolsada(false);

        Grupo grupo = new Grupo();
        grupo.setId(10L);
        grupo.setValorCredito(new BigDecimal("100000.00"));
        cota.setGrupo(grupo);

        Cliente cliente = new Cliente();
        cliente.setId(5L);
        cota.setCliente(cliente);

        // Fundo comum pago total: 2000.00
        Parcela p1 = new Parcela();
        p1.setStatus(StatusParcela.PAGA);
        p1.setValorFundoComum(new BigDecimal("1000.00"));
        p1.setPercentualFundoComum(new BigDecimal("0.010000"));

        Parcela p2 = new Parcela();
        p2.setStatus(StatusParcela.PAGA);
        p2.setValorFundoComum(new BigDecimal("1000.00"));
        p2.setPercentualFundoComum(new BigDecimal("0.010000"));

        when(cotaRepository.findById(cotaId)).thenReturn(Optional.of(cota));
        when(parcelaRepository.findByCotaId(cotaId)).thenReturn(List.of(p1, p2));
        when(cotaRepository.save(any(Cota.class))).thenAnswer(i -> i.getArgument(0));

        // --- ACT ---
        CotaReembolsoResponseDTO response = service.reembolsarCota(cotaId);

        // --- ASSERT ---
        assertNotNull(response);
        assertEquals(cotaId, response.cotaId());
        assertEquals(44, response.numeroCota());
        assertEquals(new BigDecimal("2000.00"), response.totalFundoComumPago());
        // Multa rescisória de 10% de 2000.00 = 200.00
        assertEquals(new BigDecimal("200.00"), response.multaRescisoria());
        // Valor a ser reembolsado: 1800.00
        assertEquals(new BigDecimal("1800.00"), response.valorReembolsado());
        assertTrue(response.reembolsada());

        assertTrue(cota.getReembolsada());
        assertEquals(new BigDecimal("1800.00"), cota.getValorReembolsado());
        verify(cotaRepository, times(1)).save(cota);
    }

    @Test
    @DisplayName("Deve lançar exceção ao tentar reembolsar cota ativa")
    void deveLancarExcecaoAoReembolsarCotaAtiva() {
        Long cotaId = 1L;
        Cota cota = new Cota();
        cota.setId(cotaId);
        cota.setStatus(StatusCota.ATIVA);

        when(cotaRepository.findById(cotaId)).thenReturn(Optional.of(cota));

        assertThrows(RegraDeNegocioException.class, () -> service.reembolsarCota(cotaId));
        verify(cotaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve lançar exceção ao tentar reembolsar cota já reembolsada")
    void deveLancarExcecaoAoReembolsarCotaJaReembolsada() {
        Long cotaId = 1L;
        Cota cota = new Cota();
        cota.setId(cotaId);
        cota.setStatus(StatusCota.CANCELADA);
        cota.setReembolsada(true);

        when(cotaRepository.findById(cotaId)).thenReturn(Optional.of(cota));

        assertThrows(RegraDeNegocioException.class, () -> service.reembolsarCota(cotaId));
        verify(cotaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Cenário 1: Reembolso de Cota Cancelada com Base no Valor do Bem Atualizado (ADR 005)")
    void deveCalcularReembolsoComValorBemAtualizado() {
        // --- ARRANGE ---
        Long cotaId = 1L;
        Cota cota = new Cota();
        cota.setId(cotaId);
        cota.setNumeroCota(44);
        cota.setStatus(StatusCota.CANCELADA);
        cota.setReembolsada(false);

        Grupo grupo = new Grupo();
        grupo.setId(10L);
        grupo.setValorCredito(new BigDecimal("120000.00")); // Valor reajustado (vigente na contemplação)
        cota.setGrupo(grupo);

        Cliente cliente = new Cliente();
        cliente.setId(5L);
        cota.setCliente(cliente);

        // 3 parcelas pagas de 1000.00 cada sob crédito de 100000.00 (ou seja, 1% de PAFC cada = 3.00% total)
        Parcela p1 = new Parcela();
        p1.setStatus(StatusParcela.PAGA);
        p1.setValorFundoComum(new BigDecimal("1000.00"));
        p1.setPercentualFundoComum(new BigDecimal("0.010000"));

        Parcela p2 = new Parcela();
        p2.setStatus(StatusParcela.PAGA);
        p2.setValorFundoComum(new BigDecimal("1000.00"));
        p2.setPercentualFundoComum(new BigDecimal("0.010000"));

        Parcela p3 = new Parcela();
        p3.setStatus(StatusParcela.PAGA);
        p3.setValorFundoComum(new BigDecimal("1000.00"));
        p3.setPercentualFundoComum(new BigDecimal("0.010000"));

        // Contemplação por sorteio que fixou o valor líquido no passivo de excluídos
        // PAFC = 3%
        // Valor Bruto = 3% de 120000.00 = 3600.00
        // Multa rescisória = 10% de 3600.00 = 360.00
        // Valor Líquido (liberado) = 3240.00
        Contemplacao contemplacao = new Contemplacao();
        contemplacao.setCota(cota);
        contemplacao.setValorCreditoLiberado(new BigDecimal("3240.00"));

        when(cotaRepository.findById(cotaId)).thenReturn(Optional.of(cota));
        when(contemplacaoRepository.findTopByCotaIdOrderByDataContemplacaoDesc(cotaId)).thenReturn(Optional.of(contemplacao));
        when(cotaRepository.save(any(Cota.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        CotaReembolsoResponseDTO response = service.reembolsarCota(cotaId);

        // Assert
        assertNotNull(response);
        assertEquals(new BigDecimal("3600.00"), response.totalFundoComumPago()); // Valor bruto calculado
        assertEquals(new BigDecimal("360.00"), response.multaRescisoria()); // 10%
        assertEquals(new BigDecimal("3240.00"), response.valorReembolsado()); // Líquido fixado
        assertTrue(response.reembolsada());

        // Verifica o Ledger contábil: Débito de 3240.00 na conta de excluídos e Crédito em Caixa
        verify(contabilidadeService, times(1)).registrarBaixa(grupo, cota, null, ContabilidadeService.CONTA_EXCLUIDOS_DEVOLVER, ContabilidadeService.CONTA_CAIXA, new BigDecimal("3240.00"), LocalDate.now(), "Desembolso de reembolso de excluído - Cota 44");
    }
}