package br.com.estudo.consorcio.domain.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "cotas")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Cota {

    @EqualsAndHashCode.Include
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "numero_cota", nullable = false)
    private Integer numeroCota;

    // Relacionamento: Muitas Cotas podem pertencer a Um Cliente
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    // Relacionamento: Muitas Cotas podem pertencer a Um Grupo
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "grupo_id", nullable = false)
    private Grupo grupo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusCota status;

    @Column(name = "valor_reembolsado", precision = 15, scale = 2)
    private BigDecimal valorReembolsado = BigDecimal.ZERO;

    @Column(nullable = false)
    private Boolean reembolsada = false;

    @Column(nullable = false)
    private Integer versao = 0;

    @Version
    private Long version;

    @PrePersist
    protected void onCreate() {
        if (this.status == null) {
            this.status = StatusCota.ATIVA;
        }
        if (this.valorReembolsado == null) {
            this.valorReembolsado = BigDecimal.ZERO;
        }
        if (this.reembolsada == null) {
            this.reembolsada = false;
        }
        if (this.versao == null) {
            this.versao = 0;
        }
    }
}