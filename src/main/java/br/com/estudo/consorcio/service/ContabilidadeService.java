package br.com.estudo.consorcio.service;

import br.com.estudo.consorcio.domain.model.*;
import br.com.estudo.consorcio.domain.repository.ContaContabilRepository;
import br.com.estudo.consorcio.domain.repository.LancamentoContabilRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
public class ContabilidadeService {

    private final LancamentoContabilRepository lancamentoRepository;
    private final ContaContabilRepository contaRepository;

    public static final String CONTA_CAIXA = "1.1.1.10.00-2";
    public static final String CONTA_DIREITOS_RECEBER = "1.2.1.10.00-8";
    public static final String CONTA_FUNDO_COMUM = "2.1.2.10.10-6";
    public static final String CONTA_FUNDO_RESERVA = "2.1.2.10.20-9";
    public static final String CONTA_TAXA_ADM = "2.1.2.10.30-2";
    public static final String CONTA_SEGURO = "2.1.2.10.40-5";
    public static final String CONTA_RENDIMENTO = "2.1.2.10.50-8";
    public static final String CONTA_EXCLUIDOS_DEVOLVER = "2.1.2.20.10-3";
    public static final String CONTA_CREDITOS_LIBERAR = "2.1.2.30.10-0";

    public ContabilidadeService(LancamentoContabilRepository lancamentoRepository, ContaContabilRepository contaRepository) {
        this.lancamentoRepository = lancamentoRepository;
        this.contaRepository = contaRepository;
    }

    @Transactional(readOnly = true)
    public BigDecimal calcularSaldoConta(Grupo grupo, String codigoCosif) {
        ContaContabil conta = getConta(codigoCosif);

        // FC-05 FIX: Queries agregadas no PostgreSQL — O(1) heap ao invés de carregar milhões de registros
        BigDecimal creditos = lancamentoRepository.somarCreditosPorGrupoEConta(grupo.getId(), conta.getId());
        BigDecimal debitos = lancamentoRepository.somarDebitosPorGrupoEConta(grupo.getId(), conta.getId());

        // Se a conta tem natureza CREDORA (ex: Fundo Comum 2.1.0.01), saldo = Creditos - Debitos
        if (NaturezaContabil.CREDORA.equals(conta.getNatureza())) {
            return creditos.subtract(debitos);
        } else {
            // Se natureza DEVEDORA (ex: Caixa 1.1.0.00), saldo = Debitos - Creditos
            return debitos.subtract(creditos);
        }
    }

    private ContaContabil getConta(String codigoCosif) {
        return contaRepository.findByCodigoCosif(codigoCosif)
                .orElseThrow(() -> new RuntimeException("Conta contábil não configurada no plano de contas: " + codigoCosif));
    }

    @Transactional
    public void registrarProvisao(Grupo grupo, Cota cota, Parcela parcela, String contaDebitoCosif, String contaCreditoCosif, BigDecimal valor, LocalDate dataCompetencia, String historico) {
        if (valor == null || valor.compareTo(BigDecimal.ZERO) <= 0) return;
        registrarLancamento(grupo, cota, parcela, contaDebitoCosif, contaCreditoCosif, valor, dataCompetencia, TipoOperacaoContabil.PROVISAO, historico);
    }

    @Transactional
    public void registrarBaixa(Grupo grupo, Cota cota, Parcela parcela, String contaDebitoCosif, String contaCreditoCosif, BigDecimal valor, LocalDate dataCompetencia, String historico) {
        if (valor == null || valor.compareTo(BigDecimal.ZERO) <= 0) return;
        registrarLancamento(grupo, cota, parcela, contaDebitoCosif, contaCreditoCosif, valor, dataCompetencia, TipoOperacaoContabil.BAIXA, historico);
    }

    @Transactional
    public void registrarEstorno(Grupo grupo, Cota cota, Parcela parcela, String contaDebitoCosif, String contaCreditoCosif, BigDecimal valor, LocalDate dataCompetencia, String historico) {
        if (valor == null || valor.compareTo(BigDecimal.ZERO) <= 0) return;
        registrarLancamento(grupo, cota, parcela, contaDebitoCosif, contaCreditoCosif, valor, dataCompetencia, TipoOperacaoContabil.ESTORNO, historico);
    }

    private void registrarLancamento(Grupo grupo, Cota cota, Parcela parcela, String contaDebitoCosif, String contaCreditoCosif, BigDecimal valor, LocalDate dataCompetencia, TipoOperacaoContabil tipo, String historico) {
        LancamentoContabil lancamento = new LancamentoContabil();
        lancamento.setGrupo(grupo);
        lancamento.setCota(cota);
        lancamento.setParcela(parcela);
        lancamento.setContaDebito(getConta(contaDebitoCosif));
        lancamento.setContaCredito(getConta(contaCreditoCosif));
        lancamento.setValor(valor);
        lancamento.setDataCompetencia(dataCompetencia);
        lancamento.setTipoOperacao(tipo);
        lancamento.setHistorico(historico);
        
        lancamentoRepository.save(lancamento);
    }
}
