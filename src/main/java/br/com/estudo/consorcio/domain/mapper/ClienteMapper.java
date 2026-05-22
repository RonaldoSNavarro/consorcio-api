package br.com.estudo.consorcio.domain.mapper;

import br.com.estudo.consorcio.domain.dto.ClienteRequestDTO;
import br.com.estudo.consorcio.domain.dto.ClienteResponseDTO;
import br.com.estudo.consorcio.domain.model.Cliente;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface ClienteMapper {

    ClienteMapper INSTANCE = Mappers.getMapper(ClienteMapper.class);

    Cliente toEntity(ClienteRequestDTO dto);

    ClienteResponseDTO toResponse(Cliente entity);

    @Mapping(target = "id", ignore = true) // ID não deve ser atualizado pelo DTO
    @Mapping(target = "dataCadastro", ignore = true) // Data de cadastro não deve ser atualizada
    @Mapping(target = "status", ignore = true) // Status tem método próprio no serviço
    void updateEntityFromDto(ClienteRequestDTO dto, @MappingTarget Cliente entity);
}
