# 📋 Contrato de API — Oferta de Lances (lances)

*   **Capability**: lances
*   **Versão**: v1.2
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
  "tipo": "TipoLance — EMBUTIDO | FIRME | FGTS | MISTO | SEGURO_OBITO (obrigatório)",
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
| `400` | Cota inativa ou inadimplente, assembleia fechada, ou valor de oferta acima do crédito vigente |
| `422` | Valor do lance embutido excede o limite do grupo; `MISTO` e `SEGURO_OBITO` estão bloqueados até a modelagem financeira específica |

---

### POST `/api/contemplacoes/lances/{id}/integralizar`

| Item | Valor |
|---|---|
| **Descrição** | Liquida um lance vencedor identificado e aplica a amortização escolhida na mesma transação |
| **Auth** | 🔒 Autenticado |
| **REQ-IDs** | REQ-LAN-003, REQ-LAN-005 |

**Path Parameters**: `id` (Long — identificador do lance)

**Request Body**:
```json
{ "tipoAmortizacao": "REDUCAO_PRAZO" }
```

**Response `200 OK`**: `CotaResponseDTO`

**Erros**:
| Código | Cenário |
|---|---|
| `400` | Lance não vencedor, modalidade divergente ou tipo ainda bloqueado |
| `422` | Valor do lance excede Fundo Comum pendente |

---

Os endpoints públicos de amortização por `cotaId` e valor livre foram removidos. A modalidade `DILUICAO` é enviada no mesmo endpoint de liquidação, no corpo da requisição.

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
