package br.com.estudo.consorcio.service;

import br.com.estudo.consorcio.domain.dto.GrupoEncerrarResponseDTO;
import br.com.estudo.consorcio.domain.dto.GrupoFinanceiroResponseDTO;
import br.com.estudo.consorcio.domain.dto.GrupoRequestDTO;
import br.com.estudo.consorcio.domain.dto.GrupoResponseDTO;
import br.com.estudo.consorcio.domain.enums.TipoCategoriaBacen;
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
    private final AssembleiaRepository assembleiaRepository;
    private final BcbSgsService bcbSgsService;
    private final HistoricoValorBemReferenciaRepository historicoBemRepository;
    private final BemReferenciaRepository bemReferenciaRepository;
    private final java.time.Clock clock;

    public GrupoService(GrupoRepository repository, ParcelaRepository parcelaRepository,
                        ContemplacaoRepository contemplacaoRepository, GrupoMapper mapper,
                        MovimentoFinanceiroService movimentoService, CotaRepository cotaRepository,
                        HistoricoConsorciadoService historicoService, ContabilidadeService contabilidadeService,
                        AssembleiaRepository assembleiaRepository, BcbSgsService bcbSgsService,
                        HistoricoValorBemReferenciaRepository historicoBemRepository,
                        BemReferenciaRepository bemReferenciaRepository, java.time.Clock clock) {
        this.repository = repository;
        this.parcelaRepository = parcelaRepository;
        this.contemplacaoRepository = contemplacaoRepository;
        this.mapper = mapper;
        this.movimentoService = movimentoService;
        this.cotaRepository = cotaRepository;
        this.historicoService = historicoService;
        this.contabilidadeService = contabilidadeService;
        this.assembleiaRepository = assembleiaRepository;
        this.bcbSgsService = bcbSgsService;
        this.historicoBemRepository = historicoBemRepository;
        this.bemReferenciaRepository = bemReferenciaRepository;
        this.clock = clock;
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

        if (dto.quantidadeCotas() != null && dto.quantidadeCotas() > 0) {
            grupo.setQuantidadeCotas(dto.quantidadeCotas());
        } else {
            grupo.setQuantidadeCotas(1000);
        }

        if (dto.prazoMeses() != null) {
            grupo.setPrazoMaximoMeses(dto.prazoMeses());
        }

        if (dto.prazosPermitidos() != null && !dto.prazosPermitidos().isEmpty()) {
            grupo.setPrazosPermitidos(new java.util.ArrayList<>(dto.prazosPermitidos()));
        } else if (dto.prazoMeses() != null) {
            grupo.setPrazosPermitidos(java.util.List.of(dto.prazoMeses()));
        }

        if (dto.bensPermitidos() != null && !dto.bensPermitidos().isEmpty()) {
            List<BemReferencia> bens = bemReferenciaRepository.findAllById(dto.bensPermitidos());
            grupo.setBensPermitidos(bens);
        }

        // Validação BACEN: Homogeneidade de categoria no Grupo
        validarHomogeneidadeCategoriaBem(grupo);

        // 2. Persistência
        Grupo grupoSalvo = repository.save(grupo);

        // 3. (Removido) Cotas não são mais pré-geradas. Cotas só nascem no banco após a venda efetiva.

        // 4. Criar Assembleias baseadas no prazoMaximoMeses e diaBaseAssembleias
        LocalDate currentDate = LocalDate.now(clock);
        for (int i = 1; i <= grupoSalvo.getPrazoMaximoMeses(); i++) {
            LocalDate baseDate = currentDate.plusMonths(i);
            int year = baseDate.getYear();
            int month = baseDate.getMonthValue();
            int day = Math.min(grupoSalvo.getDiaBaseAssembleias(), baseDate.lengthOfMonth());
            LocalDate dataAssembleia = LocalDate.of(year, month, day);

            Assembleia assembleia = new Assembleia();
            assembleia.setGrupo(grupoSalvo);
            assembleia.setDataAssembleia(dataAssembleia);
            assembleia.setTipo(TipoAssembleia.ORDINARIA);
            assembleia.setStatus(i == 1 ? StatusAssembleia.CAPTANDO : StatusAssembleia.AGENDADA);
            assembleiaRepository.save(assembleia);
        }

        // 5. Retorno mapeado para DTO de saída usando o mapper
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

        // Atualiza o valor do crédito do bem referência principal
        if (grupo.getBensPermitidos() != null && !grupo.getBensPermitidos().isEmpty()) {
            grupo.getBensPermitidos().get(0).setValorAtual(novoValorCredito);
        }
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

    @Transactional
    public GrupoResponseDTO reajustarGrupoPorIndice(Long grupoId, IndiceReajuste indiceOverride) {
        Grupo grupo = repository.findById(grupoId)
                .orElseThrow(() -> new RegraDeNegocioException("Grupo não encontrado."));

        if (grupo.getStatus() == StatusGrupo.ENCERRADO) {
            throw new RegraDeNegocioException("Não é possível reajustar um grupo já encerrado.");
        }

        IndiceReajuste indice = indiceOverride != null ? indiceOverride : grupo.getIndiceReajuste();
        if (indice == null || indice == IndiceReajuste.MANUAL) {
            throw new RegraDeNegocioException("Índice de reajuste econômico não parametrizado para este grupo.");
        }

        var simulacao = bcbSgsService.simularReajuste(indice, grupo.getValorCredito());
        BigDecimal novoValorCredito = simulacao.novoValorCalculado();

        // Se o valor for atualizado, registra também no histórico do Bem de Referência principal
        if (grupo.getBensPermitidos() != null && !grupo.getBensPermitidos().isEmpty()) {
            BemReferencia bem = grupo.getBensPermitidos().get(0);
            BigDecimal valorAntigoBem = bem.getValorAtual();
            bem.setValorAtual(novoValorCredito);
            bem.setDataUltimaAtualizacao(LocalDate.now());

            if (valorAntigoBem != null && valorAntigoBem.compareTo(novoValorCredito) != 0) {
                historicoBemRepository.save(HistoricoValorBemReferencia.builder()
                        .bemReferencia(bem)
                        .valorAnterior(valorAntigoBem)
                        .valorNovo(novoValorCredito)
                        .origemReajuste(indice.name())
                        .dataAtualizacao(java.time.LocalDateTime.now())
                        .build());
            }
        }

        return reajustarGrupo(grupoId, novoValorCredito);
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
                grupo.getCodigoGrupo(),
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

        LocalDate dataEncerramento = LocalDate.now(clock);
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
                    "Provisão PDD — Parcela " + parcela.getNumeroParcela() + " da Cota " + parcela.getCota().getCodigoCota()
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
                grupo.getCodigoGrupo(),
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

    private void validarHomogeneidadeCategoriaBem(Grupo grupo) {
        if (grupo.getCategoriaBem() == null || grupo.getBensPermitidos() == null || grupo.getBensPermitidos().isEmpty()) {
            return;
        }

        br.com.estudo.consorcio.domain.enums.CategoriaBem catGrupo = grupo.getCategoriaBem();

        for (BemReferencia bem : grupo.getBensPermitidos()) {
            if (bem.getCategoriaBem() != null) {
                TipoCategoriaBacen tipoBacen = bem.getCategoriaBem().getTipoBacen();
                if (!isCategoriaCompativel(catGrupo, tipoBacen)) {
                    throw new RegraDeNegocioException(
                        String.format("O bem de referência '%s' (%s) é incompatível com a categoria do grupo (%s). De acordo com as regras do BACEN, grupos só podem conter bens da mesma categoria.",
                            bem.getDescricao(), tipoBacen, catGrupo)
                    );
                }
            }
        }
    }

    private boolean isCategoriaCompativel(br.com.estudo.consorcio.domain.enums.CategoriaBem catGrupo, TipoCategoriaBacen tipoBacen) {
        if (catGrupo == null || tipoBacen == null) return true;
        return switch (catGrupo) {
            case IMOVEL -> tipoBacen == TipoCategoriaBacen.BEM_IMOVEL;
            case VEICULO_AUTOMOTOR -> tipoBacen == TipoCategoriaBacen.BEM_MOVEL_I;
            case OUTROS_BENS_MOVEIS -> tipoBacen == TipoCategoriaBacen.BEM_MOVEL_II;
            case SERVICO -> tipoBacen == TipoCategoriaBacen.SERVICO;
        };
    }
}