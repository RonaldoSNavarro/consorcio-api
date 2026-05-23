package br.com.estudo.consorcio.service;

import br.com.estudo.consorcio.domain.dto.CotaInadimplenciaResponseDTO;
import br.com.estudo.consorcio.domain.dto.ParcelaRequestDTO;
import br.com.estudo.consorcio.domain.dto.ParcelaResponseDTO;
import br.com.estudo.consorcio.domain.mapper.ParcelaMapper;
import br.com.estudo.consorcio.domain.model.*;
import br.com.estudo.consorcio.domain.repository.CotaRepository;
import br.com.estudo.consorcio.domain.repository.ParcelaRepository;
import br.com.estudo.consorcio.exception.RegraDeNegocioException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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

    public ParcelaService(ParcelaRepository parcelaRepository, CotaRepository cotaRepository,
                          ParcelaMapper mapper, MovimentoFinanceiroService movimentoService,
                          HistoricoConsorciadoService historicoService) {
        this.parcelaRepository = parcelaRepository;
        this.cotaRepository = cotaRepository;
        this.mapper = mapper;
        this.movimentoService = movimentoService;
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
    public ParcelaResponseDTO salvar(ParcelaRequestDTO dto) {
        // 1. Valida e busca a Cota
        Cota cota = cotaRepository.findById(dto.cotaId())
                .orElseThrow(() -> new RegraDeNegocioException("Cota inválida ou não encontrada no banco de dados."));

        // 2. Mapeia DTO para Entidade usando o mapper
        Parcela parcela = mapper.toEntity(dto);
        parcela.setCota(cota); // Setar a cota após a busca

        // 3. Regra de negócio: Parcela nasce PENDENTE
        parcela.setStatus(StatusParcela.PENDENTE);

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

        // --- Registrar Movimentos Financeiros (Módulo 2) ---
        Usuario usuario = getUsuarioAutenticado();
        Grupo grupo = parcelaMapeada.getCota().getGrupo();
        Cota cota = parcelaMapeada.getCota();

        movimentoService.registrarMovimento(grupo, cota, parcelaMapeada, null,
                TipoMovimentoFinanceiro.FUNDO_COMUM, NaturezaMovimento.CREDITO,
                parcelaMapeada.getValorFundoComum(), "Fundo comum pago - Parcela " + parcelaMapeada.getNumeroParcela(), usuario);

        movimentoService.registrarMovimento(grupo, cota, parcelaMapeada, null,
                TipoMovimentoFinanceiro.TAXA_ADMINISTRACAO, NaturezaMovimento.CREDITO,
                parcelaMapeada.getValorTaxaAdministracao(), "Taxa de administração paga - Parcela " + parcelaMapeada.getNumeroParcela(), usuario);

        movimentoService.registrarMovimento(grupo, cota, parcelaMapeada, null,
                TipoMovimentoFinanceiro.FUNDO_RESERVA, NaturezaMovimento.CREDITO,
                parcelaMapeada.getValorFundoReserva(), "Fundo de reserva pago - Parcela " + parcelaMapeada.getNumeroParcela(), usuario);

        movimentoService.registrarMovimento(grupo, cota, parcelaMapeada, null,
                TipoMovimentoFinanceiro.SEGURO, NaturezaMovimento.CREDITO,
                parcelaMapeada.getValorSeguro(), "Seguro pago - Parcela " + parcelaMapeada.getNumeroParcela(), usuario);

        if (parcelaMapeada.getValorMulta() != null && parcelaMapeada.getValorMulta().compareTo(BigDecimal.ZERO) > 0) {
            movimentoService.registrarMovimento(grupo, cota, parcelaMapeada, null,
                    TipoMovimentoFinanceiro.MULTA_ATRASO, NaturezaMovimento.CREDITO,
                    parcelaMapeada.getValorMulta(), "Multa por atraso paga - Parcela " + parcelaMapeada.getNumeroParcela(), usuario);
        }

        if (parcelaMapeada.getValorJuros() != null && parcelaMapeada.getValorJuros().compareTo(BigDecimal.ZERO) > 0) {
            movimentoService.registrarMovimento(grupo, cota, parcelaMapeada, null,
                    TipoMovimentoFinanceiro.JUROS_MORA, NaturezaMovimento.CREDITO,
                    parcelaMapeada.getValorJuros(), "Juros de mora pago - Parcela " + parcelaMapeada.getNumeroParcela(), usuario);
        }

        // --- Registrar Interação de Histórico (Módulo 4) ---
        historicoService.registrarInteracao(
                parcelaMapeada.getCota().getCliente(), parcelaMapeada.getCota(), parcelaMapeada.getCota().getGrupo(), parcelaMapeada,
                TipoInteracao.PAGAMENTO_PARCELA, "Pagamento da parcela número " + parcelaMapeada.getNumeroParcela(),
                parcelaMapeada.getCota().getGrupo().getValorCredito(), parcelaMapeada.getValorFundoComum(),
                parcelaMapeada.getValorTaxaAdministracao(), parcelaMapeada.getValorFundoReserva(), parcelaMapeada.getValorSeguro(),
                null, null, usuario);

        return mapper.toResponse(parcelaMapeada); // Usar o mapper
    }

    @Transactional
    public ParcelaResponseDTO estornar(Long parcelaId) {
        Parcela parcela = parcelaRepository.findById(parcelaId)
                .orElseThrow(() -> new RegraDeNegocioException("Parcela não encontrada."));

        if (parcela.getStatus() != StatusParcela.PAGA) {
            throw new RegraDeNegocioException("Apenas parcelas com status PAGA podem ser estornadas.");
        }

        Usuario usuario = getUsuarioAutenticado();
        Grupo grupo = parcela.getCota().getGrupo();
        Cota cota = parcela.getCota();

        // 1. Cria lançamentos inversos (DEBITO) para cada componente original
        movimentoService.registrarMovimento(grupo, cota, parcela, null,
                TipoMovimentoFinanceiro.ESTORNO_PAGAMENTO, NaturezaMovimento.DEBITO,
                parcela.getValorFundoComum(), "Estorno de Fundo comum - Parcela " + parcela.getNumeroParcela(), usuario);

        movimentoService.registrarMovimento(grupo, cota, parcela, null,
                TipoMovimentoFinanceiro.ESTORNO_PAGAMENTO, NaturezaMovimento.DEBITO,
                parcela.getValorTaxaAdministracao(), "Estorno de Taxa de administração - Parcela " + parcela.getNumeroParcela(), usuario);

        movimentoService.registrarMovimento(grupo, cota, parcela, null,
                TipoMovimentoFinanceiro.ESTORNO_PAGAMENTO, NaturezaMovimento.DEBITO,
                parcela.getValorFundoReserva(), "Estorno de Fundo de reserva - Parcela " + parcela.getNumeroParcela(), usuario);

        movimentoService.registrarMovimento(grupo, cota, parcela, null,
                TipoMovimentoFinanceiro.ESTORNO_PAGAMENTO, NaturezaMovimento.DEBITO,
                parcela.getValorSeguro(), "Estorno de Seguro - Parcela " + parcela.getNumeroParcela(), usuario);

        if (parcela.getValorMulta() != null && parcela.getValorMulta().compareTo(BigDecimal.ZERO) > 0) {
            movimentoService.registrarMovimento(grupo, cota, parcela, null,
                    TipoMovimentoFinanceiro.ESTORNO_PAGAMENTO, NaturezaMovimento.DEBITO,
                    parcela.getValorMulta(), "Estorno de Multa por atraso - Parcela " + parcela.getNumeroParcela(), usuario);
        }

        if (parcela.getValorJuros() != null && parcela.getValorJuros().compareTo(BigDecimal.ZERO) > 0) {
            movimentoService.registrarMovimento(grupo, cota, parcela, null,
                    TipoMovimentoFinanceiro.ESTORNO_PAGAMENTO, NaturezaMovimento.DEBITO,
                    parcela.getValorJuros(), "Estorno de Juros de mora - Parcela " + parcela.getNumeroParcela(), usuario);
        }

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

    // Os métodos de amortização continuam iguais, pois eles operam listas internas no banco
    @Transactional
    public void amortizarPorReducaoDePrazo(Long cotaId, BigDecimal valorLance) {
        List<Parcela> parcelasDeTrasParaFrente = parcelaRepository.findByCotaIdAndStatusOrderByNumeroParcelaDesc(cotaId, StatusParcela.PENDENTE);
        BigDecimal saldoLance = valorLance;

        for (Parcela parcela : parcelasDeTrasParaFrente) {
            if (saldoLance.compareTo(BigDecimal.ZERO) <= 0) break;

            if (saldoLance.compareTo(parcela.getValorParcela()) >= 0) {
                parcela.setStatus(StatusParcela.PAGA);
                parcela.setDataPagamento(LocalDate.now());
                parcela.setValorMulta(BigDecimal.ZERO);
                parcela.setValorJuros(BigDecimal.ZERO);
                parcela.setValorPago(parcela.getValorParcela());
                saldoLance = saldoLance.subtract(parcela.getValorParcela());
            } else {
                BigDecimal novoFundoComum = parcela.getValorFundoComum().subtract(saldoLance);
                parcela.setValorFundoComum(novoFundoComum);
                saldoLance = BigDecimal.ZERO;
            }
        }
        parcelaRepository.saveAll(parcelasDeTrasParaFrente);
    }

    @Transactional
    public void amortizarPorDiluicao(Long cotaId, BigDecimal valorLance) {
        List<Parcela> parcelasPendentes = parcelaRepository.findByCotaIdAndStatusOrderByNumeroParcelaAsc(cotaId, StatusParcela.PENDENTE);
        if (parcelasPendentes.isEmpty()) throw new RegraDeNegocioException("Não há parcelas pendentes para amortizar.");

        int quantidadeParcelas = parcelasPendentes.size();
        BigDecimal abatimentoPorParcela = valorLance.divide(new BigDecimal(quantidadeParcelas), 2, RoundingMode.DOWN);
        BigDecimal valorAplicado = BigDecimal.ZERO;

        for (int i = 0; i < quantidadeParcelas; i++) {
            Parcela parcela = parcelasPendentes.get(i);
            BigDecimal abatimentoAtual = (i == quantidadeParcelas - 1) ? valorLance.subtract(valorAplicado) : abatimentoPorParcela;

            BigDecimal novoFundoComum = parcela.getValorFundoComum().subtract(abatimentoAtual);
            if (novoFundoComum.compareTo(BigDecimal.ZERO) < 0) novoFundoComum = BigDecimal.ZERO;

            parcela.setValorFundoComum(novoFundoComum);
            valorAplicado = valorAplicado.add(abatimentoAtual);
        }
        parcelaRepository.saveAll(parcelasPendentes);
    }

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
                    cota.getNumeroCota(),
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

            // Modifica temporariamente a parcela para mapeamento elegante
            p.setValorMulta(m);
            p.setValorJuros(j);
            p.setValorPago(p.getValorParcela().add(m).add(j));

            detalheDtos.add(mapper.toResponse(p));
        }

        BigDecimal saldoDevedor = valorOriginalTotal.add(multaTotal).add(jurosTotal);

        return new CotaInadimplenciaResponseDTO(
                cotaId,
                cota.getNumeroCota(),
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