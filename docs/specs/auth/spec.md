# 📋 Especificação Funcional — Autenticação e Sessão (auth)

*   **Status**: IMPLEMENTED
*   **Versão**: v1.0 (Baseline Retroativo)
*   **Aprovações**: Especialista Consórcios [✅] (2026-06-14) | Especialista Contabilidade [✅] (2026-06-14)
*   **Última alteração**: Criação do baseline retroativo incorporando a revalidação ativa de login (F5-safety).

---

## 🎯 Objetivo
Garantir o acesso seguro de operadores de consórcio ao painel de gerenciamento (backoffice) por meio de autenticação stateless baseada em JWT, utilizando cookies HttpOnly e mitigando vulnerabilidades de sessões.

---

## 🧮 Regras de Negócio e Segurança

### REQ-AUTH-001: Autenticação Stateless e Emissão de Token
- **Regra**: O sistema autentica o usuário via credenciais (e-mail/senha). Em caso de sucesso, o backend gera um token JWT contendo claims de identificação (e-mail, nome) e perfis de acesso (Roles: `ROLE_OPERADOR`, `ROLE_ADMIN`).
- **Segurança**: O token JWT é retornado ao frontend encapsulado em um cookie HTTP seguro com as diretivas:
  - `HttpOnly = true` (bloqueia leitura via Javascript/XSS)
  - `SameSite = Strict` (mitiga CSRF)
  - `Secure = true` (ativo em ambiente de produção)
  - `Path = /` (disponível para todas as rotas da API)
- **Fallback**: Suporte à leitura do JWT a partir do cabeçalho HTTP `Authorization: Bearer <token>` apenas para documentação OpenAPI (Swagger UI) e integrações de microsserviços externos.

### REQ-AUTH-002: Revalidação Ativa de Sessão (F5-safety)
- **Regra**: Para contornar a limitação de não armazenar o token JWT no `localStorage` do navegador, o backend expõe o endpoint `/api/auth/me`.
- **Funcionamento**: Ao atualizar a página (F5), o frontend SPA realiza uma requisição ativa ao `/api/auth/me` transmitindo o cookie da sessão. O backend decodifica o cookie, valida o JWT e retorna os metadados do operador autenticado. Se o cookie for inválido ou ausente, a rota redireciona para `/login`.

---

## 🎯 Critérios de Aceitação (Given/When/Then)

### REQ-AUTH-001 - AC1: Emissão de Cookie de Autenticação
- **Given**: Um operador cadastrado no sistema.
- **When**: O operador envia uma requisição `POST` válida para `/api/auth/login` com e-mail e senha.
- **Then**: O sistema responde com status `200 OK` e insere o cabeçalho `Set-Cookie` contendo o JWT com as diretivas `HttpOnly`, `SameSite=Strict`, e sem expor o JWT no payload da resposta.

### REQ-AUTH-001 - AC2: Falha nas Credenciais
- **Given**: Uma tentativa de autenticação com dados inválidos.
- **When**: O cliente envia uma requisição `POST` para `/api/auth/login` com senha ou e-mail incorretos.
- **Then**: O sistema responde com status `401 Unauthorized` e um JSON de erro padronizado pelo `@ControllerAdvice`.

### REQ-AUTH-002 - AC1: Revalidação de Sessão Válida (F5-safety)
- **Given**: Um operador cuja sessão está ativa (cookie HTTP presente e JWT não expirado).
- **When**: O frontend SPA inicia e envia uma requisição `GET` para `/api/auth/me`.
- **Then**: O backend responde com status `200 OK` contendo os dados do operador (nome, e-mail e roles) permitindo que o frontend defina o estado do operador como ativo.

### REQ-AUTH-002 - AC2: Revalidação de Sessão Expirada ou Inexistente
- **Given**: Um usuário acessando a aplicação sem um cookie ativo ou com token expirado.
- **When**: O frontend tenta validar a sessão no `/api/auth/me`.
- **Then**: O backend responde com status `401 Unauthorized` e o frontend redireciona o fluxo para a tela `/login`.
