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

    @Mapping(target = "bensPermitidos", ignore = true)
    Grupo toEntity(GrupoRequestDTO dto);

    GrupoResponseDTO toResponse(Grupo entity);

    @Mapping(target = "id", ignore = true) // ID não deve ser atualizado pelo DTO
    @Mapping(target = "dataCriacao", ignore = true) // Data de criação não deve ser atualizada
    @Mapping(target = "dataInauguracao", ignore = true) // Data de inauguração tem método próprio
    @Mapping(target = "status", ignore = true) // Status tem método próprio
    @Mapping(target = "bensPermitidos", ignore = true)
    void updateEntityFromDto(GrupoRequestDTO dto, @MappingTarget Grupo entity);

    default br.com.estudo.consorcio.domain.dto.BemReferenciaResponseDTO mapBemReferencia(br.com.estudo.consorcio.domain.model.BemReferencia bem) {
        if (bem == null) return null;
        return new br.com.estudo.consorcio.domain.dto.BemReferenciaResponseDTO(
                bem.getId(),
                bem.getCategoriaBem() != null ? bem.getCategoriaBem().getId() : null,
                bem.getCategoriaBem() != null ? bem.getCategoriaBem().getNome() : null,
                bem.getCategoriaBem() != null && bem.getCategoriaBem().getTipoBacen() != null ? bem.getCategoriaBem().getTipoBacen().name() : null,
                bem.getCategoriaBem() != null && bem.getCategoriaBem().getIndiceReajustePadrao() != null ? bem.getCategoriaBem().getIndiceReajustePadrao().name() : null,
                bem.getDescricao(),
                bem.getValorAtual(),
                bem.getDataUltimaAtualizacao(),
                bem.getCodigoFipe(),
                bem.getAtivo()
        );
    }
}
