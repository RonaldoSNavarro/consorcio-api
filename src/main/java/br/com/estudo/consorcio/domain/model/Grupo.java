package br.com.estudo.consorcio.domain.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "grupos")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Grupo {

    @EqualsAndHashCode.Include
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String codigo;

    @Column(name = "valor_credito", nullable = false, precision = 15, scale = 2)
    private BigDecimal valorCredito;

    @Column(name = "prazo_meses", nullable = false)
    private Integer prazoMeses;

    @Column(name = "taxa_administracao", nullable = false, precision = 5, scale = 2)
    private BigDecimal taxaAdministracao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusGrupo status;

    @Column(name = "data_criacao", updatable = false)
    private LocalDate dataCriacao;

    @PrePersist
    protected void onCreate() {
        this.dataCriacao = LocalDate.now();
        if (this.status == null) {
            this.status = StatusGrupo.ABERTO;
        }
    }
}