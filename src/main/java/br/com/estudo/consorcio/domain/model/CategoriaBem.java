package br.com.estudo.consorcio.domain.model;

import br.com.estudo.consorcio.domain.enums.TipoCategoriaBacen;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "categorias_bem")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoriaBem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nome;

    @Enumerated(EnumType.STRING)
    private TipoCategoriaBacen tipoBacen;

    @Enumerated(EnumType.STRING)
    private IndiceReajuste indiceReajustePadrao;
}
