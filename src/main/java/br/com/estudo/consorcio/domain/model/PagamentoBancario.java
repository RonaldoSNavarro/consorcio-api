package br.com.estudo.consorcio.domain.model;

import br.com.estudo.consorcio.domain.enums.StatusPagamentoBancario;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "pagamentos_bancarios")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PagamentoBancario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cota_id", nullable = false)
    private Cota cota;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parcela_id")
    private Parcela parcela;

    @Column(name = "codigo_barras", length = 100)
    private String codigoBarras;

    @Column(name = "linha_digitavel", length = 100)
    private String linhaDigitavel;

    @Column(unique = true, length = 100)
    private String txid;

    @Column(name = "pix_copia_cola", columnDefinition = "TEXT")
    private String pixCopiaCola;

    @Column(name = "qr_code_base64", columnDefinition = "TEXT")
    private String qrCodeBase64;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private StatusPagamentoBancario status = StatusPagamentoBancario.AGUARDANDO_PAGAMENTO;

    @Column(name = "valor_cobrado", nullable = false, precision = 15, scale = 2)
    private BigDecimal valorCobrado;

    @Column(name = "valor_pago", precision = 15, scale = 2)
    private BigDecimal valorPago;

    @Column(name = "taxa_bancaria", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal taxaBancaria = BigDecimal.ZERO;

    @Column(name = "data_vencimento", nullable = false)
    private LocalDate dataVencimento;

    @Column(name = "data_pagamento")
    private LocalDateTime dataPagamento;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();
}