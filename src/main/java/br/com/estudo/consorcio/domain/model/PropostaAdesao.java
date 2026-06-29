package br.com.estudo.consorcio.domain.model;

import br.com.estudo.consorcio.domain.enums.StatusProposta;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "propostas_adesao")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PropostaAdesao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String numeroProposta;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id")
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "produto_id")
    private ProdutoConsorcio produto;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tipo_venda_id")
    private TipoVenda tipoVenda;

    private BigDecimal valorCreditoSolicitado;

    @Enumerated(EnumType.STRING)
    private StatusProposta status;

    private LocalDateTime dataProposta;

    private LocalDateTime dataAtualizacao;
}
