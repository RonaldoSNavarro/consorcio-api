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
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

@Service
public class AnaliseCreditoService {

    private final AnaliseCreditoRepository analiseCreditoRepository;
    private final CotaRepository cotaRepository;
    private final ParcelaRepository parcelaRepository;
    private final ContemplacaoRepository contemplacaoRepository;
    private final AnaliseCreditoMapper mapper;
    private final MovimentoFinanceiroService movimentoService;
    private final CotaService cotaService;
    private final HistoricoConsorciadoService historicoService;

    public AnaliseCreditoService(AnaliseCreditoRepository analiseCreditoRepository, CotaRepository cotaRepository,
                                 ParcelaRepository parcelaRepository, ContemplacaoRepository contemplacaoRepository,
                                 AnaliseCreditoMapper mapper, MovimentoFinanceiroService movimentoService,
                                 CotaService cotaService, HistoricoConsorciadoService historicoService) {
        this.analiseCreditoRepository = analiseCreditoRepository;
        this.cotaRepository = cotaRepository;
        this.parcelaRepository = parcelaRepository;
        this.contemplacaoRepository = contemplacaoRepository;
        this.mapper = mapper;
        this.movimentoService = movimentoService;
        this.cotaService = cotaService;
        this.historicoService = historicoService;
    }

    private Usuario getUsuarioAutenticado() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Usuario) {
            return (Usuario) authentication.getPrincipal();
        }
        return null;
    }

    @Transactional
    public AnaliseCreditoResponseDTO avaliarAnalise(AnaliseCreditoRequestDTO dto) {
        Cota cota = cotaRepository.findById(dto.cotaId())
                .orElseThrow(() -> new RegraDeNegocioException("Cota não encontrada."));

        if (cota.getStatus() != StatusCota.AGUARDANDO_ANALISE) {
            throw new RegraDeNegocioException("Cota não está aguardando análise de crédito pós-contemplação. Status atual: " + cota.getStatus());
        }

        AnaliseCredito analise = mapper.toEntity(dto);
        analise.setCota(cota);
        analise.setDataAnalise(LocalDate.now());

        // Buscar a última parcela gerada para obter a Parcela Base atualizada
        Parcela ultimaParcela = parcelaRepository.findTopByCotaIdOrderByNumeroParcelaDesc(cota.getId())
                .orElseThrow(() -> new RegraDeNegocioException("Não foram encontradas parcelas para validar a margem consignável."));

        BigDecimal valorParcela = ultimaParcela.getValorParcela();
        
        // Regra da margem de consignação: Parcela não deve comprometer mais de 30% da renda.
        BigDecimal limiteComprometimento = dto.rendaComprovada().multiply(new BigDecimal("0.30")).setScale(2, RoundingMode.HALF_UP);

        boolean aprovada = true;
        StringBuilder observacoesAutomaticas = new StringBuilder();
        if (dto.observacao() != null) {
            observacoesAutomaticas.append(dto.observacao());
        }

        if (valorParcela.compareTo(limiteComprometimento) > 0) {
            aprovada = false;
            observacoesAutomaticas.append(" | Reprovada: A parcela de R$ ").append(valorParcela)
                    .append(" compromete mais de 30% da renda comprovada de R$ ").append(dto.rendaComprovada())
                    .append(" (Limite: R$ ").append(limiteComprometimento).append(").");
        }

        if (!Boolean.TRUE.equals(dto.garantiaAprovada())) {
            aprovada = false;
            observacoesAutomaticas.append(" | Reprovada: A garantia exigida não foi aprovada ou foi insuficiente.");
        }

        if (aprovada) {
            analise.setStatus(StatusAnalise.APROVADA);
            analise.setObservacao(observacoesAutomaticas.toString());
        } else {
            analise.setStatus(StatusAnalise.REPROVADA);
            analise.setObservacao(observacoesAutomaticas.toString());
        }

        AnaliseCredito analiseSalva = analiseCreditoRepository.save(analise);
        Usuario usuario = getUsuarioAutenticado();

        if (analiseSalva.getStatus() == StatusAnalise.APROVADA) {
            // Cota é aprovada para pagamento de bem
            cotaService.registrarTransicaoVersao(cota, StatusCota.APROVADO, "Análise de Crédito aprovada");

            Contemplacao contemplacao = contemplacaoRepository.findTopByCotaIdOrderByDataContemplacaoDesc(cota.getId())
                    .orElseThrow(() -> new RegraDeNegocioException("Registro de contemplação não encontrado para esta cota."));

            movimentoService.registrarMovimento(cota.getGrupo(), cota, null, contemplacao,
                    TipoMovimentoFinanceiro.LIBERACAO_CREDITO, NaturezaMovimento.DEBITO,
                    contemplacao.getValorCreditoLiberado(), "Liberação de carta de crédito após aprovação na análise de crédito - Cota " + cota.getNumeroCota(), usuario);

            historicoService.registrarInteracao(
                    cota.getCliente(), cota, cota.getGrupo(), null,
                    TipoInteracao.ANALISE_CREDITO, "Análise de Crédito Aprovada",
                    null, null, null, null, null, null, null, usuario);
        } else {
            historicoService.registrarInteracao(
                    cota.getCliente(), cota, cota.getGrupo(), null,
                    TipoInteracao.ANALISE_CREDITO, "Análise de Crédito Reprovada. Motivos: " + analiseSalva.getObservacao(),
                    null, null, null, null, null, null, null, usuario);
        }

        return mapper.toResponse(analiseSalva);
    }
}
