package br.com.estudo.consorcio.domain.mapper;

import br.com.estudo.consorcio.domain.dto.CotaRequestDTO;
import br.com.estudo.consorcio.domain.dto.CotaResponseDTO;
import br.com.estudo.consorcio.domain.model.Cota;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface CotaMapper {

    CotaMapper INSTANCE = Mappers.getMapper(CotaMapper.class);

    // Mapeia CotaRequestDTO para Cota. Cliente e Grupo serão setados no serviço após busca.
    @Mapping(target = "cliente", ignore = true)
    @Mapping(target = "grupo", ignore = true)
    Cota toEntity(CotaRequestDTO dto);

    // Mapeia Cota para CotaResponseDTO, extraindo apenas os IDs de Cliente e Grupo
    @Mapping(source = "cliente.id", target = "clienteId")
    @Mapping(source = "grupo.id", target = "grupoId")
    CotaResponseDTO toResponse(Cota entity);
}
