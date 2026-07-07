package br.com.estudo.consorcio.domain.model;

import br.com.estudo.consorcio.domain.enums.TipoVendaEnum;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Configura os parâmetros de um canal de venda de proposta de adesão.
 * Um TipoVenda define o canal, percentual de comissão, se exige seguro,
 * e se o crédito do grupo é reajustável por índice.
 */
@Entity
@Table(name = "tipos_venda")
public class TipoVenda {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String nome;

    @Column(length = 500)
    private String descricao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoVendaEnum canal;

    /** Percentual de comissão sobre o valor do crédito (0.00 a 1.00). */
    @Column(nullable = false, precision = 5, scale = 4)
    private BigDecimal percentualComissao;

    /** Se true, a proposta exige contratação de seguro de vida. */
    @Column(nullable = false)
    private Boolean exigeSeguro = false;

    /** Se true, o crédito pode ser reajustado por índice (INCC, IPCA). */
    @Column(nullable = false)
    private Boolean permiteReajuste = true;

    @Column(nullable = false)
    private Boolean ativo = true;

    @Column(nullable = false)
    private Boolean parcelaUmZeroFundoComum = false;

    @Column(nullable = false)
    private Boolean liberacaoComissaoImediata = false;

    @Column(nullable = false)
    private Integer mesesGarantiaComissao = 0;

    @Column(nullable = false, precision = 5, scale = 4)
    private BigDecimal percentualEstorno = BigDecimal.ZERO;

    @Column(nullable = false, updatable = false)
    private LocalDateTime dataCriacao = LocalDateTime.now();

    // Getters e Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    public TipoVendaEnum getCanal() { return canal; }
    public void setCanal(TipoVendaEnum canal) { this.canal = canal; }
    public BigDecimal getPercentualComissao() { return percentualComissao; }
    public void setPercentualComissao(BigDecimal percentualComissao) { this.percentualComissao = percentualComissao; }
    public Boolean getExigeSeguro() { return exigeSeguro; }
    public void setExigeSeguro(Boolean exigeSeguro) { this.exigeSeguro = exigeSeguro; }
    public Boolean getPermiteReajuste() { return permiteReajuste; }
    public void setPermiteReajuste(Boolean permiteReajuste) { this.permiteReajuste = permiteReajuste; }
    public Boolean getAtivo() { return ativo; }
    public void setAtivo(Boolean ativo) { this.ativo = ativo; }
    public Boolean getParcelaUmZeroFundoComum() { return parcelaUmZeroFundoComum; }
    public void setParcelaUmZeroFundoComum(Boolean parcelaUmZeroFundoComum) { this.parcelaUmZeroFundoComum = parcelaUmZeroFundoComum; }
    public Boolean getLiberacaoComissaoImediata() { return liberacaoComissaoImediata; }
    public void setLiberacaoComissaoImediata(Boolean liberacaoComissaoImediata) { this.liberacaoComissaoImediata = liberacaoComissaoImediata; }
    public Integer getMesesGarantiaComissao() { return mesesGarantiaComissao; }
    public void setMesesGarantiaComissao(Integer mesesGarantiaComissao) { this.mesesGarantiaComissao = mesesGarantiaComissao; }
    public BigDecimal getPercentualEstorno() { return percentualEstorno; }
    public void setPercentualEstorno(BigDecimal percentualEstorno) { this.percentualEstorno = percentualEstorno; }
    public LocalDateTime getDataCriacao() { return dataCriacao; }
    public void setDataCriacao(LocalDateTime dataCriacao) { this.dataCriacao = dataCriacao; }
}
