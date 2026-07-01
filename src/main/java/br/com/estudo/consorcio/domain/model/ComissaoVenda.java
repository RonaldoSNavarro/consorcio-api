package br.com.estudo.consorcio.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "comissoes_venda")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComissaoVenda {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "corretor_id", nullable = false)
    private Corretor corretor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contrato_id", nullable = false)
    private ContratoAdesao contrato;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal valorTotalComissao;

    @Column(nullable = false, length = 20)
    private String status; // PENDENTE, DILUIDA, ESTORNADA

    @Builder.Default
    @Column(nullable = false, updatable = false)
    private LocalDateTime dataGeracao = LocalDateTime.now();
}
