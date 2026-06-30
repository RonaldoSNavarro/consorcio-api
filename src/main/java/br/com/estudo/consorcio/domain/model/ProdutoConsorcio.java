package br.com.estudo.consorcio.domain.model;

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

@Entity
@Table(name = "produtos_consorcio")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProdutoConsorcio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nome;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "bem_referencia_id")
    private BemReferencia bemReferencia;

    private Integer prazoMeses;

    private BigDecimal taxaAdministracaoPerc;

    private BigDecimal fundoReservaPerc;

    @Builder.Default
    private Boolean ativo = true;
}
