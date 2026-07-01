package br.com.estudo.consorcio.service;

import br.com.estudo.consorcio.domain.dto.GrupoEncerrarResponseDTO;
import br.com.estudo.consorcio.domain.dto.GrupoFinanceiroResponseDTO;
import br.com.estudo.consorcio.domain.dto.GrupoRequestDTO;
import br.com.estudo.consorcio.domain.dto.GrupoResponseDTO;
import br.com.estudo.consorcio.domain.mapper.GrupoMapper;
import br.com.estudo.consorcio.domain.model.*;
import br.com.estudo.consorcio.domain.repository.*;
import br.com.estudo.consorcio.exception.RegraDeNegocioException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Service
public class GrupoService {

    private final GrupoRepository repository;
    private final ParcelaRepository parcelaRepository;
    private final ContemplacaoRepository contemplacaoRepository;
    private final GrupoMapper mapper;
    private final MovimentoFinanceiroService movimentoService;
    private final CotaRepository cotaRepository;
    private final HistoricoConsorciadoService historicoService;
    private final ContabilidadeService contabilidadeService;

    public GrupoService(GrupoRepository repository, ParcelaRepository parcelaRepository,
                        ContemplacaoRepository contemplacaoRepository, GrupoMapper mapper,
                        MovimentoFinanceiroService movimentoService, CotaRepository cotaRepository,
                        HistoricoConsorciadoService historicoService, ContabilidadeService contabilidadeService) {
        this.repository = repository;
        this.parcelaRepository = parcelaRepository;
        this.contemplacaoRepository = contemplacaoRepository;
        this.mapper = mapper;
        this.movimentoService = movimentoService;
        this.cotaRepository = cotaRepository;
        this.historicoService = historicoService;
        this.contabilidadeService = contabilidadeService;
    }

    private Usuario getUsuarioAutenticado() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Usuario) {
            return (Usuario) authentication.getPrincipal();
        }
        return null;
    }

    @Transactional
    public GrupoResponseDTO salvar(GrupoRequestDTO dto) {
        // 1. Mapeamento: DTO de entrada para Entidade usando o mapper
        Grupo grupo = mapper.toEntity(dto);

        // Regra BCB: Todo grupo nasce em formação (Garantido pelo Back-end)
        grupo.setStatus(StatusGrupo.EM_FORMACAO);

        // 2. Persistência
        Grupo grupoSalvo = repository.save(grupo);

        // 3. Retorno mapeado para DTO de saída usando o mapper
        return mapper.toResponse(grupoSalvo);
    }

    @Transactional
    public GrupoResponseDTO inaugurar(Long id, LocalDate dataAssembleia) {
        Grupo grupo = repository.findById(id)
                .orElseThrow(() -> new RegraDeNegocioException("Grupo não encontrado."));

        if (grupo.getStatus() != StatusGrupo.EM_FORMACAO) {
            throw new RegraDeNegocioException("Apenas grupos em formação podem ser inaugurados.");
        }

        // Regra BCB: O grupo é inaugurado na data da 1ª Assembleia Geral Ordinária (AGO)
        grupo.setStatus(StatusGrupo.EM_ANDAMENTO);
        grupo.setDataInauguracao(dataAssembleia);

        Grupo grupoInaugurado = repository.save(grupo);

        // Atualiza todas as cotas AGUARDANDO_INAUGURACAO para ATIVA
        List<Cota> cotas = cotaRepository.findByGrupoId(id);
        Usuario usuario = getUsuarioAutenticado();
        for (Cota cota : cotas) {
            if (cota.getStatus() == br.com.estudo.consorcio.domain.model.StatusCota.AGUARDANDO_INAUGURACAO) {
                cota.setStatus(br.com.estudo.consorcio.domain.model.StatusCota.ATIVA);
                cota.setVersao(cota.getVersao() != null ? cota.getVersao() + 1 : 1);
                cotaRepository.save(cota);
                historicoService.registrarInteracao(
                        cota.getCliente(), cota, grupoInaugurado, null,
                        br.com.estudo.consorcio.domain.model.TipoInteracao.OUTROS, 
                        "Cota ativada devido à inauguração do grupo (1ª AGO).",
                        null, null, 
                        null, null, null, 
                        null, null, usuario
                );
            }
        }

        return mapper.toResponse(grupoInaugurado);
    }

    @Transactional
    public GrupoResponseDTO reajustarGrupo(Long grupoId, BigDecimal novoValorCredito) {
        Grupo grupo = repository.findById(grupoId)
                .orElseThrow(() -> new RegraDeNegocioException("Grupo não encontrado."));

        if (grupo.getStatus() == StatusGrupo.ENCERRADO) {
            throw new RegraDeNegocioException("Não é possível reajustar um grupo já encerrado.");
        }

        if (novoValorCredito == null || novoValorCredito.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RegraDeNegocioException("O valor do crédito deve ser maior que zero.");
        }

        BigDecimal antigoValorCredito = grupo.getValorCredito();
        if (novoValorCredito.compareTo(antigoValorCredito) == 0) {
            return mapper.toResponse(grupo);
        }

        // Calcula o fator de reajuste (ex: 110.000 / 100.000 = 1.10)
        BigDecimal fatorReajuste = novoValorCredito.divide(antigoValorCredito, 6, RoundingMode.HALF_UP);

        // Atualiza o valor do crédito do grupo
        grupo.setValorCredito(novoValorCredito);
        Grupo grupoSalvo = repository.save(grupo);

        // Busca e atualiza todas as parcelas PENDENTES ou ATRASADAS das cotas do grupo
        List<StatusParcela> statusesReajustaveis = List.of(StatusParcela.PENDENTE, StatusParcela.ATRASADA);
        List<Parcela> parcelasParaReajustar = parcelaRepository.findByCotaGrupoIdAndStatusIn(grupoId, statusesReajustaveis);

        for (Parcela parcela : parcelasParaReajustar) {
            BigDecimal novoFundoComum = parcela.getValorFundoComum().multiply(fatorReajuste).setScale(2, RoundingMode.HALF_UP);
            parcela.setValorFundoComum(novoFundoComum);
            // O PreUpdate/PrePersist da entidade cuidará de recalcular o total da parcela somando as partes.
        }

        parcelaRepository.saveAll(parcelasParaReajustar);

        // --- Registrar Movimento Financeiro (Módulo 2) ---
        BigDecimal diferenca = novoValorCredito.subtract(antigoValorCredito).abs();
        NaturezaMovimento natureza = novoValorCredito.compareTo(antigoValorCredito) > 0 ? NaturezaMovimento.CREDITO : NaturezaMovimento.DEBITO;
        Usuario usuario = getUsuarioAutenticado();

        movimentoService.registrarMovimento(grupoSalvo, null, null, null,
                TipoMovimentoFinanceiro.REAJUSTE_CREDITO, natureza,
                diferenca, "Reajuste do valor do crédito de R$ " + antigoValorCredito + " para R$ " + novoValorCredito, usuario);

        // --- Registrar Interação de Histórico para cada Cota (Módulo 4) ---
        List<Cota> cotas = cotaRepository.findByGrupoId(grupoId);
        for (Cota cota : cotas) {
            historicoService.registrarInteracao(
                    cota.getCliente(), cota, grupoSalvo, null,
                    TipoInteracao.REAJUSTE_CREDITO, "Crédito do grupo reajustado de R$ " + antigoValorCredito + " para R$ " + novoValorCredito,
                    novoValorCredito, null,
                    null, null, null,
                    null, null, usuario);
        }

        return mapper.toResponse(grupoSalvo);
    }

    public GrupoFinanceiroResponseDTO obterRelatorioFinanceiro(Long grupoId) {
        Grupo grupo = repository.findById(grupoId)
                .orElseThrow(() -> new RegraDeNegocioException("Grupo não encontrado."));

        // Soma arrecadada de cada componente da parcela paga
        BigDecimal totalFundoComum = parcelaRepository.somarFundoComumPorGrupoEStatus(grupoId, StatusParcela.PAGA);
        BigDecimal totalTaxaAdmin = parcelaRepository.somarTaxaAdministracaoPorGrupoEStatus(grupoId, StatusParcela.PAGA);
        BigDecimal totalFundoReserva = parcelaRepository.somarFundoReservaPorGrupoEStatus(grupoId, StatusParcela.PAGA);

        // Créditos liberados por contemplação
        BigDecimal totalCreditosLiberados = contemplacaoRepository.somarCreditosLiberadosPorGrupo(grupoId);

        // Saldo disponível
        BigDecimal saldoFC = totalFundoComum.subtract(totalCreditosLiberados);
        if (saldoFC.compareTo(BigDecimal.ZERO) < 0) {
            saldoFC = BigDecimal.ZERO;
        }
        BigDecimal saldoFR = totalFundoReserva;

        return new GrupoFinanceiroResponseDTO(
                grupoId,
                grupo.getCodigo(),
                totalFundoComum,
                totalTaxaAdmin,
                totalFundoReserva,
                totalCreditosLiberados,
                saldoFC,
                saldoFR
        );
    }

    /**
     * ADR 006 — Encerramento de grupo com baixa de inadimplência e provisão de perdas.
     * 
     * Conforme Resolução BCB nº 285/2023, o encerramento do grupo deve ocorrer no prazo
     * máximo de 120 dias após a última AGO. Parcelas inadimplentes são baixadas contabilmente
     * (PDD) e enviadas para cobrança judicial extracontábil.
     * 
     * Lançamentos contábeis gerados:
     * 1. Provisão PDD: Débito 3.1.8.10.00-1 (Despesa PDD) → Crédito 1.6.9.10.00-5 (PDD)
     * 2. Baixa do crédito: Débito 1.6.9.10.00-5 (PDD) → Crédito 1.2.1.10.00-8 (Valores a Receber)
     */
    @Transactional
    public GrupoEncerrarResponseDTO encerrarGrupo(Long grupoId) {
        Grupo grupo = repository.findById(grupoId)
                .orElseThrow(() -> new RegraDeNegocioException("Grupo não encontrado."));

        if (grupo.getStatus() == StatusGrupo.ENCERRADO) {
            throw new RegraDeNegocioException("Este grupo já está encerrado.");
        }

        if (grupo.getStatus() == StatusGrupo.EM_FORMACAO) {
            throw new RegraDeNegocioException("Não é possível encerrar um grupo que ainda está em formação.");
        }

        LocalDate dataEncerramento = LocalDate.now();
        BigDecimal valorTotalPDD = BigDecimal.ZERO;
        long totalParcelasBaixadas = 0;

        // --- Fase 1: Identificar e baixar parcelas inadimplentes (PENDENTE/ATRASADA) ---
        List<StatusParcela> statusesAbertos = List.of(StatusParcela.PENDENTE, StatusParcela.ATRASADA);
        List<Parcela> parcelasInadimplentes = parcelaRepository.findByCotaGrupoIdAndStatusIn(grupoId, statusesAbertos);

        for (Parcela parcela : parcelasInadimplentes) {
            BigDecimal valorDevido = parcela.getValorParcela();

            // 1. Provisão PDD: Débito em Despesa PDD → Crédito em (-) PDD
            contabilidadeService.registrarEncerramento(
                    grupo, parcela.getCota(), parcela,
                    ContabilidadeService.CONTA_DESPESA_PDD,
                    ContabilidadeService.CONTA_PDD,
                    valorDevido, dataEncerramento,
                    "Provisão PDD — Parcela " + parcela.getNumeroParcela() + " da Cota " + parcela.getCota().getNumeroCota()
            );

            // 2. Baixa do crédito: Débito em (-) PDD → Crédito em Valores a Receber
            contabilidadeService.registrarEncerramento(
                    grupo, parcela.getCota(), parcela,
                    ContabilidadeService.CONTA_PDD,
                    ContabilidadeService.CONTA_DIREITOS_RECEBER,
                    valorDevido, dataEncerramento,
                    "Baixa de crédito — Parcela " + parcela.getNumeroParcela() + " enviada para cobrança judicial"
            );

            // 3. Alterar status da parcela para BAIXADA
            parcela.setStatus(StatusParcela.BAIXADA);
            valorTotalPDD = valorTotalPDD.add(valorDevido);
            totalParcelasBaixadas++;
        }

        if (!parcelasInadimplentes.isEmpty()) {
            parcelaRepository.saveAll(parcelasInadimplentes);
        }

        // --- Fase 2: Encerrar o grupo ---
        grupo.setStatus(StatusGrupo.ENCERRADO);
        grupo.setDataEncerramento(dataEncerramento);
        repository.save(grupo);

        // Valor de RNP é zero nesta versão (funcionalidade de credores não procurados será implementada futuramente)
        BigDecimal valorTransferidoRNP = BigDecimal.ZERO;

        return new GrupoEncerrarResponseDTO(
                grupoId,
                grupo.getCodigo(),
                totalParcelasBaixadas,
                valorTotalPDD,
                valorTransferidoRNP,
                dataEncerramento
        );
    }

    public Page<GrupoResponseDTO> listarTodos(Pageable pageable) {
        return repository.findAll(pageable)
                .map(mapper::toResponse);
    }
}