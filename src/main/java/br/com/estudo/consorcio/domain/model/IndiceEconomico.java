package br.com.estudo.consorcio.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "indices_economicos_historico", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"tipo_indice", "data_referencia"})
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IndiceEconomico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_indice", nullable = false, length = 20)
    private IndiceReajuste tipoIndice;

    @Column(name = "data_referencia", nullable = false)
    private LocalDate dataReferencia;

    @Column(name = "valor_percentual", nullable = false, precision = 10, scale = 4)
    private BigDecimal valorPercentual;

    @Column(name = "data_captura", nullable = false)
    private LocalDateTime dataCaptura;
}
