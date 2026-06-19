package br.com.estudo.consorcio.domain.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "listas_restritivas")
public class ListaRestritiva {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    @Column(name = "documento_origem")
    private String documentoOrigem;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrigemListaRestritiva origem;

    @Column(name = "data_inclusao", nullable = false)
    private LocalDateTime dataInclusao;

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getDocumentoOrigem() {
        return documentoOrigem;
    }

    public void setDocumentoOrigem(String documentoOrigem) {
        this.documentoOrigem = documentoOrigem;
    }

    public OrigemListaRestritiva getOrigem() {
        return origem;
    }

    public void setOrigem(OrigemListaRestritiva origem) {
        this.origem = origem;
    }

    public LocalDateTime getDataInclusao() {
        return dataInclusao;
    }

    public void setDataInclusao(LocalDateTime dataInclusao) {
        this.dataInclusao = dataInclusao;
    }
}
