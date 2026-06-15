# 📋 Especificação Funcional — Restituição de Excluídos (excluidos)

*   **Status**: IMPLEMENTED
*   **Versão**: v1.0 (Baseline Retroativo)
*   **Aprovações**: Especialista Consórcios [✅] (2026-06-14) | Especialista Contabilidade [✅] (2026-06-14)
*   **Última alteração**: Baseline de cálculo de restituição de excluídos conforme a Lei Federal nº 11.795/08 (Art. 30) e a ADR 005.

---

## 🎯 Objetivo
Reger as regras de negócio e lançamentos contábeis para devolução de recursos de consorciados excluídos/cancelados, garantindo a correção do cálculo baseada no valor reajustado do bem de referência e aplicação da cláusula penal.

---

## 🧮 Regras de Negócio e Contabilidade

### REQ-EXC-001: Elegibilidade e Contemplação de Excluídos
- **Regra**: Em conformidade com o Artigo 30 da Lei nº 11.795/08, a devolução dos valores pagos ao fundo comum por cotas canceladas ou excluídas deve obrigatoriamente ocorrer por meio de contemplação por **Sorteio** em assembleia geral ordinária (AGO).
- **Notificação**: A administradora tem até 5 dias úteis após o sorteio da cota excluída para notificar o cliente e disponibilizar o Demonstrativo de Prestação de Contas.

### REQ-EXC-002: Memória de Cálculo da Restituição (ADR 005)
A devolução baseia-se no percentual amortizado do valor do bem reajustado na data da assembleia de contemplação (e não no valor nominal histórico pago), deduzida a multa rescisória (cláusula penal).
1. **Percentual Amortizado do Fundo Comum (PAFC)**:
   $$PAFC = \sum \left( \frac{\text{Valor Efetivamente Pago ao Fundo Comum de cada parcela}}{\text{Valor total do Crédito na data do respectivo pagamento}} \right)$$
2. **Valor Bruto da Devolução (VBD)**:
   $$VBD = PAFC \times \text{Valor do Bem de Referência Atualizado na data da AGO de Contemplação do Excluído}$$
3. **Valor Líquido da Devolução (VLD)** após dedução da cláusula penal de 10%:
   $$VLD = VBD \times (1 - 0,10) = VBD \times 0,90$$

### REQ-EXC-003: Destinação da Cláusula Penal (Multa Rescisória)
O sistema deve suportar duas regras parametrizáveis para destinação da multa rescisória de 10%:
- **Alternativa 1 (Padrão - Reversão ao Grupo)**: Destinada a recompor o caixa comum. O fundo comum do grupo é debitado apenas pelo valor líquido (90%), mantendo a multa no patrimônio coletivo.
- **Alternativa 2 (Reversão à Administradora)**: O fundo comum é debitado pelo valor bruto (100%), destinando 90% para o excluído e 10% para as receitas da administradora.

### REQ-EXC-004: Ajuste Monotônico e Fixação de Saldo
- **Não Sorteados**: O valor a devolver é dinâmico (em memória) e acompanha as variações e reajustes do bem de referência do grupo até o momento do sorteio.
- **Sorteados**: Uma vez contemplada por sorteio, o valor apurado é transferido para o passivo de excluídos. O saldo a devolver torna-se nominal e fixo, **não sofrendo reajustes adicionais** por variações posteriores do bem de referência.

### REQ-EXC-005: Contabilização do Sorteio e Desembolso (COSIF)
1. **Contemplação por Sorteio (Com Multa Revertida ao Grupo - Alternativa 1)**:
   - Débito: `2.1.2.10.10-6` - Fundo Comum de Grupos (Pelo valor líquido - 90%)
   - Crédito: `2.1.2.20.10-3` - Recursos de Consorciados Excluídos a Devolver (Pelo valor líquido - 90%)
2. **Contemplação por Sorteio (Com Multa Revertida à Administradora - Alternativa 2)**:
   - Débito: `2.1.2.10.10-6` - Fundo Comum de Grupos (Pelo valor bruto - 100%)
   - Crédito: `2.1.2.20.10-3` - Recursos de Consorciados Excluídos a Devolver (Pelo valor líquido - 90%)
   - Crédito: `2.1.2.10.30-2` - Taxa de Administração a Repassar (Pelo valor da multa - 10%)
3. **No Desembolso Físico ao Excluído**:
   - Débito: `2.1.2.20.10-3` - Recursos de Consorciados Excluídos a Devolver (Valor líquido - 90%)
   - Crédito: `1.1.1.10.00-2` - Bancos - Recursos de Grupos (Disponibilidade)

---

## 🎯 Critérios de Aceitação (Given/When/Then)

### REQ-EXC-002 - AC1: Cálculo Correto da Devolução
- **Given**: Um consorciado excluído que pagou R$ 5.000,00 de Fundo Comum quando o crédito era R$ 50.000,00 (amortizou exatamente $10\%$ do bem).
- **When**: A cota é sorteada em uma assembleia onde o bem de referência foi reajustado para R$ 60.000,00.
- **Then**: O sistema calcula:
  - Valor Bruto da Devolução (VBD) = R$ 6.000,00 ($10\%$ de R$ 60.000,00)
  - Valor Líquido da Devolução (VLD) = R$ 5.400,00 ($90\%$ de R$ 6.000,00)
  - Multa rescisória de 10% = R$ 600,00

### REQ-EXC-004 - AC1: Fixação de Saldo Pós-Sorteio
- **Given**: A cota excluída sorteada no AC1, com saldo a devolver fixado em R$ 5.400,00 no passivo.
- **When**: Três meses se passam e o bem de referência do grupo é reajustado para R$ 65.000,00.
- **Then**: O saldo devedor contábil a devolver ao excluído permanece nominalmente em R$ 5.400,00, sem sofrer qualquer alteração pelo novo reajuste do bem.
