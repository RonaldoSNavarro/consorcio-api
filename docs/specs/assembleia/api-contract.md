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
    "grupoId": "Long",
    "status": "StatusAssembleia â€” AGENDADA | CAPTANDO | REALIZADA | FECHADA",
    "numeroSorteado": "Integer — primeiro prêmio da extração usado no sorteio",
    "premioExcluidos": "Integer — segundo prêmio usado para excluídos",
    "numeroExtracaoLoteria": "String — concurso oficial vinculado",
    "algoritmoUsado": "AlgoritmoPedraChave",
    "pedraChaveCalculada": "Integer",
    "fallbacksAplicados": "Integer"
  }
]
```

---

### GET `/api/assembleias/grupo/{grupoId}/status/{status}`

Consulta paginada usada pela Central AGO. `status` é obrigatório (`AGENDADA`, `CAPTANDO`, `REALIZADA` ou `FECHADA`); aceita os parâmetros Spring `page` e `size`. O tamanho padrão é cinco, ordenado por `dataAssembleia`.

**Response `200 OK`**: página Spring com `content`, `totalElements`, `totalPages`, `number` e `size`.

### POST `/api/assembleias/{id}/apurar`

| Item | Valor |
|---|---|
| **Descrição** | Executa a apuração auditável: sorteio oficial, excluídos e lances |
| **Auth** | `MANAGE_GRUPOS` |

A assembleia deve estar em `REALIZADA`, após o encerramento da captação. O motor usa exclusivamente a extração da Loteria Federal mais recente cuja data seja menor ou igual à data da assembleia; ausência de extração elegível retorna `400`. O campo legado `dezenaSorteio` é ignorado e não há fallback aleatório.

```json
{ "realizarSorteio": true }
```

---

## 📐 DTOs de Referência

### POST `/api/assembleias/{id}/abrir-captacao`

| Item | Valor |
|---|---|
| **Descricao** | Abre a janela de lances de uma assembleia pre-agendada |
| **Auth** | `MANAGE_GRUPOS` |

Transita `AGENDADA` para `CAPTANDO` e registra `dataInicioCaptacao`. A chamada repetida para uma assembleia ja `CAPTANDO` e idempotente; se o dado legado nao tiver data inicial, ela e preenchida. Assembleias `REALIZADA` e `FECHADA` retornam `400`.

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
    Long id, LocalDate dataAssembleia, TipoAssembleia tipo, Long grupoId,
    StatusAssembleia status, Integer numeroSorteado, Integer premioExcluidos,
    String numeroExtracaoLoteria, AlgoritmoPedraChave algoritmoUsado,
    Integer pedraChaveCalculada, Integer fallbacksAplicados
) {}
```
