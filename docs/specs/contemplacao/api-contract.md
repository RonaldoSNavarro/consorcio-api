# 📋 Contrato de API — Apuração e Contemplações (contemplacao)

*   **Capability**: contemplacao
*   **Versão**: v1.0 (Baseline Retroativo)
*   **Spec de referência**: [spec.md](spec.md)
*   **Última alteração**: Geração retroativa baseada no código implementado.

---

## 🔐 Autenticação

Todos os endpoints requerem cookie `HttpOnly` com JWT válido.

---

## 📡 Endpoints

### POST `/api/contemplacoes`

| Item | Valor |
|---|---|
| **Descrição** | Registra uma nova contemplação (sorteio ou lance). Valida saldo do Fundo Comum no Ledger e limite de lance embutido |
| **Auth** | 🔒 Autenticado |
| **REQ-IDs** | REQ-CON-001, REQ-CON-002, REQ-CON-003, REQ-CON-004 |

**Request Body** (JSON):
```json
{
  "cotaId": "Long — ID da cota (obrigatório, positivo)",
  "assembleiaId": "Long — ID da assembleia (obrigatório, positivo)",
  "tipoContemplacao": "TipoContemplacao — SORTEIO | LANCE_LIVRE | LANCE_FIXO",
  "valorLance": "BigDecimal — valor ofertado (≥ 0, zero se sorteio)",
  "lanceEmbutido": "Boolean — se lance usa crédito próprio (obrigatório)"
}
```

**Response `201 Created`** (JSON):
```json
{
  "id": "Long",
  "cotaId": "Long",
  "assembleiaId": "Long",
  "tipoContemplacao": "TipoContemplacao",
  "valorLance": "BigDecimal",
  "dataContemplacao": "LocalDate",
  "lanceEmbutido": "Boolean",
  "valorCreditoLiberado": "BigDecimal"
}
```

**Erros**:
| Código | Cenário |
|---|---|
| `400` | Validação de entrada falhou |
| `422` | Saldo insuficiente no Fundo Comum / Lance embutido excede 30% do crédito |

---

### GET `/api/contemplacoes/assembleia/{assembleiaId}`

| Item | Valor |
|---|---|
| **Descrição** | Lista todas as cotas contempladas em uma assembleia |
| **Auth** | 🔒 Autenticado |
| **REQ-IDs** | REQ-CON-001 |

**Path Parameters**: `assembleiaId` (Long)

**Response `200 OK`**: JSON Array de `ContemplacaoResponseDTO`.

---

### POST `/api/contemplacoes/{id}/pagamento-bem`

| Item | Valor |
|---|---|
| **Descrição** | Registra o desembolso do crédito para compra do bem. Gera lançamento contábil de débito em Créditos a Liberar e crédito em Bancos |
| **Auth** | 🔒 Autenticado |
| **REQ-IDs** | REQ-CON-004 (Desembolso Efetivo) |

**Path Parameters**: `id` (Long — ID da contemplação)

**Response `200 OK`**: `ContemplacaoResponseDTO`

---

### POST `/api/contemplacoes/lances/{id}/integralizar`

| Item | Valor |
|---|---|
| **Descrição** | Confirma integralização de lance (livre ou fixo). Transita status de `PENDENTE_INTEGRALIZACAO` para `AGUARDANDO_ANALISE` e gera lançamentos contábeis |
| **Auth** | 🔒 Autenticado |
| **REQ-IDs** | REQ-CON-003, REQ-CON-006 (ADR 004) |

**Path Parameters**: `id` (Long — ID da contemplação)

**Response `200 OK`**: `CotaResponseDTO`

---

## 📐 DTOs de Referência

### Request: `ContemplacaoRequestDTO`
```java
public record ContemplacaoRequestDTO(
    @NotNull @Positive Long cotaId,
    @NotNull @Positive Long assembleiaId,
    @NotNull TipoContemplacao tipoContemplacao,
    @PositiveOrZero BigDecimal valorLance,
    @NotNull Boolean lanceEmbutido
) {}
```

### Response: `ContemplacaoResponseDTO`
```java
public record ContemplacaoResponseDTO(
    Long id, Long cotaId, Long assembleiaId, TipoContemplacao tipoContemplacao,
    BigDecimal valorLance, LocalDate dataContemplacao,
    Boolean lanceEmbutido, BigDecimal valorCreditoLiberado
) {}
```
