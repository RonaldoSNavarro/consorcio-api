package br.com.estudo.consorcio.domain.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record FipeModelosResponseDTO(
    List<FipeItemDTO> modelos,
    List<FipeItemDTO> anos
) {}
