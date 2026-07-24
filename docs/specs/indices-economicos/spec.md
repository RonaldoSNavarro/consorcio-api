# Especificação de Funcionalidade — Integração com Índices Econômicos BACEN (INCC-M, IPCA, IGP-M)

## 1. Visão Geral
Integração com a API pública do Banco Central do Brasil (SGS) para obter as variações mensais e calcular a variação acumulada dos últimos 12 meses dos principais índices de reajuste de consórcios no Brasil: **INCC-M**, **IPCA** e **IGP-M**.

## 2. Requisitos Funcionais
- **RF-01**: Consultar em tempo real as séries temporais do BACEN SGS:
  - INCC-M (Série `192` - FGV)
  - IPCA (Série `433` - IBGE)
  - IGP-M (Série `189` - FGV)
- **RF-02**: Armazenar o histórico de variações na tabela `indices_economicos_historico`.
- **RF-03**: Calcular a variação acumulada dos últimos 12 meses segundo a fórmula:
  $$\text{Fator Acumulado 12M} = \prod_{i=1}^{12} \left(1 + \frac{\text{taxa}_i}{100}\right)$$
- **RF-04**: Permitir simulação de reajuste de crédito informando um valor base.
- **RF-05**: Reajustar o grupo e seus bens de referência, recalculando proporcionalmente todas as parcelas pendentes (`PENDENTE`, `ATRASADA`).
- **RF-06**: Executar job automático `@Scheduled` no 1º dia de cada mês para os grupos em andamento que fazem aniversário no mês (`mesReajuste`).
- **RF-07**: Exibir no front-end a modal `IndicesEconomicosModal` com gráficos, tabela mensal e simulador, além das ações de reajuste nos grupos.

## 3. Requisitos Não Funcionais
- **RNF-01**: Cache local no PostgreSQL para garantir funcionamento mesmo em indisponibilidade pontual da API do BACEN.
- **RNF-02**: Performance de resposta inferior a 500ms para requisições de simulação.
