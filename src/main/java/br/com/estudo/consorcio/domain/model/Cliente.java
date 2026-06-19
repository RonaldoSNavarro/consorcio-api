package br.com.estudo.consorcio.domain.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "clientes")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Data // Anotação do Lombok que gera Getters, Setters, toString, etc.
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Cliente {

    @EqualsAndHashCode.Include
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nome;

    @Column(name = "cpf_cnpj", nullable = false, unique = true, length = 14)
    private String cpfCnpj;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(length = 20)
    private String telefone;

    @Column(name = "renda_mensal_declarada")
    private BigDecimal rendaMensalDeclarada;

    @Column(name = "patrimonio_estimado")
    private BigDecimal patrimonioEstimado;

    // --- Endereço ---
    @Column(length = 8, nullable = false)
    private String cep;

    @Column(length = 200, nullable = false)
    private String logradouro;

    @Column(length = 20, nullable = false)
    private String numero;

    @Column(length = 100)
    private String complemento;

    @Column(length = 100, nullable = false)
    private String bairro;

    @Column(length = 100, nullable = false)
    private String localidade;

    @Column(length = 2, nullable = false)
    private String uf;

    // --- Dados financeiros ---
    @Column(precision = 38, scale = 2)
    private BigDecimal patrimonio = BigDecimal.ZERO;

    @Column(name = "renda_mensal", precision = 38, scale = 2)
    private BigDecimal rendaMensal = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "nivel_risco", nullable = false, length = 10)
    private NivelRisco nivelRisco = NivelRisco.MEDIO;

    @Column(name = "data_cadastro", updatable = false)
    private LocalDate dataCadastro;

    @Enumerated(EnumType.STRING) // Adicionado o campo status
    @Column(nullable = false)
    private StatusCliente status;

    // Metodo executado automaticamente antes de salvar no banco pela primeira vez
    @PrePersist
    protected void onCreate() {
        this.dataCadastro = LocalDate.now();
        if (this.status == null) { // Definir status padrão se não for setado
            this.status = StatusCliente.ATIVO;
        }
        if (this.patrimonio == null) {
            this.patrimonio = BigDecimal.ZERO;
        }
        if (this.rendaMensal == null) {
            this.rendaMensal = BigDecimal.ZERO;
        }
        if (this.nivelRisco == null) {
            this.nivelRisco = NivelRisco.MEDIO;
        }
    }
}