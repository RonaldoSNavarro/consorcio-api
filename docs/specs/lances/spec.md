# Spec — Lances e Liquidação

- **Capability**: lances
- **Versão**: v1.2
- **Status**: IMPLEMENTED (P0)

## Requisitos

| ID | Descrição |
|---|---|
| REQ-LAN-001 | Registrar ofertas somente para cota elegível e assembleia em captação. |
| REQ-LAN-003 | Amortizar somente o Fundo Comum de parcelas pendentes de contrato efetivado. |
| REQ-LAN-005 | Liquidar lance vencedor por `lanceId`, com idempotência, trilha de status e modalidade de amortização. |

## Regras de negócio

- RN-LAN-005-01: somente lance `VENCEDOR` pode ser liquidado; a transição final é `LIQUIDADO`.
- RN-LAN-005-02: uma repetição com a mesma modalidade retorna a cota sem novo lançamento contábil ou amortização; modalidade diferente é erro de negócio.
- RN-LAN-005-03: `FIRME` e `FGTS` registram recebimento em caixa, trânsito do crédito e promovem a cota. `EMBUTIDO` não cria entrada de caixa, pois a retenção de crédito já ocorre na contemplação. `MISTO` e `SEGURO_OBITO` ficam bloqueados enquanto não houver composição financeira dos componentes.
- RN-LAN-005-04: a liquidação valida que cota, lance e contemplação pertencem à mesma assembleia.
- RN-LAN-003-01: redução de prazo e diluição exigem `ContratoAdesao` `EFETIVADO`, valor positivo e valor não superior ao Fundo Comum pendente.
- RN-LAN-003-02: amortização não altera `StatusParcela`, `valorPago` ou `dataPagamento`; recalcula `valorParcela` apenas pela redução do Fundo Comum.
- RN-LAN-003-03: diluição é proporcional ao Fundo Comum pendente e distribui centavos sem criar saldo negativo.
- RN-LAN-006-01: `MISTO` e `SEGURO_OBITO` não podem ser ofertados nem liquidados até que seus componentes financeiros sejam modelados. Lance vencedor cancelado por falta de integralização passa a `EXPIRADO`.

## Critérios de aceitação

- **AC-LAN-005-01**: dado um lance firme vencedor pendente, ao liquidá-lo com modalidade válida, então os dois movimentos contábeis, a transição da cota, a amortização e os metadados de liquidação são persistidos na mesma transação.
- **AC-LAN-005-02**: dado um lance já liquidado, quando a mesma modalidade for reenviada, então não há novo lançamento nem nova amortização.
- **AC-LAN-005-03**: dado lance embutido vencedor, quando liquidado, então não é registrada entrada em caixa.
