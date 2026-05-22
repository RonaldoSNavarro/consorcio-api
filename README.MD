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

## 🧪 Qualidade de Software e Testes

O projeto segue a pirâmide de testes para garantir que nenhuma alteração no código quebre as regras financeiras:

* **Testes Unitários (100% Branch Coverage):** Toda a camada de `Service` possui cobertura total de ramificações. Isso garante que cada `if`, `else` e `throw` das regras de negócio foi validado individualmente com **Mockito**.
* **Segurança de Tipos em Testes:** Uso de `@MockitoBean` (Spring Boot 4) e `ReflectionTestUtils` para testes de serviços de infraestrutura como o `TokenService`.
* **Testes de Integração (MockMvc):** Validação dos fluxos de entrada e saída (`Controllers`), garantindo que a serialização JSON, as validações de Bean (`@Valid`) e os filtros de segurança JWT funcionem em conjunto.

## 🛡️ Segurança e Autenticação

A proteção dos recursos financeiros é garantida via **Bearer Token**:
* **Autenticação:** O usuário obtém acesso via `/api/login`.
* **Autorização:** Um `SecurityFilter` customizado valida a assinatura JWT e as permissões antes de liberar o acesso aos `Services`.
* **Segredo JWT:** Gerenciado exclusivamente via variável de ambiente — nenhuma credencial é armazenada no código-fonte.

## 🧩 Regras de Negócio e Motores Financeiros

O sistema implementa regras de negócio financeiras extremamente robustas baseadas na **Lei 11.795/2008**, **Circulares BCB 3.432/2009 e 285/2022**, além do **Código de Defesa do Consumidor (CDC)** e da **LGPD**:

### 1. Motor de Contemplação e Lances
* **Verificação de Saldo de Caixa:** O Fundo Comum do grupo é validado antes de qualquer liberação de crédito, assegurando que o caixa arrecadado (`PAGA`) possua recursos suficientes.
* **Lance Embutido Dinâmico:** Limite parametrizado diretamente no grupo (ex: máximo de 30% do valor do crédito) que desconta o lance ofertado diretamente do crédito a ser liberado.
* **Bloqueio por Inadimplência:** Cotas com qualquer parcela pendente em atraso são sumariamente barradas do sorteio ou lances nas Assembleias Ordinárias.

### 2. Bens, Créditos e Reajuste Proporcional
* **Reajuste de Crédito Ativo:** Através do endpoint `PUT /api/grupos/{id}/reajuste`, o administrador pode atualizar o valor de crédito do grupo de consórcio.
* **Rateio de Parcelas em Aberto:** O sistema calcula dinamicamente a variação do crédito e reajusta de forma idêntica e proporcional o `valorFundoComum` de todas as parcelas pendentes (`PENDENTE` ou `ATRASADA`), garantindo que o saldo arrecadado acompanhe a valorização do bem de referência.

### 3. Consolidação Financeira e Encerramento de Grupos
* **Relatório Financeiro Unificado:** O endpoint `GET /api/grupos/{id}/financeiro` calcula de forma agregada a saúde contábil do grupo:
  * **Fundo Comum Arrecadado** e **Saldo Disponível do Fundo Comum** (descontando créditos já pagos).
  * **Taxa de Administração Arrecadada** e **Fundo de Reserva Arrecadado/Disponível**.
* **Encerramento Seguro (Compliance BCB):** O endpoint `POST /api/grupos/{id}/encerrar` impede o encerramento contábil se houver qualquer parcela em aberto (`PENDENTE` ou `ATRASADA`), assegurando quitação absoluta de passivos e ativos.

### 4. Gestão de Inadimplência e Cobrança
* **Cálculo de Penalidades Legais (Circular 3432 & CDC):** Parcelas pagas após a data de vencimento incorporam automaticamente:
  * **Multa de 2.00%** (limite do CDC) calculada sobre o valor total da parcela.
  * **Juros de mora pro-rata die de 1.00% ao mês** calculados diariamente com base nos dias exatos de atraso.
* **Relatório de Débitos da Cota:** O endpoint `GET /api/cotas/{id}/inadimplencia` detalha o histórico, quantidade de atrasos, juros acumulados, multa e o saldo devedor exato de uma cota.

### 5. Cotas Excluídas (Desistentes e Cancelados)
* **Inativação e Exclusão Segura:** Através de `POST /api/cotas/{id}/cancelar`, a cota é desativada (status `CANCELADA`) e todas as suas parcelas pendentes em aberto são sumariamente excluídas para evitar falsas projeções de arrecadação.
* **Reembolso Rescisório com Cláusula Penal (10%):** O endpoint `POST /api/cotas/{id}/reembolsar` calcula a restituição dos valores pagos ao Fundo Comum pelo consorciado excluído, retendo uma **multa rescisória de 10%** (cláusula penal) em conformidade com as jurisprudências e regulamentações do BCB.

### 6. Algoritmos de Amortização
* **Redução de Prazo:** Abate o saldo devedor quitando parcelas de trás para frente (da última parcela retroagindo).
* **Diluição de Valor:** Distribui uniformemente o valor amortizado pelas parcelas pendentes. O motor utiliza a **Regra do Centavo Perdido** para reabsorver discrepâncias e arredondamentos de dízimas na última parcela.

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