package br.com.estudo.consorcio.domain.dto;

import java.time.LocalDate;

public record ClienteResponseDTO(
        Long id,
        String nome,
        String email,
        LocalDate dataCadastro
) {}