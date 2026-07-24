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
import java.time.LocalDate;

@Entity
@Table(name = "bens_referencia")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BemReferencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "categoria_bem_id")
    private CategoriaBem categoriaBem;

    private String descricao;
    
    private BigDecimal valorAtual;
    
    private LocalDate dataUltimaAtualizacao;
    
    private String codigoFipe;

    @Builder.Default
    private Boolean ativo = true;
}
