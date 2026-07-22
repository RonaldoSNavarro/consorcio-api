package br.com.estudo.consorcio.domain.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "historico_versoes_cota")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class HistoricoVersaoCota {

    @EqualsAndHashCode.Include
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cota_id", nullable = false)
    private Cota cota;

    @Column(name = "versao_historico", nullable = false)
    private Integer versaoHistorico;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_anterior", length = 50)
    private StatusCota statusAnterior;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_novo", nullable = false, length = 50)
    private StatusCota statusNovo;

    @Column(length = 500)
    private String motivo;

    @Column(name = "data_transicao", nullable = false)
    private LocalDateTime dataTransicao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @PrePersist
    protected void onCreate() {
        if (this.dataTransicao == null) {
            this.dataTransicao = LocalDateTime.now();
        }
    }
}
