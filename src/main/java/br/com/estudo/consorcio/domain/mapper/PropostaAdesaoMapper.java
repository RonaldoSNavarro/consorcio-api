package br.com.estudo.consorcio.domain.mapper;

import br.com.estudo.consorcio.domain.dto.ContratoResponseDTO;
import br.com.estudo.consorcio.domain.dto.PropostaResponseDTO;
import br.com.estudo.consorcio.domain.model.ContratoAdesao;
import br.com.estudo.consorcio.domain.model.PropostaAdesao;
import org.springframework.stereotype.Component;

@Component
public class PropostaAdesaoMapper {

    public PropostaResponseDTO toResponse(PropostaAdesao model) {
        if (model == null) return null;
        
        return PropostaResponseDTO.builder()
                .id(model.getId())
                .numeroProposta(model.getNumeroProposta())
                .clienteId(model.getCliente() != null ? model.getCliente().getId() : null)
                .produtoId(model.getProduto() != null ? model.getProduto().getId() : null)
                .valorCreditoSolicitado(model.getValorCreditoSolicitado())
                .status(model.getStatus() != null ? model.getStatus().name() : null)
                .dataProposta(model.getDataProposta())
                .build();
    }

    public ContratoResponseDTO toContratoResponse(ContratoAdesao model) {
        if (model == null) return null;
        
        return ContratoResponseDTO.builder()
                .id(model.getId())
                .numeroContrato(model.getNumeroContrato())
                .propostaId(model.getProposta() != null ? model.getProposta().getId() : null)
                .dataAssinatura(model.getDataAssinatura())
                .status(model.getStatus() != null ? model.getStatus().name() : null)
                .build();
    }
}
