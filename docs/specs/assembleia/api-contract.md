# 📋 Contrato de API — Gestão de Assembleias (assembleia)

*   **Capability**: assembleia
*   **Versão**: v1.0 (Baseline Retroativo)
*   **Spec de referência**: [spec.md](spec.md)
*   **Última alteração**: Geração retroativa baseada no código implementado.

---

## 🔐 Autenticação

Todos os endpoints requerem cookie `HttpOnly` com JWT válido.

---

## 📡 Endpoints

### POST `/api/assembleias`

| Item | Valor |
|---|---|
| **Descrição** | Agenda uma nova assembleia vinculada a um grupo |
| **Auth** | 🔒 Autenticado |
| **REQ-IDs** | REQ-ASM-001, REQ-ASM-002 |

**Request Body** (JSON):
```json
{
  "dataAssembleia": "LocalDate — data da assembleia (futuro ou presente)",
  "tipo": "TipoAssembleia — ORDINARIA (default) | EXTRAORDINARIA",
  "grupoId": "Long — ID do grupo (obrigatório, positivo)"
}
```

**Response `201 Created`** (JSON):
```json
{
  "id": "Long",
  "dataAssembleia": "LocalDate",
  "tipo": "TipoAssembleia",
  "grupoId": "Long"
}
```

**Erros**:
| Código | Cenário |
|---|---|
| `400` | Validação de entrada falhou / Já existe assembleia CAPTANDO para o grupo |
| `404` | Grupo não encontrado |

---

### GET `/api/assembleias/grupo/{grupoId}`

| Item | Valor |
|---|---|
| **Descrição** | Lista histórico cronológico de assembleias do grupo |
| **Auth** | 🔒 Autenticado |
| **REQ-IDs** | REQ-ASM-001 |

**Path Parameters**: `grupoId` (Long)

**Response `200 OK`** (JSON Array):
```json
[
  {
    "id": "Long",
    "dataAssembleia": "LocalDate",
    "tipo": "TipoAssembleia",
    "grupoId": "Long"
  }
]
```

---

## 📐 DTOs de Referência

### Request: `AssembleiaRequestDTO`
```java
public record AssembleiaRequestDTO(
    @NotNull @FutureOrPresent LocalDate dataAssembleia,
    TipoAssembleia tipo,
    @NotNull @Positive Long grupoId
) {}
```

### Response: `AssembleiaResponseDTO`
```java
public record AssembleiaResponseDTO(
    Long id, LocalDate dataAssembleia, TipoAssembleia tipo, Long grupoId
) {}
```
