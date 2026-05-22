package br.com.estudo.consorcio.domain.mapper;

import br.com.estudo.consorcio.domain.dto.ContemplacaoRequestDTO;
import br.com.estudo.consorcio.domain.dto.ContemplacaoResponseDTO;
import br.com.estudo.consorcio.domain.model.Contemplacao;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface ContemplacaoMapper {

    ContemplacaoMapper INSTANCE = Mappers.getMapper(ContemplacaoMapper.class);

    // Mapeia ContemplacaoRequestDTO para Contemplacao. Cota e Assembleia serão setadas no serviço após busca.
    @Mapping(target = "cota", ignore = true)
    @Mapping(target = "assembleia", ignore = true)
    @Mapping(target = "dataContemplacao", ignore = true) // Definido no serviço ou @PrePersist
    @Mapping(target = "valorCreditoLiberado", ignore = true) // Calculado no serviço
    Contemplacao toEntity(ContemplacaoRequestDTO dto);

    // Mapeia Contemplacao para ContemplacaoResponseDTO, extraindo apenas os IDs de Cota e Assembleia
    @Mapping(source = "cota.id", target = "cotaId")
    @Mapping(source = "assembleia.id", target = "assembleiaId")
    ContemplacaoResponseDTO toResponse(Contemplacao entity);
}
