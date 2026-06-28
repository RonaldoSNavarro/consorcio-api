package br.com.estudo.consorcio.domain.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "rendimentos_financeiros")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RendimentoFinanceiro {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "grupo_id", nullable = false)
    private Grupo grupo;

    @Column(name = "valor_rendimento", nullable = false)
    private BigDecimal valorRendimento;

    @Column(name = "data_rendimento", nullable = false)
    private LocalDate dataRendimento;

    @Column(length = 255)
    private String descricao;
}
