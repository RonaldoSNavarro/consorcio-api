# 🏢 Consórcio API

Um sistema back-end completo para administração e gerenciamento de consórcios, desenvolvido em Java com Spring Boot. **Este projeto implementa regras de negócio financeiras reais baseadas nas normativas do Banco Central do Brasil (BCB) e no Código de Defesa do Consumidor (CDC).**

## 🚀 Tecnologias e Ferramentas

* **Java 21**
* **Spring Boot 4** (Web, Data JPA)
* **PostgreSQL** (Banco de Dados Relacional)
* **Hibernate** (Mapeamento Objeto-Relacional)
* **Maven** (Gerenciador de Dependências)
* **Git e GitHub** (Versionamento de Código)

## 🧩 Arquitetura do Domínio e Regras de Negócio Financeiras

O núcleo do sistema gerencia o ciclo de vida completo de um consórcio, respeitando regras de integridade transacional, travas financeiras e cálculos contábeis precisos:

* **Ciclo de Vida (Grupo e Cota):** Grupos nascem `EM_FORMACAO` e evoluem para `EM_ANDAMENTO` via inauguração em Assembleia. Cotas possuem travas de estado (`ATIVA`, `CONTEMPLADA`, `CANCELADA`, `INADIMPLENTE`).
* **Fracionamento de Parcelas:** O valor pago pelo cliente é dividido automaticamente (via `@PrePersist`/`@PreUpdate`) em **Fundo Comum**, **Taxa de Administração** e **Fundo de Reserva**, contabilizando o caixa real do grupo.
* **Motor de Contemplação e Saldo:** Trava de integridade financeira que varre o banco de dados e bloqueia a contemplação caso o saldo do *Fundo Comum* em caixa seja inferior ao valor do crédito exigido pela cota.

### ⚠️ Motor de Inadimplência (Cálculo de Atraso)
Implementação de rotina de cobrança para pagamentos realizados após o vencimento, aplicando:
* **Multa Moratória:** 2% fixos sobre o valor da parcela.
* **Juros de Mora:** 1% ao mês calculado de forma *pro-rata die* (proporcional aos exatos dias de atraso utilizando a API `java.time`).

### 💰 Motor de Amortização de Lances
Quando um cliente paga um lance, o sistema injeta o valor no Fundo Comum do grupo e abate o saldo devedor do cliente com duas opções matemáticas:
1. **Redução de Prazo:** Quitação de parcelas de trás para frente. Caso o saldo do lance não cubra uma parcela inteira, o "troco" é abatido do Fundo Comum da próxima parcela de trás para frente, recalculando o boleto automaticamente.
2. **Diluição de Valor:** O valor do lance é dividido igualmente entre todas as parcelas pendentes, barateando a mensalidade do cliente. A lógica inclui tratamento de dízimas periódicas e a **Regra do Centavo Perdido** (absorvendo sobras de arredondamento na última parcela para fechar a auditoria do caixa).

## 🛡️ Tratamento de Exceções (Controller Advice)
A API conta com um `GlobalExceptionHandler` que intercepta quebras de regras de negócio e converte erros internos em respostas HTTP 400 (Bad Request) com payloads JSON padronizados, garantindo segurança e clareza para a aplicação cliente.

## ⚙️ Como rodar o projeto localmente

### 1. Pré-requisitos
* Ter o **Java JDK 21** instalado.
* Ter o **PostgreSQL** rodando localmente (porta padrão `5432`).
* Ter o **Git** instalado.

### 2. Configuração do Banco de Dados
Crie um banco de dados no seu PostgreSQL local chamado `consorcio_db`. Na primeira execução, o Hibernate criará toda a estrutura de tabelas e as travas de integridade (Constraints e Checks) automaticamente.

### 3. Executar a Aplicação
Inicie a aplicação pela sua IDE ou via terminal usando o Maven:
```bash
./mvnw spring-boot:run
```

## 🔗 Principais Endpoints da API

Abaixo estão as principais rotas disponíveis (`http://localhost:8080`):

### 💸 Parcelas e Inadimplência
* `POST /api/parcelas` - Gera uma nova parcela.
* `PUT /api/parcelas/{id}/pagar?dataPagamento={data}` - Paga a parcela (calcula multa e juros automaticamente se a data de pagamento for maior que o vencimento).
* `GET /api/parcelas/cota/{cotaId}` - Histórico de parcelas de uma cota.

### 🎯 Amortização de Lances
* `POST /api/parcelas/cota/{cotaId}/lance/reducao-prazo?valorLance={valor}` - Amortiza o saldo devedor quitando parcelas de trás para frente.
* `POST /api/parcelas/cota/{cotaId}/lance/diluicao?valorLance={valor}` - Amortiza o saldo devedor diluindo o lance para baratear todas as parcelas pendentes.

### 👤 Clientes, Grupos e Cotas
* `POST /api/clientes` | `POST /api/grupos` | `POST /api/cotas`
* `PUT /api/grupos/{id}/inaugurar?dataAssembleia={data}`

### 🏛️ Assembleias e Contemplações
* `POST /api/assembleias` - Agenda uma nova assembleia.
* `POST /api/contemplacoes` - Registra contemplação (com validação de saldo em caixa).

---
*Projeto desenvolvido com foco em arquitetura limpa, tratamento de erros e regras de negócio complexas do mercado financeiro brasileiro.*