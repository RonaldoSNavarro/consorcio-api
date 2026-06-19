# 📋 Contrato de API — Autenticação e Sessão (auth)

*   **Capability**: auth
*   **Versão**: v1.0 (Baseline Retroativo)
*   **Spec de referência**: [spec.md](spec.md)
*   **Última alteração**: Geração retroativa baseada no código implementado.

---

## 🔐 Autenticação

O endpoint de login é `🔓 Público`. Os demais requerem cookie `HttpOnly` com JWT válido.

---

## 📡 Endpoints

### POST `/api/login`

| Item | Valor |
|---|---|
| **Descrição** | Autentica o operador e retorna JWT em cookie HttpOnly |
| **Auth** | 🔓 Público |
| **REQ-IDs** | REQ-AUTH-001 |

**Request Body** (JSON):
```json
{
  "login": "string — e-mail do operador",
  "senha": "string — senha do operador"
}
```

**Response `200 OK`**: Body vazio. JWT retornado via header `Set-Cookie` com diretivas `HttpOnly`, `SameSite=Strict`, `Path=/`, `MaxAge=7200`.

**Erros**:
| Código | Cenário |
|---|---|
| `401` | Credenciais inválidas (e-mail ou senha incorretos) |

---

### POST `/api/login/logout`

| Item | Valor |
|---|---|
| **Descrição** | Invalida a sessão limpando o cookie de autenticação |
| **Auth** | 🔒 Autenticado |
| **REQ-IDs** | REQ-AUTH-001 |

**Response `200 OK`**: Body vazio. Cookie `token` zerado via `Set-Cookie` com `MaxAge=0`.

---

### GET `/api/login/me`

| Item | Valor |
|---|---|
| **Descrição** | Revalida sessão ativa (F5-safety) e retorna dados do operador logado |
| **Auth** | 🔒 Cookie HttpOnly válido |
| **REQ-IDs** | REQ-AUTH-002 |

**Response `200 OK`** (JSON):
```json
{
  "login": "string — username do operador",
  "role": "string — ROLE_OPERADOR | ROLE_ADMIN",
  "nome": "string — nome completo",
  "email": "string — e-mail do operador"
}
```

**Erros**:
| Código | Cenário |
|---|---|
| `401` | Cookie ausente, JWT expirado ou sessão inválida |

---

## 📐 DTOs de Referência

### Request: `DadosAutenticacao`
```java
public record DadosAutenticacao(String login, String senha) {}
```

### Response: `DadosUsuarioLogado`
```java
public record DadosUsuarioLogado(String login, String role, String nome, String email) {}
```
