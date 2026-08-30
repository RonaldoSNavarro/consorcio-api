package br.com.estudo.consorcio.service;

import br.com.estudo.consorcio.domain.dto.CotaInadimplenciaResponseDTO;
import br.com.estudo.consorcio.domain.dto.ParcelaRequestDTO;
import br.com.estudo.consorcio.domain.dto.ParcelaResponseDTO;
import br.com.estudo.consorcio.domain.mapper.ParcelaMapper;
import br.com.estudo.consorcio.domain.model.*;
import br.com.estudo.consorcio.domain.repository.CotaRepository;
import br.com.estudo.consorcio.domain.repository.ContratoAdesaoRepository;
import br.com.estudo.consorcio.domain.repository.ParcelaRepository;
import br.com.estudo.consorcio.domain.enums.StatusContrato;
import br.com.estudo.consorcio.exception.RegraDeNegocioException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class ParcelaService {

    private final ParcelaRepository parcelaRepository;
    private final CotaRepository cotaRepository;
    private final ParcelaMapper mapper; // Injetar o mapper
    private final MovimentoFinanceiroService movimentoService;
    private final HistoricoConsorciadoService historicoService;
    private final ContabilidadeService contabilidadeService;
    private final ComissaoVendaService comissaoService;
    private final ContratoAdesaoRepository contratoRepository;

    public ParcelaService(ParcelaRepository parcelaRepository, CotaRepository cotaRepository,
                           ParcelaMapper mapper, MovimentoFinanceiroService movimentoService,
                           HistoricoConsorciadoService historicoService, ContabilidadeService contabilidadeService,
                          ComissaoVendaService comissaoService, ContratoAdesaoRepository contratoRepository) {
        this.parcelaRepository = parcelaRepository;
        this.cotaRepository = cotaRepository;
        this.mapper = mapper;
        this.movimentoService = movimentoService;
        this.historicoService = historicoService;
        this.contabilidadeService = contabilidadeService;
        this.comissaoService = comissaoService;
        this.contratoRepository = contratoRepository;
    }

    private Usuario getUsuarioAutenticado() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Usuario) {
            return (Usuario) authentication.getPrincipal();
        }
        return null;
    }

    @Transactional
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyAuthority('MANAGE_FINANCEIRO')")
    public ParcelaResponseDTO salvar(ParcelaRequestDTO dto) {
        // 1. Valida e busca a Cota
        Cota cota = cotaRepository.findById(dto.cotaId())
                .orElseThrow(() -> new RegraDeNegocioException("Cota inválida ou não encontrada no banco de dados."));

        // 2. Mapeia DTO para Entidade usando o mapper
        Parcela parcela = mapper.toEntity(dto);
        parcela.setCota(cota); // Setar a cota após a busca

        // 3. Regra de negócio: Parcela nasce PENDENTE e calcula o percentual de amortização correspondente
        parcela.setStatus(StatusParcela.PENDENTE);

        // --- Regra de Comissões e Fundo Comum Zerado (Vendas) ---
        if (parcela.getNumeroParcela() == 1 && cota.getContratoAdesao() != null 
                && cota.getContratoAdesao().getProposta() != null 
                && cota.getContratoAdesao().getProposta().getTipoVenda() != null) {
            
            TipoVenda tipoVenda = cota.getContratoAdesao().getProposta().getTipoVenda();
            if (Boolean.TRUE.equals(tipoVenda.getParcelaUmZeroFundoComum())) {
                BigDecimal fundoComumOriginal = parcela.getValorFundoComum();
                parcela.setValorTaxaAdministracao(parcela.getValorTaxaAdministracao().add(fundoComumOriginal));
                parcela.setValorFundoComum(BigDecimal.ZERO);
            }
        }

        BigDecimal valorFundoComum = parcela.getValorFundoComum();
        BigDecimal valorCredito = cota.getGrupo().getValorCredito();
        if (valorCredito != null && valorCredito.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal percentual = valorFundoComum.divide(valorCredito, 6, RoundingMode.HALF_UP);
            parcela.setPercentualFundoComum(percentual);
        }

        // O JPA chamará o @PrePersist e calculará o valorParcela (soma dos quatro)
        Parcela parcelaSalva = parcelaRepository.save(parcela);

        // --- Registrar Interação de Histórico (Módulo 4) ---
        historicoService.registrarInteracao(
                cota.getCliente(), cota, cota.getGrupo(), parcelaSalva,
                TipoInteracao.GERACAO_PARCELAS, "Geração da parcela número " + parcelaSalva.getNumeroParcela(),
                cota.getGrupo().getValorCredito(), parcelaSalva.getValorFundoComum(),
                parcelaSalva.getValorTaxaAdministracao(), parcelaSalva.getValorFundoReserva(), parcelaSalva.getValorSeguro(),
                null, null, getUsuarioAutenticado());

        return mapper.toResponse(parcelaSalva); // Usar o mapper
    }

    @Transactional
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyAuthority('MANAGE_FINANCEIRO') or @ownershipGuard.canAccessParcela(#parcelaId)")
    public ParcelaResponseDTO pagar(Long parcelaId, LocalDate dataPagamento) {
        Parcela parcela = parcelaRepository.findById(parcelaId)
                .orElseThrow(() -> new RegraDeNegocioException("Parcela não encontrada."));

        if (parcela.getStatus() == StatusParcela.PAGA) {
            throw new RegraDeNegocioException("Esta parcela já consta como paga.");
        }

        parcela.setDataPagamento(dataPagamento);

        // --- Regra de Inadimplência (BCB / CDC) ---
        if (dataPagamento.isAfter(parcela.getDataVencimento())) {
            long diasAtraso = ChronoUnit.DAYS.between(parcela.getDataVencimento(), dataPagamento);

            BigDecimal multa = parcela.getValorParcela().multiply(new BigDecimal("0.02")).setScale(2, RoundingMode.HALF_UP);
            BigDecimal taxaMensal = new BigDecimal("0.01");
            BigDecimal taxaDiaria = taxaMensal.divide(new BigDecimal("30"), 10, RoundingMode.HALF_UP);
            BigDecimal juros = parcela.getValorParcela().multiply(taxaDiaria).multiply(new BigDecimal(diasAtraso)).setScale(2, RoundingMode.HALF_UP);

            parcela.setValorMulta(multa);
            parcela.setValorJuros(juros);
            parcela.setValorPago(parcela.getValorParcela().add(multa).add(juros));
        } else {
            parcela.setValorMulta(BigDecimal.ZERO);
            parcela.setValorJuros(BigDecimal.ZERO);
            parcela.setValorPago(parcela.getValorParcela());
        }

        parcela.setStatus(StatusParcela.PAGA);
        Parcela parcelaMapeada = parcelaRepository.save(parcela);

        // --- Registrar Movimentos Financeiros (Ledger de Partidas Dobradas) ---
        Usuario usuario = getUsuarioAutenticado();
        Grupo grupo = parcelaMapeada.getCota().getGrupo();
        Cota cota = parcelaMapeada.getCota();

        // 1. Baixas (Recebimentos de Principal no Caixa)
        contabilidadeService.registrarBaixa(grupo, cota, parcelaMapeada, ContabilidadeService.CONTA_CAIXA, ContabilidadeService.CONTA_FUNDO_COMUM,
                parcelaMapeada.getValorFundoComum(), parcelaMapeada.getDataPagamento(), "Baixa de Fundo comum - Parcela " + parcelaMapeada.getNumeroParcela());

        contabilidadeService.registrarBaixa(grupo, cota, parcelaMapeada, ContabilidadeService.CONTA_CAIXA, ContabilidadeService.CONTA_TAXA_ADM,
                parcelaMapeada.getValorTaxaAdministracao(), parcelaMapeada.getDataPagamento(), "Baixa de Taxa de administração - Parcela " + parcelaMapeada.getNumeroParcela());

        contabilidadeService.registrarBaixa(grupo, cota, parcelaMapeada, ContabilidadeService.CONTA_CAIXA, ContabilidadeService.CONTA_FUNDO_RESERVA,
                parcelaMapeada.getValorFundoReserva(), parcelaMapeada.getDataPagamento(), "Baixa de Fundo de reserva - Parcela " + parcelaMapeada.getNumeroParcela());

        contabilidadeService.registrarBaixa(grupo, cota, parcelaMapeada, ContabilidadeService.CONTA_CAIXA, ContabilidadeService.CONTA_SEGURO,
                parcelaMapeada.getValorSeguro(), parcelaMapeada.getDataPagamento(), "Baixa de Seguro - Parcela " + parcelaMapeada.getNumeroParcela());

        // 2. Regime de Competência para Multa e Juros (CFC Compliance)
        if (parcelaMapeada.getValorMulta() != null && parcelaMapeada.getValorMulta().compareTo(BigDecimal.ZERO) > 0) {
            // Provisão na data de vencimento
            contabilidadeService.registrarProvisao(grupo, cota, parcelaMapeada, ContabilidadeService.CONTA_DIREITOS_RECEBER, ContabilidadeService.CONTA_FUNDO_COMUM,
                    parcelaMapeada.getValorMulta(), parcelaMapeada.getDataVencimento(), "Provisão de Multa por atraso - Parcela " + parcelaMapeada.getNumeroParcela());
            // Baixa no caixa na data de pagamento real
            contabilidadeService.registrarBaixa(grupo, cota, parcelaMapeada, ContabilidadeService.CONTA_CAIXA, ContabilidadeService.CONTA_DIREITOS_RECEBER,
                    parcelaMapeada.getValorMulta(), parcelaMapeada.getDataPagamento(), "Baixa de Multa por atraso - Parcela " + parcelaMapeada.getNumeroParcela());
        }

        if (parcelaMapeada.getValorJuros() != null && parcelaMapeada.getValorJuros().compareTo(BigDecimal.ZERO) > 0) {
            // Provisão na data de vencimento
            contabilidadeService.registrarProvisao(grupo, cota, parcelaMapeada, ContabilidadeService.CONTA_DIREITOS_RECEBER, ContabilidadeService.CONTA_FUNDO_COMUM,
                    parcelaMapeada.getValorJuros(), parcelaMapeada.getDataVencimento(), "Provisão de Juros de mora - Parcela " + parcelaMapeada.getNumeroParcela());
            // Baixa no caixa na data de pagamento real
            contabilidadeService.registrarBaixa(grupo, cota, parcelaMapeada, ContabilidadeService.CONTA_CAIXA, ContabilidadeService.CONTA_DIREITOS_RECEBER,
                    parcelaMapeada.getValorJuros(), parcelaMapeada.getDataPagamento(), "Baixa de Juros de mora - Parcela " + parcelaMapeada.getNumeroParcela());
        }

        // --- Registrar Interação de Histórico (Módulo 4) ---
        historicoService.registrarInteracao(
                parcelaMapeada.getCota().getCliente(), parcelaMapeada.getCota(), parcelaMapeada.getCota().getGrupo(), parcelaMapeada,
                TipoInteracao.PAGAMENTO_PARCELA, "Pagamento da parcela número " + parcelaMapeada.getNumeroParcela(),
                parcelaMapeada.getCota().getGrupo().getValorCredito(), parcelaMapeada.getValorFundoComum(),
                parcelaMapeada.getValorTaxaAdministracao(), parcelaMapeada.getValorFundoReserva(), parcelaMapeada.getValorSeguro(),
                null, null, usuario);

        // --- Pagamento de Comissão (Gatilho da 1ª Parcela) ---
        if (Integer.valueOf(1).equals(parcelaMapeada.getNumeroParcela()) && cota.getContratoAdesao() != null) {
            br.com.estudo.consorcio.domain.model.ContratoAdesao contrato = cota.getContratoAdesao();
            if (contrato.getProposta() != null && contrato.getProposta().getTipoVenda() != null) {
                br.com.estudo.consorcio.domain.model.TipoVenda tipoVenda = contrato.getProposta().getTipoVenda();
                if (Boolean.FALSE.equals(tipoVenda.getLiberacaoComissaoImediata())) {
                    comissaoService.buscarPorContratoEStatus(contrato.getId(), "PENDENTE").ifPresent(comissao -> {
                        comissaoService.pagarComissao(comissao);
                        
                        contabilidadeService.registrarBaixa(grupo, cota, parcelaMapeada, 
                                ContabilidadeService.CONTA_TAXA_ADM, 
                                ContabilidadeService.CONTA_CAIXA, 
                                comissao.getValorTotalComissao(), 
                                java.time.LocalDate.now(), 
                                "Pagamento de comissão pela compensação da 1ª Parcela - Contrato " + contrato.getId());
                    });
                }
            }
        }

        efetivarAdesaoAposPagamento(parcelaMapeada, dataPagamento);

        return mapper.toResponse(parcelaMapeada); // Usar o mapper
    }

    /**
     * Efetiva contrato e cota somente depois da baixa real da primeira parcela.
     * A transição participa da mesma transação dos lançamentos COSIF do pagamento.
     *
     * @param parcela primeira parcela já baixada
     * @param dataPagamento data efetiva do recebimento
     * @throws RegraDeNegocioException se a cota pendente não possuir contrato válido
     */
    private void efetivarAdesaoAposPagamento(Parcela parcela, LocalDate dataPagamento) {
        Cota cota = parcela.getCota();
        if (!Integer.valueOf(1).equals(parcela.getNumeroParcela())
                || cota.getStatus() != StatusCota.AGUARDANDO_PAGAMENTO) {
            return;
        }

        ContratoAdesao contrato = cota.getContratoAdesao();
        if (contrato == null || contrato.getStatus() != StatusContrato.PENDENTE_PAGAMENTO) {
            throw new RegraDeNegocioException("Cota aguardando pagamento deve possuir contrato PENDENTE_PAGAMENTO.");
        }

        contrato.setStatus(StatusContrato.EFETIVADO);
        contrato.setDataAssinatura(dataPagamento.atStartOfDay());
        contratoRepository.save(contrato);

        StatusCota novoStatus = cota.getGrupo().getStatus() == StatusGrupo.EM_FORMACAO
                ? StatusCota.AGUARDANDO_INAUGURACAO
                : StatusCota.ATIVA;
        cota.setStatus(novoStatus);
        cotaRepository.save(cota);
    }

    @Transactional
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyAuthority('MANAGE_FINANCEIRO')")
    public ParcelaResponseDTO estornar(Long parcelaId) {
        Parcela parcela = parcelaRepository.findById(parcelaId)
                .orElseThrow(() -> new RegraDeNegocioException("Parcela não encontrada."));

        if (parcela.getStatus() != StatusParcela.PAGA) {
            throw new RegraDeNegocioException("Apenas parcelas com status PAGA podem ser estornadas.");
        }

        Usuario usuario = getUsuarioAutenticado();
        Grupo grupo = parcela.getCota().getGrupo();
        Cota cota = parcela.getCota();

        // 1. Cria lançamentos inversos contábeis (ESTORNO) para cada componente
        LocalDate hoje = LocalDate.now();
        contabilidadeService.registrarEstorno(grupo, cota, parcela, ContabilidadeService.CONTA_FUNDO_COMUM, ContabilidadeService.CONTA_CAIXA,
                parcela.getValorFundoComum(), hoje, "Estorno de Fundo comum - Parcela " + parcela.getNumeroParcela());

        contabilidadeService.registrarEstorno(grupo, cota, parcela, ContabilidadeService.CONTA_TAXA_ADM, ContabilidadeService.CONTA_CAIXA,
                parcela.getValorTaxaAdministracao(), hoje, "Estorno de Taxa de administração - Parcela " + parcela.getNumeroParcela());

        contabilidadeService.registrarEstorno(grupo, cota, parcela, ContabilidadeService.CONTA_FUNDO_RESERVA, ContabilidadeService.CONTA_CAIXA,
                parcela.getValorFundoReserva(), hoje, "Estorno de Fundo de reserva - Parcela " + parcela.getNumeroParcela());

        contabilidadeService.registrarEstorno(grupo, cota, parcela, ContabilidadeService.CONTA_SEGURO, ContabilidadeService.CONTA_CAIXA,
                parcela.getValorSeguro(), hoje, "Estorno de Seguro - Parcela " + parcela.getNumeroParcela());

        if (parcela.getValorMulta() != null && parcela.getValorMulta().compareTo(BigDecimal.ZERO) > 0) {
            // Estorna a baixa do caixa
            contabilidadeService.registrarEstorno(grupo, cota, parcela, ContabilidadeService.CONTA_DIREITOS_RECEBER, ContabilidadeService.CONTA_CAIXA,
                    parcela.getValorMulta(), hoje, "Estorno de Baixa de Multa - Parcela " + parcela.getNumeroParcela());
            // Estorna a provisão original
            contabilidadeService.registrarEstorno(grupo, cota, parcela, ContabilidadeService.CONTA_FUNDO_COMUM, ContabilidadeService.CONTA_DIREITOS_RECEBER,
                    parcela.getValorMulta(), parcela.getDataVencimento(), "Estorno de Provisão de Multa - Parcela " + parcela.getNumeroParcela());
        }

        if (parcela.getValorJuros() != null && parcela.getValorJuros().compareTo(BigDecimal.ZERO) > 0) {
            // Estorna a baixa do caixa
            contabilidadeService.registrarEstorno(grupo, cota, parcela, ContabilidadeService.CONTA_DIREITOS_RECEBER, ContabilidadeService.CONTA_CAIXA,
                    parcela.getValorJuros(), hoje, "Estorno de Baixa de Juros - Parcela " + parcela.getNumeroParcela());
            // Estorna a provisão original
            contabilidadeService.registrarEstorno(grupo, cota, parcela, ContabilidadeService.CONTA_FUNDO_COMUM, ContabilidadeService.CONTA_DIREITOS_RECEBER,
                    parcela.getValorJuros(), parcela.getDataVencimento(), "Estorno de Provisão de Juros - Parcela " + parcela.getNumeroParcela());
        }

        reverterAdesaoAposEstorno(parcela, grupo, cota, hoje);

        // 2. Altera status da parcela para PENDENTE (reabrir cobrança)
        parcela.setStatus(StatusParcela.PENDENTE);

        // 3. Zera valorPago, valorMulta, valorJuros, dataPagamento
        parcela.setValorPago(null);
        parcela.setValorMulta(BigDecimal.ZERO);
        parcela.setValorJuros(BigDecimal.ZERO);
        parcela.setDataPagamento(null);

        Parcela parcelaSalva = parcelaRepository.save(parcela);

        return mapper.toResponse(parcelaSalva);
    }

    /**
     * Reverte a efetivação que foi produzida exclusivamente pela baixa da primeira parcela.
     *
     * <p>A reversão mantém a atomicidade entre a parcela, o contrato, a cota e a comissão,
     * conforme RN-VND-011 e RN-FUN-005. Uma adesão com pagamentos posteriores não pode ser
     * estornada isoladamente, pois isso deixaria o histórico financeiro inconsistente.</p>
     *
     * @param parcela parcela submetida ao estorno
     * @param grupo grupo vinculado à cota
     * @param cota cota vinculada à parcela
     * @param dataEstorno data contábil do estorno
     * @throws RegraDeNegocioException se houver pagamento posterior ou estados incompatíveis
     */
    private void reverterAdesaoAposEstorno(Parcela parcela, Grupo grupo, Cota cota, LocalDate dataEstorno) {
        if (!Integer.valueOf(1).equals(parcela.getNumeroParcela())) {
            return;
        }

        boolean possuiPagamentosPosteriores = parcelaRepository.findByCotaId(cota.getId()).stream()
                .anyMatch(p -> p.getNumeroParcela() != null
                        && p.getNumeroParcela() > 1
                        && p.getStatus() == StatusParcela.PAGA);
        if (possuiPagamentosPosteriores) {
            throw new RegraDeNegocioException("Não é possível estornar a primeira parcela após pagamentos posteriores.");
        }

        ContratoAdesao contrato = cota.getContratoAdesao();
        if (contrato == null
                || contrato.getStatus() != StatusContrato.EFETIVADO
                || (cota.getStatus() != StatusCota.ATIVA
                && cota.getStatus() != StatusCota.AGUARDANDO_INAUGURACAO)) {
            throw new RegraDeNegocioException("Estorno da adesão encontrou contrato ou cota em estado incompatível.");
        }

        comissaoService.buscarPorContratoEStatus(contrato.getId(), "PAGA").ifPresent(comissao -> {
            comissaoService.estornarComissao(comissao);
            contabilidadeService.registrarEstorno(grupo, cota, parcela,
                    ContabilidadeService.CONTA_CAIXA,
                    ContabilidadeService.CONTA_TAXA_ADM,
                    comissao.getValorTotalComissao(),
                    dataEstorno,
                    "Estorno de comissão pela reversão da adesão - Contrato " + contrato.getId());
        });

        contrato.setStatus(StatusContrato.PENDENTE_PAGAMENTO);
        contrato.setDataAssinatura(null);
        contratoRepository.save(contrato);

        cota.setStatus(StatusCota.AGUARDANDO_PAGAMENTO);
        cotaRepository.save(cota);
    }

    // Os métodos de amortização continuam iguais, pois eles operam listas internas no banco
    @Transactional
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyAuthority('MANAGE_FINANCEIRO')")
    public void amortizarPorReducaoDePrazo(Long cotaId, BigDecimal valorLance) {
        Cota cota = cotaRepository.findById(cotaId)
                .orElseThrow(() -> new RegraDeNegocioException("Cota não encontrada."));
        if (cota.getContratoAdesao() == null
                || cota.getContratoAdesao().getStatus() != StatusContrato.EFETIVADO) {
            throw new RegraDeNegocioException("Amortização só é permitida para cotas com adesão efetivada.");
        }

        List<Parcela> parcelasDeTrasParaFrente = parcelaRepository.findByCotaIdAndStatusOrderByNumeroParcelaDesc(cotaId, StatusParcela.PENDENTE);
        validarValorESaldoPendente(valorLance, parcelasDeTrasParaFrente);
        BigDecimal saldoLance = valorLance;

        for (Parcela parcela : parcelasDeTrasParaFrente) {
            if (saldoLance.compareTo(BigDecimal.ZERO) <= 0) break;

            BigDecimal amortizacao = saldoLance.min(parcela.getValorFundoComum());
            parcela.setValorFundoComum(parcela.getValorFundoComum().subtract(amortizacao));
            parcela.calcularValorTotal();
            saldoLance = saldoLance.subtract(amortizacao);
        }
        parcelaRepository.saveAll(parcelasDeTrasParaFrente);
    }

    @Transactional
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyAuthority('MANAGE_FINANCEIRO')")
    public void amortizarPorDiluicao(Long cotaId, BigDecimal valorLance) {
        Cota cota = cotaRepository.findById(cotaId)
                .orElseThrow(() -> new RegraDeNegocioException("Cota não encontrada."));
        if (cota.getContratoAdesao() == null
                || cota.getContratoAdesao().getStatus() != StatusContrato.EFETIVADO) {
            throw new RegraDeNegocioException("Amortização só é permitida para cotas com adesão efetivada.");
        }
        List<Parcela> parcelasPendentes = parcelaRepository.findByCotaIdAndStatusOrderByNumeroParcelaAsc(cotaId, StatusParcela.PENDENTE);
        validarValorESaldoPendente(valorLance, parcelasPendentes);
        BigInteger totalCentavos = parcelasPendentes.stream().map(p -> emCentavos(p.getValorFundoComum()))
                .reduce(BigInteger.ZERO, BigInteger::add);
        BigInteger abatimentoCentavos = emCentavos(valorLance);
        java.util.List<AmortizacaoProporcional> distribuicao = new java.util.ArrayList<>();
        BigInteger aplicado = BigInteger.ZERO;
        for (Parcela parcela : parcelasPendentes) {
            BigInteger[] divisao = abatimentoCentavos.multiply(emCentavos(parcela.getValorFundoComum())).divideAndRemainder(totalCentavos);
            distribuicao.add(new AmortizacaoProporcional(parcela, divisao[0], divisao[1]));
            aplicado = aplicado.add(divisao[0]);
        }
        distribuicao.sort(java.util.Comparator.comparing(AmortizacaoProporcional::resto).reversed()
                .thenComparing(item -> item.parcela().getNumeroParcela(),
                        java.util.Comparator.nullsLast(java.util.Comparator.reverseOrder())));
        BigInteger restante = abatimentoCentavos.subtract(aplicado);
        for (int i = 0; restante.signum() > 0; i = (i + 1) % distribuicao.size()) {
            AmortizacaoProporcional item = distribuicao.get(i);
            if (item.valorBase().compareTo(emCentavos(item.parcela().getValorFundoComum())) < 0) {
                item.adicionarCentavo();
                restante = restante.subtract(BigInteger.ONE);
            }
        }
        for (AmortizacaoProporcional item : distribuicao) {
            Parcela parcela = item.parcela();
            parcela.setValorFundoComum(deCentavos(emCentavos(parcela.getValorFundoComum()).subtract(item.valorBase())));
            parcela.calcularValorTotal();
        }
        parcelaRepository.saveAll(parcelasPendentes);
    }

    private void validarValorESaldoPendente(BigDecimal valorLance, List<Parcela> parcelasPendentes) {
        if (valorLance == null || valorLance.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RegraDeNegocioException("O valor da amortização deve ser positivo.");
        }
        BigDecimal saldoFundoComum = parcelasPendentes.stream().map(Parcela::getValorFundoComum)
                .filter(java.util.Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (valorLance.compareTo(saldoFundoComum) > 0) {
            throw new RegraDeNegocioException("O valor da amortização excede o saldo pendente de Fundo Comum.");
        }
    }

    private BigInteger emCentavos(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.UNNECESSARY).movePointRight(2).toBigIntegerExact();
    }

    private BigDecimal deCentavos(BigInteger valor) {
        return new BigDecimal(valor, 2);
    }

    private static final class AmortizacaoProporcional {
        private final Parcela parcela;
        private BigInteger valorBase;
        private final BigInteger resto;
        private AmortizacaoProporcional(Parcela parcela, BigInteger valorBase, BigInteger resto) { this.parcela = parcela; this.valorBase = valorBase; this.resto = resto; }
        private Parcela parcela() { return parcela; }
        private BigInteger valorBase() { return valorBase; }
        private BigInteger resto() { return resto; }
        private void adicionarCentavo() { valorBase = valorBase.add(BigInteger.ONE); }
    }

    @org.springframework.security.access.prepost.PreAuthorize("hasAnyAuthority('VIEW_FINANCEIRO', 'VIEW_COMPLIANCE') or @ownershipGuard.canAccessCota(#cotaId)")
    public List<ParcelaResponseDTO> listarPorCota(Long cotaId) {
        return parcelaRepository.findByCotaId(cotaId).stream()
                .map(mapper::toResponse) // Usar o mapper
                .toList();
    }

    @Transactional(readOnly = true)
    public CotaInadimplenciaResponseDTO obterInadimplenciaCota(Long cotaId) {
        Cota cota = cotaRepository.findById(cotaId)
                .orElseThrow(() -> new RegraDeNegocioException("Cota não encontrada."));

        List<Parcela> todas = parcelaRepository.findByCotaId(cotaId);
        List<Parcela> atrasadas = todas.stream()
                .filter(p -> p.getStatus() == StatusParcela.PENDENTE && p.getDataVencimento().isBefore(LocalDate.now()))
                .toList();

        if (atrasadas.isEmpty()) {
            return new CotaInadimplenciaResponseDTO(
                    cotaId,
                    cota.getCodigoCota(),
                    false,
                    0,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    List.of()
            );
        }

        BigDecimal valorOriginalTotal = BigDecimal.ZERO;
        BigDecimal multaTotal = BigDecimal.ZERO;
        BigDecimal jurosTotal = BigDecimal.ZERO;

        java.util.List<ParcelaResponseDTO> detalheDtos = new java.util.ArrayList<>();

        for (Parcela p : atrasadas) {
            long dias = ChronoUnit.DAYS.between(p.getDataVencimento(), LocalDate.now());
            BigDecimal m = p.getValorParcela().multiply(new BigDecimal("0.02")).setScale(2, RoundingMode.HALF_UP);
            BigDecimal taxaMensal = new BigDecimal("0.01");
            BigDecimal taxaDiaria = taxaMensal.divide(new BigDecimal("30"), 10, RoundingMode.HALF_UP);
            BigDecimal j = p.getValorParcela().multiply(taxaDiaria).multiply(new BigDecimal(dias)).setScale(2, RoundingMode.HALF_UP);

            valorOriginalTotal = valorOriginalTotal.add(p.getValorParcela());
            multaTotal = multaTotal.add(m);
            jurosTotal = jurosTotal.add(j);

            // FC-06 FIX: Construir DTO manualmente ao invés de modificar a entidade managed
            // Evita corrupção de dados caso o persistence context faça flush
            ParcelaResponseDTO dto = mapper.toResponse(p);
            detalheDtos.add(dto);
        }

        BigDecimal saldoDevedor = valorOriginalTotal.add(multaTotal).add(jurosTotal);

        return new CotaInadimplenciaResponseDTO(
                cotaId,
                cota.getCodigoCota(),
                true,
                atrasadas.size(),
                valorOriginalTotal,
                multaTotal,
                jurosTotal,
                saldoDevedor,
                detalheDtos
        );
    }
}
