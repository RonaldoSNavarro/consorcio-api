package br.com.estudo.consorcio.domain.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "contemplacoes")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Contemplacao {

    @EqualsAndHashCode.Include
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Relacionamento: Uma Cota pode ter apenas UMA Contemplação (OneToOne seria possível, mas ManyToOne é mais seguro para o lado "fraco" da relação em sistemas legados, mas vamos usar OneToOne aqui pois faz mais sentido)
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cota_id", nullable = false, unique = true)
    private Cota cota;

    // Relacionamento: Muitas Contemplações podem acontecer em Uma Assembleia
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assembleia_id", nullable = false)
    private Assembleia assembleia;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_contemplacao", nullable = false)
    private TipoContemplacao tipoContemplacao;

    // Só é preenchido se o tipo for LANCE_LIVRE ou LANCE_FIXO
    @Column(name = "valor_lance", precision = 15, scale = 2)
    private BigDecimal valorLance;

    @Column(name = "data_contemplacao", nullable = false)
    private LocalDate dataContemplacao;

    @PrePersist
    protected void onCreate() {
        if (this.dataContemplacao == null) {
            this.dataContemplacao = LocalDate.now();
        }
    }
}