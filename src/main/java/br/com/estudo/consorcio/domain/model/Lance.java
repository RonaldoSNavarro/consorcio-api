package br.com.estudo.consorcio.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "lances")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Lance {

    @EqualsAndHashCode.Include
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cota_id", nullable = false)
    private Cota cota;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assembleia_id", nullable = false)
    private Assembleia assembleia;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoLance tipo;

    @Column(name = "valor_oferta", nullable = false)
    private BigDecimal valorOferta;

    @Column(name = "data_oferta", nullable = false)
    private LocalDateTime dataOferta;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_apuracao", nullable = false)
    private StatusApuracaoLance statusApuracao;

    @Enumerated(EnumType.STRING)
    @Column(name = "modalidade", nullable = false)
    private ModalidadeLance modalidade;

    @Column(name = "notificar_siscoaf", nullable = false)
    private boolean notificarSiscoaf = false;

    @Version
    private Long version;

    public Lance(Long id, Cota cota, Assembleia assembleia, TipoLance tipo, BigDecimal valorOferta, LocalDateTime dataOferta, StatusApuracaoLance statusApuracao, ModalidadeLance modalidade, Long version) {
        this.id = id;
        this.cota = cota;
        this.assembleia = assembleia;
        this.tipo = tipo;
        this.valorOferta = valorOferta;
        this.dataOferta = dataOferta;
        this.statusApuracao = statusApuracao;
        this.modalidade = modalidade;
        this.version = version;
        this.notificarSiscoaf = false;
    }

    @PrePersist
    protected void onCreate() {
        if (this.dataOferta == null) {
            this.dataOferta = LocalDateTime.now();
        }
        if (this.statusApuracao == null) {
            this.statusApuracao = StatusApuracaoLance.CADASTRADO;
        }
        if (this.modalidade == null) {
            this.modalidade = ModalidadeLance.LIVRE;
        }
        atualizarNotificarSiscoaf();
    }

    @PreUpdate
    protected void onUpdate() {
        atualizarNotificarSiscoaf();
    }

    private void atualizarNotificarSiscoaf() {
        this.notificarSiscoaf = this.statusApuracao == StatusApuracaoLance.VENCEDOR
                && this.tipo == TipoLance.FIRME
                && this.valorOferta != null
                && this.valorOferta.compareTo(new BigDecimal("50000.00")) >= 0;
    }
}
