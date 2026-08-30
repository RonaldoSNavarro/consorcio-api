package br.com.estudo.consorcio.domain.model;

import br.com.estudo.consorcio.domain.enums.StatusCredenciamento;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "credenciamentos_lances")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CredenciamentoLance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cota_id", nullable = false)
    private Cota cota;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "assembleia_id", nullable = false)
    private Assembleia assembleia;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_lance", nullable = false, length = 30)
    private TipoLance tipoLance;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ModalidadeLance modalidade;

    @Column(name = "valor_lance", nullable = false, precision = 15, scale = 2)
    private BigDecimal valorLance;

    @Column(name = "percentual_lance", precision = 8, scale = 4)
    private BigDecimal percentualLance;

    @Column(name = "valor_fgts", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal valorFgts = BigDecimal.ZERO;

    @Column(name = "valor_embutido", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal valorEmbutido = BigDecimal.ZERO;

    @Column(name = "valor_proprio", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal valorProprio = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private StatusCredenciamento status = StatusCredenciamento.ATIVO;

    @Column(name = "hash_assinatura", length = 256)
    private String hashAssinatura;

    @Column(name = "ip_origem", length = 50)
    private String ipOrigem;

    @Column(name = "termos_aceitos", nullable = false)
    @Builder.Default
    private Boolean termosAceitos = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();
}