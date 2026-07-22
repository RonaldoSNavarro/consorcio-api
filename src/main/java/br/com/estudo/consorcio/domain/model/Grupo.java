package br.com.estudo.consorcio.domain.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import br.com.estudo.consorcio.domain.enums.CategoriaBem;
import br.com.estudo.consorcio.domain.enums.DestinacaoMultaRescisoria;

@Entity
@Table(name = "grupos")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Grupo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "codigo_grupo", nullable = false, unique = true)
    private String codigoGrupo;

    @Column(name = "quantidade_cotas", nullable = false)
    private Integer quantidadeCotas = 120;

    @Column(name = "dia_base_assembleias", nullable = false)
    private Integer diaBaseAssembleias = 15;

    @Column(name = "dias_antecedencia_vencimento", nullable = false)
    private Integer diasAntecedenciaVencimento = 5;

    @Column(name = "prazo_maximo_meses", nullable = false)
    private Integer prazoMaximoMeses = 120;

    @ElementCollection
    @CollectionTable(name = "grupo_prazos_permitidos", joinColumns = @JoinColumn(name = "grupo_id"))
    @Column(name = "prazo_meses")
    private List<Integer> prazosPermitidos;

    @ManyToMany
    @JoinTable(
        name = "grupo_bens_permitidos",
        joinColumns = @JoinColumn(name = "grupo_id"),
        inverseJoinColumns = @JoinColumn(name = "bem_referencia_id")
    )
    private List<BemReferencia> bensPermitidos;

    @Column(nullable = false)
    private BigDecimal taxaAdministracao;

    @Column(name = "percentual_lance_embutido_maximo", nullable = false, precision = 5, scale = 4)
    private BigDecimal percentualLanceEmbutidoMaximo = new BigDecimal("0.3000");

    @Enumerated(EnumType.STRING)
    @Column(name = "indice_reajuste")
    private IndiceReajuste indiceReajuste;

    @Column(name = "mes_reajuste")
    private Integer mesReajuste;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusGrupo status;

    @Column(nullable = false)
    private LocalDate dataCriacao = LocalDate.now();

    @Column
    private LocalDate dataInauguracao; // Data da 1ª AGO (pode ser nula no início)

    @Column(name = "data_encerramento")
    private LocalDate dataEncerramento;

    @Enumerated(EnumType.STRING)
    @Column(name = "criterio_desempate_lance", nullable = false)
    private CriterioDesempateLance criterioDesempateLance = CriterioDesempateLance.LOTERIA_FEDERAL;

    @Column(name = "percentual_lance_fixo")
    private BigDecimal percentualLanceFixo = new BigDecimal("0.2000");

    @Enumerated(EnumType.STRING)
    @Column(name = "categoria_bem", nullable = false)
    private CategoriaBem categoriaBem = CategoriaBem.OUTROS_BENS_MOVEIS;

    @Enumerated(EnumType.STRING)
    @Column(name = "destinacao_multa_rescisoria", nullable = false)
    private DestinacaoMultaRescisoria destinacaoMultaRescisoria = DestinacaoMultaRescisoria.FUNDO_RESERVA;

    @Enumerated(EnumType.STRING)
    @Column(name = "algoritmo_pedra_chave", nullable = false)
    private AlgoritmoPedraChave algoritmoPedraChave = AlgoritmoPedraChave.CENTENA;

    @Enumerated(EnumType.STRING)
    @Column(name = "direcao_fallback_sorteio", nullable = false)
    private DirecaoFallbackSorteio direcaoFallbackSorteio = DirecaoFallbackSorteio.ACIMA_DEPOIS_ABAIXO;

    @Version
    @Column(name = "versao")
    private Long versao;

    // Getters e Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCodigoGrupo() { return codigoGrupo; }
    public void setCodigoGrupo(String codigoGrupo) { this.codigoGrupo = codigoGrupo; }

    @Deprecated
    public String getCodigo() { return codigoGrupo; }
    @Deprecated
    public void setCodigo(String codigo) { this.codigoGrupo = codigo; }

    public Integer getQuantidadeCotas() { return quantidadeCotas; }
    public void setQuantidadeCotas(Integer quantidadeCotas) { this.quantidadeCotas = quantidadeCotas; }

    public Integer getDiaBaseAssembleias() { return diaBaseAssembleias; }
    public void setDiaBaseAssembleias(Integer diaBaseAssembleias) { this.diaBaseAssembleias = diaBaseAssembleias; }

    public Integer getDiasAntecedenciaVencimento() { return diasAntecedenciaVencimento; }
    public void setDiasAntecedenciaVencimento(Integer diasAntecedenciaVencimento) { this.diasAntecedenciaVencimento = diasAntecedenciaVencimento; }

    public Integer getPrazoMaximoMeses() { return prazoMaximoMeses; }
    public void setPrazoMaximoMeses(Integer prazoMaximoMeses) { this.prazoMaximoMeses = prazoMaximoMeses; }

    @Transient
    public BigDecimal getValorCredito() {
        if (bensPermitidos != null && !bensPermitidos.isEmpty() && bensPermitidos.get(0).getValorAtual() != null) {
            return bensPermitidos.get(0).getValorAtual();
        }
        return BigDecimal.valueOf(100000); // fallback
    }

    @Transient
    public void setValorCredito(BigDecimal valorCredito) {
        // ignore
    }

    @Transient
    public Integer getPrazoMeses() {
        return prazoMaximoMeses;
    }

    @Transient
    public void setPrazoMeses(Integer prazoMeses) {
        this.prazoMaximoMeses = prazoMeses;
    }

    public List<Integer> getPrazosPermitidos() { return prazosPermitidos; }
    public void setPrazosPermitidos(List<Integer> prazosPermitidos) { this.prazosPermitidos = prazosPermitidos; }

    public List<BemReferencia> getBensPermitidos() { return bensPermitidos; }
    public void setBensPermitidos(List<BemReferencia> bensPermitidos) { this.bensPermitidos = bensPermitidos; }

    public BigDecimal getTaxaAdministracao() { return taxaAdministracao; }
    public void setTaxaAdministracao(BigDecimal taxaAdministracao) { this.taxaAdministracao = taxaAdministracao; }

    public BigDecimal getPercentualLanceEmbutidoMaximo() { return percentualLanceEmbutidoMaximo; }
    public void setPercentualLanceEmbutidoMaximo(BigDecimal percentualLanceEmbutidoMaximo) { this.percentualLanceEmbutidoMaximo = percentualLanceEmbutidoMaximo; }

    public StatusGrupo getStatus() { return status; }
    public void setStatus(StatusGrupo status) { this.status = status; }

    public LocalDate getDataCriacao() { return dataCriacao; }
    public void setDataCriacao(LocalDate dataCriacao) { this.dataCriacao = dataCriacao; }

    public LocalDate getDataInauguracao() { return dataInauguracao; }
    public void setDataInauguracao(LocalDate dataInauguracao) { this.dataInauguracao = dataInauguracao; }

    public LocalDate getDataEncerramento() { return dataEncerramento; }
    public void setDataEncerramento(LocalDate dataEncerramento) { this.dataEncerramento = dataEncerramento; }

    public Long getVersao() { return versao; }
    public void setVersao(Long versao) { this.versao = versao; }

    @Deprecated
    public Long getVersion() { return versao; }
    @Deprecated
    public void setVersion(Long version) { this.versao = version; }

    public CriterioDesempateLance getCriterioDesempateLance() { return criterioDesempateLance; }
    public void setCriterioDesempateLance(CriterioDesempateLance criterioDesempateLance) { this.criterioDesempateLance = criterioDesempateLance; }

    public BigDecimal getPercentualLanceFixo() { return percentualLanceFixo; }
    public void setPercentualLanceFixo(BigDecimal percentualLanceFixo) { this.percentualLanceFixo = percentualLanceFixo; }

    public CategoriaBem getCategoriaBem() { return categoriaBem; }
    public void setCategoriaBem(CategoriaBem categoriaBem) { this.categoriaBem = categoriaBem; }

    public DestinacaoMultaRescisoria getDestinacaoMultaRescisoria() { return destinacaoMultaRescisoria; }
    public void setDestinacaoMultaRescisoria(DestinacaoMultaRescisoria destinacaoMultaRescisoria) { this.destinacaoMultaRescisoria = destinacaoMultaRescisoria; }

    public AlgoritmoPedraChave getAlgoritmoPedraChave() { return algoritmoPedraChave; }
    public void setAlgoritmoPedraChave(AlgoritmoPedraChave algoritmoPedraChave) { this.algoritmoPedraChave = algoritmoPedraChave; }

    public DirecaoFallbackSorteio getDirecaoFallbackSorteio() { return direcaoFallbackSorteio; }
    public void setDirecaoFallbackSorteio(DirecaoFallbackSorteio direcaoFallbackSorteio) { this.direcaoFallbackSorteio = direcaoFallbackSorteio; }
}