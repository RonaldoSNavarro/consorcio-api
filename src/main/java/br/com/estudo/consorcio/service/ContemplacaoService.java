package br.com.estudo.consorcio.service;

import br.com.estudo.consorcio.domain.dto.ContemplacaoRequestDTO;
import br.com.estudo.consorcio.domain.dto.ContemplacaoResponseDTO;
import br.com.estudo.consorcio.domain.dto.CotaResponseDTO;
import br.com.estudo.consorcio.domain.mapper.ContemplacaoMapper; // Importar o mapper
import br.com.estudo.consorcio.domain.mapper.CotaMapper;
import br.com.estudo.consorcio.domain.model.*;
import br.com.estudo.consorcio.domain.repository.AssembleiaRepository;
import br.com.estudo.consorcio.domain.repository.ContemplacaoRepository;
import br.com.estudo.consorcio.domain.repository.CotaRepository;
import br.com.estudo.consorcio.domain.repository.ParcelaRepository;
import br.com.estudo.consorcio.domain.repository.LanceRepository;
import br.com.estudo.consorcio.domain.repository.AlertaComplianceRepository;
import java.util.Optional;
import br.com.estudo.consorcio.exception.RegraDeNegocioException;
import org.springframework.security.core.context.SecurityContextHolder;
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
    private final ContemplacaoMapper mapper; // Injetar o mapper
    private final ContabilidadeService contabilidadeService;
    private final CotaService cotaService;
    private final HistoricoConsorciadoService historicoService;
    private final LanceRepository lanceRepository;
    private final CotaMapper cotaMapper;
    private final AlertaComplianceRepository alertaComplianceRepository;
    private final ParcelaService parcelaService;

    public ContemplacaoService(ContemplacaoRepository contemplacaoRepository, AssembleiaRepository assembleiaRepository,
                               CotaRepository cotaRepository, ParcelaRepository parcelaRepository,
                               ContemplacaoMapper mapper, ContabilidadeService contabilidadeService,
                               CotaService cotaService, HistoricoConsorciadoService historicoService,
                               LanceRepository lanceRepository, CotaMapper cotaMapper,
                               AlertaComplianceRepository alertaComplianceRepository, ParcelaService parcelaService) {
        this.contemplacaoRepository = contemplacaoRepository;
        this.assembleiaRepository = assembleiaRepository;
        this.cotaRepository = cotaRepository;
        this.parcelaRepository = parcelaRepository;
        this.mapper = mapper;
        this.contabilidadeService = contabilidadeService;
        this.cotaService = cotaService;
        this.historicoService = historicoService;
        this.lanceRepository = lanceRepository;
        this.cotaMapper = cotaMapper;
        this.alertaComplianceRepository = alertaComplianceRepository;
        this.parcelaService = parcelaService;
    }

    private Usuario getUsuarioAutenticado() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Usuario) {
            return (Usuario) authentication.getPrincipal();
        }
        return null;
    }

    @Transactional
    public ContemplacaoResponseDTO registrar(ContemplacaoRequestDTO dto) {
        // 1. Buscas de Integridade
        Assembleia assembleia = assembleiaRepository.findById(dto.assembleiaId())
                .orElseThrow(() -> new RegraDeNegocioException("Assembleia não encontrada."));

        Cota cota = cotaRepository.findById(dto.cotaId())
                .orElseThrow(() -> new RegraDeNegocioException("Cota não encontrada."));

        if (cota.getCliente() != null) {
            boolean hasRestrictedAlerts = alertaComplianceRepository.existsByClienteIdAndStatusIn(
                    cota.getCliente().getId(),
                    List.of(StatusAlertaCompliance.PENDENTE_ANALISE, StatusAlertaCompliance.CONFIRMADO)
            );
            if (hasRestrictedAlerts) {
                throw new RegraDeNegocioException("Contemplação bloqueada por Compliance/PLD: Cliente possui alertas restritivos.");
            }
        }

        if (!cota.getGrupo().getId().equals(assembleia.getGrupo().getId())) {
            throw new RegraDeNegocioException("A cota e a assembleia pertencem a grupos diferentes.");
        }

        if (cota.getStatus() != StatusCota.ATIVA && cota.getStatus() != StatusCota.CANCELADA) {
            throw new RegraDeNegocioException("Apenas cotas ATIVAS ou CANCELADAS podem ser contempladas.");
        }

        if (cota.getStatus() == StatusCota.CANCELADA && dto.tipoContemplacao() != br.com.estudo.consorcio.domain.model.TipoContemplacao.SORTEIO) {
            throw new RegraDeNegocioException("Cotas canceladas só podem ser contempladas por SORTEIO para fins de restituição.");
        }

        // Regra de Compliance Inadimplência: Bloquear contemplação se a cota possuir parcelas vencidas
        boolean possuiParcelasVencidas = parcelaRepository.existsByCotaIdAndStatusAndDataVencimentoBefore(
                cota.getId(),
                StatusParcela.PENDENTE,
                assembleia.getDataAssembleia()
        );
        if (possuiParcelasVencidas) {
            throw new RegraDeNegocioException("Não é possível contemplar a cota: existem parcelas em atraso.");
        }

        // 2. Mapeamento inicial usando o mapper
        Contemplacao contemplacao = mapper.toEntity(dto);
        contemplacao.setAssembleia(assembleia); // Setar a assembleia após a busca
        contemplacao.setCota(cota); // Setar a cota após a busca

        BigDecimal valorCreditoGrupo = assembleia.getGrupo().getValorCredito();
        BigDecimal valorCreditoLiberado = valorCreditoGrupo;
        BigDecimal multaRescisoria = BigDecimal.ZERO;

        if (cota.getStatus() == StatusCota.CANCELADA) {
            // Nova Regra ADR 005: Devolução baseada no percentual amortizado do fundo comum aplicado sobre o crédito atual do grupo
            List<Parcela> parcelasPagas = parcelaRepository.findByCotaId(cota.getId()).stream()
                    .filter(p -> p.getStatus() == StatusParcela.PAGA)
                    .toList();

            BigDecimal PAFC = BigDecimal.ZERO;
            for (Parcela p : parcelasPagas) {
                BigDecimal pct = p.getPercentualFundoComum();
                if (pct == null) {
                    // Fallback para parcelas antigas legadas
                    pct = p.getValorFundoComum().divide(valorCreditoGrupo, 6, RoundingMode.HALF_UP);
                }
                PAFC = PAFC.add(pct);
            }

            BigDecimal valorBruto = PAFC.multiply(valorCreditoGrupo).setScale(2, RoundingMode.HALF_UP);
            multaRescisoria = valorBruto.multiply(new BigDecimal("0.10")).setScale(2, RoundingMode.HALF_UP);
            valorCreditoLiberado = valorBruto.subtract(multaRescisoria).setScale(2, RoundingMode.HALF_UP);

            contemplacao.setLanceEmbutido(false);
            contemplacao.setValorLance(BigDecimal.ZERO);
        } else {
            // --- REGRA DO BANCO CENTRAL: LANCE EMBUTIDO DINÂMICO --- //
            if (Boolean.TRUE.equals(contemplacao.getLanceEmbutido())) {

                if (contemplacao.getValorLance() == null || contemplacao.getValorLance().compareTo(BigDecimal.ZERO) <= 0) {
                    throw new RegraDeNegocioException("Para lances embutidos, o valor do lance deve ser informado.");
                }

                BigDecimal limitePercentual = assembleia.getGrupo().getPercentualLanceEmbutidoMaximo();
                BigDecimal limiteEmbutido = valorCreditoGrupo.multiply(limitePercentual).setScale(2, RoundingMode.HALF_UP);

                if (contemplacao.getValorLance().compareTo(limiteEmbutido) > 0) {
                    BigDecimal percentualFormatado = limitePercentual.multiply(new BigDecimal("100")).setScale(0, RoundingMode.HALF_UP);
                    throw new RegraDeNegocioException("O valor do lance embutido não pode ultrapassar " + percentualFormatado + "% do crédito (Máximo permitido: R$ " + limiteEmbutido + ").");
                }

                valorCreditoLiberado = valorCreditoGrupo.subtract(contemplacao.getValorLance());
            }
        }

        contemplacao.setValorCreditoLiberado(valorCreditoLiberado);
        // ---------------------------------------------------------------- //

        // --- REGRA DO BANCO CENTRAL: VALIDAÇÃO DO FUNDO COMUM --- //
        // Extrai o Saldo Real via Double-entry Ledger, consertando o bug anterior
        BigDecimal saldoFundoComum = contabilidadeService.calcularSaldoConta(assembleia.getGrupo(), ContabilidadeService.CONTA_FUNDO_COMUM);

        if (saldoFundoComum.compareTo(valorCreditoLiberado) < 0) {
            throw new RegraDeNegocioException("REGRA BCB: Saldo insuficiente no Fundo Comum do grupo. Saldo atual: R$ "
                    + saldoFundoComum + " | Necessário para liberação: R$ " + valorCreditoLiberado);
        }
        // -------------------------------------------------------- //

        contemplacao.setDataContemplacao(LocalDate.now());

        // 3. Persistência
        Contemplacao contemplacaoSalva = contemplacaoRepository.save(contemplacao);

        // --- Registrar Movimento Financeiro (Ledger de Partidas Dobradas) ---
        Usuario usuario = getUsuarioAutenticado();
        Grupo grupo = assembleia.getGrupo();

        if (cota.getStatus() == StatusCota.CANCELADA) {
            // Registrar trânsito de recursos da cota cancelada: Fundo Comum -> Recursos de Excluídos a Devolver
            contabilidadeService.registrarBaixa(
                    grupo, cota, null,
                    ContabilidadeService.CONTA_FUNDO_COMUM,
                    ContabilidadeService.CONTA_EXCLUIDOS_DEVOLVER,
                    valorCreditoLiberado,
                    LocalDate.now(),
                    "Restituição de cota excluída sorteada - Cota " + cota.getCodigoCota()
            );

            // Reclassificar a multa rescisória do Fundo Comum para a conta de destino parametrizada pelo Grupo
            if (multaRescisoria.compareTo(BigDecimal.ZERO) > 0) {
                String contaDestinoMulta = (grupo.getDestinacaoMultaRescisoria() != null && grupo.getDestinacaoMultaRescisoria() == br.com.estudo.consorcio.domain.enums.DestinacaoMultaRescisoria.TAXA_ADMINISTRACAO)
                        ? ContabilidadeService.CONTA_TAXA_ADM
                        : ContabilidadeService.CONTA_FUNDO_RESERVA;

                contabilidadeService.registrarBaixa(
                        grupo, cota, null,
                        ContabilidadeService.CONTA_FUNDO_COMUM,
                        contaDestinoMulta,
                        multaRescisoria,
                        LocalDate.now(),
                        "Multa rescisória sobre restituição - Cota " + cota.getCodigoCota()
                );
            }
        } else {
            // Se for Lance Livre ou Lance Fixo (não embutido): vai para PENDENTE_INTEGRALIZACAO e não transita crédito contábil
            if ((dto.tipoContemplacao() == TipoContemplacao.LANCE_LIVRE || dto.tipoContemplacao() == TipoContemplacao.LANCE_FIXO)
                    && !Boolean.TRUE.equals(contemplacaoSalva.getLanceEmbutido())) {
                cotaService.registrarTransicaoVersao(cota, StatusCota.PENDENTE_INTEGRALIZACAO,
                        "Cota contemplada via Lance " + (dto.tipoContemplacao() == TipoContemplacao.LANCE_LIVRE ? "Livre" : "Fixo") + " - Aguardando Integralização do Lance");
            } else {
                // Sorteio ou Lance Embutido: vai direto para AGUARDANDO_ANALISE e transita o crédito para Créditos a Liberar
                cotaService.registrarTransicaoVersao(cota, StatusCota.AGUARDANDO_ANALISE, "Cota contemplada na assembleia id " + assembleia.getId() + " - Aguardando Análise de Crédito");

                // Trânsito contábil do crédito liberado para o passivo de Créditos a Liberar
                contabilidadeService.registrarBaixa(
                        grupo, cota, null,
                        ContabilidadeService.CONTA_FUNDO_COMUM,
                        ContabilidadeService.CONTA_CREDITOS_LIBERAR,
                        valorCreditoLiberado,
                        LocalDate.now(),
                        "Trânsito de crédito contemplado - Cota " + cota.getCodigoCota()
                );
            }
        }

        if (Boolean.TRUE.equals(contemplacaoSalva.getLanceEmbutido())) {
            // O lance embutido reduz o crédito e fica no fundo comum. Como o fundo comum nunca chegou a perder esse montante,
            // podemos registrar um estorno contra a provisão ou simplesmente uma retenção se for estritamente contábil.
            contabilidadeService.registrarBaixa(grupo, cota, null, ContabilidadeService.CONTA_DIREITOS_RECEBER, ContabilidadeService.CONTA_FUNDO_COMUM,
                    contemplacaoSalva.getValorLance(), LocalDate.now(), "Lance embutido retido no Fundo Comum - Cota " + cota.getCodigoCota());
        }

        // Movimento de LIBERACAO_CREDITO foi movido para o fluxo de Análise de Crédito (AnaliseCreditoService).

        // --- Registrar Interação de Histórico (Módulo 4) ---
        historicoService.registrarInteracao(
                cota.getCliente(), cota, cota.getGrupo(), null,
                TipoInteracao.CONTEMPLACAO, "Cota contemplada via " + contemplacaoSalva.getTipoContemplacao(),
                cota.getGrupo().getValorCredito(), null,
                null, null, null,
                null, null, usuario);

        // 4. Retorno Mapeado usando o mapper
        return mapper.toResponse(contemplacaoSalva);
    }

    @Transactional
    public ContemplacaoResponseDTO pagarBem(Long contemplacaoId) {
        Contemplacao contemplacao = contemplacaoRepository.findById(contemplacaoId)
                .orElseThrow(() -> new RegraDeNegocioException("Contemplação não encontrada."));

        // Regra de segurança simplificada
        if (contemplacao.getValorCreditoLiberado().compareTo(BigDecimal.ZERO) == 0) {
            throw new RegraDeNegocioException("O bem para esta contemplação já foi pago ou o valor é zero.");
        }

        Usuario usuario = getUsuarioAutenticado();
        Grupo grupo = contemplacao.getCota().getGrupo();
        Cota cota = contemplacao.getCota();

        // O bem faturado sai do Fundo Comum e vai para Fornecedores ou Caixa (liquidação)
        contabilidadeService.registrarBaixa(grupo, cota, null, ContabilidadeService.CONTA_FUNDO_COMUM, ContabilidadeService.CONTA_CAIXA,
                contemplacao.getValorCreditoLiberado(), LocalDate.now(), "Pagamento de bem alienado - Cota " + cota.getCodigoCota());
        
        // Zera o crédito para impedir duplo pagamento
        contemplacao.setValorCreditoLiberado(BigDecimal.ZERO);
        contemplacaoRepository.save(contemplacao);

        // --- Registrar Interação de Histórico (Módulo 4) ---
        historicoService.registrarInteracao(
                cota.getCliente(), cota, cota.getGrupo(), null,
                TipoInteracao.PAGAMENTO_BEM, "Pagamento de bem no valor de R$ " + contemplacao.getValorCreditoLiberado() + " realizado.",
                cota.getGrupo().getValorCredito(), null,
                null, null, null,
                "Bem faturado", contemplacao.getValorCreditoLiberado(), usuario);

        return mapper.toResponse(contemplacao);
    }

    public List<ContemplacaoResponseDTO> listarPorAssembleia(Long assembleiaId) {
        return contemplacaoRepository.findByAssembleiaId(assembleiaId).stream()
                .map(mapper::toResponse) // Usar o mapper
                .toList();
    }

    public List<ContemplacaoResponseDTO> listarPendentesIntegralizacao() {
        return contemplacaoRepository.findPendentesIntegralizacao().stream()
                .map(this::toResponseComLance)
                .toList();
    }

    private ContemplacaoResponseDTO toResponseComLance(Contemplacao contemplacao) {
        ContemplacaoResponseDTO response = mapper.toResponse(contemplacao);
        Long lanceId = lanceRepository.findByCotaIdAndAssembleiaId(
                        contemplacao.getCota().getId(), contemplacao.getAssembleia().getId())
                .map(Lance::getId)
                .orElse(null);
        return new ContemplacaoResponseDTO(
                response.id(), response.cotaId(), response.assembleiaId(), response.tipoContemplacao(),
                response.valorLance(), response.dataContemplacao(), response.lanceEmbutido(),
                response.valorCreditoLiberado(), response.codigoGrupo(), response.nomeCliente(),
                response.cpfCnpjCliente(), response.statusCota(), response.codigoCota(), lanceId);
    }
    /**
     * Liquida uma única vez o lance vencedor e aplica sua amortização na mesma transação.
     * Lances embutidos não representam entrada de caixa (ADR 004 e Resolução BCB 285/2023).
     */
    @Transactional
    public CotaResponseDTO liquidarLance(Long lanceId, TipoAmortizacaoLance tipoAmortizacao) {
        Lance lance = lanceRepository.findById(lanceId)
                .orElseThrow(() -> new RegraDeNegocioException("Lance não encontrado."));

        if (tipoAmortizacao == null) {
            throw new RegraDeNegocioException("A modalidade de amortização é obrigatória.");
        }
        if (lance.getStatusApuracao() == StatusApuracaoLance.LIQUIDADO) {
            if (lance.getTipoAmortizacao() != tipoAmortizacao) {
                throw new RegraDeNegocioException("O lance já foi liquidado com modalidade de amortização diferente.");
            }
            return cotaMapper.toResponse(lance.getCota());
        }
        if (lance.getStatusApuracao() != StatusApuracaoLance.VENCEDOR) {
            throw new RegraDeNegocioException("Este lance não foi classificado como vencedor.");
        }
        if (lance.getTipo() == TipoLance.MISTO || lance.getTipo() == TipoLance.SEGURO_OBITO) {
            throw new RegraDeNegocioException("A liquidação de lance " + lance.getTipo() + " requer modelagem financeira específica.");
        }

        Cota cota = lance.getCota();
        Contemplacao contemplacao = contemplacaoRepository.findByCotaIdAndAssembleiaId(cota.getId(), lance.getAssembleia().getId())
                .orElseThrow(() -> new RegraDeNegocioException("Contemplação não encontrada para a cota."));
        if (!contemplacao.getAssembleia().getId().equals(lance.getAssembleia().getId())) {
            throw new RegraDeNegocioException("A contemplação não pertence à assembleia do lance.");
        }

        Grupo grupo = cota.getGrupo();
        if (lance.getTipo() == TipoLance.FIRME || lance.getTipo() == TipoLance.FGTS) {
            if (cota.getStatus() != StatusCota.PENDENTE_INTEGRALIZACAO) {
                throw new RegraDeNegocioException("Esta cota não está pendente de integralização.");
            }
            contabilidadeService.registrarBaixa(grupo, cota, null, ContabilidadeService.CONTA_CAIXA,
                    ContabilidadeService.CONTA_FUNDO_COMUM, lance.getValorOferta(), LocalDate.now(),
                    "Integralização física de lance - Cota " + cota.getCodigoCota());
            contabilidadeService.registrarBaixa(grupo, cota, null, ContabilidadeService.CONTA_FUNDO_COMUM,
                    ContabilidadeService.CONTA_CREDITOS_LIBERAR, contemplacao.getValorCreditoLiberado(), LocalDate.now(),
                    "Trânsito de crédito contemplado pós-integralização - Cota " + cota.getCodigoCota());
            cotaService.registrarTransicaoVersao(cota, StatusCota.AGUARDANDO_ANALISE,
                    "Integralização do lance efetuada - Cota aguardando análise de crédito");
        } else if (lance.getTipo() != TipoLance.EMBUTIDO) {
            throw new RegraDeNegocioException("Tipo de lance não suportado para liquidação.");
        }

        if (tipoAmortizacao == TipoAmortizacaoLance.REDUCAO_PRAZO) {
            parcelaService.amortizarPorReducaoDePrazo(cota.getId(), lance.getValorOferta());
        } else {
            parcelaService.amortizarPorDiluicao(cota.getId(), lance.getValorOferta());
        }
        lance.setStatusApuracao(StatusApuracaoLance.LIQUIDADO);
        lance.setDataLiquidacao(java.time.LocalDateTime.now());
        lance.setTipoAmortizacao(tipoAmortizacao);
        lance.setAmortizacaoAplicada(true);
        lanceRepository.save(lance);

        // --- Registrar Interação de Histórico ---
        Usuario usuario = getUsuarioAutenticado();
        historicoService.registrarInteracao(
                cota.getCliente(), cota, grupo, null,
                TipoInteracao.PAGAMENTO_PARCELA, "Liquidação do lance no valor de R$ " + lance.getValorOferta() + " confirmada.",
                grupo.getValorCredito(), null,
                null, null, null,
                "Lance integralizado", lance.getValorOferta(), usuario);

        return cotaMapper.toResponse(cota);
    }

    @Transactional
    public void cancelarContemplacaoPorAtraso(Long contemplacaoId) {
        Contemplacao contemplacao = contemplacaoRepository.findById(contemplacaoId)
                .orElseThrow(() -> new RegraDeNegocioException("Contemplação não encontrada."));

        Cota cota = contemplacao.getCota();
        if (cota.getStatus() != StatusCota.PENDENTE_INTEGRALIZACAO) {
            throw new RegraDeNegocioException("Esta cota não está pendente de integralização.");
        }

        // Retorna a cota para ATIVA
        cotaService.registrarTransicaoVersao(cota, StatusCota.ATIVA, "Contemplação cancelada por atraso na integralização do lance.");

        // Atualiza o lance para INVALIDO
        Assembleia assembleia = contemplacao.getAssembleia();
        lanceRepository.findByCotaIdAndAssembleiaId(cota.getId(), assembleia.getId())
                .ifPresent(lance -> {
                    lance.setStatusApuracao(StatusApuracaoLance.EXPIRADO);
                    lanceRepository.save(lance);
                });

        // Exclui a contemplação do banco
        contemplacaoRepository.delete(contemplacao);

        // --- Registrar Interação de Histórico ---
        Usuario usuario = getUsuarioAutenticado();
        historicoService.registrarInteracao(
                cota.getCliente(), cota, cota.getGrupo(), null,
                TipoInteracao.CANCELAMENTO_COTA, "Contemplação por lance livre cancelada por atraso na integralização do lance.",
                cota.getGrupo().getValorCredito(), null,
                null, null, null,
                null, null, usuario);
    }
}
