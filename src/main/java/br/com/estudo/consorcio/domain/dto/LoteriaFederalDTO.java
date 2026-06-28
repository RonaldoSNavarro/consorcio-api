package br.com.estudo.consorcio.domain.dto;

import java.time.LocalDate;

public record LoteriaFederalDTO(
    Long id,
    String concurso,
    LocalDate dataSorteio,
    String premio1,
    String premio2,
    String premio3,
    String premio4,
    String premio5
) {}
