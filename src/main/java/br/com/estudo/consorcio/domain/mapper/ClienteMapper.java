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

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "dataCadastro", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "logradouro", ignore = true)
    @Mapping(target = "bairro", ignore = true)
    @Mapping(target = "localidade", ignore = true)
    @Mapping(target = "uf", ignore = true)
    Cliente toEntity(ClienteRequestDTO dto);

    @Mapping(source = "status", target = "statusCliente")
    ClienteResponseDTO toResponse(Cliente entity);

    @Mapping(target = "id", ignore = true) // ID não deve ser atualizado pelo DTO
    @Mapping(target = "dataCadastro", ignore = true) // Data de cadastro não deve ser atualizada
    @Mapping(target = "status", ignore = true) // Status tem método próprio no serviço
    @Mapping(target = "logradouro", ignore = true)
    @Mapping(target = "bairro", ignore = true)
    @Mapping(target = "localidade", ignore = true)
    @Mapping(target = "uf", ignore = true)
    void updateEntityFromDto(ClienteRequestDTO dto, @MappingTarget Cliente entity);
}
