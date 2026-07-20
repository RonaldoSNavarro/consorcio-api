# ðŸ“„ Spec-Driven Development: Reajustes e Encerramento (ADR 006)

**MÃ³dulo:** Encerramento de Grupos e Reajustes
*   **Status**: IMPLEMENTED v1.0
**Especialistas:** CTO, Analista de Sistemas

## 1. VisÃ£o Geral
Este documento define as regras de negÃ³cio de acordo com a ResoluÃ§Ã£o BCB nÂº 285/2023, que regulamenta a necessidade imperativa de o grupo de consÃ³rcio ser encerrado contabilmente e legalmente no prazo mÃ¡ximo de 120 dias contados da Ãºltima Assembleia Geral OrdinÃ¡ria (AGO). AlÃ©m disso, padroniza as rotinas de reajuste do valor do bem.

## 2. Requisitos de NegÃ³cio (Regras)

### REQ-ENC-001: Reajuste do Bem de ReferÃªncia
- **DescriÃ§Ã£o**: Sempre que o valor do bem de referÃªncia do grupo for reajustado (ex: INCC), o crÃ©dito das cotas ativas deve ser atualizado na mesma proporÃ§Ã£o, reajustando automaticamente o valor das parcelas futuras.
- **ValidaÃ§Ã£o**: O crÃ©dito nÃ£o pode ser reajustado se o grupo estiver ENCERRADO.

### REQ-ENC-002: Encerramento de Grupo e Baixa PDD (ADR 006)
- **DescriÃ§Ã£o**: No prazo limite de 120 dias da Ãºltima AGO, o grupo deve transitar para o status `ENCERRADO`.
- **Baixa ContÃ¡bil**: As cotas com parcelas PENDENTES ou ATRASADAS devem ter estas parcelas baixadas.
- **LanÃ§amentos no Ledger**:
  - `DÃ‰BITO`: 3.1.8.10.00-1 (Despesa de ProvisÃ£o para Devedores Duvidosos - PDD)
  - `CRÃ‰DITO`: 1.6.9.10.00-5 (Conta Retificadora PDD)
  - `DÃ‰BITO`: 1.6.9.10.00-5 (Conta Retificadora PDD)
  - `CRÃ‰DITO`: 1.2.1.10.00-8 (Valores a Receber de Consorciados)
- **CobranÃ§a Judicial**: A carteira de inadimplÃªncia torna-se extra-contÃ¡bil (jurÃ­dico).

### REQ-ENC-003: Recursos NÃ£o Procurados (RNP)
- **DescriÃ§Ã£o**: Saldos remanescentes devolvÃ­veis a consorciados excluÃ­dos que nÃ£o foram resgatados migrarÃ£o para a rubrica especÃ­fica de Recursos NÃ£o Procurados (RNP), cessando correÃ§Ã£o de taxa e encerrando o passivo circulante do grupo. *(Atualmente RNP retorna zerado como placeholder)*.

## 3. Modelo de Dados (TransiÃ§Ãµes)
- Entidade `Grupo`: AdiÃ§Ã£o do campo `dataEncerramento` e novo status `ENCERRADO`.
- Entidade `Parcela`: Status `BAIXADA`.

## 4. IntegraÃ§Ã£o Front-end
- A tela de encerramento (`EncerrarGrupoPage.jsx`) agrupa informaÃ§Ãµes do Grupo (status), Cotas (saÃºde da carteira), Assembleias (para calcular os 120 dias desde a Ãºltima AGO) e Financeiro (Fundo Comum e Reserva). O encerramento emite a mutation e o DTO final retorna o extrato da baixa do PDD para renderizaÃ§Ã£o.
