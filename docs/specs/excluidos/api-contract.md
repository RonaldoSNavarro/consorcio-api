# 📋 Contrato de API — Restituição de Excluídos (excluidos)

*   **Capability**: excluidos
*   **Versão**: v1.0 (Baseline Retroativo)
*   **Spec de referência**: [spec.md](spec.md)
*   **Última alteração**: Geração retroativa baseada no código implementado.

---

## 🔐 Autenticação

Todos os endpoints requerem cookie `HttpOnly` com JWT válido.

---

## 📡 Endpoints

### POST `/api/cotas/{id}/cancelar`

| Item | Valor |
|---|---|
| **Descrição** | Cancela uma cota ativa, excluindo parcelas pendentes e registrando auditoria de versão |
| **Auth** | 🔒 Autenticado |
| **REQ-IDs** | REQ-EXC-001 |

**Path Parameters**: `id` (Long — ID da cota)

**Response `200 OK`** (JSON):
```json
{
  "id": "Long",
  "numeroCota": "Integer",
  "clienteId": "Long",
  "grupoId": "Long",
  "status": "CANCELADA",
  "versao": "Integer"
}
```

---

### POST `/api/cotas/{id}/reembolsar`

| Item | Valor |
|---|---|
| **Descrição** | Calcula e efetua reembolso de cota cancelada conforme ADR 005 (percentual amortizado sobre bem atualizado, menos 10% de multa rescisória) |
| **Auth** | 🔒 Autenticado |
| **REQ-IDs** | REQ-EXC-002, REQ-EXC-003, REQ-EXC-004, REQ-EXC-005 |

**Path Parameters**: `id` (Long — ID da cota)

**Response `200 OK`** (JSON):
```json
{
  "cotaId": "Long",
  "codigoCota": "Integer",
  "totalFundoComumPago": "BigDecimal",
  "multaRescisoria": "BigDecimal",
  "valorReembolsado": "BigDecimal",
  "reembolsada": "Boolean"
}
```

#### Erros Possíveis

- `400 Bad Request`: Cota não está com status `CANCELADA`.
- `400 Bad Request`: Cota já foi reembolsada.
- `404 Not Found`: Cota não encontrada.

---

### `GET /cotas/pendentes-reembolso`

Retorna a lista de todas as cotas canceladas que ainda não foram reembolsadas, com simulação do cálculo do reembolso.

- **Autenticação**: Requerida (`VIEW_COTAS`)
- **Response**: `200 OK`

```json
[
  {
    "id": "Long",
    "codigoCota": "Integer",
    "clienteId": "Long",
    "clienteNome": "String",
    "cpfCnpj": "String",
    "numeroAssembleiaAGO": "String",
    "dataContemplacaoAGO": "LocalDate",
    "valorBemReferenciaAGO": "BigDecimal",
    "percentualFundoComumPago": "BigDecimal",
    "valorHistoricoPago": "BigDecimal",
    "valorBrutoRestituicao": "BigDecimal",
    "valorMultaRestituicao": "BigDecimal",
    "valorLiquidoRestituicao": "BigDecimal"
  }
]
```

---

## 2. Tipos de Dados / DTOs (Java Records)

```java
public record CotaReembolsoResponseDTO(
    Long cotaId, Integer codigoCota,
    BigDecimal totalFundoComumPago, BigDecimal multaRescisoria,
    BigDecimal valorReembolsado, Boolean reembolsada
) {}
```
