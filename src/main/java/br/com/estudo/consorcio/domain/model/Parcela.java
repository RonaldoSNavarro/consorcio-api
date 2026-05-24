package br.com.estudo.consorcio.domain.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "parcelas")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Data // Cria todos os Getters e Setters
@NoArgsConstructor // Cria construtor vazio exigido pelo JPA
@AllArgsConstructor // Cria construtor com todos os argumentos
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Parcela {

    @EqualsAndHashCode.Include
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cota_id", nullable = false)
    private Cota cota;

    @Column(nullable = false)
    private Integer numeroParcela;

    // --- FRACIONAMENTO DA PARCELA (REGRA BCB) --- //

    @Column(nullable = false)
    private BigDecimal valorFundoComum; // Dinheiro que vai para o fundo de contemplações

    @Column(nullable = false)
    private BigDecimal valorTaxaAdministracao; // Remuneração da administradora

    @Column(nullable = false)
    private BigDecimal valorFundoReserva; // Fundo de emergência do grupo

    @Column(name = "valor_seguro", nullable = false)
    private BigDecimal valorSeguro = BigDecimal.ZERO; // Seguro da cota/grupo

    @Column(nullable = false)
    private BigDecimal valorParcela; // Valor total do boleto (Soma dos quatro acima)

    // --- COLUNAS DE INADIMPLÊNCIA --- //
    @Column(precision = 15, scale = 2)
    private BigDecimal valorMulta = BigDecimal.ZERO;

    @Column(precision = 15, scale = 2)
    private BigDecimal valorJuros = BigDecimal.ZERO;

    @Column(name = "valor_pago", precision = 15, scale = 2)
    private BigDecimal valorPago;
    // -------------------------------------- //

    @Column(nullable = false)
    private LocalDate dataVencimento;

    @Column
    private LocalDate dataPagamento;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusParcela status;

    @Version
    private Long version;

    // Método do JPA: Antes de salvar ou atualizar no banco, ele soma tudo sozinho!
    @PrePersist
    @PreUpdate
    public void calcularValorTotal() {
        if (this.valorFundoComum != null && this.valorTaxaAdministracao != null 
                && this.valorFundoReserva != null && this.valorSeguro != null) {
            this.valorParcela = this.valorFundoComum
                    .add(this.valorTaxaAdministracao)
                    .add(this.valorFundoReserva)
                    .add(this.valorSeguro);
        }
    }
}