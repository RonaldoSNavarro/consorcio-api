package br.com.estudo.consorcio.service;

import br.com.estudo.consorcio.domain.model.*;
import br.com.estudo.consorcio.domain.repository.AssembleiaRepository;
import br.com.estudo.consorcio.domain.repository.ContemplacaoRepository;
import br.com.estudo.consorcio.domain.repository.CotaRepository;
import br.com.estudo.consorcio.domain.repository.ParcelaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
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

        BigDecimal valorCreditoGrupo = assembleia.getGrupo().getValorCredito();
        BigDecimal valorCreditoLiberado = valorCreditoGrupo; // Por padrão, libera o valor total

        // --- REGRA DO BANCO CENTRAL: LANCE EMBUTIDO (MÁXIMO 30%) --- //
        if (Boolean.TRUE.equals(contemplacao.getLanceEmbutido())) {

            if (contemplacao.getValorLance() == null || contemplacao.getValorLance().compareTo(BigDecimal.ZERO) <= 0) {
                throw new RuntimeException("Para lances embutidos, o valor do lance deve ser informado.");
            }

            BigDecimal limiteEmbutido = valorCreditoGrupo.multiply(new BigDecimal("0.30")).setScale(2, RoundingMode.HALF_UP);

            if (contemplacao.getValorLance().compareTo(limiteEmbutido) > 0) {
                throw new RuntimeException("O valor do lance embutido não pode ultrapassar 30% do crédito (Máximo permitido: R$ " + limiteEmbutido + ").");
            }

            // O cliente "paga" o lance com o próprio crédito, então ele recebe menos dinheiro líquido
            valorCreditoLiberado = valorCreditoGrupo.subtract(contemplacao.getValorLance());
        }

        contemplacao.setValorCreditoLiberado(valorCreditoLiberado);
        // ---------------------------------------------------------------- //

        // --- REGRA DO BANCO CENTRAL: VALIDAÇÃO DO FUNDO COMUM --- //
        BigDecimal saldoFundoComum = parcelaRepository.somarFundoComumPorGrupoEStatus(
                assembleia.getGrupo().getId(),
                StatusParcela.PAGA
        );

        // A validação agora checa se o grupo tem saldo para pagar o valor LÍQUIDO liberado
        if (saldoFundoComum.compareTo(valorCreditoLiberado) < 0) {
            throw new RuntimeException("REGRA BCB: Saldo insuficiente no Fundo Comum do grupo. Saldo atual: R$ "
                    + saldoFundoComum + " | Necessário para liberação: R$ " + valorCreditoLiberado);
        }
        // -------------------------------------------------------- //

        contemplacao.setDataContemplacao(LocalDate.now());
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