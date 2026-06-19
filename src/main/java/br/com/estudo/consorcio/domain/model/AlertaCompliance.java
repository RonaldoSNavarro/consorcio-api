package br.com.estudo.consorcio.domain.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "alertas_compliance")
public class AlertaCompliance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lista_id", nullable = false)
    private ListaRestritiva listaRestritiva;

    @Column(nullable = false, precision = 5, scale = 4)
    private java.math.BigDecimal score;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusAlertaCompliance status;

    @Column(columnDefinition = "TEXT")
    private String justificativa;

    @Column(name = "data_deteccao", nullable = false)
    private LocalDateTime dataDeteccao;

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Cliente getCliente() {
        return cliente;
    }

    public void setCliente(Cliente cliente) {
        this.cliente = cliente;
    }

    public ListaRestritiva getListaRestritiva() {
        return listaRestritiva;
    }

    public void setListaRestritiva(ListaRestritiva listaRestritiva) {
        this.listaRestritiva = listaRestritiva;
    }

    public java.math.BigDecimal getScore() {
        return score;
    }

    public void setScore(java.math.BigDecimal score) {
        this.score = score;
    }

    public StatusAlertaCompliance getStatus() {
        return status;
    }

    public void setStatus(StatusAlertaCompliance status) {
        this.status = status;
    }

    public String getJustificativa() {
        return justificativa;
    }

    public void setJustificativa(String justificativa) {
        this.justificativa = justificativa;
    }

    public LocalDateTime getDataDeteccao() {
        return dataDeteccao;
    }

    public void setDataDeteccao(LocalDateTime dataDeteccao) {
        this.dataDeteccao = dataDeteccao;
    }
}
