package br.com.estudo.consorcio.domain.mapper;

import br.com.estudo.consorcio.domain.dto.LanceResponseDTO;
import br.com.estudo.consorcio.domain.model.Lance;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface LanceMapper {

    @Mapping(target = "cotaId", source = "cota.id")
    @Mapping(target = "assembleiaId", source = "assembleia.id")
    LanceResponseDTO toResponse(Lance entity);
}
