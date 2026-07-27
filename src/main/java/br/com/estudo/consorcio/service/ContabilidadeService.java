package br.com.estudo.consorcio.service;

import br.com.estudo.consorcio.domain.model.*;
import br.com.estudo.consorcio.domain.repository.ContaContabilRepository;
import br.com.estudo.consorcio.domain.repository.LancamentoContabilRepository;
import br.com.estudo.consorcio.exception.RegraDeNegocioException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
public class ContabilidadeService {

    private final LancamentoContabilRepository lancamentoRepository;
    private final ContaContabilRepository contaRepository;

    // --- Contas COSIF do Ativo ---
    public static final String CONTA_CAIXA = "1.1.1.10.00-2";
    public static final String CONTA_DIREITOS_RECEBER = "1.2.1.10.00-8";

    // --- Contas COSIF do Passivo ---
    public static final String CONTA_FUNDO_COMUM = "2.1.2.10.10-6";
    public static final String CONTA_FUNDO_RESERVA = "2.1.2.10.20-9";
    public static final String CONTA_TAXA_ADM = "2.1.2.10.30-2";
    public static final String CONTA_SEGURO = "2.1.2.10.40-5";
    public static final String CONTA_RENDIMENTO = "2.1.2.10.50-8";
    public static final String CONTA_EXCLUIDOS_DEVOLVER = "2.1.2.20.10-3";
    public static final String CONTA_CREDITOS_LIBERAR = "2.1.2.30.10-0";
    public static final String CONTA_RECURSOS_NAO_PROCURADOS = "2.1.2.90.10-8";

    // --- Contas COSIF de Provisão (Sprint 4 — ADR 006) ---
    public static final String CONTA_PDD = "1.6.9.10.00-5";
    public static final String CONTA_DESPESA_PDD = "3.1.8.10.00-1";

    private static final Map<String, ContaOperacional> CONTAS_OPERACIONAIS = Map.ofEntries(
            Map.entry(CONTA_CAIXA, new ContaOperacional("Bancos - Recursos de Grupos (Disponibilidades)", TipoContaContabil.ATIVO, NaturezaContabil.DEVEDORA)),
            Map.entry(CONTA_DIREITOS_RECEBER, new ContaOperacional("Valores a Receber de Consorciados", TipoContaContabil.ATIVO, NaturezaContabil.DEVEDORA)),
            Map.entry(CONTA_FUNDO_COMUM, new ContaOperacional("Fundo Comum de Grupos", TipoContaContabil.PASSIVO, NaturezaContabil.CREDORA)),
            Map.entry(CONTA_FUNDO_RESERVA, new ContaOperacional("Fundo de Reserva de Grupos", TipoContaContabil.PASSIVO, NaturezaContabil.CREDORA)),
            Map.entry(CONTA_TAXA_ADM, new ContaOperacional("Taxa de Administracao a Repassar", TipoContaContabil.PASSIVO, NaturezaContabil.CREDORA)),
            Map.entry(CONTA_SEGURO, new ContaOperacional("Seguros a Repassar", TipoContaContabil.PASSIVO, NaturezaContabil.CREDORA)),
            Map.entry(CONTA_RENDIMENTO, new ContaOperacional("Rendimentos de Aplicacoes Financeiras", TipoContaContabil.PASSIVO, NaturezaContabil.CREDORA)),
            Map.entry(CONTA_EXCLUIDOS_DEVOLVER, new ContaOperacional("Recursos de Consorciados Excluidos a Devolver", TipoContaContabil.PASSIVO, NaturezaContabil.CREDORA)),
            Map.entry(CONTA_CREDITOS_LIBERAR, new ContaOperacional("Creditos a Liberar - Bens e Servicos", TipoContaContabil.PASSIVO, NaturezaContabil.CREDORA)),
            Map.entry(CONTA_RECURSOS_NAO_PROCURADOS, new ContaOperacional("Recursos Nao Procurados (RNP)", TipoContaContabil.PASSIVO, NaturezaContabil.CREDORA)),
            Map.entry(CONTA_PDD, new ContaOperacional("Provisao para Creditos de Liquidacao Duvidosa (PDD)", TipoContaContabil.ATIVO, NaturezaContabil.CREDORA)),
            Map.entry(CONTA_DESPESA_PDD, new ContaOperacional("Despesas de Provisao para Devedores Duvidosos", TipoContaContabil.DESPESA, NaturezaContabil.DEVEDORA))
    );

    public ContabilidadeService(LancamentoContabilRepository lancamentoRepository, ContaContabilRepository contaRepository) {
        this.lancamentoRepository = lancamentoRepository;
        this.contaRepository = contaRepository;
    }

    /**
     * Garante a existência do plano COSIF simplificado usado pelo domínio de consórcios.
     * A operação é idempotente e atende à rastreabilidade contábil definida na ADR 002.
     *
     * @return quantidade de contas criadas nesta execução
     */
    @Transactional
    public int provisionarPlanoContasOperacional() {
        int contasCriadas = 0;
        for (String codigoCosif : CONTAS_OPERACIONAIS.keySet()) {
            if (contaRepository.findByCodigoCosif(codigoCosif).isEmpty()) {
                criarContaOperacional(codigoCosif);
                contasCriadas++;
            }
        }
        return contasCriadas;
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

    @Transactional(readOnly = true)
    public List<ContaContabil> listarTodasContas() {
        return contaRepository.findAll();
    }

    private ContaContabil getConta(String codigoCosif) {
        return contaRepository.findByCodigoCosif(codigoCosif)
                .orElseGet(() -> criarContaOperacional(codigoCosif));
    }

    private ContaContabil criarContaOperacional(String codigoCosif) {
        ContaOperacional definicao = CONTAS_OPERACIONAIS.get(codigoCosif);
        if (definicao == null) {
            throw new RegraDeNegocioException("Conta contábil não configurada no plano de contas: " + codigoCosif);
        }

        ContaContabil conta = new ContaContabil();
        conta.setCodigoCosif(codigoCosif);
        conta.setNome(definicao.nome());
        conta.setTipo(definicao.tipo());
        conta.setNatureza(definicao.natureza());
        conta.setAtiva(true);
        return contaRepository.save(conta);
    }

    private record ContaOperacional(String nome, TipoContaContabil tipo, NaturezaContabil natureza) { }

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

    @Transactional
    public void registrarEncerramento(Grupo grupo, Cota cota, Parcela parcela, String contaDebitoCosif, String contaCreditoCosif, BigDecimal valor, LocalDate dataCompetencia, String historico) {
        if (valor == null || valor.compareTo(BigDecimal.ZERO) <= 0) return;
        registrarLancamento(grupo, cota, parcela, contaDebitoCosif, contaCreditoCosif, valor, dataCompetencia, TipoOperacaoContabil.ENCERRAMENTO, historico);
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
