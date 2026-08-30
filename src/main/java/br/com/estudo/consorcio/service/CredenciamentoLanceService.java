package br.com.estudo.consorcio.service;

import br.com.estudo.consorcio.domain.dto.CredenciamentoLanceRequestDTO;
import br.com.estudo.consorcio.domain.dto.CredenciamentoLanceResponseDTO;
import br.com.estudo.consorcio.domain.enums.StatusCredenciamento;
import br.com.estudo.consorcio.domain.model.Assembleia;
import br.com.estudo.consorcio.domain.model.Cota;
import br.com.estudo.consorcio.domain.model.CredenciamentoLance;
import br.com.estudo.consorcio.domain.model.Parcela;
import br.com.estudo.consorcio.domain.model.StatusAssembleia;
import br.com.estudo.consorcio.domain.model.StatusCota;
import br.com.estudo.consorcio.domain.model.StatusParcela;
import br.com.estudo.consorcio.domain.repository.AssembleiaRepository;
import br.com.estudo.consorcio.domain.repository.CotaRepository;
import br.com.estudo.consorcio.domain.repository.CredenciamentoLanceRepository;
import br.com.estudo.consorcio.domain.repository.ParcelaRepository;
import br.com.estudo.consorcio.exception.RecursoNaoEncontradoException;
import br.com.estudo.consorcio.exception.RegraDeNegocioException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;

@Service
public class CredenciamentoLanceService {

    private final CredenciamentoLanceRepository credenciamentoRepository;
    private final CotaRepository cotaRepository;
    private final AssembleiaRepository assembleiaRepository;
    private final ParcelaRepository parcelaRepository;

    public CredenciamentoLanceService(CredenciamentoLanceRepository credenciamentoRepository,
                                    CotaRepository cotaRepository,
                                    AssembleiaRepository assembleiaRepository,
                                    ParcelaRepository parcelaRepository) {
        this.credenciamentoRepository = credenciamentoRepository;
        this.cotaRepository = cotaRepository;
        this.assembleiaRepository = assembleiaRepository;
        this.parcelaRepository = parcelaRepository;
    }

    @Transactional
    public CredenciamentoLanceResponseDTO credenciarLance(CredenciamentoLanceRequestDTO dto, String ipOrigem) {
        Cota cota = cotaRepository.findById(dto.cotaId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cota não encontrada"));

        Assembleia assembleia = assembleiaRepository.findById(dto.assembleiaId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Assembleia não encontrada"));

        // RN-CPL-001: Apenas cotas ATIVAS
        if (cota.getStatus() != StatusCota.ATIVA) {
            throw new RegraDeNegocioException("Cota não se encontra ATIVA para participar do credenciamento");
        }

        // RN-CPL-001: Sem parcelas em atraso
        List<Parcela> parcelas = parcelaRepository.findByCotaId(cota.getId());
        boolean possuiAtraso = parcelas.stream().anyMatch(p -> p.getStatus() == StatusParcela.ATRASADA);
        if (possuiAtraso) {
            throw new RegraDeNegocioException("Cota com pendências financeiras não é elegível para lance");
        }

        // RN-CPL-003: Janela de captação deve estar aberta (AGENDADA)
        if (assembleia.getStatus() != StatusAssembleia.AGENDADA) {
            throw new RegraDeNegocioException("Janela de captação de lances encerrada para esta assembleia");
        }

        // RN-CPL-002: Apenas um credenciamento ativo por cota/assembleia
        credenciamentoRepository.findByCotaIdAndAssembleiaIdAndStatus(cota.getId(), assembleia.getId(), StatusCredenciamento.ATIVO)
                .ifPresent(c -> {
                    throw new RegraDeNegocioException("Já existe um lance ativo registrado para esta cota nesta assembleia");
                });

        // Hash SHA-256 de auditoria do comprovante
        String assinatura = gerarHashAssinatura(cota.getId(), assembleia.getId(), dto.valorLance());

        CredenciamentoLance lance = CredenciamentoLance.builder()
                .cota(cota)
                .assembleia(assembleia)
                .tipoLance(dto.tipoLance())
                .modalidade(dto.modalidade())
                .valorLance(dto.valorLance())
                .valorFgts(dto.valorFgts())
                .valorEmbutido(dto.valorEmbutido())
                .valorProprio(dto.valorProprio())
                .termosAceitos(dto.termosAceitos() != null ? dto.termosAceitos() : true)
                .ipOrigem(ipOrigem)
                .hashAssinatura(assinatura)
                .status(StatusCredenciamento.ATIVO)
                .build();

        lance = credenciamentoRepository.save(lance);

        return toDTO(lance);
    }

    @Transactional
    public void cancelarCredenciamento(Long credenciamentoId) {
        CredenciamentoLance lance = credenciamentoRepository.findById(credenciamentoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Credenciamento de lance não encontrado"));

        if (lance.getAssembleia().getStatus() != StatusAssembleia.AGENDADA) {
            throw new RegraDeNegocioException("Não é possível cancelar lance após o encerramento da janela de captação");
        }

        lance.setStatus(StatusCredenciamento.CANCELADO);
        lance.setUpdatedAt(LocalDateTime.now());
        credenciamentoRepository.save(lance);
    }

    @Transactional(readOnly = true)
    public List<CredenciamentoLanceResponseDTO> listarPorAssembleia(Long assembleiaId) {
        return credenciamentoRepository.findByAssembleiaIdAndStatus(assembleiaId, StatusCredenciamento.ATIVO)
                .stream().map(this::toDTO).toList();
    }

    private String gerarHashAssinatura(Long cotaId, Long assembleiaId, java.math.BigDecimal valor) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            String raw = cotaId + "-" + assembleiaId + "-" + valor + "-" + System.currentTimeMillis();
            byte[] digest = md.digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            return "HASH-" + System.currentTimeMillis();
        }
    }

    private CredenciamentoLanceResponseDTO toDTO(CredenciamentoLance c) {
        return new CredenciamentoLanceResponseDTO(
                c.getId(),
                c.getCota().getId(),
                c.getCota().getCodigoCota(),
                c.getAssembleia().getId(),
                c.getTipoLance(),
                c.getModalidade(),
                c.getValorLance(),
                c.getValorFgts(),
                c.getValorEmbutido(),
                c.getValorProprio(),
                c.getStatus(),
                c.getHashAssinatura(),
                c.getCreatedAt()
        );
    }
}