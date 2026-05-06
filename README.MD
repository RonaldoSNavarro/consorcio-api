# 🏢 Consórcio API

Um sistema back-end completo para administração e gerenciamento de consórcios, desenvolvido em Java com Spring Boot. **Este projeto implementa regras de negócio financeiras reais baseadas nas normativas do Banco Central do Brasil (BCB).**

## 🚀 Tecnologias e Ferramentas

* **Java 21**
* **Spring Boot 4** (Web, Data JPA)
* **PostgreSQL** (Banco de Dados Relacional)
* **Hibernate** (Mapeamento Objeto-Relacional)
* **Maven** (Gerenciador de Dependências)
* **Git e GitHub** (Versionamento de Código)

## 🧩 Arquitetura do Domínio e Regras de Negócio (BCB)

O núcleo do sistema gerencia o ciclo de vida completo de um consórcio, respeitando regras de integridade transacional e travas financeiras:

* **Cliente:** Cadastros blindados com validação de unicidade para CPF/CNPJ e E-mail.
* **Grupo:** Agrupamento financeiro de cotas. Todo grupo nasce com o status `EM_FORMACAO` e seu status evolui para `EM_ANDAMENTO` apenas quando ocorre a inauguração oficial na 1ª Assembleia Geral Ordinária (AGO).
* **Cota:** A ligação que vincula um Cliente a um Grupo específico. Possui travas de estado (`ATIVA`, `CONTEMPLADA`, `CANCELADA`, `INADIMPLENTE`).
* **Parcela:** Controle financeiro fracionado. O valor pago pelo cliente é dividido automaticamente (via `@PrePersist`/`@PreUpdate`) em **Fundo Comum**, **Taxa de Administração** e **Fundo de Reserva**, contabilizando o caixa real do grupo.
* **Assembleia:** Eventos oficiais do grupo, classificados como `ORDINARIA` (AGO) ou `EXTRAORDINARIA` (AGE).
* **Contemplação:** Motor principal do sistema com **trava de integridade financeira**. O sistema varre o banco de dados somando o *Fundo Comum* de todas as parcelas pagas do grupo e bloqueia a contemplação (retornando erro amigável ao usuário) caso o saldo em caixa seja inferior ao valor do crédito exigido pela cota.

## 🛡️ Tratamento de Exceções (Controller Advice)
A API conta com um `GlobalExceptionHandler` que intercepta quebras de regras de negócio (ex: tentativa de contemplação sem saldo) e converte erros internos em respostas HTTP 400 (Bad Request) com payloads JSON padronizados, garantindo segurança e clareza para a aplicação cliente.

## ⚙️ Como rodar o projeto localmente

### 1. Pré-requisitos
* Ter o **Java JDK 21** instalado.
* Ter o **PostgreSQL** rodando localmente (porta padrão `5432`).
* Ter o **Git** instalado.

### 2. Configuração do Banco de Dados
Crie um banco de dados no seu PostgreSQL local chamado `consorcio_db`. Na primeira execução, o Hibernate criará toda a estrutura de tabelas e as travas de integridade (Constraints e Checks) automaticamente.

### 3. Clonar o Repositório
Abra o seu terminal e execute:
```bash
git clone [https://github.com/RonaldoSNavarro/consorcio-api.git](https://github.com/RonaldoSNavarro/consorcio-api.git)
cd consorcio-api
```

### 4. Variáveis de Ambiente
O projeto utiliza uma variável de ambiente para proteger a senha do banco de dados. Configure-a no seu sistema ou IDE:
* **Nome da Variável:** `DB_PASSWORD`
* **Valor:** `<sua-senha-do-postgres>`

### 5. Executar a Aplicação
Inicie a aplicação pela sua IDE ou via terminal usando o Maven:
```bash
./mvnw spring-boot:run
```

## 🔗 Endpoints (Rotas da API)

Por padrão, a API estará disponível em `http://localhost:8080`. Abaixo estão as principais rotas disponíveis:

### 👤 Clientes
* `POST /api/clientes` - Cadastra um novo cliente.
* `GET /api/clientes` - Retorna a lista de clientes.

### 👥 Grupos
* `POST /api/grupos` - Cadastra um novo grupo (nasce `EM_FORMACAO`).
* `PUT /api/grupos/{id}/inaugurar?dataAssembleia={data}` - Inaugura o grupo (muda para `EM_ANDAMENTO`).
* `GET /api/grupos` - Retorna a lista de grupos.

### 🎟️ Cotas
* `POST /api/cotas` - Adquire uma nova cota.
* `GET /api/cotas` - Lista todas as cotas.

### 💸 Parcelas
* `POST /api/parcelas` - Gera uma nova parcela com cálculo automático de fundos.
* `PUT /api/parcelas/{id}/pagar` - Realiza o pagamento de uma parcela (alimenta o saldo do grupo).
* `GET /api/parcelas/cota/{cotaId}` - Histórico de parcelas de uma cota.

### 🏛️ Assembleias
* `POST /api/assembleias` - Agenda uma nova assembleia para um grupo.
* `GET /api/assembleias/grupo/{grupoId}` - Lista todas as assembleias de um grupo específico.

### 🎉 Contemplações
* `POST /api/contemplacoes` - Registra uma nova contemplação mediante validação de saldo do Fundo Comum.
* `GET /api/contemplacoes/assembleia/{assembleiaId}` - Lista todos os contemplados de uma assembleia.

---
*Projeto desenvolvido com foco em arquitetura limpa, tratamento de erros e regras de negócio complexas do mercado financeiro brasileiro.*