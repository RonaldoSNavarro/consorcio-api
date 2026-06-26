package br.com.estudo.consorcio.domain.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "compliance_execucao_log")
public class ComplianceExecucaoLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "data_execucao", nullable = false)
    private LocalDateTime dataExecucao = LocalDateTime.now();

    @Column(name = "trigger_execucao", nullable = false)
    private String triggerExecucao; // CRON ou MANUAL

    @Column(name = "ofac_status")
    private String ofacStatus;

    @Column(name = "pep_registros")
    private Integer pepRegistros;

    @Column(name = "onu_registros")
    private Integer onuRegistros;

    @Column(name = "ibge_registros")
    private Integer ibgeRegistros;

    @Column(name = "ofac_registros")
    private Integer ofacRegistros;

    @Column(name = "duracao_ms")
    private Long duracaoMs;

    @Column(columnDefinition = "TEXT")
    private String erros;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public LocalDateTime getDataExecucao() { return dataExecucao; }
    public void setDataExecucao(LocalDateTime dataExecucao) { this.dataExecucao = dataExecucao; }
    public String getTriggerExecucao() { return triggerExecucao; }
    public void setTriggerExecucao(String triggerExecucao) { this.triggerExecucao = triggerExecucao; }
    public String getOfacStatus() { return ofacStatus; }
    public void setOfacStatus(String ofacStatus) { this.ofacStatus = ofacStatus; }
    public Integer getPepRegistros() { return pepRegistros; }
    public void setPepRegistros(Integer pepRegistros) { this.pepRegistros = pepRegistros; }
    public Integer getOnuRegistros() { return onuRegistros; }
    public void setOnuRegistros(Integer onuRegistros) { this.onuRegistros = onuRegistros; }
    public Integer getIbgeRegistros() { return ibgeRegistros; }
    public void setIbgeRegistros(Integer ibgeRegistros) { this.ibgeRegistros = ibgeRegistros; }
    public Integer getOfacRegistros() { return ofacRegistros; }
    public void setOfacRegistros(Integer ofacRegistros) { this.ofacRegistros = ofacRegistros; }
    public Long getDuracaoMs() { return duracaoMs; }
    public void setDuracaoMs(Long duracaoMs) { this.duracaoMs = duracaoMs; }
    public String getErros() { return erros; }
    public void setErros(String erros) { this.erros = erros; }
}
