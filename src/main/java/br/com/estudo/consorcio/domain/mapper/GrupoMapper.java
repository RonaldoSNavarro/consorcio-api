package br.com.estudo.consorcio.domain.mapper;

import br.com.estudo.consorcio.domain.dto.GrupoRequestDTO;
import br.com.estudo.consorcio.domain.dto.GrupoResponseDTO;
import br.com.estudo.consorcio.domain.model.Grupo;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface GrupoMapper {

    GrupoMapper INSTANCE = Mappers.getMapper(GrupoMapper.class);

    Grupo toEntity(GrupoRequestDTO dto);

    GrupoResponseDTO toResponse(Grupo entity);

    @Mapping(target = "id", ignore = true) // ID não deve ser atualizado pelo DTO
    @Mapping(target = "dataCriacao", ignore = true) // Data de criação não deve ser atualizada
    @Mapping(target = "dataInauguracao", ignore = true) // Data de inauguração tem método próprio
    @Mapping(target = "status", ignore = true) // Status tem método próprio
    void updateEntityFromDto(GrupoRequestDTO dto, @MappingTarget Grupo entity);
}
