package br.com.estudo.consorcio.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "lances")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Lance {

    @EqualsAndHashCode.Include
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cota_id", nullable = false)
    private Cota cota;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assembleia_id", nullable = false)
    private Assembleia assembleia;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoLance tipo;

    @Column(name = "valor_oferta", nullable = false)
    private BigDecimal valorOferta;

    @Column(name = "data_oferta", nullable = false)
    private LocalDateTime dataOferta;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_apuracao", nullable = false)
    private StatusApuracaoLance statusApuracao;

    @Version
    private Long version;

    @PrePersist
    protected void onCreate() {
        if (this.dataOferta == null) {
            this.dataOferta = LocalDateTime.now();
        }
        if (this.statusApuracao == null) {
            this.statusApuracao = StatusApuracaoLance.CADASTRADO;
        }
    }
}
