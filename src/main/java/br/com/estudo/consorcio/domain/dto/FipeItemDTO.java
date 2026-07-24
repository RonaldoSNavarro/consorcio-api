package br.com.estudo.consorcio.domain.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record FipeItemDTO(
    String nome,
    String codigo
) {}
