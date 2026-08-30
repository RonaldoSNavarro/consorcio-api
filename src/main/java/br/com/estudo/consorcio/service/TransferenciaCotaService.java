package br.com.estudo.consorcio.service;

import br.com.estudo.consorcio.domain.dto.TransferenciaCotaRequestDTO;
import br.com.estudo.consorcio.domain.dto.TransferenciaCotaResponseDTO;
import br.com.estudo.consorcio.domain.enums.CanalNotificacao;
import br.com.estudo.consorcio.domain.enums.StatusTransferencia;
import br.com.estudo.consorcio.domain.enums.TipoNotificacao;
import br.com.estudo.consorcio.domain.model.*;
import br.com.estudo.consorcio.domain.repository.ClienteRepository;
import br.com.estudo.consorcio.domain.repository.CotaRepository;
import br.com.estudo.consorcio.domain.repository.TransferenciaCotaRepository;
import br.com.estudo.consorcio.exception.RecursoNaoEncontradoException;
import br.com.estudo.consorcio.exception.RegraDeNegocioException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class TransferenciaCotaService {

    private final TransferenciaCotaRepository transferenciaRepository;
    private final CotaRepository cotaRepository;
    private final ClienteRepository clienteRepository;
    private final NotificacaoService notificacaoService;

    public TransferenciaCotaService(TransferenciaCotaRepository transferenciaRepository,
                                    CotaRepository cotaRepository,
                                    ClienteRepository clienteRepository,
                                    NotificacaoService notificacaoService) {
        this.transferenciaRepository = transferenciaRepository;
        this.cotaRepository = cotaRepository;
        this.clienteRepository = clienteRepository;
        this.notificacaoService = notificacaoService;
    }

    @Transactional
    public TransferenciaCotaResponseDTO solicitarTransferencia(TransferenciaCotaRequestDTO dto) {
        Cota cota = cotaRepository.findById(dto.cotaId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cota não encontrada com ID: " + dto.cotaId()));

        if (cota.getStatus() != StatusCota.ATIVA && cota.getStatus() != StatusCota.CONTEMPLADA) {
            throw new RegraDeNegocioException("Transferência permitida apenas para cotas ATIVAS ou CONTEMPLADAS. Status atual: " + cota.getStatus());
        }

        Cliente cessionario = clienteRepository.findById(dto.cessionarioId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cessionário não encontrado com ID: " + dto.cessionarioId()));

        if (cessionario.getStatus() != StatusCliente.ATIVO) {
            throw new RegraDeNegocioException("Cessionário deve ser um cliente ativo");
        }

        if (cota.getCliente().getId().equals(cessionario.getId())) {
            throw new RegraDeNegocioException("O cedente e o cessionário não podem ser o mesmo cliente");
        }

        BigDecimal valorCredito = (cota.getGrupo() != null && cota.getGrupo().getValorCredito() != null)
                ? cota.getGrupo().getValorCredito()
                : new BigDecimal("100000.00");

        BigDecimal taxa = dto.taxaTransferencia() != null ? dto.taxaTransferencia() :
                valorCredito.multiply(new BigDecimal("0.02")); // 2% taxa padrão

        TransferenciaCota transf = TransferenciaCota.builder()
                .cota(cota)
                .cedente(cota.getCliente())
                .cessionario(cessionario)
                .dataSolicitacao(LocalDateTime.now())
                .taxaTransferencia(taxa)
                .status(StatusTransferencia.EM_ANALISE_CREDITO)
                .observacao(dto.observacao())
                .build();

        transf = transferenciaRepository.save(transf);

        // Notifica as partes
        notificacaoService.criarENotificar(
                TipoNotificacao.PAGAMENTO,
                cota.getCliente(),
                cota,
                "Solicitação de Transferência de Cota",
                "Sua solicitação de transferência da cota " + cota.getCodigoCota() + " foi recebida e está em análise de crédito.",
                CanalNotificacao.EMAIL
        );

        return toDTO(transf);
    }

    @Transactional
    public TransferenciaCotaResponseDTO avaliarTransferencia(Long transferenciaId, boolean aprovado, String motivoRecusa) {
        TransferenciaCota transf = transferenciaRepository.findById(transferenciaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Transferência não encontrada com ID: " + transferenciaId));

        if (transf.getStatus() != StatusTransferencia.EM_ANALISE_CREDITO && transf.getStatus() != StatusTransferencia.SOLICITADA) {
            throw new RegraDeNegocioException("Transferência não está em análise de crédito");
        }

        if (aprovado) {
            transf.setStatus(StatusTransferencia.PENDENTE_PAGAMENTO_TAXA);
        } else {
            transf.setStatus(StatusTransferencia.RECUSADA);
            transf.setMotivoRecusa(motivoRecusa != null ? motivoRecusa : "Reprovado na esteira de crédito/compliance");
        }

        transf.setUpdatedAt(LocalDateTime.now());
        return toDTO(transferenciaRepository.save(transf));
    }

    @Transactional
    public TransferenciaCotaResponseDTO efetivarTransferencia(Long transferenciaId) {
        TransferenciaCota transf = transferenciaRepository.findById(transferenciaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Transferência não encontrada com ID: " + transferenciaId));

        if (transf.getStatus() != StatusTransferencia.PENDENTE_PAGAMENTO_TAXA && transf.getStatus() != StatusTransferencia.APROVADA) {
            throw new RegraDeNegocioException("Transferência deve estar aprovada e com taxa quitada para efetivação");
        }

        Cota cota = transf.getCota();
        Cliente novoTitular = transf.getCessionario();

        cota.setCliente(novoTitular);
        cotaRepository.save(cota);

        transf.setStatus(StatusTransferencia.EFETIVADA);
        transf.setDataEfetivacao(LocalDateTime.now());
        transf.setUpdatedAt(LocalDateTime.now());
        transf = transferenciaRepository.save(transf);

        // Notifica novo titular
        notificacaoService.criarENotificar(
                TipoNotificacao.PAGAMENTO,
                novoTitular,
                cota,
                "Transferência de Cota Concluída",
                "Parabéns! Você é o novo titular da cota " + cota.getCodigoCota() + " do grupo " + (cota.getGrupo() != null ? cota.getGrupo().getCodigoGrupo() : "") + ".",
                CanalNotificacao.EMAIL
        );

        return toDTO(transf);
    }

    @Transactional(readOnly = true)
    public List<TransferenciaCotaResponseDTO> listarPorCota(Long cotaId) {
        return transferenciaRepository.findByCotaIdOrderByDataSolicitacaoDesc(cotaId)
                .stream()
                .map(this::toDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TransferenciaCotaResponseDTO> listarTodas() {
        return transferenciaRepository.findAll()
                .stream()
                .map(this::toDTO)
                .toList();
    }

    private TransferenciaCotaResponseDTO toDTO(TransferenciaCota t) {
        return new TransferenciaCotaResponseDTO(
                t.getId(),
                t.getCota().getId(),
                t.getCota().getGrupo() != null ? t.getCota().getGrupo().getCodigoGrupo() : null,
                t.getCota().getCodigoCota(),
                t.getCedente().getId(),
                t.getCedente().getNome(),
                t.getCedente().getCpfCnpj(),
                t.getCessionario().getId(),
                t.getCessionario().getNome(),
                t.getCessionario().getCpfCnpj(),
                t.getDataSolicitacao(),
                t.getDataEfetivacao(),
                t.getTaxaTransferencia(),
                t.getStatus(),
                t.getMotivoRecusa(),
                t.getObservacao()
        );
    }
}