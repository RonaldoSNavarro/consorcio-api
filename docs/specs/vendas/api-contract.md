# 📡 API Contract — Vendas de Proposta

## Endpoints de Bens

### GET /api/vendas/bens-referencia
Lista bens de referência disponíveis.

### POST /api/vendas/bens-referencia
Cria bem de referência (Ex: Onix 1.0, Apartamento).

## Endpoints de Produtos

### GET /api/vendas/produtos
Lista produtos/planos de consórcio disponíveis.

### POST /api/vendas/produtos
Cria um novo produto. Role: ADMIN, GERENTE.

## Endpoints de Proposta e Contrato

### POST /api/vendas/propostas
Cria uma intenção de compra de cota.
Body:
```json
{
  "clienteId": 1,
  "produtoId": 2,
  "tipoVendaId": 1,
  "valorCreditoSolicitado": 100000.00
}
```
Response: Proposta (Status EM_ANALISE).

### POST /api/vendas/propostas/{id}/aprovar
Aprova a proposta após análise.
Retorna o Contrato de Adesão criado (Status PENDENTE_PAGAMENTO) e a respectiva fatura da 1ª parcela gerada.

### POST /api/vendas/contratos/{id}/efetivar
Simula o pagamento da 1ª parcela. Aloca o cliente no Grupo mais adequado e cria a `Cota`, efetivando o ContratoAdesao.
### GET /api/vendas/propostas/pendentes-risco
Retorna as propostas que ficaram retidas na an�lise de risco de PLD/FT (status PENDENTE_ANALISE_RISCO).
Utilizado pelo dashboard de compliance.

### POST /api/vendas/propostas/{id}/analise-risco
Submete a decis�o da an�lise de risco de uma proposta retida.
Body (AnaliseRiscoRequestDTO):
`json
{
  "aprovada": false,
  "justificativa": "Cliente consta na lista de PEP sem comprova��o de renda compat�vel."
}
`
Role: ANALISTA_COMPLIANCE, ADMIN.
