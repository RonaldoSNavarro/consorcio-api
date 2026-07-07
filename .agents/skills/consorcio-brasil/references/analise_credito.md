# Análise de Crédito (Pós-Contemplação)

Após a cota ser sorteada ou arrematar um lance, o consorciado **não recebe o dinheiro imediatamente**. O processo exige a aprovação em uma rígida esteira de Análise de Crédito, gerenciada pela entidade `AnaliseCredito` e pelo `AnaliseCreditoService`.

---

## 1. Fluxo de Status de Contemplação (Cota)
1. **PENDENTE_INTEGRALIZACAO:** Venceu o lance livre, aguarda compensação bancária (prazo: 2 a 5 dias).
2. **AGUARDANDO_ANALISE:** Sorteio realizado ou lance pago. Início do envio de documentos de renda e garantias.
3. **APROVADO:** Análise de crédito aprovada.
4. **CREDITO_LIBERADO:** Carta de crédito emitida.
5. **CREDITO_UTILIZADO:** Pagamento feito ao fornecedor do bem (concessionária, imobiliária, etc).

---

## 2. Regras de Aprovação de Crédito

Para garantir que o consorciado continuará pagando as parcelas restantes (que agora não contam com o saldo devedor garantido pelo fundo, visto que o bem será entregue), as seguintes validações são feitas:

### Margem de Consignação
A parcela atualizada do consórcio **não pode exceder 30% da renda mensal líquida comprovada** do cliente.
- Caso exceda, o sistema exige a inclusão de um **Devedor Solidário** (Fiador) com renda compatível para compor a margem.

### Restrições Cadastrais
- Consulta a birôs de crédito (SPC/Serasa) no momento da aprovação.
- Ausência de pendências com a Receita Federal.
- Re-checagem em listas restritivas (Compliance/PLD).

### Garantia (Alienação Fiduciária)
O bem adquirido deve ter liquidez e valor de mercado igual ou superior ao saldo devedor da cota.
- **Veículos:** Geralmente limitados a até 5 anos de uso (conforme tabela FIPE).
- **Imóveis:** Exigem vistoria de engenharia e laudo de avaliação.
- O bem é alienado fiduciariamente em favor da administradora do grupo, com inclusão de gravame (SNG para veículos, Cartório de Imóveis para imóveis).

---

## 3. Reprovação e Reversão (Descontemplação)

Caso o consorciado não consiga comprovar renda ou não apresente garantias adequadas em um prazo contratual (ex: 30 a 90 dias úteis):
- O status de `AGUARDANDO_ANALISE` pode ser revertido.
- A **Descontemplação** cancela o crédito, estorna os registros contábeis de provisão e a cota retorna ao status de `ATIVA`.
- Os valores pagos via lance podem ser abatidos no saldo devedor, devolvidos, ou mantidos em caixa, dependendo da regra da administradora.
