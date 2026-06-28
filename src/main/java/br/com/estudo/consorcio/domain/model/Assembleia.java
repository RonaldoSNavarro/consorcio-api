package br.com.estudo.consorcio.domain.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "assembleias")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Assembleia {

    @EqualsAndHashCode.Include
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Relacionamento: Muitas Assembleias pertencem a Um Grupo
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "grupo_id", nullable = false)
    private Grupo grupo;

    @Column(name = "data_assembleia", nullable = false)
    private LocalDate dataAssembleia;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoAssembleia tipo;

    @Column(name = "data_inicio_captacao")
    private LocalDateTime dataInicioCaptacao;

    @Column(name = "data_fim_captacao")
    private LocalDateTime dataFimCaptacao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusAssembleia status;

    @Column(name = "numero_sorteado")
    private Integer numeroSorteado;

    @Column(name = "premio_excluidos")
    private Integer premioExcluidos;

    @Column(name = "numero_extracao_loteria")
    private String numeroExtracaoLoteria;

    @Enumerated(EnumType.STRING)
    @Column(name = "algoritmo_usado")
    private AlgoritmoPedraChave algoritmoUsado;

    @Column(name = "pedra_chave_calculada")
    private Integer pedraChaveCalculada;

    @Column(name = "fallbacks_aplicados")
    private Integer fallbacksAplicados;

    @Version
    private Long version;

    @PrePersist
    protected void onCreate() {
        if (this.tipo == null) {
            this.tipo = TipoAssembleia.ORDINARIA;
        }
        if (this.status == null) {
            this.status = StatusAssembleia.AGENDADA;
        }
    }
}