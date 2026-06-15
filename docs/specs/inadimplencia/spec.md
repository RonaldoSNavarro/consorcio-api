# 📋 Especificação Funcional — Mora e Inadimplência (inadimplencia)

*   **Status**: IMPLEMENTED
*   **Versão**: v1.0 (Baseline Retroativo)
*   **Aprovações**: Especialista Consórcios [✅] (2026-06-14) | Especialista Contabilidade [✅] (2026-06-14)
*   **Última alteração**: Baseline de cálculo de juros e multas de mora e destinação legal dos encargos para o Fundo Comum.

---

## 🎯 Objetivo
Reger os cálculos de encargos financeiros decorrentes do atraso no pagamento das parcelas de consórcio, assegurando o cumprimento da legislação vigente e a correta reversão contábil dos encargos ao grupo.

---

## 🧮 Regras de Negócio e Contabilidade

### REQ-INA-001: Encargos de Mora
- **Multa de Mora**: Fator fixo de **2,00%** aplicado sobre o valor total da prestação em atraso.
- **Juros de Mora**: Fator de **1,00% ao mês** ($0,0333\%$ ao dia), calculado *pro rata die* baseado nos dias de atraso decorridos entre a data de vencimento e a data de pagamento:
  $$\text{Juros} = \text{Valor Parcela} \times \left(\frac{0,01}{30}\right) \times \text{Dias de Atraso}$$

### REQ-INA-002: Destinação Legal dos Recursos
- **Regra**: Em conformidade com a Lei nº 11.795/08 (Art. 25), o valor total arrecadado com juros e multas de mora **deve reverter 100% para o Fundo Comum** do grupo de consórcio, visando recompor o caixa comum devido ao atraso.
- **Contabilidade (COSIF)**:
  - Débito: `1.1.1.10.00-2` - Bancos - Recursos de Grupos (Disponibilidades)
  - Crédito: `2.1.2.10.10-6` - Fundo Comum de Grupos (Pelo valor da multa + juros recebidos)

### REQ-INA-003: Cálculo Volátil e Dinâmico
- **Regra**: Juros e multas de parcelas em aberto são simulados em memória dinamicamente durante consultas. A gravação física dos encargos no banco de dados só ocorre no momento da quitação da parcela (`pagar`), evitando flush e recálculos desnecessários no Hibernate.

---

## 🎯 Critérios de Aceitação (Given/When/Then)

### REQ-INA-001 - AC1: Cálculo Correto da Multa e Juros Pro Rata
- **Given**: Uma parcela no valor total de R$ 1.000,00 com vencimento em 10/10/2026.
- **When**: O consorciado efetua o pagamento em 20/10/2026 (10 dias de atraso).
- **Then**: O sistema simula e cobra:
  - Multa = R$ 20,00 ($2\%$)
  - Juros = R$ 3,33 ($1.000 \times (0,01 / 30) \times 10$)
  - Valor cobrado no boleto = R$ 1.023,33

### REQ-INA-002 - AC1: Lançamento de Mora no Fundo Comum
- **Given**: O recebimento de R$ 1.023,33 referente a uma parcela paga com atraso (Fundo Comum original de R$ 800,00 + R$ 200,00 de taxas + R$ 23,33 de encargos moratórios).
- **When**: O sistema processa o pagamento da parcela.
- **Then**: O lançamento contábil destina R$ 823,33 para a conta `2.1.2.10.10-6 - Fundo Comum de Grupos` (o fundo comum base mais os R$ 23,33 de encargos de mora) e R$ 200,00 para as contas de taxas de administração e reservas.
