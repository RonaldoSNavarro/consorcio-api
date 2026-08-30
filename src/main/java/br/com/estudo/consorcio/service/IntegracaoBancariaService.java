package br.com.estudo.consorcio.service;

import br.com.estudo.consorcio.domain.dto.BoletoPixResponseDTO;
import br.com.estudo.consorcio.domain.dto.WebhookPixRequestDTO;
import br.com.estudo.consorcio.domain.enums.CanalNotificacao;
import br.com.estudo.consorcio.domain.enums.StatusPagamentoBancario;
import br.com.estudo.consorcio.domain.enums.TipoNotificacao;
import br.com.estudo.consorcio.domain.model.PagamentoBancario;
import br.com.estudo.consorcio.domain.model.Parcela;
import br.com.estudo.consorcio.domain.model.StatusParcela;
import br.com.estudo.consorcio.domain.repository.PagamentoBancarioRepository;
import br.com.estudo.consorcio.domain.repository.ParcelaRepository;
import br.com.estudo.consorcio.exception.RecursoNaoEncontradoException;
import br.com.estudo.consorcio.exception.RegraDeNegocioException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class IntegracaoBancariaService {

    private final PagamentoBancarioRepository pagamentoRepository;
    private final ParcelaRepository parcelaRepository;
    private final NotificacaoService notificacaoService;

    public IntegracaoBancariaService(PagamentoBancarioRepository pagamentoRepository,
                                   ParcelaRepository parcelaRepository,
                                   NotificacaoService notificacaoService) {
        this.pagamentoRepository = pagamentoRepository;
        this.parcelaRepository = parcelaRepository;
        this.notificacaoService = notificacaoService;
    }

    @Transactional
    public BoletoPixResponseDTO emitirCobrancaParcela(Long parcelaId) {
        Parcela parcela = parcelaRepository.findById(parcelaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Parcela não encontrada"));

        if (parcela.getStatus() == StatusParcela.PAGA) {
            throw new RegraDeNegocioException("Parcela já se encontra quitada");
        }

        String txid = "TXID-" + UUID.randomUUID().toString().substring(0, 18).toUpperCase();
        String linhaDigitavel = "23790.00109 00001.234567 89012.345678 1 95000000" + parcela.getValorParcela().intValue();
        String codigoBarras = "2379195000000" + parcela.getValorParcela().intValue() + "0000123456789012345678";
        String pixCopiaECola = "00020126580014br.gov.bcb.pix0136" + UUID.randomUUID() + "520400005303986540" + parcela.getValorParcela() + "5802BR5913CONSORCIO API6009SAO PAULO62070503***6304";

        PagamentoBancario pag = PagamentoBancario.builder()
                .cota(parcela.getCota())
                .parcela(parcela)
                .txid(txid)
                .linhaDigitavel(linhaDigitavel)
                .codigoBarras(codigoBarras)
                .pixCopiaCola(pixCopiaECola)
                .valorCobrado(parcela.getValorParcela())
                .dataVencimento(parcela.getDataVencimento())
                .status(StatusPagamentoBancario.AGUARDANDO_PAGAMENTO)
                .build();

        pag = pagamentoRepository.save(pag);

        return toDTO(pag);
    }

    @Transactional
    public BoletoPixResponseDTO processarWebhookPix(WebhookPixRequestDTO dto) {
        PagamentoBancario pag = pagamentoRepository.findByTxid(dto.txid())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Pagamento com txid não encontrado: " + dto.txid()));

        if (pag.getStatus() == StatusPagamentoBancario.PAGO) {
            return toDTO(pag); // Idempotente
        }

        pag.setValorPago(dto.valorPago());
        pag.setTaxaBancaria(dto.taxaBancaria() != null ? dto.taxaBancaria() : BigDecimal.ZERO);
        pag.setDataPagamento(LocalDateTime.now());
        pag.setStatus(StatusPagamentoBancario.PAGO);
        pag.setUpdatedAt(LocalDateTime.now());
        pag = pagamentoRepository.save(pag);

        // Baixa na parcela associada
        if (pag.getParcela() != null) {
            Parcela parcela = pag.getParcela();
            parcela.setStatus(StatusParcela.PAGA);
            parcela.setValorPago(dto.valorPago());
            parcela.setDataPagamento(LocalDate.now());
            parcelaRepository.save(parcela);
        }

        // Notifica o consorciado
        notificacaoService.criarENotificar(
                TipoNotificacao.PAGAMENTO,
                pag.getCota().getCliente(),
                pag.getCota(),
                "Confirmação de Pagamento Recebido",
                "Seu pagamento no valor de R$ " + dto.valorPago() + " referente à cota " + pag.getCota().getCodigoCota() + " foi processado com sucesso.",
                CanalNotificacao.EMAIL
        );

        return toDTO(pag);
    }

    private BoletoPixResponseDTO toDTO(PagamentoBancario p) {
        return new BoletoPixResponseDTO(
                p.getId(),
                p.getCota().getId(),
                p.getParcela() != null ? p.getParcela().getId() : null,
                p.getLinhaDigitavel(),
                p.getCodigoBarras(),
                p.getTxid(),
                p.getPixCopiaCola(),
                p.getValorCobrado(),
                p.getDataVencimento(),
                p.getStatus()
        );
    }
}