# Inadimplência, Cobrança e PDD

Este documento detalha as regras de negócio aplicáveis a cotas inadimplentes, o fluxo de cobrança e a Provisão para Devedores Duvidosos (PDD) no sistema de consórcios.

---

## 1. Ciclo de Vida da Inadimplência (Aging)

A inadimplência é monitorada pelo `VerificadorInadimplenciaJob`, que roda diariamente (tipicamente à 1h da manhã). O status da cota muda com base no número de parcelas atrasadas (aging):

- **0 atrasadas:** Status `ATIVA` (ou auto-reativação de `SUSPENSA` para `ATIVA` caso o cliente quite a dívida).
- **1 a 2 parcelas atrasadas:** Status `SUSPENSA`. O cliente perde o direito de participar de sorteios e ofertar lances, mas ainda não é excluído. Recebe notificações de cobrança.
- **≥ 3 parcelas atrasadas:**
  - Se a cota **NÃO foi contemplada:** Status passa para `EXCLUIDA`. A cota é cancelada, a vaga volta para o grupo (podendo ser vendida como cota de reposição ou absorvida), e o cliente passa a concorrer aos sorteios de excluídos para restituição parcial.
  - Se a cota **JÁ foi contemplada (bem entregue):** Status passa para `EM_EXECUCAO` ou `JURIDICO`. Como o bem já está alienado fiduciariamente ao grupo, inicia-se o processo de busca e apreensão ou execução de garantia.

> [!IMPORTANT]
> **Notificações Obrigatórias:** A administradora deve notificar formalmente o consorciado sobre o atraso antes da exclusão definitiva ou envio para cobrança judicial.

---

## 2. Encargos Moratórios

Quando o consorciado atrasa o pagamento de uma parcela, incidem encargos de mora (multa e juros), que são revertidos a favor do fundo comum do grupo e/ou da taxa de administração (depende do contrato/DestinacaoMultaRescisoria).

### Taxas aplicadas
- **Multa por atraso:** 2% (fixo) sobre o valor da parcela em atraso.
- **Juros de mora:** 1% ao mês, calculado *pro rata die* (proporcional aos dias de atraso).

### Fórmula de Juros Pro Rata Die
```
Dias de Atraso = Data Atual - Data de Vencimento
Juros (%) = (1% / 30) * Dias de Atraso
Valor Juros = Valor da Parcela * Juros (%)
```

### Provisão de Competência (CFC)
Em compliance com o Conselho Federal de Contabilidade (CFC):
- Os valores de multa e juros esperados são **provisionados** contabilisticamente na data de vencimento não paga.
- A **baixa** (realização) contábil ocorre no efetivo pagamento.
- Se houver renegociação com isenção ou estorno (ex: erro bancário), o sistema gera um *estorno de parcela* (reversão completa com lançamentos COSIF inversos).

---

## 3. PDD (Provisão para Devedores Duvidosos)

De acordo com a regulamentação e a ADR 006, é obrigatório registrar a PDD para proteger o grupo de perdas esperadas e para fins de encerramento do grupo.

- **Conta COSIF:** Débito em Despesa (3.1.8.10.00-1) e Crédito em Ativo Retificador (1.6.9.10.00-5).
- **Write-off (Baixa para prejuízo):** Valores devidos e não recuperados (ex: cota excluída que ficou devendo saldo após leilão do bem) são eventualmente "baixados" contra a conta de PDD.
- **Encerramento:** O grupo pode ser encerrado em até 120 dias da última AGO. Dívidas ainda não cobradas são baixadas para prejuízo no balancete consolidado e enviadas para cobrança judicial extraordinária, destravando o fechamento do grupo.

---

## 4. Status Relevantes Envolvidos

### Enum `StatusCota` (14 valores no sistema)
Estados associados à jornada de cobrança:
1. `AGUARDANDO_PAGAMENTO`: Parcela inicial ainda não compensada.
2. `ATIVA`: Adimplente.
3. `INADIMPLENTE`: Com atraso (usado como flag/sub-estado em algumas visualizações).
4. `SUSPENSA`: 1 ou 2 atrasos.
5. `EXCLUIDA`: ≥ 3 atrasos (não contemplado).
6. `EM_EXECUCAO`: ≥ 3 atrasos (contemplado, buscando apreensão).
7. `JURIDICO`: Processo judicial ativo (revisão contratual, reintegração de posse).

### Enum `StatusParcela`
1. `PENDENTE`: Aguardando vencimento.
2. `PAGA`: Liquidada.
3. `ATRASADA`: Vencida e não paga.
4. `BAIXADA`: Removida por renegociação, agrupamento ou write-off (PDD).
