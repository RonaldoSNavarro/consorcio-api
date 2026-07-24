# Tasks SDD — Módulo de Vendas

- **Capability**: vendas
- **Spec**: [spec.md](spec.md) v2.2
- **API Contract**: [api-contract.md](api-contract.md) v2.2
- **Status**: IMPLEMENTED

## BUG-PLD-VND-001 — REQ-VND-008

- [x] **[BACKEND] REQ-VND-008** — Executar o cruzamento PLD/FT antes da persistência da proposta.
- [x] **[BACKEND] REQ-VND-008** — Persistir clientes com risco `ALTO` ou alertas `PENDENTE_ANALISE`/`CONFIRMADO` em `PENDENTE_ANALISE_RISCO`.
- [x] **[BACKEND] REQ-VND-008** — Preservar `EM_ANALISE` para clientes sem retenção de risco.
- [x] **[BACKEND] REQ-VND-008** — Manter propostas retidas disponíveis em `GET /api/vendas/propostas/pendentes-risco`.
- [x] **[QA] AC-VND-008-01** — Cobrir regressão garantindo persistência da proposta restritiva, sem exceção na criação.
- [x] **[QA] AC-VND-008-02** — Executar testes direcionados de `PropostaAdesaoService` e do fluxo de Compliance.

## Evidências

- `PropostaAdesaoService.criarProposta`
- `ComplianceChallengerTest`
- Resultado: 12 testes backend aprovados, sem falhas.

## BUG-FIN-VND-002 — REQ-VND-004, REQ-VND-005 e RN-VND-009

- [x] **[BACKEND]** Criar a cota em `AGUARDANDO_PAGAMENTO` durante a aprovação da venda.
- [x] **[BACKEND]** Gerar a primeira parcela em `PENDENTE`, sem data ou valor pago.
- [x] **[BACKEND]** Tornar a preparação do contrato idempotente e sem simulação de pagamento.
- [x] **[BACKEND]** Efetivar contrato/cota somente em `ParcelaService.pagar()` para a parcela nº 1.
- [x] **[QA]** Cobrir status, campos financeiros e ausência de arrecadação antecipada.
- [x] **[FRONTEND]** Encerrar o wizard após a aprovação, sem chamar a simulação de pagamento.
- [x] **[FRONTEND]** Informar que a venda foi registrada e a primeira parcela está pendente.

## Evidências v2.2

- `PropostaAdesaoServiceTest` e `ParcelaServiceTest`
- Resultado backend: 158 testes aprovados, 0 falhas, 2 ignorados.
- `hooks.test.jsx` e `AnaliseRiscoPage.test.jsx`
- Resultado frontend direcionado: 16 testes aprovados, 0 falhas.
