# Spec — Composição de Fundos e Parcelas

- **Capability**: fundos
- **Versão**: v1.2
- **Status**: IMPLEMENTED
- **Última alteração**: Reversão atômica da adesão e bloqueio de quitação implícita — origem: CR-FIN-VND-003.

## Requisitos

| ID | Descrição |
|---|---|
| REQ-FUN-001 | Gerar e listar parcelas discriminando Fundo Comum, Taxa de Administração, Fundo de Reserva e Seguro. |
| REQ-FUN-002 | Calcular o total da parcela com escala monetária consistente. |
| REQ-FUN-003 | Registrar cada pagamento e estorno no ledger de partidas dobradas COSIF. |
| REQ-FUN-004 | Ao pagar a primeira parcela de adesão, efetivar o contrato e ativar a cota de forma atômica. |
| REQ-FUN-005 | Manter provisionadas, de forma idempotente, as 12 contas COSIF utilizadas pelos fluxos financeiros, de contemplação, restituição e encerramento. |

## Regras

- RN-FUN-001: Toda parcela nova nasce em `PENDENTE`.
- RN-FUN-002: Somente a operação de pagamento preenche `dataPagamento`, `valorPago` e status `PAGA`.
- RN-FUN-003: Parcela `PENDENTE` não compõe arrecadação, saldo de Fundo Comum nem dashboard financeiro.
- RN-FUN-004: O pagamento da parcela nº 1 promove o contrato de `PENDENTE_PAGAMENTO` para `EFETIVADO` e a cota de `AGUARDANDO_PAGAMENTO` para o estado compatível com o grupo.
- RN-FUN-005: O estorno da parcela nº 1, quando não houver pagamentos posteriores, reverte contrato e cota aos estados pendentes na mesma transação dos lançamentos de estorno.
- RN-FUN-006: Amortização não representa pagamento; não pode definir `PAGA`, `dataPagamento` ou `valorPago`.
- RN-FUN-007: Amortização de lance é acionada exclusivamente pela liquidação rastreável de lance vencedor; o abatimento não pode exceder o Fundo Comum pendente e deve recalcular o total da parcela.
- RN-FUN-008: As 12 contas COSIF padronizadas do domínio são provisionadas de forma idempotente na inicialização e também sob demanda na transação do lançamento quando ausentes. Uma conta fora desse conjunto deve retornar erro de negócio, nunca HTTP 500 ou persistência parcial.

## Critério de Aceitação — REQ-FUN-004

- **Given** a primeira parcela pendente de uma cota em `AGUARDANDO_PAGAMENTO`;
- **When** o Financeiro confirma o pagamento;
- **Then** pagamento, ledger, contrato e cota são atualizados na mesma transação;
- **And** uma falha em qualquer etapa reverte toda a operação.

## Critério de Aceitação — REQ-FUN-004 (estorno)

- **Given** uma primeira parcela paga sem pagamentos posteriores;
- **When** o Financeiro registra seu estorno;
- **Then** parcela, ledger, contrato e cota retornam de forma atômica ao estado anterior à baixa.

## Critério de Aceitação — REQ-FUN-005

- **Given** uma base nova ou legada com apenas parte do plano COSIF;
- **When** a aplicação é inicializada;
- **Then** as 12 contas padronizadas ficam disponíveis sem duplicação;
- **And** uma segunda inicialização não cria novos registros nem altera códigos contábeis.
