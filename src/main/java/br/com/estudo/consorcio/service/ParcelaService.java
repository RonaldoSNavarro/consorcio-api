package br.com.estudo.consorcio.service;

import br.com.estudo.consorcio.domain.model.Parcela;
import br.com.estudo.consorcio.domain.model.StatusParcela;
import br.com.estudo.consorcio.domain.repository.CotaRepository;
import br.com.estudo.consorcio.domain.repository.ParcelaRepository;
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
    public Parcela salvar(Parcela parcela) {
        if (parcela.getCota() == null || parcela.getCota().getId() == null || !cotaRepository.existsById(parcela.getCota().getId())) {
            throw new RuntimeException("Cota inválida ou não encontrada no banco de dados.");
        }

        if (parcela.getStatus() == null) {
            parcela.setStatus(StatusParcela.PENDENTE);
        }

        return parcelaRepository.save(parcela);
    }

    @Transactional
    public Parcela pagar(Long parcelaId, LocalDate dataPagamento) {
        Parcela parcela = parcelaRepository.findById(parcelaId)
                .orElseThrow(() -> new RuntimeException("Parcela não encontrada."));

        if (parcela.getStatus() == StatusParcela.PAGA) {
            throw new RuntimeException("Esta parcela já consta como paga.");
        }

        parcela.setDataPagamento(dataPagamento);

        // Regra de Inadimplência (BCB / CDC)
        if (dataPagamento.isAfter(parcela.getDataVencimento())) {
            long diasAtraso = ChronoUnit.DAYS.between(parcela.getDataVencimento(), dataPagamento);

            // Multa: 2% sobre o valor total da parcela
            BigDecimal multa = parcela.getValorParcela()
                    .multiply(new BigDecimal("0.02"))
                    .setScale(2, RoundingMode.HALF_UP);

            // Juros de Mora: 1% ao mês (pro-rata die)
            BigDecimal taxaMensal = new BigDecimal("0.01");
            BigDecimal taxaDiaria = taxaMensal.divide(new BigDecimal("30"), 10, RoundingMode.HALF_UP);
            BigDecimal juros = parcela.getValorParcela()
                    .multiply(taxaDiaria)
                    .multiply(new BigDecimal(diasAtraso))
                    .setScale(2, RoundingMode.HALF_UP);

            parcela.setValorMulta(multa);
            parcela.setValorJuros(juros);
            parcela.setValorPago(parcela.getValorParcela().add(multa).add(juros));
        } else {
            // Pagamento em dia ou antecipado
            parcela.setValorMulta(BigDecimal.ZERO);
            parcela.setValorJuros(BigDecimal.ZERO);
            parcela.setValorPago(parcela.getValorParcela());
        }

        parcela.setStatus(StatusParcela.PAGA);

        return parcelaRepository.save(parcela);
    }

    public List<Parcela> listarPorCota(Long cotaId) {
        return parcelaRepository.findByCotaId(cotaId);
    }
}