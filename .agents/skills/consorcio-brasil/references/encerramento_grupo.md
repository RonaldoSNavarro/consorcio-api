# Encerramento de Grupo e Liquidação

Este documento descreve o fluxo de encerramento de um grupo de consórcio, suas fases, exigências do Banco Central e os procedimentos contábeis de liquidação final (ADR 006).

---

## 1. Prazos Legais (Resolução BCB 285/2023, Art. 36)

O encerramento do grupo deve ocorrer em um prazo máximo de **120 dias** contados da data da realização da última Assembleia Geral Ordinária (AGO) prevista no contrato, ou da data em que todos os consórcios e obrigações do grupo foram liquidados antecipadamente.

---

## 2. Ciclo de Vida do Encerramento

### `StatusEncerramento` (4 estados)
1. **AGENDADO:** Última AGO realizada, início do relógio de 120 dias.
2. **EM_LIQUIDACAO:** Administradora está ativamente liquidando ativos e passivos (pagando credores, cobrando devedores).
3. **ENCERRADO_CONTABIL:** Balancete zerado internamente (passivos transferidos/procurados).
4. **ENCERRADO_DEFINITIVO:** BCB notificado formalmente sobre o encerramento do grupo.

O `GrupoService` e o `EncerramentoService` coordenam essas transições.

---

## 3. Passos da Liquidação Financeira

No período de 120 dias, a administradora deve zera o balancete COSIF do grupo realizando:

### 3.1. Distribuição de Fundo de Reserva
Se houver saldo positivo na conta `2.1.2.10.20-9` (Fundo de Reserva), este valor deve ser rateado proporcionalmente e devolvido aos consorciados ativos que cumpriram com suas obrigações.

### 3.2. Baixa de Inadimplência (Write-off via PDD - ADR 006)
O grupo **não pode ser encerrado** com direitos a receber pendentes no balancete principal de liquidação rápida.
- Dívidas ativas de consorciados inadimplentes (não recuperadas via cobrança ou leilão) são rebaixadas para prejuízo contábil.
- Lançamento: **Débito** em `3.1.8.10.00-1` (Despesa PDD) e **Crédito** em `1.6.9.10.00-5` (PDD Ativo Retificador).
- Após essa baixa, as parcelas assumem o status `BAIXADA`.
- Os títulos correspondentes são transferidos para carteira de cobrança judicial extraordinária (sob risco da administradora e/ou fundo comum).

### 3.3. RNP - Recursos Não Procurados
Valores de restituição devidos a excluídos (ou créditos de ativos não utilizados) que não foram resgatados pelos consorciados por falha de contato ou inércia são transferidos para a conta **RNP (`2.1.2.90.10-8`)**.
- A administradora deve tentar localizar o credor.
- Esses recursos ficam à disposição e têm prazo prescricional legal, momento em que a administradora segue a regulação específica (recolhimento/baixa).

---

## 4. Conclusão (Notificação BCB)

Quando todas as provisões são realizadas, a conta do `Fundo Comum` e demais obrigações ativas ficam zeradas.
O status do grupo muda para `ENCERRADO_DEFINITIVO` e o Banco Central é notificado via arquivo de remessa/API.
