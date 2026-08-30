package br.com.estudo.consorcio.service;

import br.com.estudo.consorcio.domain.dto.SimulacaoApuracaoResponseDTO;
import br.com.estudo.consorcio.domain.enums.StatusCredenciamento;
import br.com.estudo.consorcio.domain.model.Assembleia;
import br.com.estudo.consorcio.domain.model.Cota;
import br.com.estudo.consorcio.domain.model.CredenciamentoLance;
import br.com.estudo.consorcio.domain.model.StatusCota;
import br.com.estudo.consorcio.domain.model.StatusParcela;
import br.com.estudo.consorcio.domain.repository.AssembleiaRepository;
import br.com.estudo.consorcio.domain.repository.CotaRepository;
import br.com.estudo.consorcio.domain.repository.CredenciamentoLanceRepository;
import br.com.estudo.consorcio.domain.repository.ParcelaRepository;
import br.com.estudo.consorcio.exception.RecursoNaoEncontradoException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

@Service
public class AssembleiaAvancadaService {

    private final AssembleiaRepository assembleiaRepository;
    private final CotaRepository cotaRepository;
    private final ParcelaRepository parcelaRepository;
    private final CredenciamentoLanceRepository credenciamentoRepository;

    public AssembleiaAvancadaService(AssembleiaRepository assembleiaRepository,
                                   CotaRepository cotaRepository,
                                   ParcelaRepository parcelaRepository,
                                   CredenciamentoLanceRepository credenciamentoRepository) {
        this.assembleiaRepository = assembleiaRepository;
        this.cotaRepository = cotaRepository;
        this.parcelaRepository = parcelaRepository;
        this.credenciamentoRepository = credenciamentoRepository;
    }

    @Transactional(readOnly = true)
    public SimulacaoApuracaoResponseDTO simularApuracao(Long assembleiaId) {
        Assembleia assembleia = assembleiaRepository.findById(assembleiaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Assembleia não encontrada"));

        Long grupoId = assembleia.getGrupo().getId();
        BigDecimal saldoFC = parcelaRepository.somarFundoComumPorGrupoEStatus(grupoId, StatusParcela.PAGA);
        if (saldoFC == null) saldoFC = BigDecimal.ZERO;
        BigDecimal valorCredito = assembleia.getGrupo().getValorCredito();

        List<Cota> todasCotas = cotaRepository.findByGrupoId(grupoId);
        List<Cota> cotasAtivas = (todasCotas != null) ? todasCotas.stream()
                .filter(c -> c.getStatus() == StatusCota.ATIVA)
                .toList() : List.of();

        List<Integer> sorteados = new ArrayList<>();
        BigDecimal saldoRestante = saldoFC;

        for (Cota cota : cotasAtivas) {
            if (saldoRestante.compareTo(valorCredito) >= 0) {
                sorteados.add(cota.getCodigoCota());
                saldoRestante = saldoRestante.subtract(valorCredito);
                break; // Simulação: 1 contemplado por sorteio prioritário
            }
        }

        List<CredenciamentoLance> lances = credenciamentoRepository.findByAssembleiaIdAndStatus(assembleiaId, StatusCredenciamento.ATIVO);
        List<Integer> lancesVencedores = new ArrayList<>();

        if (lances != null) {
            for (CredenciamentoLance lance : lances) {
                if (lance != null && lance.getCota() != null && saldoRestante.compareTo(valorCredito) >= 0) {
                    lancesVencedores.add(lance.getCota().getCodigoCota());
                    BigDecimal val = lance.getValorLance() != null ? lance.getValorLance() : BigDecimal.ZERO;
                    saldoRestante = saldoRestante.subtract(valorCredito).add(val);
                }
            }
        }

        String hash = gerarHashAuditoria(assembleiaId, sorteados, lancesVencedores);

        return new SimulacaoApuracaoResponseDTO(
                assembleia.getId(),
                grupoId,
                saldoFC,
                sorteados.size(),
                sorteados,
                lancesVencedores.size(),
                lancesVencedores,
                saldoRestante,
                hash
        );
    }

    @Transactional
    public String gerarAuditTrailSha256(Long assembleiaId) {
        Assembleia assembleia = assembleiaRepository.findById(assembleiaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Assembleia não encontrada"));

        return "SHA256-" + gerarHashAuditoria(assembleiaId, List.of(assembleia.getNumeroSorteado() != null ? assembleia.getNumeroSorteado() : 0), List.of());
    }

    private String gerarHashAuditoria(Long assembleiaId, List<Integer> sorteados, List<Integer> lances) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            String raw = "ASS-" + assembleiaId + "-S:" + sorteados.toString() + "-L:" + lances.toString();
            byte[] digest = md.digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            return "MOCK-HASH-" + assembleiaId;
        }
    }
}