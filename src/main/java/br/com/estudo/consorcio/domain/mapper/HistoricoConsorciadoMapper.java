package br.com.estudo.consorcio.domain.mapper;

import br.com.estudo.consorcio.domain.dto.HistoricoConsorciadoResponseDTO;
import br.com.estudo.consorcio.domain.model.HistoricoConsorciado;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface HistoricoConsorciadoMapper {

    HistoricoConsorciadoMapper INSTANCE = Mappers.getMapper(HistoricoConsorciadoMapper.class);

    @Mapping(target = "clienteId", source = "cliente.id")
    @Mapping(target = "cotaId", source = "cota.id")
    @Mapping(target = "grupoId", source = "grupo.id")
    @Mapping(target = "parcelaId", source = "parcela.id")
        @Mapping(target = "nomeUsuario", source = "usuario.username")
    HistoricoConsorciadoResponseDTO toResponse(HistoricoConsorciado entity);
}
