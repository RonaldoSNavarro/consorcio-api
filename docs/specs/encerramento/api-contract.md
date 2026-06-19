# 📋 Contrato de API — Reajustes e Encerramento (encerramento)

*   **Capability**: encerramento
*   **Versão**: v1.0 (Baseline Retroativo)
*   **Spec de referência**: [spec.md](spec.md)
*   **Última alteração**: Geração retroativa baseada no código implementado.

---

## 🔐 Autenticação

Todos os endpoints requerem cookie `HttpOnly` com JWT válido.

---

## 📡 Endpoints

### PUT `/api/grupos/{id}/reajuste`

| Item | Valor |
|---|---|
| **Descrição** | Atualiza o valor do crédito do grupo e reajusta proporcionalmente todas as parcelas PENDENTE e ATRASADA |
| **Auth** | 🔒 Autenticado |
| **REQ-IDs** | REQ-ENC-001 |

**Path Parameters**: `id` (Long — ID do grupo)
**Query Parameters**: `novoValorCredito` (BigDecimal — novo valor do bem de referência)

**Response `200 OK`** (JSON):
```json
{
  "id": "Long",
  "codigo": "String",
  "valorCredito": "BigDecimal — novo valor atualizado",
  "prazoMeses": "Integer",
  "taxaAdministracao": "BigDecimal",
  "status": "StatusGrupo",
  "dataCriacao": "LocalDate",
  "dataInauguracao": "LocalDate"
}
```

---

### POST `/api/grupos/{id}/encerrar`

| Item | Valor |
|---|---|
| **Descrição** | Encerra o grupo contabilmente. Baixa inadimplência para PDD e transfere recursos não procurados para RNP (ADR 006) |
| **Auth** | 🔒 Autenticado |
| **REQ-IDs** | REQ-ENC-002, REQ-ENC-003 |

**Path Parameters**: `id` (Long — ID do grupo)

**Response `200 OK`** (JSON):
```json
{
  "grupoId": "Long",
  "codigo": "String",
  "totalParcelasBaixadas": "long — qtd de parcelas baixadas para PDD",
  "valorTotalPDD": "BigDecimal — total provisionado em PDD",
  "valorTransferidoRNP": "BigDecimal — total transferido para RNP",
  "dataEncerramento": "LocalDate"
}
```

**Erros**:
| Código | Cenário |
|---|---|
| `422` | Grupo já encerrado ou condições de encerramento não satisfeitas |

---

### GET `/api/grupos/{id}/financeiro`

| Item | Valor |
|---|---|
| **Descrição** | Relatório financeiro consolidado do grupo (arrecadação FC, TA, FR e créditos liberados) |
| **Auth** | 🔒 Autenticado |
| **REQ-IDs** | REQ-ENC-001 |

**Path Parameters**: `id` (Long — ID do grupo)

**Response `200 OK`** (JSON):
```json
{
  "grupoId": "Long",
  "codigoGrupo": "String",
  "totalFundoComumArrecadado": "BigDecimal",
  "totalTaxaAdministracaoArrecadada": "BigDecimal",
  "totalFundoReservaArrecadado": "BigDecimal",
  "totalCreditosLiberados": "BigDecimal",
  "saldoDisponivelFundoComum": "BigDecimal",
  "saldoDisponivelFundoReserva": "BigDecimal"
}
```

---

## 📐 DTOs de Referência

### Response: `GrupoEncerrarResponseDTO`
```java
public record GrupoEncerrarResponseDTO(
    Long grupoId, String codigo, long totalParcelasBaixadas,
    BigDecimal valorTotalPDD, BigDecimal valorTransferidoRNP,
    LocalDate dataEncerramento
) {}
```

### Response: `GrupoFinanceiroResponseDTO`
```java
public record GrupoFinanceiroResponseDTO(
    Long grupoId, String codigoGrupo,
    BigDecimal totalFundoComumArrecadado, BigDecimal totalTaxaAdministracaoArrecadada,
    BigDecimal totalFundoReservaArrecadado, BigDecimal totalCreditosLiberados,
    BigDecimal saldoDisponivelFundoComum, BigDecimal saldoDisponivelFundoReserva
) {}
```
