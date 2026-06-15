# 📋 Especificação Funcional — Composição de Fundos e Parcelas (fundos)

*   **Status**: IMPLEMENTED
*   **Versão**: v1.0 (Baseline Retroativo)
*   **Aprovações**: Especialista Consórcios [✅] (2026-06-14) | Especialista Contabilidade [✅] (2026-06-14)
*   **Última alteração**: Baseline de motores de cálculo de parcelas e regras de composição patrimonial de fundos.

---

## 🎯 Objetivo
Definir a regra de formação, cálculo e segregação das parcelas mensais cobradas dos consorciados, garantindo a destinação contábil correta para o Fundo Comum, Fundo de Reserva, Taxa de Administração e Seguros.

---

## 🧮 Regras de Negócio e Contabilidade

### REQ-FUN-001: Composição e Cálculo da Parcela Mensal
- **Regra**: O valor total da prestação mensal cobrada do consorciado é a soma de quatro frações básicas calculadas a partir do valor do crédito e dos percentuais configurados no grupo:
  $$\text{Valor Parcela} = \text{Fundo Comum} + \text{Taxa de Administração} + \text{Fundo de Reserva} + \text{Seguro}$$
- **Fundo Comum (FC)**: Percentual amortizado mensalmente (ex: se o prazo é 60 meses, o FC mensal de amortização linear é de $100\% / 60 = 1,6667\%$ do valor do crédito). Destina-se unicamente às contemplações.
- **Taxa de Administração (TA)**: Percentual destinado a remunerar a gestão da administradora (diluído pelos meses do grupo, ex: $15\% / 60 = 0,25\%$ a.m.).
- **Fundo de Reserva (FR)**: Cobrança destinada a cobrir eventuais insuficiências de caixa coletivas (ex: $2\% / 60 = 0,0333\%$ a.m.).
- **Seguro (SEG)**: Cobertura contratada conforme a apólice do grupo (prestamista ou quebra de garantia).

### REQ-FUN-002: Hook de Consistência e Arredondamento
- **Regra**: O cálculo da soma dos componentes da `Parcela` é executado no backend de forma automática por hooks de banco de dados (`@PrePersist` e `@PreUpdate` no JPA). Isso evita inconsistências de arredondamento de centavos no banco.

### REQ-FUN-003: Segregação Contábil (Patrimônio de Afetação)
- **Regra**: Todo recurso que transita em uma `Parcela` deve ser direcionado à sua respectiva conta de destino no Razão Contábil através de partidas dobradas (COSIF):
  - Fundo Comum cobrado: Crédito na conta de obrigação do grupo (`2.1.2.10.10-6 - Fundo Comum de Grupos`).
  - Taxa de Administração: Crédito em receita/provisão da administradora (`2.1.2.10.30-2 - Taxa de Administração a Repassar`).
  - Fundo de Reserva: Crédito na conta de provisão de reserva do grupo (`2.1.2.10.20-9 - Fundo de Reserva de Grupos`).

---

## 🎯 Critérios de Aceitação (Given/When/Then)

### REQ-FUN-001 - AC1: Cálculo Correto dos Componentes
- **Given**: Um grupo com prazo de 60 meses, taxa de administração total de 15%, fundo de reserva de 2% e seguro de 0,05% a.m., com crédito de R$ 60.000,00.
- **When**: O sistema gera a parcela mensal para uma cota ativa do grupo.
- **Then**: O sistema calcula os componentes da parcela exatamente como:
  - Fundo Comum = R$ 1.000,00 ($1,6667\%$)
  - Taxa de Administração = R$ 150,00 ($0,25\%$)
  - Fundo de Reserva = R$ 20,00 ($0,0333\%$)
  - Seguro = R$ 30,00 ($0,05\%$)
  - Valor Total da Parcela = R$ 1.200,00

### REQ-FUN-002 - AC1: Acionamento Automático do Hook JPA
- **Given**: Um objeto `Parcela` instanciado com os componentes parciais preenchidos na memória.
- **When**: O repositório realiza a persistência (`save`) ou atualização da entidade `Parcela`.
- **Then**: A soma final é calculada na entidade antes de persistir, gravando R$ 1.200,00 no campo `valorTotal` da tabela de parcelas de forma consistente.
