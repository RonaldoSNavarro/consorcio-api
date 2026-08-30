package br.com.estudo.consorcio.service;

import br.com.estudo.consorcio.domain.dto.BoletoPixResponseDTO;
import br.com.estudo.consorcio.domain.dto.WebhookPixRequestDTO;
import br.com.estudo.consorcio.domain.enums.StatusPagamentoBancario;
import br.com.estudo.consorcio.domain.model.StatusParcela;
import br.com.estudo.consorcio.domain.model.Cliente;
import br.com.estudo.consorcio.domain.model.Cota;
import br.com.estudo.consorcio.domain.model.PagamentoBancario;
import br.com.estudo.consorcio.domain.model.Parcela;
import br.com.estudo.consorcio.domain.repository.PagamentoBancarioRepository;
import br.com.estudo.consorcio.domain.repository.ParcelaRepository;
import org.junit.jupiter.api.BeforeEach;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IntegracaoBancariaServiceTest {

    @Mock
    private PagamentoBancarioRepository pagamentoRepository;

    @Mock
    private ParcelaRepository parcelaRepository;

    @Mock
    private NotificacaoService notificacaoService;

    @InjectMocks
    private IntegracaoBancariaService service;

    private Parcela parcela;
    private Cota cota;
    private Cliente cliente;

    @BeforeEach
    void setUp() {
        cliente = new Cliente();
        cliente.setId(1L);
        cliente.setNome("Joao Silva");

        cota = new Cota();
        cota.setId(10L);
        cota.setCodigoCota(5);
        cota.setCliente(cliente);

        parcela = new Parcela();
        parcela.setId(50L);
        parcela.setNumeroParcela(1);
        parcela.setValorParcela(new BigDecimal("1250.00"));
        parcela.setDataVencimento(LocalDate.now().plusDays(10));
        parcela.setStatus(StatusParcela.PENDENTE);
        parcela.setCota(cota);
    }

    @Test
    @DisplayName("Deve emitir cobrança de boleto e PIX para parcela pendente")
    void deveEmitirCobrancaParcela() {
        when(parcelaRepository.findById(50L)).thenReturn(Optional.of(parcela));
        when(pagamentoRepository.save(any(PagamentoBancario.class))).thenAnswer(i -> {
            PagamentoBancario p = i.getArgument(0);
            p.setId(1L);
            return p;
        });

        BoletoPixResponseDTO cobranca = service.emitirCobrancaParcela(50L);

        assertNotNull(cobranca);
        assertEquals(50L, cobranca.parcelaId());
        assertEquals(StatusPagamentoBancario.AGUARDANDO_PAGAMENTO, cobranca.status());
        assertNotNull(cobranca.linhaDigitavel());
        assertNotNull(cobranca.pixCopiaECola());
    }

    @Test
    @DisplayName("Deve processar webhook de pagamento PIX e quitar parcela")
    void deveProcessarWebhookPix() {
        PagamentoBancario pag = PagamentoBancario.builder()
                .id(1L)
                .txid("TXID-123456")
                .cota(cota)
                .parcela(parcela)
                .valorCobrado(new BigDecimal("1250.00"))
                .dataVencimento(LocalDate.now().plusDays(5))
                .status(StatusPagamentoBancario.AGUARDANDO_PAGAMENTO)
                .build();

        WebhookPixRequestDTO webhook = new WebhookPixRequestDTO("TXID-123456", new BigDecimal("1250.00"), new BigDecimal("1.50"), "E2E-999");

        when(pagamentoRepository.findByTxid("TXID-123456")).thenReturn(Optional.of(pag));
        when(pagamentoRepository.save(any(PagamentoBancario.class))).thenAnswer(i -> i.getArgument(0));

        BoletoPixResponseDTO resultado = service.processarWebhookPix(webhook);

        assertNotNull(resultado);
        assertEquals(StatusPagamentoBancario.PAGO, resultado.status());
        assertEquals(StatusParcela.PAGA, parcela.getStatus());
        verify(parcelaRepository).save(parcela);
        verify(notificacaoService).criarENotificar(any(), any(), any(), anyString(), anyString(), any());
    }
}