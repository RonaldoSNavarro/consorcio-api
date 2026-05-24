# 🏢 Consórcio API

Um sistema back-end de alta performance para administração e gerenciamento de consórcios, desenvolvido em **Java 21** com **Spring Boot 4.0.6** (Spring Framework 7). Este projeto implementa regras de negócio financeiras reais baseadas nas normativas do Banco Central do Brasil (BCB) e no Código de Defesa do Consumidor (CDC).

## 🚀 Tecnologias e Ferramentas

* **Java 21:** Uso de *Virtual Threads* para escalabilidade e *Records* para imutabilidade.
* **Spring Boot 4.0.6:** (Web, Data JPA, Security) com Spring Framework 7 e Hibernate 7.
* **Spring Security 7 & JWT (Auth0 4.5):** Autenticação stateless e autorização baseada em tokens.
* **PostgreSQL:** Persistência de dados financeiros.
* **Flyway 11 (via Starter):** Versionamento e migração de schema do banco de dados.
* **Jackson 3:** Serialização/desserialização JSON de alta performance.
* **MapStruct 1.6:** Mapeamento declarativo entre entidades e DTOs.
* **JUnit 5, Mockito & MockMvc:** Ecossistema completo de testes para garantir a integridade do sistema.
* **Springdoc OpenAPI 3.0 (Swagger):** Documentação interativa personalizada e protegida.
* **Lombok:** Redução de código boilerplate.

## 🏗️ Arquitetura e Padrões de Projeto

* **Arquitetura de DTOs (Records):** Isolamento total das entidades para segurança e estabilidade de contratos.
* **Mapeamento Unidirecional:** Separação clara entre dados de entrada validados e saída filtrada.
* **Global Exception Handling:** Centralização do tratamento de erros com respostas HTTP padronizadas.
* **Inativação Lógica em Conformidade Legal:** A exclusão física de clientes é substituída por inativação lógica para conformidade regulatória (LGPD Art. 16, II & IV, e Resolução 285 do Banco Central do Brasil).
* **Mapeamento Declarativo com MapStruct:** Camada de conversão compilada de alta performance entre entidades e DTOs, garantindo separação rígida e eliminando boilerplate.
* **Bloqueio Otimista (Optimistic Locking):** Prevenção de concorrência e race conditions (condições de corrida) por meio do mapeamento de atributos `@Version` nas tabelas críticas (`cotas`, `grupos` e `parcelas`), impedindo sobrescrita de dados financeiros por requisições concorrentes.
* **Paginação Robusta de Recursos:** Migração de listagens gerais para o padrão `Pageable` do Spring Data nos endpoints de Clientes, Grupos e Cotas, protegendo a memória Heap contra gargalos de volume massivo.

## 🧪 Qualidade de Software e Testes

O projeto segue a pirâmide de testes para garantir que nenhuma alteração no código quebre as regras financeiras. A cobertura foi expandida para **71 testes unitários e de integração 100% verdes**:

* **Testes Unitários (100% Service & Validator Coverage):** Toda a camada de `Service` possui cobertura total. Criamos suítes de teste de baixo nível como `CpfCnpjValidatorTest` e `ViaCepServiceTest` (usando mocks avançados da API fluente do `RestClient` do Spring).
* **Módulos de Auditoria e Livro Razão:** Testados exaustivamente em `MovimentoFinanceiroServiceTest` e `HistoricoConsorciadoServiceTest` para validar consistência de saldos contábeis e a fórmula regulatória da categoria.
* **Testes de Integração (MockMvc):** Validação dos fluxos de entrada e saída (`Controllers`), garantindo que a serialização JSON, as validações de Bean (`@Valid`), os filtros de segurança JWT e os tratamentos do `GlobalExceptionHandler` funcionem perfeitamente.

## 🛡️ Segurança e Autenticação

A proteção da aplicação segue os mais altos padrões de cibersegurança e conformidade regulatória:

* **Sessão Segura com JWT em Cookies HttpOnly:** A autenticação é stateless, mas evita armazenamento inseguro no LocalStorage/SessionStorage do navegador. O token JWT gerado no `/api/login` é retornado através de um cookie HTTP seguro com atributos `HttpOnly`, `Secure` (em produção), `SameSite=Strict` e `Path=/`.
* **Revogação Local (Logout):** Um endpoint dedicado `/api/logout` invalida a sessão local no cliente limpando o cookie com expiração imediata.
* **Filtro de Segurança Adaptativo:** O `SecurityFilter` intercepta requisições lendo o token prioritariamente a partir do cookie HttpOnly `jwt`, mantendo retrocompatibilidade transparente com o cabeçalho tradicional `Authorization: Bearer <token>` para integrações M2M (Machine-to-Machine) ou ferramentas de teste (Postman/Swagger).
* **Proteção Ativa contra IDOR (Insecure Direct Object Reference):** Camada rigorosa de validação de propriedade na camada de serviço (`ClienteService` e `CotaService`). O usuário autenticado só pode obter, visualizar ou modificar recursos associados diretamente à sua identidade (comparando o subject do token com `cpfCnpj` ou `email`). Tentativas de acesso a registros alheios através de IDs sequenciais ou aleatórios são bloqueadas imediatamente, a menos que o usuário possua perfil administrativo (`ROLE_ADMIN`).
* **Segredo JWT:** Gerenciado exclusivamente via variável de ambiente — nenhuma credencial sensível é exposta no código-fonte.

## 🧩 Regras de Negócio e Motores Financeiros

O sistema implementa regras de negócio financeiras extremamente robustas baseadas na **Lei 11.795/2008**, **Circulares BCB 3.432/2009, 285/2022 e 285/2022**, além do **Código de Defesa do Consumidor (CDC)** e da **LGPD**:

### 1. Cadastro de Clientes e Validações de Compliance
* **Integração ViaCEP Autocomplete:** O endpoint `/busca-cep/{cep}` consome a API externa do ViaCEP e preenche automaticamente dados geográficos (`logradouro`, `bairro`, `localidade`, `uf`).
* **Validação Algorítmica CPF/CNPJ:** Filtro customizado `@ValidCpfCnpj` com cálculo mod 11 completo. Rejeita automaticamente documentos com comprimento inválido ou dígitos repetidos em sequência.
* **LGPD Mascaramento de Dados:** Dados sensíveis de retorno (como Nome, CPF/CNPJ, Email e Telefone) são automaticamente mascarados na resposta da API para conformidade absoluta de privacidade.

### 2. Motor Financeiro e Composição de Parcela (4 Componentes)
* **Nova Fórmula de Parcela Base:**
  $$\text{Parcela base} = \text{Fundo Comum} + \text{Taxa de Administração} + \text{Fundo de Reserva} + \text{Seguros}$$
  *(Onde Fundo Comum = valor_credito / prazo)*.
* **Livro Razão (Ledger) do Grupo:** Cada lançamento de entrada e saída é auditado na tabela `movimentos_financeiros` como `CREDITO` ou `DEBITO` da perspectiva do fundo.
* **Prevenção de Colisão Sequencial:** O saldo acumulado é obtido usando ordenação de chave primária sequencial (`findFirstByGrupoIdOrderByIdDesc`) para prevenir falhas de concorrência e timestamp.
* **Reversão Contábil Inversa (Estorno de Parcela):** A reabertura de uma parcela `PAGA` reverte de forma idêntica cada componente original em lançamentos de `DEBITO` inversos, retornando o status para `PENDENTE`.

### 3. Motor de Assembleia, Contemplação e Lances (Double-Entry Ledger)
* **Motor Dinâmico de Lances e Sorteios:** A apuração de assembleias (Status: `AGENDADA`, `CAPTANDO`, `REALIZADA`, `FECHADA`) permite receber ofertas (Firme e Embutido) que são ocultadas até o fim da janela de captação.
* **Cruzamento de Saldo Contábil Real:** O algoritmo de contemplação cruzada bloqueia insolvência de grupos não consultando a "soma isolada de pagamentos" (que mascara despesas antigas). O motor extrai dinamicamente do Ledger o **Saldo Líquido Real** do `2.1.0.01 Fundo Comum` (Créditos - Débitos já faturados) antes de cada conversão de lance vencedor.
* **Lance Embutido Contábil e Teto Percentual:** O valor ofertado é deduzido do crédito a liberar, respeitando o limite teto parametrizado do grupo (ex: máx. 30%), e os recursos contábeis do lance são integralizados e retidos no Fundo Comum.
* **Bloqueio por Inadimplência Legal:** Cotas com qualquer parcela `PENDENTE` em atraso não podem ofertar lances ou participar da apuração da assembleia ativa.

### 4. Versionamento de Cotas (Auditoria de Estados)
* **Versão Incremental:** A cota possui versionamento estruturado.
  - **Versão 0:** Cota em situação cadastral normal (Ativa, Contemplada, etc.).
  - **Versão 1:** Cota excluída ou encaminhada ao jurídico.
* **Rastreabilidade Histórica:** Cada transição de estado da cota (Exclusão, Reingresso, Jurídico) é gravada na tabela `historico_versoes_cota` com o status anterior, novo, motivo da alteração e usuário responsável.

### 5. Histórico e Auditoria do Consorciado (BCB Compliance)
* **Auditoria de Interações:** Snapshots financeiros persistidos para toda e qualquer interação (geração de parcelas, pagamentos, reajustes, contemplações).
* **Fórmula de Valor da Categoria:**
  $$\text{Valor da Categoria} = \text{Parcela base} \times \text{prazo}$$
  Calculado e persistido dinamicamente a cada movimentação para controle e conformidade regulatória perante fiscalizações.

### 6. Bens, Créditos e Reajuste Proporcional
* **Reajuste de Crédito Ativo:** Através do endpoint `PUT /api/grupos/{id}/reajuste`, o administrador pode atualizar o valor de crédito do grupo de consórcio.
* **Rateio de Parcelas em Aberto:** O sistema calcula dinamicamente a variação do crédito e reajusta de forma idêntica e proporcional o `valorFundoComum` de todas as parcelas pendentes (`PENDENTE` ou `ATRASADA`), garantindo que o saldo arrecadado acompanhe a valorização do bem de referência.

### 7. Consolidação Financeira e Encerramento de Grupos
* **Relatório Financeiro Unificado:** O endpoint `GET /api/grupos/{id}/financeiro` calcula de forma agregada a saúde contábil do grupo:
  * **Fundo Comum Arrecadado** e **Saldo Disponível do Fundo Comum** (descontando créditos já pagos).
  * **Taxa de Administração Arrecadada** e **Fundo de Reserva Arrecadado/Disponível**.
* **Encerramento Seguro (Compliance BCB):** O endpoint `POST /api/grupos/{id}/encerrar` impede o encerramento contábil se houver qualquer parcela em aberto (`PENDENTE` ou `ATRASADA`), assegurando quitação absoluta de passivos e ativos.

### 8. Gestão de Inadimplência e Cobrança
* **Cálculo de Penalidades Legais (Circular 3432 & CDC):** Parcelas pagas após a data de vencimento incorporam automaticamente:
  * **Multa de 2.00%** (limite do CDC) calculada sobre o valor total da parcela.
  * **Juros de mora pro-rata die de 1.00% ao mês** calculados diariamente com base nos dias exatos de atraso.
* **Relatório de Débitos da Cota:** O endpoint `GET /api/cotas/{id}/inadimplencia` detalha o histórico, quantidade de atrasos, juros acumulados, multa e o saldo devedor exato de uma cota.

### 9. Cotas Excluídas (Desistentes e Cancelados)
* **Inativação e Exclusão Segura:** Através de `POST /api/cotas/{id}/cancelar`, a cota é desativada (status `CANCELADA`) e todas as suas parcelas pendentes em aberto são sumariamente excluídas para evitar falsas projeções de arrecadação.
* **Reembolso Rescisório com Cláusula Penal (10%):** O endpoint `POST /api/cotas/{id}/reembolsar` calcula a restituição dos valores pagos ao Fundo Comum pelo consorciado excluído, retendo uma **multa rescisória de 10%** (cláusula penal) em conformidade com as jurisprudências e regulamentações do BCB.

### 10. Algoritmos de Amortização
* **Redução de Prazo:** Abate o saldo devedor quitando parcelas de trás para frente (da última parcela retroagindo).
* **Diluição de Valor:** Distribui uniformemente o valor amortizado pelas parcelas pendentes. O motor utiliza a **Regra do Centavo Perdido** para reabsorver discrepâncias e arredondamentos de dízimas na última parcela.

### 11. Análise de Crédito Pós-Contemplação (Retenção e Validação)
* **Retenção de Crédito:** Uma vez contemplada, a cota não gera liberação automática de recursos. O status transita de `ATIVA` para `AGUARDANDO_ANALISE`.
* **Margem Consignável:** Regra alinhada às normativas do BCB: a parcela da cota não pode comprometer mais que **30% da renda comprovada** do consorciado. O cálculo é feito de forma dinâmica recuperando o valor atualizado da Parcela Base.
* **Validação de Garantias:** Exige aprovação explícita (fiador/bens) antes da liberação.
* **Efetivação Contábil:** Apenas quando a análise recebe `status: APROVADA`, a cota avança para o estado `APROVADO` e o `MovimentoFinanceiro` de `LIBERACAO_CREDITO` é registrado no livro razão.

## ⚙️ Configuração e Setup

### Pré-requisitos
* Java 21
* PostgreSQL 16+

### Variáveis de ambiente

Configure as variáveis abaixo no seu ambiente ou no IntelliJ em **Edit Configurations → Environment Variables**. Consulte o `.env.example` na raiz do projeto para referência.

| Variável | Descrição |
|---|---|
| `DB_USER` | Usuário do PostgreSQL |
| `DB_PASSWORD` | Senha do PostgreSQL |
| `JWT_SECRET` | Chave secreta para assinar tokens JWT (mín. 32 caracteres) |

Para gerar um `JWT_SECRET` seguro no PowerShell:
```powershell
[Convert]::ToBase64String((1..32 | ForEach-Object { [byte](Get-Random -Max 256) }))
```

### Banco de dados

O schema é gerenciado pelo **Flyway**. Ao subir a aplicação pela primeira vez, todas as tabelas são criadas automaticamente. Novas alterações de schema devem ser adicionadas como migrações versionadas em `src/main/resources/db/migration` seguindo o padrão `V2__descricao.sql`, `V3__descricao.sql`, etc.

### Rodando o projeto

```bash
./mvnw spring-boot:run
```

## 📖 Documentação da API (Swagger)

A interface foi organizada para refletir o ciclo de vida real de um consórcio:
1.  **Auth:** Login e geração de Token.
2.  **Clientes, Grupos e Cotas:** Configuração da base e vendas.
3.  **Parcelas:** Gestão de cobranças e pagamentos.
4.  **Assembleias e Contemplações:** Eventos de entrega de bens.

> **Acesse em:** `http://localhost:8080/swagger-ui.html`

---
**Projeto desenvolvido por Ronaldo Navarro** - Foco em Engenharia de Back-end e Sistemas Financeiros.