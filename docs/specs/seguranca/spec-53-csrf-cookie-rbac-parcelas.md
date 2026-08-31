# Spec 53 — Emissão de Cookie CSRF para SPAs e Harmonização de RBAC em Parcelas

- **Capability**: seguranca / fundos
- **Versão**: v1.0
- **Status**: SPECIFIED
- **Data**: 2026-08-30
- **Origem**: Incidente HTTP 403 em `PUT /api/parcelas/{id}/pagar` (ADR 017 & ADR 020)

---

## 1. Contexto e Diagnóstico

Ao submeter a requisição mutatória `PUT /api/parcelas/{id}/pagar?dataPagamento=YYYY-MM-DD` a partir do frontend React, o servidor Spring Boot respondeu com `HTTP 403 Forbidden`.

### 1.1 Causas Técnicas
1. **Deferred CSRF Token no Spring Security 6+:** A configuração com `CookieCsrfTokenRepository.withHttpOnlyFalse()` adota carregamento postergado (*lazy/deferred*). Como as rotas de login estão em `ignoringRequestMatchers` e as consultas GET do frontend não acedem explicitamente ao atributo de requisição do `CsrfToken`, o cabeçalho `Set-Cookie: XSRF-TOKEN=...` nunca é emitido para o navegador.
2. **Falha na Injeção do Cabeçalho `X-XSRF-TOKEN` no Cliente HTTP:** O `fetchApi` do SPA lê `getCookie('XSRF-TOKEN')`. Na ausência do cookie, o cabeçalho `X-XSRF-TOKEN` é omitido nas chamadas `PUT`, acionando o bloqueio de segurança do `CsrfFilter`.
3. **Restrição Rígida de RBAC em `ParcelaController`:** Os métodos de `ParcelaController` exigem exclusivamente `hasAuthority('MANAGE_FINANCEIRO')`, rejeitando usuários autenticados sob perfis administrativos (`ADMIN`, `OPERADOR`) que possuam a role no padrão `ROLE_ADMIN`.

---

## 2. Requisitos (REQ-IDs)

| ID | Descrição |
|---|---|
| **REQ-SEG-010** | O backend deve emitir e manter atualizado o cookie `XSRF-TOKEN` (HttpOnly=false) em todas as respostas HTTP para que o frontend SPA possa realizar requisições mutatórias protegidas por CSRF. |
| **REQ-SEG-011** | O Spring Security deve configurar o `CsrfTokenRequestAttributeHandler` com nome de atributo desabilitado (`null`), assegurando que o token CSRF seja exposto tanto como atributo de requisição quanto gravado no cookie `XSRF-TOKEN`. |
| **REQ-AUT-005** | O controlador `ParcelaController` deve aceitar requisições de pagamento, estorno e cadastro de parcelas originadas por usuários com as roles `ADMIN`, `FINANCEIRO` ou `OPERADOR`, além da autoridade explícita `MANAGE_FINANCEIRO`. |
| **REQ-AUT-006** | A consulta de parcelas por cota (`GET /api/parcelas/cota/{cotaId}`) deve aceitar usuários com `VIEW_FINANCEIRO`, `ADMIN`, `FINANCEIRO`, `OPERADOR` e `CONSORCIADO`. |

---

## 3. Regras de Negócio e Segurança (RN-IDs)

- **RN-SEG-010 (Emissão Contínua de CSRF):** O filtro `CsrfCookieFilter` deve ser executado no pipeline do Spring Security após a autenticação, invocando `csrfToken.getToken()` para forçar a escrita do cookie `XSRF-TOKEN` a cada requisição.
- **RN-SEG-011 (Compatibilidade SPA):** Requisições mutatórias (`POST`, `PUT`, `DELETE`, `PATCH`) em rotas protegidas que apresentem cabeçalho `X-XSRF-TOKEN` coincidente com o cookie `XSRF-TOKEN` devem ser aceitas pelo `CsrfFilter`.
- **RN-AUT-010 (RBAC de Gestão Financeira):** Ações financeiras de quitação (`pagar`), reversão (`estornar`) e criação de parcelas exigem `hasAnyRole('ADMIN', 'FINANCEIRO', 'OPERADOR') or hasAnyAuthority('MANAGE_FINANCEIRO', 'ROLE_ADMIN')`.
- **RN-AUT-011 (RBAC de Consulta Financeira):** Ações de leitura de extrato de parcelas exigem `hasAnyRole('ADMIN', 'FINANCEIRO', 'OPERADOR', 'CONSORCIADO') or hasAnyAuthority('VIEW_FINANCEIRO', 'ROLE_ADMIN')`.

---

## 4. Invariantes

1. O mecanismo de autenticação via cookies `HttpOnly` com token JWT (ADR 001 / ADR 012) permanece inviolado e seguro contra XSS.
2. A proteção contra CSRF permanece ativa para todas as rotas da API (exceto `/api/login/**`, `/api/webhooks/**`, Swagger e rotas públicas já catalogadas).
3. Todas as baixas e estornos continuam realizando lançamentos contábeis no padrão COSIF com integridade de partidas dobradas (ADR 002).

---

## 5. Critérios de Aceite (Given / When / Then)

### CA-01: Emissão do Cookie `XSRF-TOKEN` em requisições autenticadas e públicas
- **Given** um cliente HTTP (SPA ou teste) enviando uma requisição GET para qualquer rota (ex: `/api/login/me`, `/api/health`, `/api/cotas/buscar`);
- **When** a resposta HTTP é processada pelo `CsrfCookieFilter`;
- **Then** a resposta deve incluir o cabeçalho `Set-Cookie` com `XSRF-TOKEN=<token>; Path=/; SameSite=Lax` (ou cookie acessível via JavaScript);
- **And** o status HTTP retornado deve ser 200 OK.

### CA-02: Registro de pagamento de parcela com Token CSRF válido e Role ADMIN
- **Given** um usuário autenticado com `ROLE_ADMIN` e um cookie `XSRF-TOKEN` válido;
- **When** o cliente envia `PUT /api/parcelas/{id}/pagar?dataPagamento=2026-08-31` contendo o cabeçalho `X-XSRF-TOKEN` preenchido com o valor do cookie;
- **Then** o Spring Security valida o CSRF e a autorização com sucesso;
- **And** a baixa da parcela é efetuada retornando `HTTP 200 OK` com status `PAGA`.

### CA-03: Bloqueio de pagamento de parcela quando Token CSRF for omitido ou inválido
- **Given** uma requisição `PUT /api/parcelas/{id}/pagar?dataPagamento=2026-08-31` enviada sem o cabeçalho `X-XSRF-TOKEN`;
- **When** o `CsrfFilter` do Spring Security intercepta a chamada;
- **Then** a requisição deve ser rejeitada com `HTTP 403 Forbidden`.

### CA-04: Registro de pagamento de parcela com autoridade funcional `MANAGE_FINANCEIRO`
- **Given** um usuário autenticado com perfil operacional contendo apenas a permissão `MANAGE_FINANCEIRO`;
- **When** o cliente envia `PUT /api/parcelas/{id}/pagar` com o cabeçalho `X-XSRF-TOKEN` correto;
- **Then** o pagamento é processado com sucesso retornando `HTTP 200 OK`.

### CA-05: Estorno de pagamento de parcela com Token CSRF e Role ADMIN
- **Given** uma parcela com status `PAGA` e um usuário com `ROLE_ADMIN`;
- **When** o cliente envia `POST /api/parcelas/{id}/estornar` com o cabeçalho `X-XSRF-TOKEN`;
- **Then** o estorno contábil é realizado com sucesso retornando `HTTP 200 OK` com status `PENDENTE`.

---

## 6. Entidades e Componentes Afetados

1. `consorcio-api`:
   - `br.com.estudo.consorcio.config.CsrfCookieFilter` (NOVO): Filtro para forçar a resolução e escrita do cookie CSRF.
   - `br.com.estudo.consorcio.config.SecurityConfigurations`: Registro do `CsrfTokenRequestAttributeHandler`, `CsrfCookieFilter` e adequação da cadeia de filtros.
   - `br.com.estudo.consorcio.controller.ParcelaController`: Atualização das anotações `@PreAuthorize`.
   - `br.com.estudo.consorcio.controller.ParcelaControllerTest`: Atualização dos testes unitários e de integração de segurança.
2. `docs`:
   - Atualização de `api-contract.md` e `tasks.md`.

---

## 7. Rastreabilidade CA → Teste

| Critério de Aceite | Classe de Teste | Método de Teste |
|---|---|---|
| CA-01 | `SecurityFilterTest` / `CsrfSecurityTest` | `deveEmitirCookieXsrfTokenEmRequisicaoGet()` |
| CA-02 | `ParcelaControllerTest` | `devePagarParcelaComCsrfERoleAdmin()` |
| CA-03 | `ParcelaControllerTest` | `deveRejeitarPagamentoSemTokenCsrf()` |
| CA-04 | `ParcelaControllerTest` | `devePagarParcelaComAuthorityManageFinanceiro()` |
| CA-05 | `ParcelaControllerTest` | `deveEstornarParcelaComCsrfValido()` |
