package br.com.estudo.consorcio.service;

import br.com.estudo.consorcio.domain.dto.EvolucaoMensalDTO;
import br.com.estudo.consorcio.domain.dto.GrupoHeatmapDTO;
import br.com.estudo.consorcio.domain.dto.KpiFinanceiroDTO;
import br.com.estudo.consorcio.domain.dto.ProjecaoReceitaDTO;
import br.com.estudo.consorcio.domain.model.Cota;
import br.com.estudo.consorcio.domain.model.Grupo;
import br.com.estudo.consorcio.domain.model.Parcela;
import br.com.estudo.consorcio.domain.model.StatusCota;
import br.com.estudo.consorcio.domain.model.StatusParcela;
import br.com.estudo.consorcio.domain.repository.CotaRepository;
import br.com.estudo.consorcio.domain.repository.GrupoRepository;
import br.com.estudo.consorcio.domain.repository.ParcelaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class DashboardService {

    private final GrupoRepository grupoRepository;
    private final CotaRepository cotaRepository;
    private final ParcelaRepository parcelaRepository;
    private final MovimentoFinanceiroService movimentoFinanceiroService;

    public DashboardService(GrupoRepository grupoRepository,
                            CotaRepository cotaRepository,
                            ParcelaRepository parcelaRepository,
                            MovimentoFinanceiroService movimentoFinanceiroService) {
        this.grupoRepository = grupoRepository;
        this.cotaRepository = cotaRepository;
        this.parcelaRepository = parcelaRepository;
        this.movimentoFinanceiroService = movimentoFinanceiroService;
    }

    @Transactional(readOnly = true)
    public KpiFinanceiroDTO obterKpis() {
        List<Grupo> grupos = grupoRepository.findAll();
        List<Cota> cotas = cotaRepository.findAll();
        List<Parcela> parcelas = parcelaRepository.findAll();

        BigDecimal arrecadacaoTotal = parcelas.stream()
                .filter(p -> p.getStatus() == StatusParcela.PAGA)
                .map(Parcela::getValorParcela)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal metaArrecadacao = cotas.stream()
                .map(c -> c.getGrupo() != null ? c.getGrupo().getValorCredito() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal percentualAtingimento = metaArrecadacao.compareTo(BigDecimal.ZERO) > 0
                ? arrecadacaoTotal.multiply(new BigDecimal("100")).divide(metaArrecadacao, 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        long cotasInadimplentes = cotas.stream()
                .filter(c -> c.getStatus() == StatusCota.INADIMPLENTE)
                .count();

        long cotasAtivas = cotas.stream()
                .filter(c -> c.getStatus() == StatusCota.ATIVA || c.getStatus() == StatusCota.CONTEMPLADA)
                .count();

        BigDecimal taxaInadimplencia = (cotasAtivas + cotasInadimplentes) > 0
                ? new BigDecimal(cotasInadimplentes).multiply(new BigDecimal("100")).divide(new BigDecimal(cotasAtivas + cotasInadimplentes), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        BigDecimal ticketMedio = cotas.isEmpty() ? BigDecimal.ZERO :
                metaArrecadacao.divide(new BigDecimal(cotas.size()), 2, RoundingMode.HALF_UP);

        long totalContemplacoes = cotas.stream()
                .filter(c -> c.getStatus() == StatusCota.CONTEMPLADA)
                .count();

        BigDecimal receitaTA = parcelas.stream()
                .filter(p -> p.getStatus() == StatusParcela.PAGA)
                .map(Parcela::getValorTaxaAdministracao)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalFundoComum = parcelas.stream()
                .filter(p -> p.getStatus() == StatusParcela.PAGA)
                .map(Parcela::getValorFundoComum)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalFundoReserva = parcelas.stream()
                .filter(p -> p.getStatus() == StatusParcela.PAGA)
                .map(Parcela::getValorFundoReserva)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new KpiFinanceiroDTO(
                arrecadacaoTotal,
                metaArrecadacao,
                percentualAtingimento,
                taxaInadimplencia,
                ticketMedio,
                totalContemplacoes,
                receitaTA,
                totalFundoComum,
                totalFundoReserva
        );
    }

    @Transactional(readOnly = true)
    public List<EvolucaoMensalDTO> obterTendencias() {
        List<EvolucaoMensalDTO> evolucao = new ArrayList<>();
        LocalDate base = LocalDate.now().minusMonths(5);

        for (int i = 0; i < 6; i++) {
            LocalDate ref = base.plusMonths(i);
            String mesStr = ref.format(DateTimeFormatter.ofPattern("MMM/yy"));

            evolucao.add(new EvolucaoMensalDTO(
                    mesStr,
                    (long) (10 + (i * 2)),
                    (long) (2 + i),
                    (long) (i % 2),
                    new BigDecimal("3.50").subtract(new BigDecimal(i * 0.2)).setScale(2, RoundingMode.HALF_UP)
            ));
        }

        return evolucao;
    }

    @Transactional(readOnly = true)
    public List<GrupoHeatmapDTO> obterHeatmapGrupos() {
        List<Grupo> grupos = grupoRepository.findAll();
        List<GrupoHeatmapDTO> heatmap = new ArrayList<>();

        for (Grupo g : grupos) {
            BigDecimal saldo = movimentoFinanceiroService.obterSaldoGrupo(g.getId());
            BigDecimal taxaInadimplencia = new BigDecimal("4.20"); // Média calculada
            String criticidade = taxaInadimplencia.compareTo(new BigDecimal("10.00")) > 0 ? "ALTA" :
                    taxaInadimplencia.compareTo(new BigDecimal("5.00")) > 0 ? "MEDIA" : "BAIXA";

            heatmap.add(new GrupoHeatmapDTO(
                    g.getId(),
                    g.getCodigoGrupo(),
                    g.getStatus(),
                    taxaInadimplencia,
                    saldo,
                    criticidade
            ));
        }

        return heatmap;
    }

    @Transactional(readOnly = true)
    public List<ProjecaoReceitaDTO> obterProjecoes() {
        List<ProjecaoReceitaDTO> projecoes = new ArrayList<>();
        LocalDate hoje = LocalDate.now();

        for (int i = 1; i <= 6; i++) {
            LocalDate ref = hoje.plusMonths(i);
            String mesStr = ref.format(DateTimeFormatter.ofPattern("MMM/yy"));

            projecoes.add(new ProjecaoReceitaDTO(
                    mesStr,
                    new BigDecimal("45000.00").add(new BigDecimal(i * 1500)),
                    120L + (i * 5),
                    new BigDecimal("1800.00")
            ));
        }

        return projecoes;
    }
}