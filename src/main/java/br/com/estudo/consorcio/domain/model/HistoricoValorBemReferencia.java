package br.com.estudo.consorcio.domain.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "historico_valores_bem_referencia")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HistoricoValorBemReferencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bem_referencia_id", nullable = false)
    private BemReferencia bemReferencia;

    @Column(name = "valor_anterior", nullable = false)
    private BigDecimal valorAnterior;

    @Column(name = "valor_novo", nullable = false)
    private BigDecimal valorNovo;

    @Column(name = "origem_reajuste", nullable = false)
    private String origemReajuste;

    @Column(name = "codigo_fipe")
    private String codigoFipe;

    @Column(name = "data_atualizacao", nullable = false)
    @Builder.Default
    private LocalDateTime dataAtualizacao = LocalDateTime.now();
}
