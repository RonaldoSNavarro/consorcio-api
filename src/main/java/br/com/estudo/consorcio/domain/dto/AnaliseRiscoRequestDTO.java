package br.com.estudo.consorcio.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnaliseRiscoRequestDTO {
    private boolean aprovada;
    private String justificativa;
}
