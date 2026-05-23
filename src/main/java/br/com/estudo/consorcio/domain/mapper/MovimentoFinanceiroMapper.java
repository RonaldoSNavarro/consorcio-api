package br.com.estudo.consorcio.domain.mapper;

import br.com.estudo.consorcio.domain.dto.MovimentoFinanceiroResponseDTO;
import br.com.estudo.consorcio.domain.model.MovimentoFinanceiro;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface MovimentoFinanceiroMapper {

    MovimentoFinanceiroMapper INSTANCE = Mappers.getMapper(MovimentoFinanceiroMapper.class);

    @Mapping(target = "grupoId", source = "grupo.id")
    @Mapping(target = "cotaId", source = "cota.id")
    @Mapping(target = "parcelaId", source = "parcela.id")
    @Mapping(target = "contemplacaoId", source = "contemplacao.id")
    @Mapping(target = "nomeUsuario", source = "usuario.username")
    MovimentoFinanceiroResponseDTO toResponse(MovimentoFinanceiro entity);
}
