# 📋 Spec — Módulo de Vendas de Proposta (vendas)

- **Capability**: vendas
- **Versão**: v2.3
- **Status**: IMPLEMENTED
- **Última alteração**: Proteção contra quitação implícita por amortização e reversão atômica do estorno da adesão — origem: CR-FIN-VND-003.

## 1. Contexto

Estruturação profissional do processo de comercialização de cotas de consórcio. O sistema atende aos requisitos do BACEN ao definir a venda como uma esteira contendo:
Produtos/Planos de Consórcio → Proposta de Adesão → Análise → Contrato de Adesão → Cota aguardando pagamento → Pagamento da 1ª Parcela → Ativação da Cota.
Toda carta de crédito é lastreada em um "Bem de Referência" (Ex: Tabela FIPE para veículos, INCC para Imóveis).

## 2. Requisitos (User Stories)

| ID | Descrição |
|---|---|
| REQ-VND-001 | CRUD de Categorias de Bens (Móvel I, Móvel II, Imóvel, Serviço) e Bens de Referência (O lastro). |
| REQ-VND-002 | CRUD de Produtos de Consórcio (Planos pré-configurados pela administradora com prazo e taxas). |
| REQ-VND-003 | Geração de Proposta de Adesão, vinculando Cliente, Produto, Valor de Crédito e Corretor. Propostas comuns nascem em `EM_ANALISE`; propostas sujeitas à RN-VND-008 nascem em `PENDENTE_ANALISE_RISCO`. |
| REQ-VND-004 | Aprovação da Proposta, gerando o `ContratoAdesao` em `PENDENTE_PAGAMENTO`, a `Cota` em `AGUARDANDO_PAGAMENTO` e a primeira parcela em `PENDENTE`. |
| REQ-VND-005 | Efetivação do `ContratoAdesao` exclusivamente mediante pagamento real da primeira parcela no módulo Financeiro, promovendo a `Cota` para `ATIVA` ou `AGUARDANDO_INAUGURACAO`. |
| REQ-VND-006 | CRUD de Corretores e Tipos de Venda para originação de propostas e controle de intermediários. |
| REQ-VND-007 | Geração de Comissões atreladas às vendas efetuadas, permitindo repasses e regras de estorno por inadimplência. |
| REQ-VND-008 | Reter para análise do Compliance, sem descartar o registro da venda, propostas de clientes com risco `ALTO` ou alertas restritivos `PENDENTE_ANALISE`/`CONFIRMADO`. |

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
2. Cliente escolhe o produto e assina a `PropostaAdesao` intermediado por um `Corretor` e atrelado a um `TipoVenda`.
3. O sistema executa o cruzamento PLD/FT antes de persistir a proposta:
   - sem risco alto ou alerta restritivo: status `EM_ANALISE`;
   - com risco `ALTO` ou alerta `PENDENTE_ANALISE`/`CONFIRMADO`: status `PENDENTE_ANALISE_RISCO`, sem gerar contrato ou cota.
4. Propostas retidas são decididas pela equipe de Compliance. A aprovação libera a continuidade; a reprovação exige justificativa auditável e impede o contrato.
5. O sistema cria automaticamente o `ContratoAdesao` em `PENDENTE_PAGAMENTO`.
6. A **Alocação Inteligente** localiza o grupo elegível e cria a `Cota` em `AGUARDANDO_PAGAMENTO`.
7. A primeira parcela nasce em `PENDENTE`, sem `dataPagamento` e sem `valorPago`. Nenhum valor compõe a arrecadação ou o ledger antes da baixa financeira.
8. O cliente realiza o pagamento da adesão pelo módulo Financeiro.
9. `ParcelaService.pagar()` registra as partidas dobradas, histórico e comissão; em seguida promove o contrato para `EFETIVADO` e a cota para `ATIVA` ou `AGUARDANDO_INAUGURACAO`, conforme o status do grupo.

## 5. Regras de Negócio
- RN-VND-001: Uma Proposta não pode ser aprovada se o cliente for Inativo.
- RN-VND-002: O valor do crédito na proposta deve respeitar limites da categoria do bem.
- RN-VND-003: A aprovação da venda gera a Cota em `AGUARDANDO_PAGAMENTO` e a primeira parcela em `PENDENTE`. A parcela não possui data/valor de pagamento e não integra arrecadação até a baixa real no Financeiro.
- RN-VND-004: Propostas Reprovadas são canceladas definitivamente.
- RN-VND-005 (Comissionamento): A comissão do corretor é diluída ao longo das parcelas pagas pelo cliente. Comissões poderão ser bloqueadas ou estornadas caso o cliente se torne inadimplente.
- RN-VND-006 (Alocação Inteligente): O sistema aloca exclusivamente grupos existentes (`EM_ANDAMENTO` e `EM_FORMACAO`) com vagas e categoria compatível; nunca cria grupos automaticamente durante a venda.
- RN-VND-007 (Geração de Cota): Ao registrar a venda, a cota gerada deve obrigatoriamente receber um número sequencial calculado dinamicamente de acordo com o total de cotas atuais do grupo alocado (restrição NOT NULL no banco de dados).
- RN-VND-009 (Baixa da Adesão): Somente `ParcelaService.pagar()` pode mudar a primeira parcela para `PAGA`, preencher `dataPagamento`/`valorPago`, registrar o ledger COSIF e efetivar `ContratoAdesao`/`Cota`.
- RN-VND-010 (Amortização): Amortização de lance reduz exclusivamente o componente de Fundo Comum de parcelas futuras de cotas já efetivadas; ela não altera nenhuma parcela para `PAGA`, não preenche dados de pagamento e não pode ser executada para cotas em `AGUARDANDO_PAGAMENTO`.
- RN-VND-011 (Estorno da Adesão): Ao estornar a primeira parcela de uma adesão sem pagamentos posteriores, o sistema reverte na mesma transação a parcela para `PENDENTE`, o contrato para `PENDENTE_PAGAMENTO`, a cota para `AGUARDANDO_PAGAMENTO`, a assinatura e qualquer comissão liberada por esse pagamento.

## 6. Diretrizes Técnicas / Notas de Arquitetura
- **Persistência / Serialização:** Entidades chave do módulo (como `ProdutoConsorcio` e `BemReferencia`) possuem dependências aninhadas. Deve-se adotar `FetchType.EAGER` ou Projetar em DTOs a fim de contornar `LazyInitializationException` no momento de retorno via Jackson API.
- **Payload DTO:** Formulários do Front-end devem repassar campos mandatórios explícitos (ex: `valorCreditoSolicitado`), mesmo que implícitos pelo produto selecionado.
- **Segurança (LGPD):** O filtro anti-intrusão ofusca nomes se detectar requisições aceleradas. O bypass de ambiente local foi incluído para o desenvolvimento no frontend.
- RN-VND-008 (Compliance e PLD/FT): Propostas originadas para clientes com risco `ALTO` ou alertas restritivos nos status `PENDENTE_ANALISE`/`CONFIRMADO` devem ser persistidas em `PENDENTE_ANALISE_RISCO`. A detecção não retorna erro de negócio na criação e não descarta a intenção de venda. Essas propostas devem ser revisadas obrigatoriamente pela equipe de Compliance antes de aprovação, contrato, pagamento ou geração de cota. Se reprovadas, o sistema exige justificativa auditável e impede a geração do contrato.

## 7. Critérios de Aceitação — REQ-VND-008

### AC-VND-008-01 — Registrar e reter proposta restritiva

- **Given** um cliente ativo com risco `ALTO` ou alerta restritivo `PENDENTE_ANALISE`/`CONFIRMADO`;
- **When** o operador cria uma proposta válida;
- **Then** o backend responde com sucesso e persiste a proposta em `PENDENTE_ANALISE_RISCO`;
- **And** a proposta fica disponível na consulta de pendências de risco;
- **And** nenhum contrato ou cota é criado antes da decisão do Compliance.

### AC-VND-008-02 — Preservar o fluxo comum

- **Given** um cliente ativo sem risco alto e sem alertas restritivos;
- **When** o operador cria uma proposta válida;
- **Then** a proposta é persistida em `EM_ANALISE`;
- **And** o fluxo normal de aprovação e efetivação permanece disponível.

## 8. Critérios de Aceitação — BUG-FIN-VND-002

### AC-VND-009-01 — Registrar venda sem antecipar recebimento

- **Given** uma proposta apta e aprovada;
- **When** o sistema registra a venda;
- **Then** cria o contrato em `PENDENTE_PAGAMENTO`;
- **And** cria a cota em `AGUARDANDO_PAGAMENTO`;
- **And** cria a parcela nº 1 em `PENDENTE`, com `dataPagamento = null` e `valorPago = null`;
- **And** a parcela não integra a arrecadação do grupo.

### AC-VND-009-02 — Efetivar somente após pagamento real

- **Given** uma cota em `AGUARDANDO_PAGAMENTO` com a parcela nº 1 em `PENDENTE`;
- **When** o Financeiro registra o pagamento da parcela;
- **Then** a parcela passa para `PAGA` e gera os lançamentos COSIF;
- **And** o contrato passa para `EFETIVADO`;
- **And** a cota passa para `ATIVA` se o grupo está `EM_ANDAMENTO`, ou `AGUARDANDO_INAUGURACAO` se está `EM_FORMACAO`.

### AC-VND-009-03 — Impedir quitação implícita por amortização

- **Given** uma cota em `AGUARDANDO_PAGAMENTO`;
- **When** o Financeiro solicita amortização por lance;
- **Then** a operação é rejeitada;
- **And** nenhuma parcela é marcada como `PAGA` fora de `ParcelaService.pagar()`.

### AC-VND-009-04 — Reverter integralmente a adesão estornada

- **Given** uma primeira parcela paga, contrato `EFETIVADO` e cota ativa;
- **When** o Financeiro estorna a primeira parcela sem pagamentos posteriores;
- **Then** os lançamentos COSIF são estornados;
- **And** a parcela volta para `PENDENTE`;
- **And** contrato e cota retornam para os estados pendentes de pagamento;
- **And** qualquer comissão liberada pelo pagamento é estornada.
