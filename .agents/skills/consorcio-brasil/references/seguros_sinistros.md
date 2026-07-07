# Seguros e Sinistros

O seguro prestamista é um componente opcional no consórcio, voltado a garantir o cumprimento das obrigações financeiras ou a entrega do bem em caso de adversidades ocorridas com o consorciado. A regulamentação principal advém da SUSEP (Superintendência de Seguros Privados).

---

## 1. Tipos de Seguro (`TipoSeguro`)

O sistema suporta três principais tipos de apólices integradas aos produtos de consórcio:
1. **VIDA (Óbito):** Quita o saldo devedor do consorciado em caso de falecimento. Se o valor da apólice exceder o saldo devedor, o excedente é pago aos herdeiros legais.
2. **INVALIDEZ:** Cobre o saldo devedor em caso de invalidez permanente total ou parcial.
3. **DESEMPREGO (Perda de Renda):** Cobre um número predeterminado de parcelas (ex: até 6 meses) em caso de desemprego involuntário, evitando a exclusão do consorciado do grupo.

---

## 2. Composição na Parcela e Contabilidade

Se o produto de consórcio exigir (ou o cliente optar por) seguro, o valor do prêmio compõe o boleto mensal.

### Cálculo do Prêmio
O prêmio do seguro é, na maioria das vezes, um **percentual aplicado mensalmente** sobre:
- O valor do crédito atualizado (mais comum); ou
- O saldo devedor do momento.

### Contabilidade (COSIF)
Quando a parcela é paga:
- O valor correspondente ao prêmio do seguro é creditado no Passivo, conta **`2.1.2.10.40-5` (Seguro)**.
- Essa conta representa uma obrigação de repasse da Administradora de Consórcios para a Seguradora parceira.

---

## 3. Gestão de Sinistros

Caso o evento segurado ocorra, é aberto um **Sinistro**.

### Ciclo de Vida do Sinistro (`StatusSinistro`)
1. **ABERTO:** Comunicado inicial do sinistro (ex: apresentação de certidão de óbito).
2. **EM_ANALISE:** Seguradora analisando a documentação e cobertura.
3. **APROVADO / RECUSADO:** Decisão final sobre a cobertura.
4. **PAGO:** Indenização depositada no grupo.

---

## 4. O "Lance Seguro Óbito" (`LANCE_SEGURO_OBITO`)

No caso de sinistro por **ÓBITO** de um consorciado ainda *não contemplado*, ocorre um evento especial no sistema para garantir o acesso da família ao bem sem distorcer o fluxo de caixa do grupo.

1. A seguradora indeniza o grupo no valor exato do saldo devedor.
2. O sistema gera automaticamente um **LANCE_SEGURO_OBITO** no valor da indenização.
3. Por ter o valor do saldo devedor integral (geralmente alto), esse lance ganha a contemplação na próxima assembleia.
4. A cota é contemplada (status quitada/alienação nula), e os herdeiros recebem a carta de crédito para uso.
