# 🏢 Consórcio API

Um sistema back-end de alta performance para administração e gerenciamento de consórcios, desenvolvido em **Java 21** com **Spring Boot 4**. Este projeto implementa regras de negócio financeiras reais baseadas nas normativas do Banco Central do Brasil (BCB) e no Código de Defesa do Consumidor (CDC).

## 🚀 Tecnologias e Ferramentas

* **Java 21:** Uso de *Virtual Threads* para escalabilidade e *Records* para imutabilidade.
* **Spring Boot 4:** (Web, Data JPA, Security).
* **Spring Security & JWT (Auth0):** Autenticação stateless e autorização baseada em tokens.
* **PostgreSQL:** Persistência de dados financeiros.
* **JUnit 5, Mockito & MockMvc:** Ecossistema completo de testes para garantir a integridade do sistema.
* **Springdoc OpenAPI (Swagger):** Documentação interativa personalizada e protegida.
* **Lombok:** Redução de código boilerplate.

## 🏗️ Arquitetura e Padrões de Projeto

* **Arquitetura de DTOs (Records):** Isolamento total das entidades para segurança e estabilidade de contratos.
* **Mapeamento Unidirecional:** Separação clara entre dados de entrada validados e saída filtrada.
* **Global Exception Handling:** Centralização do tratamento de erros com respostas HTTP padronizadas.

## 🧪 Qualidade de Software e Testes

O projeto segue a pirâmide de testes para garantir que nenhuma alteração no código quebre as regras financeiras:

* **Testes Unitários (100% Branch Coverage):** Toda a camada de `Service` possui cobertura total de ramificações. Isso garante que cada `if`, `else` e `throw` das regras de negócio foi validado individualmente com **Mockito**.
* **Segurança de Tipos em Testes:** Uso de `@MockitoBean` (Spring Boot 4) e `ReflectionTestUtils` para testes de serviços de infraestrutura como o `TokenService`.
* **Testes de Integração (MockMvc):** Validação dos fluxos de entrada e saída (`Controllers`), garantindo que a serialização JSON, as validações de Bean (`@Valid`) e os filtros de segurança JWT funcionem em conjunto.

## 🛡️ Segurança e Autenticação

A proteção dos recursos financeiros é garantida via **Bearer Token**:
* **Autenticação:** O usuário obtém acesso via `/api/login`.
* **Autorização:** Um `SecurityFilter` customizado valida a assinatura JWT e as permissões antes de liberar o acesso aos `Services`.

## 🧩 Regras de Negócio e Motores Financeiros

* **Motor de Contemplação:** Validação de saldo em caixa (Fundo Comum) e trava de **Lance Embutido** (limitado a 30% do crédito).
* **Gestão de Inadimplência:** Cálculo automático de multa (2%) e juros de mora pro-rata die (1% ao mês).
* **Algoritmos de Amortização:**
    * **Redução de Prazo:** Quitação de parcelas de trás para frente.
    * **Diluição de Valor:** Reequilíbrio do saldo devedor com tratamento matemático da **Regra do Centavo Perdido** (ajuste de resíduos de dízimas na última parcela).

## 📖 Documentação da API (Swagger)

A interface foi organizada para refletir o ciclo de vida real de um consórcio:
1.  **Auth:** Login e geração de Token.
2.  **Clientes, Grupos e Cotas:** Configuração da base e vendas.
3.  **Parcelas:** Gestão de cobranças e pagamentos.
4.  **Assembleias e Contemplações:** Eventos de entrega de bens.

> **Acesse em:** `http://localhost:8080/swagger-ui.html`

---
**Projeto desenvolvido por Ronaldo Navarro** - Foco em Engenharia de Back-end e Sistemas Financeiros.