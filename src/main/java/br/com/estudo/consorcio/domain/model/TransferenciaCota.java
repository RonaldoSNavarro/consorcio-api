package br.com.estudo.consorcio.domain.model;

import br.com.estudo.consorcio.domain.enums.StatusTransferencia;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transferencia_cotas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransferenciaCota {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cota_id", nullable = false)
    private Cota cota;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cedente_id", nullable = false)
    private Cliente cedente;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cessionario_id", nullable = false)
    private Cliente cessionario;

    @Column(name = "data_solicitacao", nullable = false)
    @Builder.Default
    private LocalDateTime dataSolicitacao = LocalDateTime.now();

    @Column(name = "data_efetivacao")
    private LocalDateTime dataEfetivacao;

    @Column(name = "taxa_transferencia", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal taxaTransferencia = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @Builder.Default
    private StatusTransferencia status = StatusTransferencia.SOLICITADA;

    @Column(name = "motivo_recusa", columnDefinition = "TEXT")
    private String motivoRecusa;

    @Column(columnDefinition = "TEXT")
    private String observacao;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();
}