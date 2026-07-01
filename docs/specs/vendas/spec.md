# 📋 Spec — Módulo de Vendas de Proposta (vendas)

- **Capability**: vendas
- **Versão**: v2.0 (Refatoração SDD - Mercado BACEN)
- **Status**: IMPLEMENTED

## 1. Contexto

Estruturação profissional do processo de comercialização de cotas de consórcio. O sistema atende aos requisitos do BACEN ao definir a venda como uma esteira contendo:
Produtos/Planos de Consórcio → Proposta de Adesão → Análise → Contrato de Adesão → Pagamento da 1ª Parcela → Geração da Cota.
Toda carta de crédito é lastreada em um "Bem de Referência" (Ex: Tabela FIPE para veículos, INCC para Imóveis).

## 2. Requisitos (User Stories)

| ID | Descrição |
|---|---|
| REQ-VND-001 | CRUD de Categorias de Bens (Móvel I, Móvel II, Imóvel, Serviço) e Bens de Referência (O lastro). |
| REQ-VND-002 | CRUD de Produtos de Consórcio (Planos pré-configurados pela administradora com prazo e taxas). |
| REQ-VND-003 | Geração de Proposta de Adesão, vinculando Cliente, Produto, Valor de Crédito e Corretor, nascendo em `EM_ANALISE`. |
| REQ-VND-004 | Aprovação da Proposta, gerando o `Contrato de Adesão` em status pendente e a fatura da 1ª parcela. |
| REQ-VND-005 | Efetivação do Contrato mediante confirmação do pagamento da 1ª parcela, gerando a `Cota` no grupo via Alocação Inteligente. |
| REQ-VND-006 | CRUD de Corretores e Tipos de Venda para originação de propostas e controle de intermediários. |
| REQ-VND-007 | Geração de Comissões atreladas às vendas efetuadas, permitindo repasses e regras de estorno por inadimplência. |

## 3. Entidades Base

### Categoria de Bem e Bem de Referência
A cota deve estar sempre atrelada a um bem que dite o seu reajuste.
* `CategoriaBem`: Veículos Automotores, Imóveis, etc.
* `BemReferencia`: Chevrolet Onix 1.0 (R$ 80.000,00).

### Produto / Plano
* `ProdutoConsorcio`: Produto Auto 100 meses (Tx. Adm: 15%, Fundo Reserva: 2%).

### Proposta e Contrato
* `PropostaAdesao`: Intenção do cliente de comprar X crédito no Plano Y, intermediado por um `Corretor`.
* `ContratoAdesao`: Oficialização da proposta, aguardando assinatura e pagamento.

### CRM e Vendas (Comissões)
* `Corretor`: Entidade que originou a venda.
* `TipoVenda`: Define o percentual de comissionamento de acordo com o produto ou modalidade de venda.
* `ComissaoVenda`: Registro de valores de comissão a serem pagos, diluídos ou estornados conforme as parcelas pagas pelo cliente.

## 4. Fluxo Principal

1. Operador lista `ProdutosConsorcio` disponíveis.
2. Cliente escolhe o produto e assina a `PropostaAdesao` intermediado por um `Corretor` e atrelado a um `TipoVenda` (Status: EM_ANALISE).
3. Backoffice analisa e aprova a proposta (Status: APROVADA).
4. O sistema cria automaticamente o `ContratoAdesao` e emite a fatura da 1ª parcela. **Nota:** A 1ª parcela de adesão compõe-se integralmente de Taxa de Administração e Fundo de Reserva.
5. Cliente realiza o pagamento da adesão.
6. Sistema aciona rotina de **Alocação Inteligente**: Localiza o melhor grupo aberto ou em formação (`EM_ANDAMENTO` ou `EM_FORMACAO`) cruzando a `CategoriaBem` e limite numérico de cotas. 
7. Se não houver grupo disponível, cria-se automaticamente um novo grupo em formação (`EM_FORMACAO`).
8. Sistema cria `Cota`, efetiva contrato (Status: EFETIVADO) e gera registros de `ComissaoVenda` atrelados ao corretor.

## 5. Regras de Negócio
- RN-VND-001: Uma Proposta não pode ser aprovada se o cliente for Inativo.
- RN-VND-002: O valor do crédito na proposta deve respeitar limites da categoria do bem.
- RN-VND-003: Contrato só gera Cota após pagamento da 1ª parcela de adesão (obrigatório). A primeira parcela é composta 100% por encargos administrativos (Tx Adm + Fundo Reserva).
- RN-VND-004: Propostas Reprovadas são canceladas definitivamente.
- RN-VND-005 (Comissionamento): A comissão do corretor é diluída ao longo das parcelas pagas pelo cliente. Comissões poderão ser bloqueadas ou estornadas caso o cliente se torne inadimplente.
- RN-VND-006 (Alocação Inteligente): O sistema sempre prioriza preencher grupos existentes (`EM_ANDAMENTO` e `EM_FORMACAO`) antes de abrir novos. O limite da query atual do sistema está em 100 cotas/grupo como hard-limit, atrelado à categoria de bem.

## 6. Diretrizes Técnicas / Notas de Arquitetura
- **Persistência / Serialização:** Entidades chave do módulo (como `ProdutoConsorcio` e `BemReferencia`) possuem dependências aninhadas. Deve-se adotar `FetchType.EAGER` ou Projetar em DTOs a fim de contornar `LazyInitializationException` no momento de retorno via Jackson API.
- **Payload DTO:** Formulários do Front-end devem repassar campos mandatórios explícitos (ex: `valorCreditoSolicitado`), mesmo que implícitos pelo produto selecionado.
- **Segurança (LGPD):** O filtro anti-intrusão ofusca nomes se detectar requisições aceleradas. O bypass de ambiente local foi incluído para o desenvolvimento no frontend.
