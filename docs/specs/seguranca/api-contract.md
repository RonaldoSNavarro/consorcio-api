# Contrato de API de Autenticação (Lote 1)

## 1. Endpoints Legados (Mantidos Temporariamente)

### `POST /api/login`
- **Descrição:** Realiza a autenticação legada utilizando usuário e senha da base de dados local.
- **Corpo da Requisição:** `{"login": "...", "senha": "..."}`
- **Resposta Sucesso:** HTTP 200 OK
  - **Headers:** Define `Set-Cookie: token=<JWT_HMAC256>; HttpOnly; SameSite=Strict; Max-Age=7200`
- **Nota de Migração:** Endpoint depreciado; será removido no Lote 3.

### `POST /api/login/logout`
- **Descrição:** Finaliza a sessão legada removendo o cookie.
- **Resposta Sucesso:** HTTP 200 OK
  - **Headers:** Define `Set-Cookie: token=; HttpOnly; SameSite=Strict; Max-Age=0`
- **Nota de Migração:** Endpoint depreciado.

## 2. Novos Endpoints OIDC (ADR 008)

### `GET /api/auth/keycloak-config`
- **Descrição:** Retorna a configuração do Keycloak para que os clients (ex: frontend React) possam inicializar o adapter dinamicamente sem configurações de URL fixas.
- **Autenticação Requerida:** Nenhuma (Público)
- **Resposta de Sucesso:** HTTP 200 OK
  ```json
  {
    "url": "http://localhost:8180",
    "realm": "consorcio",
    "clientId": "consorcio-frontend"
  }
  ```

## 3. Validação de Token e Fluxo OIDC

### Validação OAuth2 Resource Server
A API atua como um Servidor de Recursos OAuth2 e intercepta requisições contendo tokens OIDC RS256:

- **Cabeçalho:** `Authorization: Bearer <TOKEN_RS256>`
- **Validação:** A API valida a assinatura chamando periodicamente a JWKS URI do Keycloak (`/realms/consorcio/protocol/openid-connect/certs`).
- **Extração de Permissões (Claims):**
  A classe `KeycloakJwtConverter` converte o array contido no claim `realm_access.roles` no equivalente com prefixo `ROLE_` no contexto Spring Security, suportando as anotações existentes.

```json
{
  "iss": "http://localhost:8180/realms/consorcio",
  "sub": "UUID",
  "typ": "Bearer",
  "preferred_username": "gestor",
  "realm_access": {
    "roles": ["GESTOR"]
  }
}
```
