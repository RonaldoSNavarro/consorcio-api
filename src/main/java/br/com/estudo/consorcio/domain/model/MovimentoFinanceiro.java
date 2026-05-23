package br.com.estudo.consorcio.domain.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "movimentos_financeiros")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class MovimentoFinanceiro {

    @EqualsAndHashCode.Include
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "grupo_id", nullable = false)
    private Grupo grupo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cota_id")
    private Cota cota;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parcela_id")
    private Parcela parcela;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contemplacao_id")
    private Contemplacao contemplacao;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_movimento", nullable = false, length = 50)
    private TipoMovimentoFinanceiro tipoMovimento;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private NaturezaMovimento natureza;

    @Column(nullable = false, precision = 38, scale = 2)
    private BigDecimal valor;

    @Column(name = "saldo_anterior", precision = 38, scale = 2)
    private BigDecimal saldoAnterior;

    @Column(name = "saldo_posterior", precision = 38, scale = 2)
    private BigDecimal saldoPosterior;

    @Column(length = 500)
    private String descricao;

    @Column(name = "data_movimento", nullable = false)
    private LocalDateTime dataMovimento;

    @Column(name = "data_referencia")
    private LocalDate dataReferencia;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @PrePersist
    protected void onCreate() {
        if (this.dataMovimento == null) {
            this.dataMovimento = LocalDateTime.now();
        }
        if (this.dataReferencia == null) {
            this.dataReferencia = LocalDate.now();
        }
    }
}
