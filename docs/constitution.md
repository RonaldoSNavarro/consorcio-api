# 📜 Constituição do Projeto — Consórcio API

Este documento define os princípios fundamentais, regras arquiteturais inegociáveis, padrões de código e o protocolo de desenvolvimento aplicados a todo o ecossistema do **Consórcio API**. Todos os agentes e desenvolvedores devem seguir estas diretrizes sem exceção.

---

## ⛔ 1. Princípio Central da Fonte Única da Verdade (Single Source of Truth)

1. **A pasta `docs/` é a única fonte da verdade**: Nenhuma linha de código deve ser escrita ou modificada com base em suposições ou históricos de chat fragmentados.
2. **Spec-Driven Development (SDD)**: O ciclo de desenvolvimento exige que a especificação de uma funcionalidade (`spec.md`) seja criada e travada (`LOCKED`) antes do início de qualquer implementação.
3. **Protocolo de Desvio de Especificação (Spec Drift)**:
   - O código reflete a especificação. Se a implementação diverge do spec, o Code Review ou o QA rejeitarão a alteração.
   - Se durante o desenvolvimento, revisão ou teste for detectada uma lacuna, caso de borda omitido ou inconsistência na especificação, **a implementação deve ser interrompida imediatamente**.
   - O spec deve ser reaberto, corrigido (gerando um novo versionamento/PATCH pelo Analista de Sistemas) e aprovado. Apenas após a republicação do spec com status atualizado, o desenvolvedor retomará a implementação.

---

## 🏛️ 2. Arquitetura e Integração

1. **Separação Estrita de Repositórios**:
   - Backend (`consorcio-api`): API REST stateless desenvolvida em Java 21 e Spring Boot 4.0.6.
   - Frontend (`front_end_consorcio-api`): SPA cliente desenvolvido em React 18 e Vite. Não há renderização do lado do servidor (SSR).
2. **Contratos de API como Gates de Planejamento**:
   - Antes do desenvolvimento paralelizável de backend e frontend, o contrato da API (`api-contract.md`) deve ser definido, detalhando endpoints, métodos HTTP, payloads (JSON), cabeçalhos e códigos de retorno de erro.

---

## ⚙️ 3. Padrões de Código Inegociáveis (Backend - Java 21 & Spring Boot 4)

1. **Virtual Threads**: Uso ativo de Virtual Threads para concorrência de I/O. Nenhuma operação bloqueante (I/O, chamadas de banco, conexões externas) deve comprometer a plataforma de threads.
2. **Lógica de Negócio em `@Service`**: Camadas de Controllers, Repositories, Mappers e DTOs devem ser estritamente stateless. Qualquer regra de cálculo, validação regulatória ou contábil deve residir exclusivamente em classes `@Service`.
3. **Java Records**: DTOs de entrada e saída (Request/Response) e value objects imutáveis devem ser implementados como `record`.
4. **Mapeamento Declarativo com MapStruct 1.6**: Conversão de entidades para DTOs e vice-versa deve ser declarativa. Mapeamento manual em classes `@Service` é proibido.
5. **Transacionalidade Explícita**: Operações financeiras complexas (lances, contemplações, baixas de parcelas) devem ter controle de transação explícito (`@Transactional`) com rollback garantido para exceções de runtime.
6. **Tratamento Global de Exceções**: Uso obrigatório de `@ControllerAdvice` para capturar exceções de negócio e retornar formatos de erro padronizados. Stack traces internos (Hibernate, JVM) nunca devem vazar ao cliente.
7. **Migrations Imutáveis**: Banco de dados gerenciado via Flyway 11. Migrations executadas nunca devem ser alteradas. Alterações estruturais exigem uma nova migration incremental.

---

## 🎨 4. Padrões de Código Inegociáveis (Frontend - React 18 & Vite)

1. **Gerenciamento de Estado do Servidor**: Uso absoluto de **TanStack Query (React Query) v5**. É proibido o uso de `useState` ou `useContext` locais para armazenar dados replicados da API.
2. **Invalidação de Cache Imediata**: Toda mutação financeira crítica (lances, contemplações, parcelamento) deve executar a invalidação explícita imediata de cache (`queryClient.invalidateQueries`) para refletir dados atualizados e evitar cliques duplos.
3. **Formulários e Validação Estrita**: Utilização obrigatória de **React Hook Form** integrado com schemas **Zod** para validação no lado do cliente (CPF/CNPJ, e-mail, formato monetário e de data, prevenção de valores negativos).
4. **Proxy do Backend para Serviços de Terceiros**: O frontend nunca deve se comunicar diretamente com APIs externas (como ViaCEP). O autocomplete de endereços deve ser feito via proxy implementado no backend.

---

## 🔒 5. Segurança, LGPD & Compliance

1. **Gestão de Sessão Segura**: O JWT de autenticação deve ser armazenado exclusivamente em cookies HTTP seguros (`HttpOnly = true`, `SameSite = Strict`, `Secure = true` em produção). O frontend não deve persistir tokens no LocalStorage.
2. **Proteção contra IDOR (Insecure Direct Object Reference)**: A camada `@Service` no backend deve validar a propriedade do recurso (comparar o identificador do usuário autenticado no token Spring Security com o proprietário do recurso solicitado) antes de retornar ou modificar os dados, exceto para perfis `ROLE_ADMIN`.
3. **Prevenção de Injeção e Scripts Maliciosos**: Validação rigorosa contra SQL Injection (via JPA parameter binding) e XSS (sanitização de strings no Zod e no backend).
4. **LGPD (Lei Geral de Proteção de Dados)**:
   - Dados pessoais sensíveis do consorciado (CPF, endereço, renda) não devem ser impressos nos logs estruturados do sistema.
   - Logs de nível `INFO` registram apenas eventos de negócio; logs de nível `ERROR` gravam falhas sem exposição de dados pessoais.

---

## 🧮 6. Regras Contábeis Inegociáveis (Ledger)

1. **Partidas Dobradas**: Cada evento financeiro (lances, contemplações, restituições, inadimplência) deve gerar lançamentos contábeis equivalentes de débito e crédito no Razão (`LancamentoContabil`).
2. **Patrimônio de Afetação**: O saldo de caixa de cada grupo de consórcio deve ser segregado contábil e logicamente da administradora, com contas COSIF estruturadas de 8 dígitos.
3. **Regime de Competência**: Taxas de administração e taxas de adesão cobradas pela administradora devem ser reconhecidas em receitas segundo o regime de competência.
