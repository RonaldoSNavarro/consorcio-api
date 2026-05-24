package br.com.estudo.consorcio.domain.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "contas_contabeis")
public class ContaContabil {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "codigo_cosif", nullable = false, unique = true, length = 30)
    private String codigoCosif;

    @Column(nullable = false, length = 100)
    private String nome;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoContaContabil tipo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private NaturezaContabil natureza;

    @Column(nullable = false)
    private Boolean ativa = true;

    @Column(name = "data_criacao", nullable = false, updatable = false)
    private LocalDateTime dataCriacao;

    @PrePersist
    protected void onCreate() {
        if (this.dataCriacao == null) {
            this.dataCriacao = LocalDateTime.now();
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCodigoCosif() { return codigoCosif; }
    public void setCodigoCosif(String codigoCosif) { this.codigoCosif = codigoCosif; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public TipoContaContabil getTipo() { return tipo; }
    public void setTipo(TipoContaContabil tipo) { this.tipo = tipo; }

    public NaturezaContabil getNatureza() { return natureza; }
    public void setNatureza(NaturezaContabil natureza) { this.natureza = natureza; }

    public Boolean getAtiva() { return ativa; }
    public void setAtiva(Boolean ativa) { this.ativa = ativa; }

    public LocalDateTime getDataCriacao() { return dataCriacao; }
}
