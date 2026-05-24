package br.com.estudo.consorcio.service;

import br.com.estudo.consorcio.domain.dto.AnaliseCreditoRequestDTO;
import br.com.estudo.consorcio.domain.dto.AnaliseCreditoResponseDTO;
import br.com.estudo.consorcio.domain.mapper.AnaliseCreditoMapper;
import br.com.estudo.consorcio.domain.model.*;
import br.com.estudo.consorcio.domain.repository.AnaliseCreditoRepository;
import br.com.estudo.consorcio.domain.repository.ContemplacaoRepository;
import br.com.estudo.consorcio.domain.repository.CotaRepository;
import br.com.estudo.consorcio.domain.repository.ParcelaRepository;
import br.com.estudo.consorcio.exception.RegraDeNegocioException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnaliseCreditoServiceTest {

    @Mock
    private AnaliseCreditoRepository analiseCreditoRepository;
    @Mock
    private CotaRepository cotaRepository;
    @Mock
    private ParcelaRepository parcelaRepository;
    @Mock
    private ContemplacaoRepository contemplacaoRepository;
    @Mock
    private AnaliseCreditoMapper mapper;
    @Mock
    private MovimentoFinanceiroService movimentoService;
    @Mock
    private CotaService cotaService;
    @Mock
    private HistoricoConsorciadoService historicoService;

    @InjectMocks
    private AnaliseCreditoService analiseCreditoService;

    private Cota cota;
    private Cliente cliente;
    private Grupo grupo;

    @BeforeEach
    void setUp() {
        cliente = new Cliente();
        cliente.setId(1L);

        grupo = new Grupo();
        grupo.setId(1L);
        grupo.setValorCredito(new BigDecimal("10000.00"));

        cota = new Cota();
        cota.setId(1L);
        cota.setCliente(cliente);
        cota.setGrupo(grupo);
        cota.setStatus(StatusCota.AGUARDANDO_ANALISE);

        Usuario usuario = new Usuario("admin", "123");
        usuario.setId(1L);

        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(usuario);
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    void deveAprovarAnaliseCredito() {
        // Arrange
        BigDecimal rendaComprovada = new BigDecimal("4000.00");
        AnaliseCreditoRequestDTO requestDTO = new AnaliseCreditoRequestDTO(1L, rendaComprovada, true, "OK");

        when(cotaRepository.findById(1L)).thenReturn(Optional.of(cota));

        AnaliseCredito analiseEntity = new AnaliseCredito();
        when(mapper.toEntity(requestDTO)).thenReturn(analiseEntity);

        Parcela ultimaParcela = new Parcela();
        ultimaParcela.setValorParcela(new BigDecimal("1000.00")); // Limite é 1200.00 (30% de 4000)
        when(parcelaRepository.findTopByCotaIdOrderByNumeroParcelaDesc(1L)).thenReturn(Optional.of(ultimaParcela));

        when(analiseCreditoRepository.save(any(AnaliseCredito.class))).thenAnswer(i -> {
            AnaliseCredito saved = i.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        Contemplacao contemplacao = new Contemplacao();
        contemplacao.setValorCreditoLiberado(new BigDecimal("10000.00"));
        when(contemplacaoRepository.findTopByCotaIdOrderByDataContemplacaoDesc(1L)).thenReturn(Optional.of(contemplacao));

        AnaliseCreditoResponseDTO responseDTO = new AnaliseCreditoResponseDTO(1L, 1L, rendaComprovada, true, StatusAnalise.APROVADA, LocalDate.now(), "OK");
        when(mapper.toResponse(any())).thenReturn(responseDTO);

        // Act
        AnaliseCreditoResponseDTO result = analiseCreditoService.avaliarAnalise(requestDTO);

        // Assert
        assertEquals(StatusAnalise.APROVADA, result.status());
        verify(cotaService).registrarTransicaoVersao(eq(cota), eq(StatusCota.APROVADO), anyString());
        verify(movimentoService).registrarMovimento(any(), any(), any(), any(), eq(TipoMovimentoFinanceiro.LIBERACAO_CREDITO), eq(NaturezaMovimento.DEBITO), any(), anyString(), any());
    }

    @Test
    void deveReprovarAnalisePorMargem() {
        // Arrange
        BigDecimal rendaComprovada = new BigDecimal("3000.00");
        AnaliseCreditoRequestDTO requestDTO = new AnaliseCreditoRequestDTO(1L, rendaComprovada, true, "OK");

        when(cotaRepository.findById(1L)).thenReturn(Optional.of(cota));

        AnaliseCredito analiseEntity = new AnaliseCredito();
        when(mapper.toEntity(requestDTO)).thenReturn(analiseEntity);

        Parcela ultimaParcela = new Parcela();
        ultimaParcela.setValorParcela(new BigDecimal("1000.00")); // Limite é 900 (30% de 3000) -> Reprova
        when(parcelaRepository.findTopByCotaIdOrderByNumeroParcelaDesc(1L)).thenReturn(Optional.of(ultimaParcela));

        when(analiseCreditoRepository.save(any(AnaliseCredito.class))).thenAnswer(i -> {
            AnaliseCredito saved = i.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        AnaliseCreditoResponseDTO responseDTO = new AnaliseCreditoResponseDTO(1L, 1L, rendaComprovada, true, StatusAnalise.REPROVADA, LocalDate.now(), "Reprovada");
        when(mapper.toResponse(any())).thenReturn(responseDTO);

        // Act
        AnaliseCreditoResponseDTO result = analiseCreditoService.avaliarAnalise(requestDTO);

        // Assert
        assertEquals(StatusAnalise.REPROVADA, result.status());
        verify(cotaService, never()).registrarTransicaoVersao(any(), any(), anyString());
        verify(movimentoService, never()).registrarMovimento(any(), any(), any(), any(), any(), any(), any(), anyString(), any());
    }

    @Test
    void deveReprovarAnalisePorGarantia() {
        // Arrange
        BigDecimal rendaComprovada = new BigDecimal("5000.00");
        AnaliseCreditoRequestDTO requestDTO = new AnaliseCreditoRequestDTO(1L, rendaComprovada, false, "OK"); // Garantia false

        when(cotaRepository.findById(1L)).thenReturn(Optional.of(cota));

        AnaliseCredito analiseEntity = new AnaliseCredito();
        when(mapper.toEntity(requestDTO)).thenReturn(analiseEntity);

        Parcela ultimaParcela = new Parcela();
        ultimaParcela.setValorParcela(new BigDecimal("1000.00")); // Limite é 1500 (30% de 5000) -> Passa na margem
        when(parcelaRepository.findTopByCotaIdOrderByNumeroParcelaDesc(1L)).thenReturn(Optional.of(ultimaParcela));

        when(analiseCreditoRepository.save(any(AnaliseCredito.class))).thenAnswer(i -> {
            AnaliseCredito saved = i.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        AnaliseCreditoResponseDTO responseDTO = new AnaliseCreditoResponseDTO(1L, 1L, rendaComprovada, false, StatusAnalise.REPROVADA, LocalDate.now(), "Reprovada");
        when(mapper.toResponse(any())).thenReturn(responseDTO);

        // Act
        AnaliseCreditoResponseDTO result = analiseCreditoService.avaliarAnalise(requestDTO);

        // Assert
        assertEquals(StatusAnalise.REPROVADA, result.status());
    }
}
