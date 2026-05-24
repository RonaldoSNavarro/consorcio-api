package br.com.estudo.consorcio.service;

import br.com.estudo.consorcio.domain.model.*;
import br.com.estudo.consorcio.domain.repository.ContaContabilRepository;
import br.com.estudo.consorcio.domain.repository.LancamentoContabilRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class ContabilidadeService {

    private final LancamentoContabilRepository lancamentoRepository;
    private final ContaContabilRepository contaRepository;

    public static final String CONTA_CAIXA = "1.1.0.00";
    public static final String CONTA_DIREITOS_RECEBER = "1.2.0.00";
    public static final String CONTA_FUNDO_COMUM = "2.1.0.01";
    public static final String CONTA_FUNDO_RESERVA = "2.1.0.02";
    public static final String CONTA_TAXA_ADM = "2.1.0.03";
    public static final String CONTA_SEGURO = "2.1.0.04";
    public static final String CONTA_RENDIMENTO = "3.1.0.01";

    public ContabilidadeService(LancamentoContabilRepository lancamentoRepository, ContaContabilRepository contaRepository) {
        this.lancamentoRepository = lancamentoRepository;
        this.contaRepository = contaRepository;
    }

    @Transactional(readOnly = true)
    public BigDecimal calcularSaldoConta(Grupo grupo, String codigoCosif) {
        ContaContabil conta = getConta(codigoCosif);
        
        List<LancamentoContabil> lancamentos = lancamentoRepository.findByGrupoId(grupo.getId(), org.springframework.data.domain.Pageable.unpaged()).getContent();

        BigDecimal creditos = lancamentos.stream()
                .filter(l -> l.getContaCredito().getId().equals(conta.getId()))
                .map(LancamentoContabil::getValor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal debitos = lancamentos.stream()
                .filter(l -> l.getContaDebito().getId().equals(conta.getId()))
                .map(LancamentoContabil::getValor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

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
