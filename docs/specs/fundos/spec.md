# Spec — Composição de Fundos e Parcelas

- **Capability**: fundos
- **Versão**: v1.1
- **Status**: IMPLEMENTED
- **Última alteração**: Integração da baixa da primeira parcela com a efetivação do contrato — origem: BUG-FIN-VND-002.

## Requisitos

| ID | Descrição |
|---|---|
| REQ-FUN-001 | Gerar e listar parcelas discriminando Fundo Comum, Taxa de Administração, Fundo de Reserva e Seguro. |
| REQ-FUN-002 | Calcular o total da parcela com escala monetária consistente. |
| REQ-FUN-003 | Registrar cada pagamento e estorno no ledger de partidas dobradas COSIF. |
| REQ-FUN-004 | Ao pagar a primeira parcela de adesão, efetivar o contrato e ativar a cota de forma atômica. |

## Regras

- RN-FUN-001: Toda parcela nova nasce em `PENDENTE`.
- RN-FUN-002: Somente a operação de pagamento preenche `dataPagamento`, `valorPago` e status `PAGA`.
- RN-FUN-003: Parcela `PENDENTE` não compõe arrecadação, saldo de Fundo Comum nem dashboard financeiro.
- RN-FUN-004: O pagamento da parcela nº 1 promove o contrato de `PENDENTE_PAGAMENTO` para `EFETIVADO` e a cota de `AGUARDANDO_PAGAMENTO` para o estado compatível com o grupo.

## Critério de Aceitação — REQ-FUN-004

- **Given** a primeira parcela pendente de uma cota em `AGUARDANDO_PAGAMENTO`;
- **When** o Financeiro confirma o pagamento;
- **Then** pagamento, ledger, contrato e cota são atualizados na mesma transação;
- **And** uma falha em qualquer etapa reverte toda a operação.
