# Relatórios e PLD/FT

Status: IMPLEMENTED
Versão: v1.1
Aprovações: Especialista Consórcios [✅] (19/06) | Especialista Contabilidade [✅] (19/06)
Última alteração: Correções regulatórias do limite PLD/FT e inclusão de inadimplência (Doc 2080).

## 1. Visão Geral
Esta capability define as regras para a extração de relatórios gerenciais, balancetes contábeis e alertas de monitoramento para Prevenção à Lavagem de Dinheiro e Financiamento ao Terrorismo (PLD/FT). O desenvolvimento no backend precedeu a especificação, configurando um SPEC DRIFT que agora é retro-documentado para validação regulatória e contábil.

## 2. Requisitos Funcionais e Regras de Negócio

### REQ-RELATORIOS-001: Alerta PLD/FT (Monitoramento)
- **Descrição**: O sistema deve monitorar lances suspeitos para apoiar o compliance com normas do Banco Central de PLD/FT.
- **Regras**:
  - Todo lance que for declarado VENCEDOR e cujo valor ofertado for igual ou superior a R$ 50.000,00 (LIMITE_PLD_FT) deve ser sinalizado.
  - O relatório deve ser filtrado por data de início e fim da oferta (`dataOferta`) contemplando apenas lances integralizados (vencedores).
  - O acesso é estritamente restrito aos perfis `ROLE_ADMIN` e `ROLE_AUDITOR`.
- **Acceptance Criteria**:
  - **Given** um usuário com perfil `ROLE_AUDITOR`
  - **When** solicitar o relatório de PLD/FT informando as datas do mês atual
  - **Then** deve receber a lista de lances vencedores maiores ou iguais a R$ 50.000,00 contendo dados do consorciado (CPF/CNPJ, Nome), valor da oferta, grupo e cota.

### REQ-RELATORIOS-002: Balancete Contábil (Doc 4110)
- **Descrição**: Geração do balancete consolidado das contas COSIF vinculadas ao grupo de consórcio.
- **Regras**:
  - Exibir a lista de contas contábeis mapeadas no grupo com seus respectivos saldos apurados em uma `dataReferencia`.
  - O saldo deve refletir os débitos e créditos registrados até as 23:59:59 da data solicitada.
  - O acesso é restrito aos perfis `ROLE_ADMIN` e `ROLE_AUDITOR`.
- **Acceptance Criteria**:
  - **Given** um grupo com histórico de arrecadação e contemplações
  - **When** um `ROLE_ADMIN` solicita o balancete de uma data de referência
  - **Then** o sistema retorna o saldo apurado de cada conta COSIF (natureza devedora ou credora).

### REQ-RELATORIOS-003: Estatísticas do Grupo (Doc 2080)
- **Descrição**: Resumo consolidado das movimentações do grupo no período para fins gerenciais.
- **Regras**:
  - Contabilizar o volume de adesões (cotas ativas) e exclusões (cotas canceladas).
  - Somar a quantidade de cotas inadimplentes no grupo (em atraso).
  - Listar o número total de lances ofertados e lances vencedores.
  - Segmentar o volume de contemplações por tipo (Sorteio vs Lance).
  - Somar o valor total de créditos efetivamente liberados no período.
- **Acceptance Criteria**:
  - **Given** que o grupo teve assembleias no mês passado
  - **When** extraídas as estatísticas de primeiro a trinta do mês
  - **Then** os totais de lances ofertados, vencedores, contemplações geradas e cotas inadimplentes devem coincidir com os registros do sistema no período.
