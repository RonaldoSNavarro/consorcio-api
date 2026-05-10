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

    @Transactional
    public void amortizarPorReducaoDePrazo(Long cotaId, BigDecimal valorLance) {
        // 1. Pega todas as parcelas pendentes de trás para frente (Ex: 60, 59, 58...)
        List<Parcela> parcelasDeTrasParaFrente = parcelaRepository
                .findByCotaIdAndStatusOrderByNumeroParcelaDesc(cotaId, StatusParcela.PENDENTE);

        BigDecimal saldoLance = valorLance;

        for (Parcela parcela : parcelasDeTrasParaFrente) {
            // Se o dinheiro do lance acabou, paramos o loop
            if (saldoLance.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }

            // Cenário A: O saldo do lance é maior ou igual ao valor da parcela inteira
            if (saldoLance.compareTo(parcela.getValorParcela()) >= 0) {
                parcela.setStatus(StatusParcela.PAGA);
                parcela.setDataPagamento(LocalDate.now());

                // Como ele antecipou, não cobramos multa nem juros
                parcela.setValorMulta(BigDecimal.ZERO);
                parcela.setValorJuros(BigDecimal.ZERO);
                parcela.setValorPago(parcela.getValorParcela());

                // Subtrai do lance o valor que acabamos de usar
                saldoLance = saldoLance.subtract(parcela.getValorParcela());
            }
            // Cenário B: O saldo do lance não quita a parcela inteira
            else {
                // O lance abate o Fundo Comum (FC) da parcela atual.
                // Ex: Parcela tem FC de R$ 1000. Sobrou R$ 400 de lance. O novo FC será R$ 600.
                BigDecimal novoFundoComum = parcela.getValorFundoComum().subtract(saldoLance);
                parcela.setValorFundoComum(novoFundoComum);

                // Não precisamos setar o novo valorParcela, pois o @PreUpdate da entidade
                // vai somar esse novo FC com a Taxa e a Reserva automaticamente antes de salvar!

                saldoLance = BigDecimal.ZERO; // Zeramos o saldo para encerrar o loop no próximo giro
            }
        }

        // Salva todas as parcelas modificadas de uma vez só
        parcelaRepository.saveAll(parcelasDeTrasParaFrente);
    }

    @Transactional
    public void amortizarPorDiluicao(Long cotaId, BigDecimal valorLance) {
        // 1. Pega todas as parcelas pendentes
        List<Parcela> parcelasPendentes = parcelaRepository
                .findByCotaIdAndStatusOrderByNumeroParcelaAsc(cotaId, StatusParcela.PENDENTE);

        if (parcelasPendentes.isEmpty()) {
            throw new RuntimeException("Não há parcelas pendentes para amortizar.");
        }

        // 2. Descobre quantas parcelas vão receber o desconto
        int quantidadeParcelas = parcelasPendentes.size();
        BigDecimal qtdDecimal = new BigDecimal(quantidadeParcelas);

        // 3. Divide o lance pelo número de parcelas (Arredondando para baixo, 2 casas)
        BigDecimal abatimentoPorParcela = valorLance.divide(qtdDecimal, 2, RoundingMode.DOWN);

        // Variável para rastrear quanto do lance já aplicamos de fato
        BigDecimal valorAplicado = BigDecimal.ZERO;

        for (int i = 0; i < quantidadeParcelas; i++) {
            Parcela parcela = parcelasPendentes.get(i);
            BigDecimal abatimentoAtual = abatimentoPorParcela;

            // 4. A Regra do Centavo Perdido: Se for a ÚLTIMA parcela do loop,
            // o abatimento dela será tudo o que sobrou do lance original.
            if (i == quantidadeParcelas - 1) {
                abatimentoAtual = valorLance.subtract(valorAplicado);
            }

            // 5. Abate do Fundo Comum
            BigDecimal novoFundoComum = parcela.getValorFundoComum().subtract(abatimentoAtual);

            // Trava de segurança: Se o lance diluído for maior que o FC da parcela, zera o FC.
            if (novoFundoComum.compareTo(BigDecimal.ZERO) < 0) {
                novoFundoComum = BigDecimal.ZERO;
            }

            parcela.setValorFundoComum(novoFundoComum);

            // Registra que esse pedaço do lance já foi usado
            valorAplicado = valorAplicado.add(abatimentoAtual);
        }

        // 6. Salva tudo. O @PreUpdate da Parcela vai somar o novo FC com as taxas e atualizar o valor total!
        parcelaRepository.saveAll(parcelasPendentes);
    }

    public List<Parcela> listarPorCota(Long cotaId) {
        return parcelaRepository.findByCotaId(cotaId);
    }
}