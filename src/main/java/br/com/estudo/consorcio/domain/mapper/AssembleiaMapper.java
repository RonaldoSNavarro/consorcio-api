package br.com.estudo.consorcio.domain.mapper;

import br.com.estudo.consorcio.domain.dto.AssembleiaRequestDTO;
import br.com.estudo.consorcio.domain.dto.AssembleiaResponseDTO;
import br.com.estudo.consorcio.domain.model.Assembleia;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface AssembleiaMapper {

    AssembleiaMapper INSTANCE = Mappers.getMapper(AssembleiaMapper.class);

    // Mapeia AssembleiaRequestDTO para Assembleia. Grupo será setado no serviço após busca.
    @Mapping(target = "grupo", ignore = true)
    @Mapping(target = "tipo", ignore = true) // Definido no serviço
    Assembleia toEntity(AssembleiaRequestDTO dto);

    // Mapeia Assembleia para AssembleiaResponseDTO, extraindo apenas o ID do Grupo
    @Mapping(source = "grupo.id", target = "grupoId")
    AssembleiaResponseDTO toResponse(Assembleia entity);
}
