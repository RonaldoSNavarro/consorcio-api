package br.com.estudo.consorcio.domain.mapper;

import br.com.estudo.consorcio.domain.dto.CotaRequestDTO;
import br.com.estudo.consorcio.domain.dto.CotaResponseDTO;
import br.com.estudo.consorcio.domain.model.Cota;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import br.com.estudo.consorcio.domain.repository.ParcelaRepository;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Mapper(componentModel = "spring")
public abstract class CotaMapper {

    @Autowired
    protected ParcelaRepository parcelaRepository;

    // Mapeia CotaRequestDTO para Cota. Cliente e Grupo serão setados no serviço após busca.
    @Mapping(target = "cliente", ignore = true)
    @Mapping(target = "grupo", ignore = true)
    public abstract Cota toEntity(CotaRequestDTO dto);

    // Mapeia Cota para CotaResponseDTO, extraindo apenas os IDs de Cliente e Grupo
    @Mapping(source = "cliente.id", target = "clienteId")
    @Mapping(source = "grupo.id", target = "grupoId")
    @Mapping(source = "grupo.codigoGrupo", target = "codigoGrupo")
    @Mapping(source = "cliente.nome", target = "nomeConsorciado")
    @Mapping(source = "bemReferencia.id", target = "bemReferenciaId")
    @Mapping(source = "bemReferencia.descricao", target = "nomeBemReferencia")
    @Mapping(source = "bemReferencia.valorAtual", target = "valorBemReferencia")
    @Mapping(source = "grupo.categoriaBem", target = "categoriaBem")
    @Mapping(target = "percentualPago", expression = "java(calcularPercentualPago(entity))")
    @Mapping(target = "percentualAPagar", expression = "java(calcularPercentualAPagar(entity))")
    public abstract CotaResponseDTO toResponse(Cota entity);

    protected BigDecimal calcularPercentualPago(Cota entity) {
        if (entity.getId() == null) return BigDecimal.ZERO;
        BigDecimal total = parcelaRepository.somarValorTotalPorCota(entity.getId());
        if (total != null && total.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal pago = parcelaRepository.somarValorPagoPorCota(entity.getId());
            if (pago == null) pago = BigDecimal.ZERO;
            return pago.multiply(BigDecimal.valueOf(100)).divide(total, 2, RoundingMode.HALF_UP);
        }
        return BigDecimal.ZERO;
    }

    protected BigDecimal calcularPercentualAPagar(Cota entity) {
        return BigDecimal.valueOf(100).subtract(calcularPercentualPago(entity));
    }
}
