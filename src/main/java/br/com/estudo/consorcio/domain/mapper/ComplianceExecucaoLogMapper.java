package br.com.estudo.consorcio.domain.mapper;

import br.com.estudo.consorcio.domain.dto.ComplianceExecucaoLogResponseDTO;
import br.com.estudo.consorcio.domain.model.ComplianceExecucaoLog;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface ComplianceExecucaoLogMapper {

    ComplianceExecucaoLogMapper INSTANCE = Mappers.getMapper(ComplianceExecucaoLogMapper.class);

    ComplianceExecucaoLogResponseDTO toResponse(ComplianceExecucaoLog entity);
}
