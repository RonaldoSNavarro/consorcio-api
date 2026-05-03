# 🏢 Consórcio API

Um sistema back-end completo para administração e gerenciamento de consórcios, desenvolvido em Java com Spring Boot.

## 🚀 Tecnologias e Ferramentas

* **Java 21**
* **Spring Boot 3** (Web, Data JPA)
* **PostgreSQL** (Banco de Dados Relacional)
* **Hibernate** (Mapeamento Objeto-Relacional)
* **Maven** (Gerenciador de Dependências)
* **Git e GitHub** (Versionamento de Código)

## 🧩 Arquitetura do Domínio

O núcleo atual do sistema gerencia três entidades principais:
* **Cliente:** Pessoas cadastradas com validação de CPF/CNPJ e E-mail únicos.
* **Grupo:** Agrupamento financeiro de cotas com código único, valor de crédito, prazo e taxa de administração.
* **Cota:** A ligação transacional que vincula um Cliente a um Grupo específico, possuindo status de ativação automático.

## ⚙️ Como rodar o projeto localmente

### 1. Pré-requisitos
* Ter o **Java JDK 21** instalado.
* Ter o **PostgreSQL** rodando localmente (porta padrão `5432`).
* Ter o **Git** instalado.

### 2. Configuração do Banco de Dados
Crie um banco de dados no seu PostgreSQL local chamado `consorcio_db`.

### 3. Clonar o Repositório
Abra o seu terminal e execute:

git clone [https://github.com/ronaldosnavarro/consorcio-api.git](https://github.com/ronaldosnavarro/consorcio-api.git)

### 4. Variáveis de Ambiente
O projeto utiliza uma variável de ambiente para proteger a senha do banco de dados (não versionada por questões de segurança). Antes de executar a aplicação, configure a variável no seu sistema ou nas configurações de execução (*Run/Debug Configurations*) da sua IDE:
* **Nome da Variável:** `DB_PASSWORD`
* **Valor:** `<sua-senha-do-postgres>`

### 5. Executar a Aplicação
Inicie a aplicação pela sua IDE rodando a classe `ConsorcioApiApplication.java` ou via terminal usando o Maven:
```bash
./mvnw spring-boot:run
```
*As tabelas e relacionamentos serão gerados automaticamente pelo Hibernate na primeira inicialização.*

## 🔗 Endpoints (Rotas da API)

Por padrão, a API estará disponível em `http://localhost:8080`. Abaixo estão as rotas disponíveis para teste via Postman ou Insomnia:

### 👤 Clientes
* `POST /api/clientes` - Cadastra um novo cliente.
* `GET /api/clientes` - Retorna a lista de todos os clientes.

### 👥 Grupos
* `POST /api/grupos` - Cadastra um novo grupo.
* `GET /api/grupos` - Retorna a lista de todos os grupos.

### 🎟️ Cotas
* `POST /api/cotas` - Adquire uma nova cota (vincula o ID de um Cliente ao ID de um Grupo).
* `GET /api/cotas` - Retorna todas as cotas cadastradas no sistema.
* `GET /api/cotas/cliente/{clienteId}` - Retorna todas as cotas pertencentes a um cliente específico.
* `GET /api/cotas/grupo/{grupoId}` - Retorna todas as cotas pertencentes a um grupo específico.

### 💸 Parcelas
* `POST /api/parcelas` - Gera uma nova parcela vinculada a uma cota (nasce com status `PENDENTE`).
* `PUT /api/parcelas/{id}/pagar` - Realiza o pagamento de uma parcela (muda status para `PAGA` e registra a data).
* `GET /api/parcelas/cota/{cotaId}` - Retorna todo o histórico de parcelas de uma cota específica.

---
*Projeto desenvolvido como parte do aprimoramento prático em Arquitetura de Software e APIs REST.*