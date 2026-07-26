# 📡 API Contract — Vendas de Proposta

- **Capability**: vendas
- **Versão**: v2.3
- **Status**: LOCKED
- **Spec**: [spec.md](spec.md)
- **Última alteração**: Reversão da adesão e amortização sem quitação implícita — origem: CR-FIN-VND-003.

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
  "grupoId": 10,
  "valorCreditoSolicitado": 100000.00
}
```
Response: `201 Created`.

- `status = EM_ANALISE`: cliente sem retenção de risco;
- `status = PENDENTE_ANALISE_RISCO`: cliente com risco `ALTO` ou alerta restritivo `PENDENTE_ANALISE`/`CONFIRMADO`.

Exemplo de proposta encaminhada ao Compliance:

```json
{
  "id": 123,
  "numeroProposta": "PROP-A1B2C3D4",
  "clienteId": 1,
  "produtoId": 2,
  "grupoId": null,
  "codigoGrupo": null,
  "valorCreditoSolicitado": 100000.00,
  "status": "PENDENTE_ANALISE_RISCO",
  "dataProposta": "2026-07-24T15:00:00"
}
```

> A existência de alerta restritivo não produz erro HTTP na criação. A proposta é registrada e retida para análise antes da geração de contrato ou cota.

### POST /api/vendas/propostas/{id}/aprovar
Aprova a proposta após análise.
Retorna o Contrato de Adesão em `PENDENTE_PAGAMENTO`. A operação também aloca a
`Cota` em `AGUARDANDO_PAGAMENTO` e gera a primeira parcela em `PENDENTE`.

Pré-condição: a proposta deve estar em `EM_ANALISE`. Propostas em `PENDENTE_ANALISE_RISCO` devem passar pelo endpoint de análise de risco.

### POST /api/vendas/contratos/{id}/efetivar
Endpoint legado e idempotente. Garante a alocação da cota e a geração das parcelas
pendentes, mas **não simula pagamento** e não muda o contrato para `EFETIVADO`.
O pagamento deve ocorrer em `PUT /api/parcelas/{parcelaId}/pagar`.

### PUT /api/parcelas/{parcelaId}/pagar

Ao pagar a parcela nº 1 de uma cota em `AGUARDANDO_PAGAMENTO`:

- registra a baixa contábil e o histórico do pagamento;
- muda o `ContratoAdesao` para `EFETIVADO`;
- muda a cota para `ATIVA` ou `AGUARDANDO_INAUGURACAO`.

### POST /api/parcelas/{parcelaId}/estornar

Estorna o pagamento e seus lançamentos COSIF. Para a primeira parcela de uma adesão,
somente é permitido quando não houver parcelas posteriores pagas; na mesma transação a
parcela retorna a `PENDENTE`, o contrato a `PENDENTE_PAGAMENTO` e a cota a
`AGUARDANDO_PAGAMENTO`.

### POST /api/parcelas/cota/{cotaId}/lance/reducao-prazo

Amortiza exclusivamente o Fundo Comum de parcelas futuras de uma cota já efetivada.
Não marca parcelas como `PAGA` nem registra pagamento. Cotas em
`AGUARDANDO_PAGAMENTO` recebem `409 Conflict`.
### GET /api/vendas/propostas/pendentes-risco
Retorna as propostas que ficaram retidas na análise de risco de PLD/FT (status `PENDENTE_ANALISE_RISCO`).
Utilizado pelo dashboard de compliance.

### POST /api/vendas/propostas/{id}/analise-risco
Submete a decisão da análise de risco de uma proposta retida.
Body (AnaliseRiscoRequestDTO):

```json
{
  "aprovada": false,
  "justificativa": "Cliente consta na lista de PEP sem comprovação de renda compatível."
}
```

Role: ANALISTA_COMPLIANCE, ADMIN.

Resposta:
- `200 OK` com o `ContratoResponseDTO` quando a proposta é aprovada;
- `204 No Content` quando a proposta é reprovada: a decisão e sua justificativa foram persistidas, mas nenhum contrato é gerado. O cliente deve tratar essa resposta como sucesso e atualizar a lista de pendências.
