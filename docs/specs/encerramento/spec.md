# 📋 Especificação Funcional — Reajustes e Encerramento (encerramento)

*   **Status**: IMPLEMENTED
*   **Versão**: v1.0 (Baseline Retroativo)
*   **Aprovações**: Especialista Consórcios [✅] (2026-06-14) | Especialista Contabilidade [✅] (2026-06-14)
*   **Última alteração**: Baseline de reajuste do bem de referência e encerramento contábil com transferência para RNP e provisão de PDD (ADR 006).

---

## 🎯 Objetivo
Definir as regras para atualização dos valores das parcelas em aberto decorrentes do reajuste do bem de referência, além de reger o processo contábil de encerramento do grupo, garantindo a baixa de inadimplentes e destinação de recursos não procurados.

---

## 🧮 Regras de Negócio e Contabilidade

### REQ-ENC-001: Motor de Reajuste do Bem de Referência
- **Regra**: O valor do bem de referência do grupo é atualizado periodicamente por índice (IPCA, FIPE, etc.).
- **Impacto nas Parcelas**: O fator de reajuste é calculado como:
  $$\text{Fator} = \frac{\text{Novo Valor do Crédito}}{\text{Valor do Crédito Antigo}}$$
- Todas as parcelas com status `PENDENTE` e `ATRASADA` vinculadas ao grupo têm seu `valorFundoComum` multiplicado pelo Fator. Os hooks JPA recalculam o valor total das parcelas e geram lançamentos contábeis correspondentes de ajuste patrimonial no Ledger.

### REQ-ENC-002: Encerramento de Grupo e Baixa de Inadimplência (ADR 006)
- **Regra**: Em conformidade com as normas do Banco Central, o encerramento do grupo deve ser processado formalmente no prazo de **120 dias contados da data da última AGO**.
- **Tratamento de Inadimplência**: A existência de parcelas pendentes de cobrança não bloqueia o encerramento sistêmico. As cotas inadimplentes são baixadas do balancete ativo do grupo (gerando provisão de devedores e perdas contábeis) e enviadas para o setor de cobrança jurídica extraordinária:
  - Débito: `Provisões para Perdas sobre Inadimplência de Cotas` (PDD)
  - Crédito: `Contas a Receber de Consorciados` (Baixa do ativo do grupo)

### REQ-ENC-003: Recursos Não Procurados (RNP)
- **Regra**: Saldos remanescentes de consorciados credores (excluídos contemplados ou saldos finais de cotas quitadas) que não foram reclamados após o encerramento oficial do grupo devem ser transferidos para a conta de **Recursos Não Procurados (RNP)** da administradora:
  - Débito: `2.1.2.20.10-3` - Recursos de Consorciados Excluídos a Devolver (ou conta de saldo comum correspondente)
  - Crédito: `2.1.2.40.30-2` - Recursos de Consórcios Não Procurados (RNP)

---

## 🎯 Critérios de Aceitação (Given/When/Then)

### REQ-ENC-001 - AC1: Reajuste Monotônico de Parcelas
- **Given**: Uma parcela no status `PENDENTE` com Fundo Comum original de R$ 500,00 e valor total de R$ 600,00 (referente a um crédito de R$ 50.000,00).
- **When**: O bem de referência é reajustado em 10%, passando o crédito para R$ 55.000,00 (Fator 1,10).
- **Then**: O sistema atualiza o Fundo Comum da parcela para R$ 550,00 ($500 \times 1,10$) e o valor total do boleto passa a ser R$ 650,00.

### REQ-ENC-002 - AC1: Encerramento com Baixa para PDD
- **Given**: Um grupo com 120 dias decorridos da última assembleia ordinária, possuindo R$ 10.000,00 em inadimplência ativa de parcelas de cotas contempladas.
- **When**: O operador do sistema dispara a rotina de Encerramento Contábil do Grupo.
- **Then**: O sistema realiza a baixa contábil da dívida contra a provisão de devedores duvidosos (PDD), liquida o grupo sistemicamente alterando o status para `ENCERRADO` e não impede a conclusão da rotina.

### REQ-ENC-003 - AC1: Transferência para Recursos Não Procurados (RNP)
- **Given**: Uma cota de excluído contemplado que não retirou seu saldo de R$ 1.500,00.
- **When**: Ocorre o processamento de encerramento definitivo do grupo (120 dias).
- **Then**: O sistema transfere o saldo contábil de R$ 1.500,00 para a conta de Recursos Não Procurados (`2.1.2.40.30-2 - Recursos de Consórcios Não Procurados`).
