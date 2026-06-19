# 📋 Contrato de API — [Nome da Capability] ([capability])

*   **Capability**: [capability]
*   **Versão**: v1.0
*   **Spec de referência**: [spec.md](../specs/[capability]/spec.md)
*   **Última alteração**: [descrição]

---

## 🔐 Autenticação

Todos os endpoints requerem autenticação via cookie `HttpOnly` contendo JWT válido, exceto quando indicado como `🔓 Público`.

---

## 📡 Endpoints

### [VERBO] `/api/[recurso]`

| Item | Valor |
|---|---|
| **Descrição** | [breve descrição] |
| **Auth** | 🔒 `ROLE_OPERADOR` / `ROLE_ADMIN` |
| **REQ-IDs** | REQ-[CAP]-NNN |

**Request Body** (JSON):
```json
{
  "campo1": "tipo — descrição",
  "campo2": "tipo — descrição"
}
```

**Response `200 OK`** (JSON):
```json
{
  "campo1": "tipo — descrição",
  "campo2": "tipo — descrição"
}
```

**Erros**:
| Código | Cenário |
|---|---|
| `400` | Validação de entrada falhou |
| `401` | Cookie/JWT ausente ou expirado |
| `403` | Sem permissão para o recurso |
| `404` | Recurso não encontrado |
| `409` | Conflito de estado (ex: assembleia já captando) |
| `422` | Regra de negócio violada |

---

## 📐 DTOs de Referência

### Request: `[NomeRequestDTO]`
```java
public record [NomeRequestDTO](
    // campos com anotações de validação
) {}
```

### Response: `[NomeResponseDTO]`
```java
public record [NomeResponseDTO](
    // campos
) {}
```
