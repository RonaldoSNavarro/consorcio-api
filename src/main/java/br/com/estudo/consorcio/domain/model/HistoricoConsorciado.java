package br.com.estudo.consorcio.domain.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "historico_consorciado")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class HistoricoConsorciado {

    @EqualsAndHashCode.Include
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cota_id")
    private Cota cota;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "grupo_id")
    private Grupo grupo;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_interacao", nullable = false, length = 50)
    private TipoInteracao tipoInteracao;

    @Column(length = 1000)
    private String descricao;

    // --- Snapshot financeiro ---
    @Column(name = "valor_credito", precision = 38, scale = 2)
    private BigDecimal valorCredito;

    @Column(name = "valor_fundo_comum", precision = 38, scale = 2)
    private BigDecimal valorFundoComum;

    @Column(name = "valor_fundo_reserva", precision = 38, scale = 2)
    private BigDecimal valorFundoReserva;

    @Column(name = "valor_seguro", precision = 38, scale = 2)
    private BigDecimal valorSeguro;

    @Column(name = "valor_categoria", precision = 38, scale = 2)
    private BigDecimal valorCategoria;

    // --- Dados do bem ---
    @Column(name = "descricao_bem", length = 500)
    private String descricaoBem;

    @Column(name = "valor_bem", precision = 38, scale = 2)
    private BigDecimal valorBem;

    // --- Dados de parcela ---
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parcela_id")
    private Parcela parcela;

    @Column(name = "numero_parcela")
    private Integer numeroParcela;

    @Column(name = "valor_parcela", precision = 38, scale = 2)
    private BigDecimal valorParcela;

    @Column(name = "data_interacao", nullable = false)
    private LocalDateTime dataInteracao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @PrePersist
    protected void onCreate() {
        if (this.dataInteracao == null) {
            this.dataInteracao = LocalDateTime.now();
        }
    }
}
