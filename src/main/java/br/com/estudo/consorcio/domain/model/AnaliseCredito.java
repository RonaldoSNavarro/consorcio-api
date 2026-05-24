package br.com.estudo.consorcio.domain.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "analises_credito")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class AnaliseCredito {

    @EqualsAndHashCode.Include
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cota_id", nullable = false)
    private Cota cota;

    @Column(name = "renda_comprovada", nullable = false, precision = 38, scale = 2)
    private BigDecimal rendaComprovada;

    @Column(name = "garantia_aprovada", nullable = false)
    private Boolean garantiaAprovada = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusAnalise status;

    @Column(name = "data_analise", nullable = false)
    private LocalDate dataAnalise;

    @Column(columnDefinition = "TEXT")
    private String observacao;

    @Column(nullable = false)
    private Integer versao = 0;

    @PrePersist
    protected void onCreate() {
        if (this.dataAnalise == null) {
            this.dataAnalise = LocalDate.now();
        }
        if (this.status == null) {
            this.status = StatusAnalise.EM_ANALISE;
        }
        if (this.garantiaAprovada == null) {
            this.garantiaAprovada = false;
        }
        if (this.versao == null) {
            this.versao = 0;
        }
    }
}
