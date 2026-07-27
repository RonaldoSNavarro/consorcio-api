package br.com.estudo.consorcio.domain.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "cotas")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Cota {

    @EqualsAndHashCode.Include
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "codigo_cota", nullable = false)
    private Integer codigoCota;

    // Relacionamento: Muitas Cotas podem pertencer a Um Cliente
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id")
    private Cliente cliente;

    // Relacionamento: Muitas Cotas podem pertencer a Um Grupo
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "grupo_id", nullable = false)
    private Grupo grupo;

    /**
     * Operational fields synchronized from the foreign-key relationships.
     * The relationship fields remain the referential source of truth.
     */
    @Column(name = "codigo_grupo", length = 255)
    private String codigoGrupo;

    @Column(name = "nome_cliente", length = 255)
    private String nomeCliente;

    @Column(name = "cpf_cliente", length = 20)
    private String cpfCliente;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contrato_adesao_id", unique = true)
    private ContratoAdesao contratoAdesao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bem_referencia_id")
    private BemReferencia bemReferencia;

    @Column(name = "prazo_meses")
    private Integer prazoMeses;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusCota status;

    @Column(name = "valor_reembolsado", precision = 15, scale = 2)
    private BigDecimal valorReembolsado = BigDecimal.ZERO;

    @Column(nullable = false)
    private Boolean reembolsada = false;

    @Column(name = "versao_historico", nullable = false)
    private Integer versaoHistorico = 0;

    @Version
    @Column(name = "versao")
    private Long versao;

    @PrePersist
    protected void onCreate() {
        if (this.status == null) {
            this.status = StatusCota.ATIVA;
        }
        if (this.valorReembolsado == null) {
            this.valorReembolsado = BigDecimal.ZERO;
        }
        if (this.reembolsada == null) {
            this.reembolsada = false;
        }
        if (this.versaoHistorico == null) {
            this.versaoHistorico = 0;
        }
    }
}
