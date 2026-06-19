# 📋 Contrato de API — Oferta de Lances (lances)

*   **Capability**: lances
*   **Versão**: v1.1
*   **Spec de referência**: [spec.md](spec.md)
*   **Última alteração**: Inclusão da modalidade (LIVRE, FIXO) nos DTOs de Lance.

---

## 🔐 Autenticação

Todos os endpoints requerem cookie `HttpOnly` com JWT válido.

---

## 📡 Endpoints

### POST `/api/lances`

| Item | Valor |
|---|---|
| **Descrição** | Cadastra uma nova proposta de lance (Livre ou Fixo) para a assembleia aberta (status CAPTANDO) |
| **Auth** | 🔒 Autenticado |
| **REQ-IDs** | REQ-LAN-001 (Elegibilidade), REQ-LAN-002 (Lance Embutido), REQ-LAN-004 (Lance Fixo) |

**Request Body** (JSON):
```json
{
  "cotaId": "Long — ID da cota (obrigatório, positivo)",
  "assembleiaId": "Long — ID da assembleia (obrigatório, positivo)",
  "tipo": "TipoLance — EMBUTIDO | FIRME | MISTO (obrigatório)",
  "modalidade": "ModalidadeLance — LIVRE | FIXO (obrigatório)",
  "valorOferta": "BigDecimal — valor ofertado (≥ 0, calculado automaticamente se modalidade for FIXO)"
}
```

**Response `201 Created`** (JSON):
```json
{
  "id": "Long",
  "cotaId": "Long",
  "assembleiaId": "Long",
  "tipo": "TipoLance",
  "modalidade": "ModalidadeLance",
  "valorOferta": "BigDecimal",
  "dataOferta": "LocalDateTime",
  "statusApuracao": "StatusApuracaoLance"
}
```

**Erros**:
| Código | Cenário |
|---|---|
| `400` | Cota inativa ou inadimplente, ou assembleia fechada |
| `422` | Valor do lance embutido excede o limite do grupo |

---

### POST `/api/parcelas/cota/{cotaId}/lance/reducao-prazo`

| Item | Valor |
|---|---|
| **Descrição** | Amortiza lance pago via redução de prazo (quita parcelas de trás para frente) |
| **Auth** | 🔒 Autenticado |
| **REQ-IDs** | REQ-LAN-003 (Redução de Prazo) |

**Path Parameters**: `cotaId` (Long)
**Query Parameters**: `valorLance` (BigDecimal — valor do lance pago)

**Response `200 OK`**: `"Amortização por redução de prazo realizada com sucesso!"`

**Erros**:
| Código | Cenário |
|---|---|
| `400` | Cota não encontrada ou valor inválido |
| `422` | Valor do lance excede saldo devedor |

---

### POST `/api/parcelas/cota/{cotaId}/lance/diluicao`

| Item | Valor |
|---|---|
| **Descrição** | Amortiza lance pago via diluição de valor (reduz cada parcela restante proporcionalmente) |
| **Auth** | 🔒 Autenticado |
| **REQ-IDs** | REQ-LAN-003 (Diluição de Valor) |

**Path Parameters**: `cotaId` (Long)
**Query Parameters**: `valorLance` (BigDecimal — valor do lance pago)

**Response `200 OK`**: `"Amortização por diluição do valor das parcelas realizada com sucesso!"`

---

## 📐 DTOs de Referência

### Request: `LanceRequestDTO`
```java
public record LanceRequestDTO(
    @NotNull Long cotaId,
    @NotNull Long assembleiaId,
    @NotNull TipoLance tipo,
    @NotNull ModalidadeLance modalidade,
    @NotNull @Positive BigDecimal valorOferta
) {}
```

### Response: `LanceResponseDTO`
```java
public record LanceResponseDTO(
    Long id, Long cotaId, Long assembleiaId, TipoLance tipo,
    ModalidadeLance modalidade, BigDecimal valorOferta, 
    LocalDateTime dataOferta, StatusApuracaoLance statusApuracao
) {}
```
