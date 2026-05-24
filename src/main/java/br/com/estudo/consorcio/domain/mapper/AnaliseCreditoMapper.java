package br.com.estudo.consorcio.domain.mapper;

import br.com.estudo.consorcio.domain.dto.AnaliseCreditoRequestDTO;
import br.com.estudo.consorcio.domain.dto.AnaliseCreditoResponseDTO;
import br.com.estudo.consorcio.domain.model.AnaliseCredito;
import org.springframework.stereotype.Component;

@Component
public class AnaliseCreditoMapper {

    public AnaliseCredito toEntity(AnaliseCreditoRequestDTO dto) {
        AnaliseCredito entidade = new AnaliseCredito();
        entidade.setRendaComprovada(dto.rendaComprovada());
        entidade.setGarantiaAprovada(dto.garantiaAprovada());
        entidade.setObservacao(dto.observacao());
        return entidade;
    }

    public AnaliseCreditoResponseDTO toResponse(AnaliseCredito entidade) {
        return new AnaliseCreditoResponseDTO(
                entidade.getId(),
                entidade.getCota() != null ? entidade.getCota().getId() : null,
                entidade.getRendaComprovada(),
                entidade.getGarantiaAprovada(),
                entidade.getStatus(),
                entidade.getDataAnalise(),
                entidade.getObservacao()
        );
    }
}
