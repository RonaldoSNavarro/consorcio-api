package br.com.estudo.consorcio.domain.dto;

import java.util.List;

/**
 * DTO retornado pelo endpoint POST /api/compliance/sincronizar.
 * Informa o resultado de cada fonte de dados processada durante a sincronização.
 */
public record ComplianceSyncResultDTO(
        String ofacStatus,
        int ofacRegistros,
        int pepRegistros,
        int onuRegistros,
        int ibgeRegistros,
        List<String> erros
) {
    public static ComplianceSyncResultDTO iniciado() {
        return new ComplianceSyncResultDTO("INICIADO_EM_BACKGROUND", 0, 0, 0, 0, List.of());
    }
}
