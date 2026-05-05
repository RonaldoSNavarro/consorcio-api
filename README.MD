# 🏢 Consórcio API

Um sistema back-end completo para administração e gerenciamento de consórcios, desenvolvido em Java com Spring Boot. **Este projeto implementa regras de negócio reais baseadas nas normativas do Banco Central do Brasil (BCB).**

## 🚀 Tecnologias e Ferramentas

* **Java 21**
* **Spring Boot 4** (Web, Data JPA)
* **PostgreSQL** (Banco de Dados Relacional)
* **Hibernate** (Mapeamento Objeto-Relacional)
* **Maven** (Gerenciador de Dependências)
* **Git e GitHub** (Versionamento de Código)

## 🧩 Arquitetura do Domínio e Regras de Negócio (BCB)

O núcleo do sistema gerencia o ciclo de vida completo de um consórcio, respeitando regras de integridade transacional:

* **Cliente:** Cadastros blindados com validação de unicidade para CPF/CNPJ e E-mail.
* **Grupo:** Agrupamento financeiro de cotas. Todo grupo nasce com o status `EM_FORMACAO` e seu status evolui para `EM_ANDAMENTO` apenas quando ocorre a inauguração oficial na 1ª Assembleia Geral Ordinária (AGO).
* **Cota:** A ligação que vincula um Cliente a um Grupo específico. Possui travas de estado (`ATIVA`, `CONTEMPLADA`, `CANCELADA`, `INADIMPLENTE`).
* **Parcela:** Controle financeiro atrelado à Cota, com transição automatizada de `PENDENTE` para `PAGA` e registro de data de recebimento.
* **Assembleia:** Eventos oficiais do grupo, classificados como `ORDINARIA` (AGO - encontros mensais) ou `EXTRAORDINARIA` (AGE - decisões excepcionais).
* **Contemplação:** O motor principal do sistema. Realiza a entrega do crédito por `SORTEIO`, `LANCE_LIVRE` ou `LANCE_FIXO`. O sistema garante via regras de negócio que apenas cotas ativas do mesmo grupo da assembleia possam ser sorteadas, altera automaticamente o status da cota para `CONTEMPLADA` e impede dupla contemplação.

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
* `GET /api/cotas/cliente/{clienteId}` - Lista cotas de um cliente.
* `GET /api/cotas/grupo/{grupoId}` - Lista cotas de um grupo.

### 💸 Parcelas
* `POST /api/parcelas` - Gera uma nova parcela.
* `PUT /api/parcelas/{id}/pagar` - Realiza o pagamento de uma parcela.
* `GET /api/parcelas/cota/{cotaId}` - Histórico de parcelas de uma cota.

### 🏛️ Assembleias
* `POST /api/assembleias` - Agenda uma nova assembleia para um grupo.
* `GET /api/assembleias/grupo/{grupoId}` - Lista todas as assembleias de um grupo específico.

### 🎉 Contemplações
* `POST /api/contemplacoes` - Registra uma nova contemplação (Sorteio ou Lance).
* `GET /api/contemplacoes/assembleia/{assembleiaId}` - Lista todos os contemplados de uma assembleia específica.

---
*Projeto desenvolvido com foco em arquitetura limpa e regras de negócio do mercado financeiro brasileiro.*