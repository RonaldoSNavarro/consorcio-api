# ðŸ“‚ Contexto do Projeto â€” ConsÃ³rcio API

Este documento descreve o estado atual do desenvolvimento, a stack de referÃªncia aprovada pela arquitetura (Backend & Frontend), as Architecture Decision Records (ADRs) vigentes e as diretrizes tÃ©cnicas inegociÃ¡veis do projeto **ConsÃ³rcio API**.

---

## ðŸ› ï¸� 1. Stack TecnolÃ³gica de ReferÃªncia

### Backend (consorcio-api)
*   **Linguagem:** Java 21 (com uso ativo de Virtual Threads para concorrÃªncia de I/O)
*   **Framework Base:** Spring Boot 4.0.6 / Spring Framework 7
*   **PersistÃªncia:** JPA / Hibernate 7
*   **Banco de Dados:** PostgreSQL (patrimÃ´nio de afetaÃ§Ã£o por grupo via segregaÃ§Ã£o contÃ¡bil)
*   **Migrations:** Flyway 11 (migrations imutÃ¡veis)
*   **SeguranÃ§a:** Spring Security 7 + Auth0 JWT 4.5 (autenticaÃ§Ã£o stateless baseada em claims)
*   **Mapeamento:** MapStruct 1.6 (geraÃ§Ã£o de cÃ³digo em tempo de compilaÃ§Ã£o)
*   **SerializaÃ§Ã£o:** Jackson 3 (suporte nativo a Java Records)
*   **DocumentaÃ§Ã£o:** Springdoc OpenAPI 3.0 (Swagger UI protegido)

### Frontend (front_end_consorcio-api)
*   **Framework:** React 18.3.1 + Vite 5.4.10
*   **Server State & Cache:** TanStack Query (React Query) v5
*   **Gerenciamento de FormulÃ¡rios:** React Hook Form v7
*   **ValidaÃ§Ã£o Client-Side:** Zod v4 (schemas de validaÃ§Ã£o estrita)
*   **Roteamento:** React Router DOM v7
*   **Testes:** Vitest + Testing Library + Happy DOM
*   **EstilizaÃ§Ã£o:** Tailwind CSS (tema escuro HSL harmÃ´nico)

---

## ðŸ’» 2. AnÃ¡lise TÃ©cnica do Frontend

Realizamos uma anÃ¡lise profunda do repositÃ³rio `front_end_consorcio-api` e constatamos a conformidade com as diretrizes do projeto:
1.  **Isolamento do Server State:** O frontend adota de forma absoluta o **TanStack Query** para gerenciar dados da API, tratando chamadas via hooks (`useQuery` e `useMutation`) e executando a invalidaÃ§Ã£o correta do cache do navegador apÃ³s operaÃ§Ãµes mutÃ¡veis (`queryClient.invalidateQueries`).
2.  **ValidaÃ§Ã£o Estrita de FormulÃ¡rios:** FormulÃ¡rios como `ClienteForm` e `CotaForm` utilizam o resolver do Zod (`zodResolver`) com esquemas rÃ­gidos (`clienteSchema`) que validam formato de CPF/CNPJ, e-mail e impedem valores negativos no lado do cliente.
3.  **Proxy ViaCEP Integrado:** O autocomplete de endereÃ§os consome a rota do backend `/api/clientes/busca-cep/{cep}` em vez de chamar o ViaCEP diretamente do navegador, mantendo conformidade de seguranÃ§a.
4.  **Uso de Credenciais Compartilhadas:** RequisiÃ§Ãµes no `api.js` utilizam a diretiva `credentials: 'include'`, permitindo que o cookie seguro contendo o JWT seja automaticamente transmitido em todas as chamadas.

### ðŸ”´ Vulnerabilidade Identificada (SessÃ£o & F5)
O `AuthContext.jsx` inicializa o token a partir do `localStorage.getItem('consorcio_api_token')`. Contudo, por diretrizes de seguranÃ§a de sessÃµes reais, o JWT Ã© armazenado em Cookies HttpOnly e nunca exposto ao LocalStorage. 
*   **Impacto:** Se o usuÃ¡rio atualizar a pÃ¡gina (F5), o estado do React Ã© redefinido. Como a chave `'consorcio_api_token'` nÃ£o Ã© de fato persistida no localStorage no fluxo real (visando mitigar roubos de sessÃ£o via XSS), o frontend inicializa o token como `null` e redireciona indevidamente o operador para a tela de `/login`, mesmo que o cookie HTTP seguro da sessÃ£o ainda esteja ativo no navegador.
*   **SoluÃ§Ã£o:** ImplementaÃ§Ã£o do mecanismo de validaÃ§Ã£o ativa (F5-safety) detalhado na **ADR 007**.

---

## ðŸ�›ï¸� 3. Registro de DecisÃµes de Arquitetura (ADRs Vigentes)

### ADR 001: GestÃ£o de SessÃ£o via Cookies HttpOnly e SameSite
*   **Contexto:** Armazenar tokens JWT no LocalStorage do navegador expÃµe a aplicaÃ§Ã£o a ataques de roubo de sessÃ£o via Cross-Site Scripting (XSS).
*   **DecisÃ£o:** O backend de autenticaÃ§Ã£o retorna o token JWT encapsulado em um cookie HTTP seguro com as diretivas `HttpOnly = true`, `SameSite = Strict`, `Secure = true` (em prod) e escopo limitado (`Path = /`). O `SecurityFilter` lÃª o token a partir deste cookie e suporta fallback via header `Authorization: Bearer <token>` para integraÃ§Ãµes externas de API e Swagger.

### ADR 002: Auditoria ContÃ¡bil de Dupla Entrada (Ledger)
*   **Contexto:** Eventos crÃ­ticos de caixa (lances, contemplaÃ§Ãµes e liquidaÃ§Ã£o de parcelas) nÃ£o podem ser tratados como operaÃ§Ãµes simples de CRUD (que geram vulnerabilidades de concorrÃªncia e perdas de rastro auditÃ¡vel).
*   **DecisÃ£o:** Todo evento financeiro que gera impacto no caixa do grupo gera lanÃ§amentos contÃ¡beis equivalentes de dÃ©bito e crÃ©dito no RazÃ£o (`LancamentoContabil`). O saldo de caixa de qualquer conta Ã© apurado agregando os dÃ©bitos e crÃ©ditos indexados com base na natureza contÃ¡bil COSIF de 8 dÃ­gitos.

### ADR 003: ValidaÃ§Ã£o contra Vulnerabilidade IDOR na Camada de ServiÃ§o
*   **Contexto:** ParÃ¢metros de requisiÃ§Ã£o HTTP contendo IDs numÃ©ricos sequenciais expÃµem a API a consultas nÃ£o autorizadas caso o usuÃ¡rio altere o ID na requisiÃ§Ã£o (IDOR).
*   **DecisÃ£o:** A camada de `@Service` executa checagem de propriedade antes de retornar qualquer dado. O e-mail/CPF no token do Spring Security Ã© comparado com os dados da cota/cliente consultados. A consulta de IDs alheios sÃ³ Ã© deferida para contas com permissÃ£o `ROLE_ADMIN`.

### ADR 004: HomologaÃ§Ã£o de Lance Livre via Status IntermediÃ¡rio (Pendente de IntegralizaÃ§Ã£o)
*   **Contexto:** A apuraÃ§Ã£o atual homologa lances livres e contempla a cota de imediato na assembleia. Lances livres reais exigem um prazo de pagamento de 2 a 5 dias para integralizaÃ§Ã£o fÃ­sica dos recursos no banco. Liberar o crÃ©dito antes do recebimento expÃµe o grupo a quebra de caixa.
*   **DecisÃ£o:** Introduziremos o status intermediÃ¡rio `PENDENTE_INTEGRALIZACAO` na contemplaÃ§Ã£o por lance livre. A cota sÃ³ avanÃ§a para `AGUARDANDO_ANALISE` e o crÃ©dito transita para `CrÃ©ditos a Liberar` apÃ³s a compensaÃ§Ã£o bancÃ¡ria do lance.
*   **ConsequÃªncia:** EliminaÃ§Ã£o total do risco de inadimplÃªncia de lances durante a assembleia ordinÃ¡ria.

### ADR 005: Reajuste e RestituiÃ§Ã£o Legal de ExcluÃ­dos com Base no Valor do Bem Atualizado
*   **Contexto:** O cÃ¡lculo atual de restituiÃ§Ã£o a consorciados excluÃ­dos devolve o valor nominal histÃ³rico pago ao fundo comum. O Artigo 30 da Lei 11.795/08 estabelece que a devoluÃ§Ã£o deve basear-se no percentual amortizado aplicado sobre o valor do bem de referÃªncia atualizado na AGO de contemplaÃ§Ã£o.
*   **DecisÃ£o:** Atualizaremos a fÃ³rmula de restituiÃ§Ã£o de cotas `CANCELADA`. O sistema obterÃ¡ o percentual acumulado amortizado, aplicarÃ¡ sobre o preÃ§o reajustado vigente do bem na assembleia que sorteou a cota excluÃ­da, e descontarÃ¡ a multa rescisÃ³ria de 10% (clÃ¡usula penal).
*   **ConsequÃªncia:** EliminaÃ§Ã£o de passivos e litÃ­gios judiciais por devoluÃ§Ãµes subfaturadas.

### ADR 006: Encerramento de Grupo com Baixa de InadimplÃªncia e ProvisÃ£o de Perdas
*   **Contexto:** O sistema impede o encerramento do grupo se houver qualquer parcela em aberto. O regulamento do BCB obriga o encerramento no prazo de 120 dias da Ãºltima assembleia ordinÃ¡ria.
*   **DecisÃ£o:** Permitiremos o encerramento contÃ¡bil no prazo legal. Parcelas inadimplentes ativas pendentes de cobranÃ§a serÃ£o baixadas do balancete consolidado do grupo (gerando provisÃ£o de devedores/perdas) e enviadas para cobranÃ§a judicial extraordinÃ¡ria.
*   **ConsequÃªncia:** Cumprimento rigoroso do prazo do BCB de 120 dias sem impedir o fluxo legal do encerramento.

### ADR 007: GestÃ£o de SessÃ£o no Frontend via VerificaÃ§Ã£o Ativa (F5-safety)
*   **Contexto:** A inicializaÃ§Ã£o do estado de login no frontend via localStorage falha sob a seguranÃ§a do JWT HttpOnly Cookie, causando redirecionamento indesejado ao login ao atualizar a pÃ¡gina (F5).
*   **DecisÃ£o:** Criaremos o endpoint `/api/auth/me` no backend para validar o cookie da sessÃ£o em tempo real. No `AuthContext.jsx`, a verificaÃ§Ã£o inicial farÃ¡ uma requisiÃ§Ã£o ativa a esse endpoint. Se o backend validar o cookie e retornar os dados do usuÃ¡rio, o token em memÃ³ria serÃ¡ definido como `"cookie_managed"` e o operador permanecerÃ¡ logado de forma fluida.
*   **ConsequÃªncia:** CorreÃ§Ã£o definitiva do bug de F5, mantendo o nÃ­vel de seguranÃ§a exigido (sem persistÃªncia de JWT no localstorage).

### ADR 008: Integração OIDC com Keycloak (Resource Server em Bridge Mode)
*   **Contexto:** A aplicação de estudos utilizava geração de token JWT estática local (HMAC256). Práticas de mercado exigem centralização de identidade e gestão profissional de acessos em um IdP (Identity Provider) externo.
*   **Decisão:** O backend evoluiu para um OAuth2 Resource Server. Os tokens (RS256) emitidos pelo servidor Keycloak (conteinerizado e conectado a um banco isolado) são validados assimetricamente via JWKS URI.
*   **Consequência:** Centralização do cadastro e ciclo de vida de usuários. Compatibilidade mantida através de um `BearerTokenResolver` customizado para não quebrar integrações legadas em modo Bridge.

### ADR 009: Delegação de Autorização por Escopo (OAuth2 Scopes)
*   **Contexto:** RBAC rígido usando `hasRole()` no backend limitava a flexibilidade para aplicações frontend de terceiros.
*   **Decisão:** O frontend passará a atuar como um *Public Client* usando Authorization Code Flow com PKCE (Lote 2). A autorização baseada puramente em papéis será complementada/substituída por escopos concedidos (`SCOPE_read:cotas`, `SCOPE_write:lances`).
*   **Consequência:** Aumento exponencial da segurança da SPA, eliminando senhas e cookies locais legados em favor de fluxos redirecionados seguros.

### ADR 010: Controle Granular de Propriedade ABAC (@OwnershipGuard)
*   **Contexto:** Controles IDOR globais na camada de Serviço (ADR 003) se tornaram difíceis de escalar para múltiplos domínios (Cotas, Lances, Assembleias).
*   **Decisão:** Centralizar a checagem de propriedade de recursos na anotação de segurança do Spring (`@PreAuthorize("@ownershipGuard.isOwner(authentication, #id)")`), operando puramente como ABAC (Attribute-Based Access Control).
*   **Consequência:** Código de negócio purgado de lógica de verificação de permissões, aumentando coesão técnica.

### ADR 011: Observabilidade de Segurança e Audit Trail
*   **Contexto:** Ações de negócio críticas ocorriam sem rastro forense ou auditoria formal, impossibilitando accountability em caso de incidentes de compliance.
*   **Decisão:** Implementação de Logs de Auditoria injetando metadados estruturados (IP, CPF/Username do JWT, Horário, Ação e Recurso).
*   **Consequência:** Conformidade elevada a padrões institucionais e regulatórios estritos, permitindo análise retroativa por analistas SOC/Compliance.

---

## ðŸ“ˆ 4. Estado Atual do Projeto

- **Fase Atual:** Projeto Integrado e Estabilizado (Esteira de Vendas CRM/Comercial 100% finalizada e OtimizaÃ§Ãµes de Performance aplicadas).
- **Status:** Todas as 13 exigÃªncias regulatÃ³rias (GAPs) da `consorcio-brasil` foram integradas Ã  API e testadas com sucesso via Frontend real em modo de ProduÃ§Ã£o (Zero Mocks). A capability de **Vendas (Proposta -> Contrato -> Cota)** foi validada de ponta a ponta. Adotamos o uso hÃ­brido de DTOs Projections (Spring Data JPA) e *Materialized Views* (PostgreSQL via Flyway V45) para aniquilar gargalos de Fetch N+1 nas Queries de totalizaÃ§Ã£o financeira. A API estÃ¡ agora 100% aderente Ã s diretrizes tÃ©cnicas da Lei 11.795/08 e Circular/ResoluÃ§Ã£o BACEN. O sistema atingiu a sua maturidade funcional absoluta.
- **Artefatos Gerados:** 
  - [constitution.md](file:///f:/Dev/Projetos/consorcio-api/docs/constitution.md) (PrincÃ­pios e regras tÃ©cnicas inegociÃ¡veis).
  - [REQUIREMENTS.md](file:///f:/Dev/Projetos/consorcio-api/docs/REQUIREMENTS.md) (Ã�ndice geral e modelos de dados compartilhados).
  - [PROJECT_CONTEXT.md](file:///f:/Dev/Projetos/consorcio-api/docs/PROJECT_CONTEXT.md) (Este documento).
  - [specs/](file:///f:/Dev/Projetos/consorcio-api/docs/specs/) (DiretÃ³rio contendo as 10 especificaÃ§Ãµes modulares â€” cada uma com `spec.md`, `api-contract.md` e `tasks.md`).
  - [atas/](file:///f:/Dev/Projetos/consorcio-api/docs/atas/) (Registro cronolÃ³gico das retrospectivas de sprint).
  - [templates/](file:///f:/Dev/Projetos/consorcio-api/docs/templates/) (Templates padronizados para spec, api-contract e tasks).
  - [traceability-matrix.md](file:///f:/Dev/Projetos/consorcio-api/docs/traceability-matrix.md) (Matriz de rastreabilidade REQ-ID â†’ CÃ³digo â†’ Teste).

---

## âš ï¸� PadrÃµes de CÃ³digo InegociÃ¡veis

> Para a lista completa e detalhada de padrÃµes inegociÃ¡veis, consulte [constitution.md](file:///f:/Dev/Projetos/consorcio-api/docs/constitution.md) (SeÃ§Ãµes 3, 4, 5 e 6).

## ? Fases do Projeto

* **Fase 1:** Backend Estrutural e Autenticação ?
* **Fase 2:** Domínio de Consórcios e Orquestração (BCB) ?
* **Fase 3:** Frontend SPA e Automação de QA E2E (Playwright) ?

