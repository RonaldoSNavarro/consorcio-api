package br.com.estudo.consorcio.service;

import br.com.estudo.consorcio.domain.model.*;
import br.com.estudo.consorcio.domain.repository.AssembleiaRepository;
import br.com.estudo.consorcio.domain.repository.ContemplacaoRepository;
import br.com.estudo.consorcio.domain.repository.CotaRepository;
import br.com.estudo.consorcio.domain.repository.ParcelaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class ContemplacaoService {

    private final ContemplacaoRepository contemplacaoRepository;
    private final AssembleiaRepository assembleiaRepository;
    private final CotaRepository cotaRepository;
    private final ParcelaRepository parcelaRepository;

    public ContemplacaoService(ContemplacaoRepository contemplacaoRepository, AssembleiaRepository assembleiaRepository, CotaRepository cotaRepository, ParcelaRepository parcelaRepository) {
        this.contemplacaoRepository = contemplacaoRepository;
        this.assembleiaRepository = assembleiaRepository;
        this.cotaRepository = cotaRepository;
        this.parcelaRepository = parcelaRepository;
    }

    @Transactional
    public Contemplacao registrar(Contemplacao contemplacao) {
        Assembleia assembleia = assembleiaRepository.findById(contemplacao.getAssembleia().getId())
                .orElseThrow(() -> new RuntimeException("Assembleia não encontrada."));

        Cota cota = cotaRepository.findById(contemplacao.getCota().getId())
                .orElseThrow(() -> new RuntimeException("Cota não encontrada."));

        if (!cota.getGrupo().getId().equals(assembleia.getGrupo().getId())) {
            throw new RuntimeException("A cota e a assembleia pertencem a grupos diferentes.");
        }

        if (cota.getStatus() != StatusCota.ATIVA) {
            throw new RuntimeException("Apenas cotas com status ATIVA podem ser contempladas.");
        }

        // --- REGRA DO BANCO CENTRAL: VALIDAÇÃO DO FUNDO COMUM --- //
        BigDecimal saldoFundoComum = parcelaRepository.somarFundoComumPorGrupoEStatus(
                assembleia.getGrupo().getId(),
                StatusParcela.PAGA
        );

        BigDecimal valorCredito = assembleia.getGrupo().getValorCredito();

        if (saldoFundoComum.compareTo(valorCredito) < 0) {
            throw new RuntimeException("REGRA BCB: Saldo insuficiente no Fundo Comum do grupo. Saldo atual: R$ "
                    + saldoFundoComum + " | Necessário: R$ " + valorCredito);
        }
        // -------------------------------------------------------- //

        contemplacao.setAssembleia(assembleia);
        contemplacao.setCota(cota);
        Contemplacao contemplacaoSalva = contemplacaoRepository.save(contemplacao);

        cota.setStatus(StatusCota.CONTEMPLADA);
        cotaRepository.save(cota);

        return contemplacaoSalva;
    }

    public List<Contemplacao> listarPorAssembleia(Long assembleiaId) {
        return contemplacaoRepository.findByAssembleiaId(assembleiaId);
    }
}