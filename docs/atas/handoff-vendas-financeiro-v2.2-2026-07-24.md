# Handoff — Vendas e Financeiro v2.2

- **Data:** 2026-07-24
- **Capabilities:** `vendas`, `fundos`, `compliance`
- **Origem:** `BUG-FIN-VND-002` e `BUG-PLD-VND-001`
- **Status SDD:** ✅ IMPLEMENTED → REVIEWED → QA APPROVED → SIGN-OFF TÉCNICO
- **Commit:** não realizado, conforme orientação do usuário

## 1. Resultado entregue

O registro da venda e o recebimento da primeira parcela foram separados.

Fluxo canônico:

1. A proposta comum nasce em `EM_ANALISE`.
2. Proposta com risco alto ou alerta restritivo nasce em `PENDENTE_ANALISE_RISCO` e segue para Compliance.
3. A aprovação, comum ou pelo Compliance, cria:
   - contrato `PENDENTE_PAGAMENTO`;
   - cota `AGUARDANDO_PAGAMENTO`;
   - parcela nº 1 `PENDENTE`, sem `dataPagamento` ou `valorPago`.
4. Somente a baixa real da parcela nº 1 em `ParcelaService.pagar()`:
   - gera os movimentos financeiros e lançamentos COSIF;
   - efetiva o contrato;
   - promove a cota para `AGUARDANDO_INAUGURACAO`, se o grupo estiver `EM_FORMACAO`, ou `ATIVA` nos demais grupos elegíveis.

O endpoint legado `/api/vendas/contratos/{id}/efetivar` permanece compatível e idempotente, mas não simula pagamento.

## 2. Backend

Principais mudanças:

- `PropostaAdesaoService` prepara contrato, cota e cronograma sem antecipar recebimento.
- `ParcelaService.pagar()` passou a efetivar contrato e cota dentro da mesma transação do pagamento.
- Aprovação do Compliance reutiliza o mesmo fluxo de preparação.
- A composição monetária preexistente das parcelas foi preservada; a correção ficou restrita aos estados e ao momento da efetivação.

Cobertura:

- `PropostaAdesaoServiceTest`
- `ParcelaServiceTest`
- `ComplianceChallengerTest`

## 3. Frontend

Principais mudanças:

- `useVendaProposta` não chama mais o endpoint legado de efetivação.
- Toast de sucesso: `Venda registrada! Primeira parcela pendente de pagamento.`
- Navegação pós-venda direcionada para `/financeiro`.
- Dashboard inclui `AGUARDANDO_PAGAMENTO` e `AGUARDANDO_INAUGURACAO` em “Cotas Emitidas”.
- Pagamento e estorno invalidam `dashboardStats`.
- Aprovação de risco usa `ConfirmDialog`, sem confirmação nativa do navegador.

## 4. Evidências de qualidade

| Validação | Resultado |
|---|---|
| Backend — suíte completa | 158 testes, 0 falhas, 2 ignorados |
| Backend — regressão final direcionada | 12 testes, 0 falhas |
| Frontend — suíte completa | 55 testes, 0 falhas |
| Frontend — testes direcionados | 16 testes, 0 falhas |
| Build Vite de produção | Aprovado |
| QA real pós-rebuild | Login, Dashboard e Financeiro aprovados contra backend/PostgreSQL reais |
| Docker Compose | Backend e frontend reconstruídos e ativos |

## 5. Estado operacional

- `consorcio-api-backend`: ativo na porta `8081`.
- `consorcio-api-frontend`: ativo na porta `80`.
- Frontend: `http://localhost`.
- `/actuator/health` retorna `403` por proteção de segurança; a operação do backend foi confirmada pelo login e pelas consultas reais da UI.

## 6. Pendências e limites conhecidos

1. Registros históricos já persistidos como pagos não foram migrados, pois não é possível inferir com segurança quais recebimentos foram reais.
2. O lint global possui baseline preexistente de 753 problemas, incluindo artefatos gerados pelo Playwright e débitos antigos de PropTypes/imports. Build e testes permanecem aprovados.
3. O bundle principal do frontend permanece acima de 500 kB e gera warning não bloqueante.
4. `docker-compose.yml` contém a chave obsoleta `version`, também como warning não bloqueante.
5. A API do Cortex respondeu `Unexpected response type` em `query`, `write_page` e `lint`. A consolidação foi solicitada, mas não pôde ser confirmada pelo conector.

## 7. Artefatos SDD atualizados

Backend:

- `docs/specs/vendas/spec.md` v2.2
- `docs/specs/vendas/api-contract.md` v2.2
- `docs/specs/vendas/tasks.md`
- `docs/specs/fundos/spec.md` v1.1
- `docs/specs/fundos/api-contract.md` v1.1
- `docs/specs/fundos/tasks.md`
- `docs/traceability-matrix.md`
- `docs/PROJECT_CONTEXT.md`

Frontend:

- `docs/specs/vendas/ui-spec.md` v2.2
- `docs/specs/vendas/tasks.md`
- `docs/specs/fundos/ui-spec.md` v1.1
- `docs/traceability-matrix.md`
- `docs/PROJECT_CONTEXT.md`

## 8. Regra para continuidade

> Nunca marcar a primeira parcela como paga durante criação ou aprovação da proposta. Pagamento exige baixa financeira explícita e auditável. Qualquer alteração nessa transição deve atualizar previamente as specs SDD de `vendas` e `fundos`.

## 9. Próximo agente

Antes de continuar:

1. Ler este handoff.
2. Consultar as specs v2.2 de `vendas` e v1.1 de `fundos`.
3. Preservar as alterações locais e não realizar commit sem autorização.
4. Verificar se o Cortex voltou a responder e, em caso positivo, consolidar esta ata como página `decision` com as tags `vendas`, `financeiro`, `compliance`, `sdd` e `v2.2`.
