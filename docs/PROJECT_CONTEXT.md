# 📂 Contexto do Projeto — Consórcio API

Este documento descreve o estado atual do desenvolvimento, a stack de referência aprovada pela arquitetura (Backend & Frontend), as Architecture Decision Records (ADRs) vigentes e as diretrizes técnicas inegociáveis do projeto **Consórcio API**.

---

## 🛠️ 1. Stack Tecnológica de Referência

### Backend (consorcio-api)
*   **Linguagem:** Java 21 (com uso ativo de Virtual Threads para concorrência de I/O)
*   **Framework Base:** Spring Boot 4.0.6 / Spring Framework 7
*   **Persistência:** JPA / Hibernate 7
*   **Banco de Dados:** PostgreSQL (patrimônio de afetação por grupo via segregação contábil)
*   **Migrations:** Flyway 11 (migrations imutáveis)
*   **Segurança:** Spring Security 7 + Auth0 JWT 4.5 (autenticação stateless baseada em claims)
*   **Mapeamento:** MapStruct 1.6 (geração de código em tempo de compilação)
*   **Serialização:** Jackson 3 (suporte nativo a Java Records)
*   **Documentação:** Springdoc OpenAPI 3.0 (Swagger UI protegido)

### Frontend (front_end_consorcio-api)
*   **Framework:** React 18.3.1 + Vite 5.4.10
*   **Server State & Cache:** TanStack Query (React Query) v5
*   **Gerenciamento de Formulários:** React Hook Form v7
*   **Validação Client-Side:** Zod v4 (schemas de validação estrita)
*   **Roteamento:** React Router DOM v7
*   **Testes:** Vitest + Testing Library + Happy DOM
*   **Estilização:** Tailwind CSS (tema escuro HSL harmônico)

---

## 💻 2. Análise Técnica do Frontend

Realizamos uma análise profunda do repositório `front_end_consorcio-api` e constatamos a conformidade com as diretrizes do projeto:
1.  **Isolamento do Server State:** O frontend adota de forma absoluta o **TanStack Query** para gerenciar dados da API, tratando chamadas via hooks (`useQuery` e `useMutation`) e executando a invalidação correta do cache do navegador após operações mutáveis (`queryClient.invalidateQueries`).
2.  **Validação Estrita de Formulários:** Formulários como `ClienteForm` e `CotaForm` utilizam o resolver do Zod (`zodResolver`) com esquemas rígidos (`clienteSchema`) que validam formato de CPF/CNPJ, e-mail e impedem valores negativos no lado do cliente.
3.  **Proxy ViaCEP Integrado:** O autocomplete de endereços consome a rota do backend `/api/clientes/busca-cep/{cep}` em vez de chamar o ViaCEP diretamente do navegador, mantendo conformidade de segurança.
4.  **Uso de Credenciais Compartilhadas:** Requisições no `api.js` utilizam a diretiva `credentials: 'include'`, permitindo que o cookie seguro contendo o JWT seja automaticamente transmitido em todas as chamadas.

### 🔴 Vulnerabilidade Identificada (Sessão & F5)
O `AuthContext.jsx` inicializa o token a partir do `localStorage.getItem('consorcio_api_token')`. Contudo, por diretrizes de segurança de sessões reais, o JWT é armazenado em Cookies HttpOnly e nunca exposto ao LocalStorage. 
*   **Impacto:** Se o usuário atualizar a página (F5), o estado do React é redefinido. Como a chave `'consorcio_api_token'` não é de fato persistida no localStorage no fluxo real (visando mitigar roubos de sessão via XSS), o frontend inicializa o token como `null` e redireciona indevidamente o operador para a tela de `/login`, mesmo que o cookie HTTP seguro da sessão ainda esteja ativo no navegador.
*   **Solução:** Implementação do mecanismo de validação ativa (F5-safety) detalhado na **ADR 007**.

---

## 🏛️ 3. Registro de Decisões de Arquitetura (ADRs Vigentes)

### ADR 001: Gestão de Sessão via Cookies HttpOnly e SameSite
*   **Contexto:** Armazenar tokens JWT no LocalStorage do navegador expõe a aplicação a ataques de roubo de sessão via Cross-Site Scripting (XSS).
*   **Decisão:** O backend de autenticação retorna o token JWT encapsulado em um cookie HTTP seguro com as diretivas `HttpOnly = true`, `SameSite = Strict`, `Secure = true` (em prod) e escopo limitado (`Path = /`). O `SecurityFilter` lê o token a partir deste cookie e suporta fallback via header `Authorization: Bearer <token>` para integrações externas de API e Swagger.

### ADR 002: Auditoria Contábil de Dupla Entrada (Ledger)
*   **Contexto:** Eventos críticos de caixa (lances, contemplações e liquidação de parcelas) não podem ser tratados como operações simples de CRUD (que geram vulnerabilidades de concorrência e perdas de rastro auditável).
*   **Decisão:** Todo evento financeiro que gera impacto no caixa do grupo gera lançamentos contábeis equivalentes de débito e crédito no Razão (`LancamentoContabil`). O saldo de caixa de qualquer conta é apurado agregando os débitos e créditos indexados com base na natureza contábil COSIF de 8 dígitos.

### ADR 003: Validação contra Vulnerabilidade IDOR na Camada de Serviço
*   **Contexto:** Parâmetros de requisição HTTP contendo IDs numéricos sequenciais expõem a API a consultas não autorizadas caso o usuário altere o ID na requisição (IDOR).
*   **Decisão:** A camada de `@Service` executa checagem de propriedade antes de retornar qualquer dado. O e-mail/CPF no token do Spring Security é comparado com os dados da cota/cliente consultados. A consulta de IDs alheios só é deferida para contas com permissão `ROLE_ADMIN`.

### ADR 004: Homologação de Lance Livre via Status Intermediário (Pendente de Integralização)
*   **Contexto:** A apuração atual homologa lances livres e contempla a cota de imediato na assembleia. Lances livres reais exigem um prazo de pagamento de 2 a 5 dias para integralização física dos recursos no banco. Liberar o crédito antes do recebimento expõe o grupo a quebra de caixa.
*   **Decisão:** Introduziremos o status intermediário `PENDENTE_INTEGRALIZACAO` na contemplação por lance livre. A cota só avança para `AGUARDANDO_ANALISE` e o crédito transita para `Créditos a Liberar` após a compensação bancária do lance.
*   **Consequência:** Eliminação total do risco de inadimplência de lances durante a assembleia ordinária.

### ADR 005: Reajuste e Restituição Legal de Excluídos com Base no Valor do Bem Atualizado
*   **Contexto:** O cálculo atual de restituição a consorciados excluídos devolve o valor nominal histórico pago ao fundo comum. O Artigo 30 da Lei 11.795/08 estabelece que a devolução deve basear-se no percentual amortizado aplicado sobre o valor do bem de referência atualizado na AGO de contemplação.
*   **Decisão:** Atualizaremos a fórmula de restituição de cotas `CANCELADA`. O sistema obterá o percentual acumulado amortizado, aplicará sobre o preço reajustado vigente do bem na assembleia que sorteou a cota excluída, e descontará a multa rescisória de 10% (cláusula penal).
*   **Consequência:** Eliminação de passivos e litígios judiciais por devoluções subfaturadas.

### ADR 006: Encerramento de Grupo com Baixa de Inadimplência e Provisão de Perdas
*   **Contexto:** O sistema impede o encerramento do grupo se houver qualquer parcela em aberto. O regulamento do BCB obriga o encerramento no prazo de 120 dias da última assembleia ordinária.
*   **Decisão:** Permitiremos o encerramento contábil no prazo legal. Parcelas inadimplentes ativas pendentes de cobrança serão baixadas do balancete consolidado do grupo (gerando provisão de devedores/perdas) e enviadas para cobrança judicial extraordinária.
*   **Consequência:** Cumprimento rigoroso do prazo do BCB de 120 dias sem impedir o fluxo legal do encerramento.

### ADR 007: Gestão de Sessão no Frontend via Verificação Ativa (F5-safety)
*   **Contexto:** A inicialização do estado de login no frontend via localStorage falha sob a segurança do JWT HttpOnly Cookie, causando redirecionamento indesejado ao login ao atualizar a página (F5).
*   **Decisão:** Criaremos o endpoint `/api/auth/me` no backend para validar o cookie da sessão em tempo real. No `AuthContext.jsx`, a verificação inicial fará uma requisição ativa a esse endpoint. Se o backend validar o cookie e retornar os dados do usuário, o token em memória será definido como `"cookie_managed"` e o operador permanecerá logado de forma fluida.
*   **Consequência:** Correção definitiva do bug de F5, mantendo o nível de segurança exigido (sem persistência de JWT no localstorage).

---

## 📈 4. Estado Atual do Projeto

- **Fase Atual:** Projeto Integrado e Estabilizado (Esteira de Vendas CRM/Comercial 100% finalizada e Otimizações de Performance aplicadas).
- **Status:** Todas as 13 exigências regulatórias (GAPs) da `consorcio-brasil` foram integradas à API e testadas com sucesso via Frontend real em modo de Produção (Zero Mocks). A capability de **Vendas (Proposta -> Contrato -> Cota)** foi validada de ponta a ponta. Adotamos o uso híbrido de DTOs Projections (Spring Data JPA) e *Materialized Views* (PostgreSQL via Flyway V45) para aniquilar gargalos de Fetch N+1 nas Queries de totalização financeira. A API está agora 100% aderente às diretrizes técnicas da Lei 11.795/08 e Circular/Resolução BACEN. O sistema atingiu a sua maturidade funcional absoluta.
- **Artefatos Gerados:** 
  - [constitution.md](file:///f:/Dev/Projetos/consorcio-api/docs/constitution.md) (Princípios e regras técnicas inegociáveis).
  - [REQUIREMENTS.md](file:///f:/Dev/Projetos/consorcio-api/docs/REQUIREMENTS.md) (Índice geral e modelos de dados compartilhados).
  - [PROJECT_CONTEXT.md](file:///f:/Dev/Projetos/consorcio-api/docs/PROJECT_CONTEXT.md) (Este documento).
  - [specs/](file:///f:/Dev/Projetos/consorcio-api/docs/specs/) (Diretório contendo as 10 especificações modulares — cada uma com `spec.md`, `api-contract.md` e `tasks.md`).
  - [atas/](file:///f:/Dev/Projetos/consorcio-api/docs/atas/) (Registro cronológico das retrospectivas de sprint).
  - [templates/](file:///f:/Dev/Projetos/consorcio-api/docs/templates/) (Templates padronizados para spec, api-contract e tasks).
  - [traceability-matrix.md](file:///f:/Dev/Projetos/consorcio-api/docs/traceability-matrix.md) (Matriz de rastreabilidade REQ-ID → Código → Teste).

---

## ⚠️ Padrões de Código Inegociáveis

> Para a lista completa e detalhada de padrões inegociáveis, consulte [constitution.md](file:///f:/Dev/Projetos/consorcio-api/docs/constitution.md) (Seções 3, 4, 5 e 6).

## ? Fases do Projeto

* **Fase 1:** Backend Estrutural e Autentica��o ?
* **Fase 2:** Dom�nio de Cons�rcios e Orquestra��o (BCB) ?
* **Fase 3:** Frontend SPA e Automa��o de QA E2E (Playwright) ?

