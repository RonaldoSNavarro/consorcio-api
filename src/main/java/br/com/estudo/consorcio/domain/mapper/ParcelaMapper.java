package br.com.estudo.consorcio.domain.mapper;

import br.com.estudo.consorcio.domain.dto.ParcelaRequestDTO;
import br.com.estudo.consorcio.domain.dto.ParcelaResponseDTO;
import br.com.estudo.consorcio.domain.model.Parcela;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface ParcelaMapper {

    ParcelaMapper INSTANCE = Mappers.getMapper(ParcelaMapper.class);

    // Mapeia ParcelaRequestDTO para Parcela. Cota será setada no serviço após busca.
    @Mapping(target = "cota", ignore = true)
    @Mapping(target = "valorParcela", ignore = true) // Calculado no @PrePersist/@PreUpdate
    @Mapping(target = "valorMulta", ignore = true) // Definido no serviço
    @Mapping(target = "valorJuros", ignore = true) // Definido no serviço
    @Mapping(target = "valorPago", ignore = true) // Definido no serviço
    @Mapping(target = "dataPagamento", ignore = true) // Definido no serviço
    @Mapping(target = "status", ignore = true) // Definido no serviço
    @Mapping(target = "percentualFundoComum", ignore = true) // Calculado no serviço
    Parcela toEntity(ParcelaRequestDTO dto);

    // Mapeia Parcela para ParcelaResponseDTO, extraindo apenas o ID da Cota
    @Mapping(source = "cota.id", target = "cotaId")
    ParcelaResponseDTO toResponse(Parcela entity);
}
