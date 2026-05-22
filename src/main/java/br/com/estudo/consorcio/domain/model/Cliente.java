package br.com.estudo.consorcio.domain.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
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
    }
}