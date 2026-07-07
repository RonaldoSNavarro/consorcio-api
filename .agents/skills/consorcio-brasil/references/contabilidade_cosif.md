# Contabilidade e Ledger (Padrão COSIF)

Este documento detalha o modelo de contabilidade de dupla entrada (Double-Entry Ledger) utilizado na API, essencial para a rastreabilidade financeira do grupo de consórcio e cumprimento das exigências do BACEN (ADR 002).

---

## 1. Princípio da Dupla Entrada

No sistema de consórcio, **eventos financeiros não são tratados como meras atualizações de saldo (CRUD)**. Para cada evento que envolve dinheiro, gera-se um registro histórico (imutável) e lançamentos contábeis equivalentes de Débito (D) e Crédito (C).

- **Princípio das Partidas Dobradas:** Todo débito possui um crédito correspondente de igual valor. (Σ Débitos = Σ Créditos).
- Isso garante a auditabilidade total da origem e destino dos recursos de acordo com as normas contábeis do Banco Central (Plano Contábil das Instituições do Sistema Financeiro Nacional - COSIF).

---

## 2. O Plano de Contas COSIF (Simplificado para Consórcios)

O sistema utiliza contas contábeis padronizadas com 8 dígitos, classificadas por Natureza (`DEVEDORA` ou `CREDORA`) e Tipo (`ATIVO`, `PASSIVO`, `PATRIMONIO_LIQUIDO`, `RESULTADO`, `COMPENSACAO`).

### Contas Principais Mapeadas no Sistema

**Ativo (Natureza Devedora)**
- `1.1.1.10.00-2` — Caixa / Disponibilidades
- `1.2.1.10.00-8` — Direitos a Receber (Parcelas a vencer)
- `1.6.9.10.00-5` — PDD - Provisão para Devedores Duvidosos (Conta Retificadora, natureza credora)

**Passivo (Natureza Credora)**
- `2.1.2.10.10-6` — Fundo Comum (Obrigações com consorciados)
- `2.1.2.10.20-9` — Fundo de Reserva
- `2.1.2.10.30-2` — Taxa de Administração (Obrigação com a administradora até o repasse)
- `2.1.2.10.40-5` — Seguro (Obrigação com a seguradora)
- `2.1.2.10.50-8` — Rendimento de Aplicações Financeiras
- `2.1.2.20.10-3` — Excluídos a Devolver (Restituições pendentes)
- `2.1.2.30.10-0` — Créditos a Liberar (Consorciados contemplados)
- `2.1.2.90.10-8` — Recursos Não Procurados (RNP)

**Resultado (Despesas/Receitas)**
- `3.1.8.10.00-1` — Despesa com PDD (Natureza Devedora)

---

## 3. Dinâmica de Saldo

O saldo de uma conta não é armazenado estaticamente, mas calculado dinamicamente pela agregação de lançamentos.

A fórmula depende da **Natureza da Conta**:
- **Contas de Natureza Credora (ex: Passivo, Fundo Comum):** `Saldo = Σ Créditos - Σ Débitos`
- **Contas de Natureza Devedora (ex: Ativo, Caixa):** `Saldo = Σ Débitos - Σ Créditos`

---

## 4. Exemplos de Lançamentos por Evento (`TipoOperacaoContabil`)

As operações típicas (`PROVISAO`, `BAIXA`, `ESTORNO`, `ENCERRAMENTO`) geram os seguintes pares contábeis:

### Evento: Pagamento de Parcela (Baixa)
Quando o consorciado paga R$ 1.000,00 (R$ 700 FC + R$ 200 TA + R$ 100 FR):
- **Débito:** `1.1.1.10.00-2` (Caixa) - R$ 1.000,00 (Entrou dinheiro no caixa)
- **Crédito:** `2.1.2.10.10-6` (Fundo Comum) - R$ 700,00 (Aumentou a obrigação do grupo)
- **Crédito:** `2.1.2.10.30-2` (Taxa Admin) - R$ 200,00
- **Crédito:** `2.1.2.10.20-9` (Fundo Reserva) - R$ 100,00

### Evento: Contemplação (Provisão de Crédito a Liberar)
Quando um consorciado é sorteado para uma carta de R$ 100.000,00:
- **Débito:** `2.1.2.10.10-6` (Fundo Comum) - R$ 100.000,00 (Reduz o fundo comum)
- **Crédito:** `2.1.2.30.10-0` (Créditos a Liberar) - R$ 100.000,00 (Reserva o dinheiro para o contemplado)

### Evento: Pagamento do Bem ao Fornecedor (Baixa)
Quando a administradora paga a concessionária pelo carro do contemplado:
- **Débito:** `2.1.2.30.10-0` (Créditos a Liberar) - R$ 100.000,00
- **Crédito:** `1.1.1.10.00-2` (Caixa) - R$ 100.000,00 (Saiu dinheiro do caixa)

### Evento: Exclusão de Consorciado Inadimplente
- **Débito:** `2.1.2.10.10-6` (Fundo Comum) - Valor amortizado (Reduz o passivo com o fundo comum)
- **Crédito:** `2.1.2.20.10-3` (Excluídos a Devolver) - Valor amortizado (Cria a obrigação de devolução futura)

### Evento: Provisão para Devedores Duvidosos (PDD)
- **Débito:** `3.1.8.10.00-1` (Despesa PDD)
- **Crédito:** `1.6.9.10.00-5` (PDD - Ativo retificador)

---

## 5. Reconciliação Financeira

A checagem de integridade (ex: "temos saldo suficiente para fazer assembleia hoje?") é feita consultando o saldo da conta `2.1.2.10.10-6` (Fundo Comum).
Graças ao uso de *Materialized Views*, essa agregação é feita de forma rápida sem sobrecarregar o banco de dados de transações diárias.
