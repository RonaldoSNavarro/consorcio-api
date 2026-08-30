# Tasks SDD — Módulo de Vendas

- **Capability**: vendas
- **Spec**: [spec.md](spec.md) v2.3
- **API Contract**: [api-contract.md](api-contract.md) v2.3
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

## CR-FIN-VND-003 — Consistência da amortização e do estorno da adesão

- [x] **[SPEC] RN-VND-006** — Alinhar a alocação a grupos existentes, sem criação automática.
- [x] **[SPEC] RN-VND-010/RN-VND-011** — Definir amortização sem quitação implícita e reversão atômica da adesão.
- [x] **[BACKEND] RN-VND-010** — Bloquear amortização para cota pendente e impedir que a operação marque parcelas como `PAGA`.
- [x] **[BACKEND] RN-VND-011** — Reverter contrato, cota e comissão no estorno da primeira parcela.
- [x] **[QA] AC-VND-009-03/04** — Cobrir os dois cenários de regressão.

## BUG-VND-004 — Capacidade do Grupo e Reserva de Cota

- [x] **[BACKEND] RN-VND-012** — Reservar cota `DISPONIVEL` existente antes de criar uma nova cota sequencial.
- [x] **[BACKEND] RN-VND-012** — Validar a vaga antes da aprovação da proposta e do contrato para impedir persistência parcial.
- [x] **[FRONTEND] AC-VND-012-01** — Exibir a capacidade total na listagem de grupos e calcular vagas a partir de `codigoCota` e status `DISPONIVEL`.
- [x] **[QA] AC-VND-012-01** — Cobrir a aprovação com cota disponível e a apresentação da capacidade.

## BUG-FIN-VND-005 — Busca por código do grupo e da cota

- [x] **[BACKEND] RN-VND-014** — Filtrar pela relação canônica `Cota.grupo.codigoGrupo` e por `Cota.codigoCota`.
- [x] **[FRONTEND] RN-VND-014** — Usar chamada explícita por `codigoGrupo` e `codigoCota`, sem parâmetros posicionais ambíguos.
- [x] **[QA] RN-VND-014** — Validar a busca combinada e publicar o bundle atualizado do Financeiro.
