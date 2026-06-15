# 📋 Especificação Funcional — Apuração e Contemplações (contemplacao)

*   **Status**: IMPLEMENTED
*   **Versão**: v1.0 (Baseline Retroativo)
*   **Aprovações**: Especialista Consórcios [✅] (2026-06-14) | Especialista Contabilidade [✅] (2026-06-14)
*   **Última alteração**: Baseline de motores de apuração de lances, integração com Ledger Contábil e status intermediário de integralização.

---

## 🎯 Objetivo
Reger o motor de apuração da assembleia ordinária para classificar ofertas de lances, efetuar desempates, validar a existência de caixa real no Fundo Comum do grupo através do Ledger Contábil e gerir o ciclo de vida da contemplação.

---

## 🧮 Regras de Negócio e Contabilidade

### REQ-CON-001: Motor de Apuração e Ordenação
- **Regra**: Durante a AGO, as propostas de lances são ordenadas pelo percentual ofertado em relação ao saldo devedor da cota (do maior para o menor).
- **Desempate**: Se houver empate nos percentuais, aplica-se o critério configurado no grupo (`criterioDesempateLance`), como o número de cota mais próximo do número sorteado naquela assembleia.
- **⚠️ LACUNA IDENTIFICADA**: O REQUIREMENTS.md original define o tipo de contemplação `LANCE_FIXO`, mas não descreve seu motor de apuração ou como o percentual fixado do grupo é determinado para o sorteio do fixo. Atualmente, o sistema assume que o Lance Fixo segue o critério do sorteio entre os habilitados.

### REQ-CON-002: Checagem de Saldo em Tempo Real (Ledger Contábil)
- **Regra**: O sistema só homologa uma contemplação se o saldo de caixa do Fundo Comum do grupo comportar a liberação do crédito. O motor calcula:
  $$\text{Impacto Líquido Caixa} = \text{Crédito do Grupo} - \text{Valor do Lance}$$
- O saldo disponível é consultado no Ledger somando débitos e créditos na conta `2.1.2.10.10-6 - Fundo Comum de Grupos` associada ao grupo da assembleia.

### REQ-CON-003: Fluxo de Homologação de Lance Livre (ADR 004)
- **Regra**: A contemplação por Lance Livre não é imediata. Ela entra no status intermediário `PENDENTE_INTEGRALIZACAO`.
- O consorciado tem o prazo de 2 a 5 dias úteis para pagar o boleto do lance. O crédito só passa para a conta contábil de `Créditos a Liberar` e a cota avança para `AGUARDANDO_ANALISE` após a compensação bancária do lance.

### REQ-CON-004: Lançamentos Contábeis do Ciclo de Contemplação
1. **Na Oferta de Lance (Contas de Compensação)**:
   - Débito: `9.1.1.10.00-3` - Lances a Integralizar
   - Crédito: `9.9.1.10.00-1` - Lances Ofertados
2. **Na Integralização do Lance (Compensação física)**:
   - Débito: `1.1.1.10.00-2` - Bancos - Recursos de Grupos (Disponibilidade)
   - Crédito: `2.1.2.10.10-6` - Fundo Comum de Grupos
3. **Na Homologação Definitiva da Contemplação**:
   - Débito: `2.1.2.10.10-6` - Fundo Comum de Grupos (Redução do saldo comum)
   - Crédito: `2.1.2.30.10-0` - Créditos a Liberar (Bens Contemplados a Entregar)
   - *Valor*: Crédito bruto do grupo menos o lance embutido (se aplicável).
4. **No Desembolso Efetivo do Crédito (Compra do bem)**:
   - Débito: `2.1.2.30.10-0` - Créditos a Liberar
   - Crédito: `1.1.1.10.00-2` - Bancos - Recursos de Grupos

---

## 🎯 Critérios de Aceitação (Given/When/Then)

### REQ-CON-002 - AC1: Validação de Saldo Insuficiente no Grupo
- **Given**: Uma assembleia ordinária em apuração. O saldo do Fundo Comum do grupo no Ledger é de R$ 50.000,00.
- **When**: O motor tenta contemplar uma cota com crédito de R$ 100.000,00 por meio de sorteio (impacto de R$ 100.000,00).
- **Then**: O sistema bloqueia a contemplação por falta de fundos e sinaliza erro ao operador, mantendo a cota sem contemplação.

### REQ-CON-003 - AC1: Status de Transição do Lance Livre (ADR 004)
- **Given**: Uma cota declarada vencedora por lance livre em assembleia.
- **When**: O motor de apuração executa o resultado da assembleia.
- **Then**: O status da contemplação é definido como `PENDENTE_INTEGRALIZACAO` e nenhum lançamento contábil é feito nas contas de passivo real (`Créditos a Liberar`).

### REQ-CON-003 - AC2: Compensação do Boleto e Liberação de Crédito
- **Given**: Uma contemplação no status `PENDENTE_INTEGRALIZACAO`.
- **When**: Ocorre a conciliação bancária indicando o recebimento do valor do lance.
- **Then**: O sistema altera o status da contemplação para `AGUARDANDO_ANALISE` e gera os lançamentos contábeis de débito em `Fundo Comum de Grupos` e crédito em `Créditos a Liberar`.
