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
  "numeroCota": "Integer",
  "totalFundoComumPago": "BigDecimal — soma histórica paga ao FC",
  "multaRescisoria": "BigDecimal — 10% do valor bruto da devolução",
  "valorReembolsado": "BigDecimal — valor líquido (90% do VBD)",
  "reembolsada": "Boolean — true após efetivação"
}
```

**Erros**:
| Código | Cenário |
|---|---|
| `400` | Cota não está cancelada |
| `422` | Cota já foi reembolsada |

---

## 📐 DTOs de Referência

### Response: `CotaReembolsoResponseDTO`
```java
public record CotaReembolsoResponseDTO(
    Long cotaId, Integer numeroCota,
    BigDecimal totalFundoComumPago, BigDecimal multaRescisoria,
    BigDecimal valorReembolsado, Boolean reembolsada
) {}
```
