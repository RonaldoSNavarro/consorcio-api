package br.com.estudo.consorcio.service;

import br.com.estudo.consorcio.domain.dto.ParcelaRequestDTO;
import br.com.estudo.consorcio.domain.dto.ParcelaResponseDTO;
import br.com.estudo.consorcio.domain.model.Cota;
import br.com.estudo.consorcio.domain.model.Parcela;
import br.com.estudo.consorcio.domain.model.StatusParcela;
import br.com.estudo.consorcio.domain.repository.CotaRepository;
import br.com.estudo.consorcio.domain.repository.ParcelaRepository;
import br.com.estudo.consorcio.exception.RegraDeNegocioException;
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

    public ParcelaService(ParcelaRepository parcelaRepository, CotaRepository cotaRepository) {
        this.parcelaRepository = parcelaRepository;
        this.cotaRepository = cotaRepository;
    }

    @Transactional
    public ParcelaResponseDTO salvar(ParcelaRequestDTO dto) {
        // 1. Valida e busca a Cota
        Cota cota = cotaRepository.findById(dto.cotaId())
                .orElseThrow(() -> new RegraDeNegocioException("Cota inválida ou não encontrada no banco de dados."));

        // 2. Mapeia DTO para Entidade
        Parcela parcela = new Parcela();
        parcela.setCota(cota);
        parcela.setNumeroParcela(dto.numeroParcela());
        parcela.setValorFundoComum(dto.valorFundoComum());
        parcela.setValorTaxaAdministracao(dto.valorTaxaAdministracao());
        parcela.setValorFundoReserva(dto.valorFundoReserva());
        parcela.setDataVencimento(dto.dataVencimento());

        // 3. Regra de negócio: Parcela nasce PENDENTE
        parcela.setStatus(StatusParcela.PENDENTE);

        // O JPA chamará o @PrePersist e calculará o valorParcela (soma dos três)
        Parcela parcelaSalva = parcelaRepository.save(parcela);

        return converterParaResponseDTO(parcelaSalva);
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

        return converterParaResponseDTO(parcelaMapeada);
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
                .map(this::converterParaResponseDTO)
                .toList();
    }

    // Método centralizado de conversão
    private ParcelaResponseDTO converterParaResponseDTO(Parcela parcela) {
        return new ParcelaResponseDTO(
                parcela.getId(),
                parcela.getCota().getId(),
                parcela.getNumeroParcela(),
                parcela.getValorFundoComum(),
                parcela.getValorTaxaAdministracao(),
                parcela.getValorFundoReserva(),
                parcela.getValorParcela(),
                parcela.getValorMulta(),
                parcela.getValorJuros(),
                parcela.getValorPago(),
                parcela.getDataVencimento(),
                parcela.getDataPagamento(),
                parcela.getStatus()
        );
    }
}