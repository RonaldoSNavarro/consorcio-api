package br.com.estudo.consorcio.domain.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "grupos")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Grupo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String codigo;

    @Column(nullable = false)
    private BigDecimal valorCredito;

    @Column(nullable = false)
    private Integer prazoMeses;

    @Column(nullable = false)
    private BigDecimal taxaAdministracao;

    @Column(name = "percentual_lance_embutido_maximo", nullable = false)
    private BigDecimal percentualLanceEmbutidoMaximo = new BigDecimal("0.3000");

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusGrupo status;

    @Column(nullable = false)
    private LocalDate dataCriacao = LocalDate.now();

    @Column
    private LocalDate dataInauguracao; // Data da 1ª AGO (pode ser nula no início)

    // Getters e Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }

    public BigDecimal getValorCredito() { return valorCredito; }
    public void setValorCredito(BigDecimal valorCredito) { this.valorCredito = valorCredito; }

    public Integer getPrazoMeses() { return prazoMeses; }
    public void setPrazoMeses(Integer prazoMeses) { this.prazoMeses = prazoMeses; }

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
}