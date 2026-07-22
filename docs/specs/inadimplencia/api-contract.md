# 📋 Contrato de API — Mora e Inadimplência (inadimplencia)

*   **Capability**: inadimplencia
*   **Versão**: v1.0 (Baseline Retroativo)
*   **Spec de referência**: [spec.md](spec.md)
*   **Última alteração**: Geração retroativa baseada no código implementado.

---

## 🔐 Autenticação

Todos os endpoints requerem cookie `HttpOnly` com JWT válido.

---

## 📡 Endpoints

### PUT `/api/parcelas/{id}/pagar`

| Item | Valor |
|---|---|
| **Descrição** | Registra pagamento de parcela com cálculo automático de multa (2%) e juros pro rata die (1% a.m.) em caso de atraso |
| **Auth** | 🔒 Autenticado |
| **REQ-IDs** | REQ-INA-001, REQ-INA-002, REQ-INA-003 |

**Path Parameters**: `id` (Long — ID da parcela)
**Query Parameters**: `dataPagamento` (LocalDate — data efetiva do pagamento)

**Response `200 OK`** (JSON):
```json
{
  "id": "Long",
  "cotaId": "Long",
  "numeroParcela": "Integer",
  "valorParcela": "BigDecimal — valor original",
  "valorMulta": "BigDecimal — 2% se atraso, 0 se em dia",
  "valorJuros": "BigDecimal — pro rata die se atraso, 0 se em dia",
  "valorPago": "BigDecimal — total cobrado",
  "dataVencimento": "LocalDate",
  "dataPagamento": "LocalDate",
  "status": "PAGA"
}
```

---

### GET `/api/cotas/{id}/inadimplencia`

| Item | Valor |
|---|---|
| **Descrição** | Calcula inadimplência simulada em memória (juros e multas acumulados) sem persistir no banco |
| **Auth** | 🔒 Autenticado |
| **REQ-IDs** | REQ-INA-001, REQ-INA-003 |

**Path Parameters**: `id` (Long — ID da cota)

**Response `200 OK`** (JSON):
```json
{
  "cotaId": "Long",
  "numeroCota": "Integer",
  "possuiInadimplencia": "Boolean",
  "quantidadeParcelasAtrasadas": "Integer",
  "valorOriginalAtrasado": "BigDecimal",
  "multaAcumulada": "BigDecimal",
  "jurosAcumulados": "BigDecimal",
  "saldoDevedorTotal": "BigDecimal",
  "parcelasAtrasadas": "ParcelaResponseDTO[]"
}
```

---

### POST `/api/parcelas/{id}/estornar`

| Item | Valor |
|---|---|
| **Descrição** | Estorna pagamento de parcela, realizando lançamento contábil inverso |
| **Auth** | 🔒 Autenticado |
| **REQ-IDs** | REQ-INA-002 |

**Path Parameters**: `id` (Long — ID da parcela)

**Response `200 OK`**: `ParcelaResponseDTO` com status revertido para `PENDENTE`.

---

## 📐 DTOs de Referência

### Response: `CotaInadimplenciaResponseDTO`
```java
public record CotaInadimplenciaResponseDTO(
    Long cotaId, Integer codigoCota, Boolean possuiInadimplencia,
    Integer quantidadeParcelasAtrasadas, BigDecimal valorOriginalAtrasado,
    BigDecimal multaAcumulada, BigDecimal jurosAcumulados,
    BigDecimal saldoDevedorTotal, List<ParcelaResponseDTO> parcelasAtrasadas
) {}
```
