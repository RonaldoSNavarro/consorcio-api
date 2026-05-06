package br.com.estudo.consorcio.domain.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "parcelas")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Parcela {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cota_id", nullable = false)
    private Cota cota;

    @Column(nullable = false)
    private Integer numeroParcela;

    // --- FRACIONAMENTO DA PARCELA (REGRA BCB) --- //

    @Column(nullable = false)
    private BigDecimal valorFundoComum; // Dinheiro que vai para o fundo de contemplações

    @Column(nullable = false)
    private BigDecimal valorTaxaAdministracao; // Remuneração da administradora

    @Column(nullable = false)
    private BigDecimal valorFundoReserva; // Fundo de emergência do grupo

    @Column(nullable = false)
    private BigDecimal valorParcela; // Valor total do boleto (Soma dos três acima)

    // -------------------------------------------- //

    @Column(nullable = false)
    private LocalDate dataVencimento;

    @Column
    private LocalDate dataPagamento;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusParcela status;

    // Método mágico do JPA: Antes de salvar ou atualizar no banco, ele soma tudo sozinho!
    @PrePersist
    @PreUpdate
    public void calcularValorTotal() {
        if (this.valorFundoComum != null && this.valorTaxaAdministracao != null && this.valorFundoReserva != null) {
            this.valorParcela = this.valorFundoComum
                    .add(this.valorTaxaAdministracao)
                    .add(this.valorFundoReserva);
        }
    }

    // Getters e Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Cota getCota() { return cota; }
    public void setCota(Cota cota) { this.cota = cota; }

    public Integer getNumeroParcela() { return numeroParcela; }
    public void setNumeroParcela(Integer numeroParcela) { this.numeroParcela = numeroParcela; }

    public BigDecimal getValorFundoComum() { return valorFundoComum; }
    public void setValorFundoComum(BigDecimal valorFundoComum) { this.valorFundoComum = valorFundoComum; }

    public BigDecimal getValorTaxaAdministracao() { return valorTaxaAdministracao; }
    public void setValorTaxaAdministracao(BigDecimal valorTaxaAdministracao) { this.valorTaxaAdministracao = valorTaxaAdministracao; }

    public BigDecimal getValorFundoReserva() { return valorFundoReserva; }
    public void setValorFundoReserva(BigDecimal valorFundoReserva) { this.valorFundoReserva = valorFundoReserva; }

    public BigDecimal getValorParcela() { return valorParcela; }
    public void setValorParcela(BigDecimal valorParcela) { this.valorParcela = valorParcela; }

    public LocalDate getDataVencimento() { return dataVencimento; }
    public void setDataVencimento(LocalDate dataVencimento) { this.dataVencimento = dataVencimento; }

    public LocalDate getDataPagamento() { return dataPagamento; }
    public void setDataPagamento(LocalDate dataPagamento) { this.dataPagamento = dataPagamento; }

    public StatusParcela getStatus() { return status; }
    public void setStatus(StatusParcela status) { this.status = status; }
}