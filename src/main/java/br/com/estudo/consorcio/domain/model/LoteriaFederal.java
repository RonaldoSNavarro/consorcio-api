package br.com.estudo.consorcio.domain.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "loteria_federal")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoteriaFederal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String concurso;

    @Column(name = "data_sorteio", nullable = false)
    private LocalDate dataSorteio;

    @Column(name = "premio_1", nullable = false)
    private String premio1;

    @Column(name = "premio_2", nullable = false)
    private String premio2;

    @Column(name = "premio_3", nullable = false)
    private String premio3;

    @Column(name = "premio_4", nullable = false)
    private String premio4;

    @Column(name = "premio_5", nullable = false)
    private String premio5;
}
