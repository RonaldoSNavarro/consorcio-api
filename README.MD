# 🏢 Consórcio API

Um sistema back-end de alta performance para administração e gerenciamento de consórcios, desenvolvido em **Java 21** com **Spring Boot 4**. **Este projeto implementa regras de negócio financeiras reais baseadas nas normativas do Banco Central do Brasil (BCB) e no Código de Defesa do Consumidor (CDC).**

## 🚀 Tecnologias e Ferramentas

* **Java 21:** Uso de **Virtual Threads** para escalabilidade e **Records** para imutabilidade e concisão de dados.
* **Spring Boot 4** (Web, Data JPA, Security).
* **Spring Security & JWT (Auth0):** Autenticação *stateless* e autorização baseada em tokens criptográficos.
* **PostgreSQL:** Banco de dados relacional para persistência de dados financeiros.
* **Springdoc OpenAPI (Swagger):** Documentação interativa personalizada e protegida.
* **Lombok:** Redução de código *boilerplate* nas entidades de domínio.

## 🏗️ Arquitetura e Padrões de Projeto

O sistema foi refatorado para seguir padrões rigorosos de engenharia de software:
* **Arquitetura de DTOs (Records):** Implementação de *Data Transfer Objects* para isolar as entidades do banco de dados. Isso impede a exposição de dados sensíveis e garante contratos de API estáveis.
* **Mapeamento Unidirecional:** Separação clara entre `RequestDTO` (dados de entrada validados) e `ResponseDTO` (dados de saída filtrados).
* **Global Exception Handling:** Centralização do tratamento de erros, convertendo exceções de negócio em respostas HTTP padronizadas.

## 🛡️ Segurança e Autenticação

A proteção dos recursos financeiros é garantida via **Bearer Token**:
1. **Autenticação:** O usuário obtém acesso via `/api/login`.
2. **Autorização:** Um `SecurityFilter` customizado intercepta as requisições, valida a assinatura JWT e verifica as permissões antes de liberar o acesso aos Services.
3. **Persistência:** Uso de senhas criptografadas e tokens com tempo de expiração controlado.

## 🧩 Regras de Negócio e Motores Financeiros

* **Motor de Contemplação:** Validação de saldo real em caixa (**Fundo Comum**) e implementação da regra de **Lance Embutido** (limitado a 30% do crédito conforme norma do BCB).
* **Gestão de Inadimplência:** Cálculo automático de multa (2%) e juros de mora *pro-rata die* (1% ao mês) para pagamentos em atraso.
* **Algoritmos de Amortização:**
    1. **Redução de Prazo:** Quitação de parcelas de trás para frente.
    2. **Diluição de Valor:** Reequilíbrio do saldo devedor nas mensalidades vincendas com tratamento de resíduos matemáticos (**Regra do Centavo Perdido**).

## 📖 Documentação da API (Swagger)

A interface do Swagger foi organizada cronologicamente para facilitar o entendimento do fluxo de um consórcio:
1. **Auth:** Login e geração de Token.
2. **Clientes, Grupos e Cotas:** Configuração da base e vendas.
3. **Parcelas:** Gestão de cobranças e pagamentos.
4. **Assembleias e Contemplações:** Eventos de entrega de bens.

Acesse em: `http://localhost:8080/swagger-ui.html`

---
*Projeto desenvolvido por Ronaldo Navarro - Foco em Engenharia de Back-end e Sistemas Financeiros.*