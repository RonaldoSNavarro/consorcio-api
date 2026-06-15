# 📋 Especificação Funcional — Seguros (seguros)

*   **Status**: IMPLEMENTED
*   **Versão**: v1.0 (Baseline Retroativo)
*   **Aprovações**: Especialista Consórcios [✅] (2026-06-14) | Especialista Contabilidade [✅] (2026-06-14)
*   **Última alteração**: Baseline de cobrança e repasse de prêmios de seguro.

---

## 🎯 Objetivo
Reger a cobrança e repasse de valores de seguro vinculados às cotas de consórcio (como seguro prestamista ou seguro de quebra de garantia), garantindo a proteção financeira do grupo e da administradora.

---

## 🧮 Regras de Negócio e Contabilidade

### REQ-SEG-001: Cobrança do Prêmio de Seguro
- **Regra**: O valor do seguro é cobrado como parte integrante da parcela mensal (`valorSeguro`). O percentual ou valor fixo é definido em contrato na formação do grupo e varia conforme a idade do participante ou valor do crédito.
- **Tipos de Seguro**:
  - **Seguro Prestamista**: Cobre o saldo devedor da cota em caso de falecimento ou invalidez permanente do consorciado.
  - **Seguro Quebra de Garantia**: Protege o grupo em caso de inadimplência de cotas já contempladas.

### REQ-SEG-002: Lançamento Contábil de Repasse (COSIF)
- **Regra**: Os valores cobrados dos consorciados a título de seguro não pertencem à administradora nem ao fundo comum do grupo. Eles são passivos circulantes a repassar à seguradora parceira:
  - Débito: `1.1.1.10.00-2` - Bancos - Recursos de Grupos (Disponibilidade)
  - Crédito: `2.1.2.10.40-5` - Prêmios de Seguros a Recolher

---

## 🎯 Critérios de Aceitação (Given/When/Then)

### REQ-SEG-001 - AC1: Cálculo do Seguro na Parcela
- **Given**: Uma cota com percentual de seguro prestamista definido de 0,05% a.m. sobre o valor do crédito de R$ 60.000,00.
- **When**: O sistema gera a parcela mensal da cota.
- **Then**: O valor de R$ 30,00 é atribuído ao campo `valorSeguro` na entidade `Parcela`.

### REQ-SEG-002 - AC1: Destinação Contábil de Seguro Recebido
- **Given**: O pagamento de uma parcela contendo R$ 30,00 de seguro.
- **When**: O sistema processa o recebimento e faz a contabilização.
- **Then**: O valor de R$ 30,00 é creditado especificamente na conta `2.1.2.10.40-5 - Prêmios de Seguros a Recolher` no Ledger Contábil.
