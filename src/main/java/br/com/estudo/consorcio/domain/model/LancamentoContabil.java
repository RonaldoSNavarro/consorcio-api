package br.com.estudo.consorcio.domain.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "lancamentos_contabeis")
public class LancamentoContabil {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "grupo_id", nullable = false)
    private Grupo grupo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cota_id")
    private Cota cota;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parcela_id")
    private Parcela parcela;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conta_debito_id", nullable = false)
    private ContaContabil contaDebito;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conta_credito_id", nullable = false)
    private ContaContabil contaCredito;

    @Column(nullable = false, precision = 38, scale = 2)
    private BigDecimal valor;

    @Column(name = "data_competencia", nullable = false)
    private LocalDate dataCompetencia;

    @Column(name = "data_lancamento", nullable = false, updatable = false)
    private LocalDateTime dataLancamento;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_operacao", nullable = false, length = 50)
    private TipoOperacaoContabil tipoOperacao;

    @Column(nullable = false, length = 500)
    private String historico;

    @PrePersist
    protected void onCreate() {
        if (this.dataLancamento == null) {
            this.dataLancamento = LocalDateTime.now();
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Grupo getGrupo() { return grupo; }
    public void setGrupo(Grupo grupo) { this.grupo = grupo; }

    public Cota getCota() { return cota; }
    public void setCota(Cota cota) { this.cota = cota; }

    public Parcela getParcela() { return parcela; }
    public void setParcela(Parcela parcela) { this.parcela = parcela; }

    public ContaContabil getContaDebito() { return contaDebito; }
    public void setContaDebito(ContaContabil contaDebito) { this.contaDebito = contaDebito; }

    public ContaContabil getContaCredito() { return contaCredito; }
    public void setContaCredito(ContaContabil contaCredito) { this.contaCredito = contaCredito; }

    public BigDecimal getValor() { return valor; }
    public void setValor(BigDecimal valor) { this.valor = valor; }

    public LocalDate getDataCompetencia() { return dataCompetencia; }
    public void setDataCompetencia(LocalDate dataCompetencia) { this.dataCompetencia = dataCompetencia; }

    public LocalDateTime getDataLancamento() { return dataLancamento; }

    public TipoOperacaoContabil getTipoOperacao() { return tipoOperacao; }
    public void setTipoOperacao(TipoOperacaoContabil tipoOperacao) { this.tipoOperacao = tipoOperacao; }

    public String getHistorico() { return historico; }
    public void setHistorico(String historico) { this.historico = historico; }
}
