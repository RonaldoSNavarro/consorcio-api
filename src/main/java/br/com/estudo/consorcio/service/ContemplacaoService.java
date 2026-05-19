package br.com.estudo.consorcio.service;

import br.com.estudo.consorcio.domain.dto.ContemplacaoRequestDTO;
import br.com.estudo.consorcio.domain.dto.ContemplacaoResponseDTO;
import br.com.estudo.consorcio.domain.model.*;
import br.com.estudo.consorcio.domain.repository.AssembleiaRepository;
import br.com.estudo.consorcio.domain.repository.ContemplacaoRepository;
import br.com.estudo.consorcio.domain.repository.CotaRepository;
import br.com.estudo.consorcio.domain.repository.ParcelaRepository;
import br.com.estudo.consorcio.exception.RegraDeNegocioException;
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
    public ContemplacaoResponseDTO registrar(ContemplacaoRequestDTO dto) {
        // 1. Buscas de Integridade
        Assembleia assembleia = assembleiaRepository.findById(dto.assembleiaId())
                .orElseThrow(() -> new RegraDeNegocioException("Assembleia não encontrada."));

        Cota cota = cotaRepository.findById(dto.cotaId())
                .orElseThrow(() -> new RegraDeNegocioException("Cota não encontrada."));

        if (!cota.getGrupo().getId().equals(assembleia.getGrupo().getId())) {
            throw new RegraDeNegocioException("A cota e a assembleia pertencem a grupos diferentes.");
        }

        if (cota.getStatus() != StatusCota.ATIVA) {
            throw new RegraDeNegocioException("Apenas cotas com status ATIVA podem ser contempladas.");
        }

        // 2. Mapeamento inicial
        Contemplacao contemplacao = new Contemplacao();
        contemplacao.setAssembleia(assembleia);
        contemplacao.setCota(cota);
        contemplacao.setTipoContemplacao(dto.tipoContemplacao());
        contemplacao.setValorLance(dto.valorLance());
        contemplacao.setLanceEmbutido(dto.lanceEmbutido() != null ? dto.lanceEmbutido() : false);

        BigDecimal valorCreditoGrupo = assembleia.getGrupo().getValorCredito();
        BigDecimal valorCreditoLiberado = valorCreditoGrupo;

        // --- REGRA DO BANCO CENTRAL: LANCE EMBUTIDO (MÁXIMO 30%) --- //
        if (Boolean.TRUE.equals(contemplacao.getLanceEmbutido())) {

            if (contemplacao.getValorLance() == null || contemplacao.getValorLance().compareTo(BigDecimal.ZERO) <= 0) {
                throw new RegraDeNegocioException("Para lances embutidos, o valor do lance deve ser informado.");
            }

            BigDecimal limiteEmbutido = valorCreditoGrupo.multiply(new BigDecimal("0.30")).setScale(2, RoundingMode.HALF_UP);

            if (contemplacao.getValorLance().compareTo(limiteEmbutido) > 0) {
                throw new RegraDeNegocioException("O valor do lance embutido não pode ultrapassar 30% do crédito (Máximo permitido: R$ " + limiteEmbutido + ").");
            }

            valorCreditoLiberado = valorCreditoGrupo.subtract(contemplacao.getValorLance());
        }

        contemplacao.setValorCreditoLiberado(valorCreditoLiberado);
        // ---------------------------------------------------------------- //

        // --- REGRA DO BANCO CENTRAL: VALIDAÇÃO DO FUNDO COMUM --- //
        BigDecimal saldoFundoComum = parcelaRepository.somarFundoComumPorGrupoEStatus(
                assembleia.getGrupo().getId(),
                StatusParcela.PAGA
        );

        if (saldoFundoComum.compareTo(valorCreditoLiberado) < 0) {
            throw new RegraDeNegocioException("REGRA BCB: Saldo insuficiente no Fundo Comum do grupo. Saldo atual: R$ "
                    + saldoFundoComum + " | Necessário para liberação: R$ " + valorCreditoLiberado);
        }
        // -------------------------------------------------------- //

        contemplacao.setDataContemplacao(LocalDate.now());

        // 3. Persistência
        Contemplacao contemplacaoSalva = contemplacaoRepository.save(contemplacao);

        cota.setStatus(StatusCota.CONTEMPLADA);
        cotaRepository.save(cota);

        // 4. Retorno Mapeado
        return converterParaResponseDTO(contemplacaoSalva);
    }

    public List<ContemplacaoResponseDTO> listarPorAssembleia(Long assembleiaId) {
        return contemplacaoRepository.findByAssembleiaId(assembleiaId).stream()
                .map(this::converterParaResponseDTO)
                .toList();
    }

    // Método de conversão centralizado
    private ContemplacaoResponseDTO converterParaResponseDTO(Contemplacao contemplacao) {
        return new ContemplacaoResponseDTO(
                contemplacao.getId(),
                contemplacao.getCota().getId(),
                contemplacao.getAssembleia().getId(),
                contemplacao.getTipoContemplacao(),
                contemplacao.getValorLance(),
                contemplacao.getDataContemplacao(),
                contemplacao.getLanceEmbutido(),
                contemplacao.getValorCreditoLiberado()
        );
    }
}