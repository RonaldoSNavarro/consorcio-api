# 📄 Spec-Driven Development: Reajustes e Encerramento (ADR 006)

**Módulo:** Encerramento de Grupos e Reajustes
**Status:** IMPLEMENTED (Backend) / IN PROGRESS (Frontend)
**Especialistas:** CTO, Analista de Sistemas

## 1. Visão Geral
Este documento define as regras de negócio de acordo com a Resolução BCB nº 285/2023, que regulamenta a necessidade imperativa de o grupo de consórcio ser encerrado contabilmente e legalmente no prazo máximo de 120 dias contados da última Assembleia Geral Ordinária (AGO). Além disso, padroniza as rotinas de reajuste do valor do bem.

## 2. Requisitos de Negócio (Regras)

### REQ-ENC-001: Reajuste do Bem de Referência
- **Descrição**: Sempre que o valor do bem de referência do grupo for reajustado (ex: INCC), o crédito das cotas ativas deve ser atualizado na mesma proporção, reajustando automaticamente o valor das parcelas futuras.
- **Validação**: O crédito não pode ser reajustado se o grupo estiver ENCERRADO.

### REQ-ENC-002: Encerramento de Grupo e Baixa PDD (ADR 006)
- **Descrição**: No prazo limite de 120 dias da última AGO, o grupo deve transitar para o status `ENCERRADO`.
- **Baixa Contábil**: As cotas com parcelas PENDENTES ou ATRASADAS devem ter estas parcelas baixadas.
- **Lançamentos no Ledger**:
  - `DÉBITO`: 3.1.8.10.00-1 (Despesa de Provisão para Devedores Duvidosos - PDD)
  - `CRÉDITO`: 1.6.9.10.00-5 (Conta Retificadora PDD)
  - `DÉBITO`: 1.6.9.10.00-5 (Conta Retificadora PDD)
  - `CRÉDITO`: 1.2.1.10.00-8 (Valores a Receber de Consorciados)
- **Cobrança Judicial**: A carteira de inadimplência torna-se extra-contábil (jurídico).

### REQ-ENC-003: Recursos Não Procurados (RNP)
- **Descrição**: Saldos remanescentes devolvíveis a consorciados excluídos que não foram resgatados migrarão para a rubrica específica de Recursos Não Procurados (RNP), cessando correção de taxa e encerrando o passivo circulante do grupo. *(Atualmente RNP retorna zerado como placeholder)*.

## 3. Modelo de Dados (Transições)
- Entidade `Grupo`: Adição do campo `dataEncerramento` e novo status `ENCERRADO`.
- Entidade `Parcela`: Status `BAIXADA`.

## 4. Integração Front-end
- A tela de encerramento (`EncerrarGrupoPage.jsx`) agrupa informações do Grupo (status), Cotas (saúde da carteira), Assembleias (para calcular os 120 dias desde a última AGO) e Financeiro (Fundo Comum e Reserva). O encerramento emite a mutation e o DTO final retorna o extrato da baixa do PDD para renderização.
