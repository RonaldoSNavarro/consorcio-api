# RelatÃ³rios e PLD/FT

*   **Status**: IMPLEMENTED v1.0
VersÃ£o: v1.1
AprovaÃ§Ãµes: Especialista ConsÃ³rcios [âœ…] (19/06) | Especialista Contabilidade [âœ…] (19/06)
Ãšltima alteraÃ§Ã£o: CorreÃ§Ãµes regulatÃ³rias do limite PLD/FT e inclusÃ£o de inadimplÃªncia (Doc 2080).

## 1. VisÃ£o Geral
Esta capability define as regras para a extraÃ§Ã£o de relatÃ³rios gerenciais, balancetes contÃ¡beis e alertas de monitoramento para PrevenÃ§Ã£o Ã  Lavagem de Dinheiro e Financiamento ao Terrorismo (PLD/FT). O desenvolvimento no backend precedeu a especificaÃ§Ã£o, configurando um SPEC DRIFT que agora Ã© retro-documentado para validaÃ§Ã£o regulatÃ³ria e contÃ¡bil.

## 2. Requisitos Funcionais e Regras de NegÃ³cio

### REQ-RELATORIOS-001: Alerta PLD/FT (Monitoramento)
- **DescriÃ§Ã£o**: O sistema deve monitorar lances suspeitos para apoiar o compliance com normas do Banco Central de PLD/FT.
- **Regras**:
  - Todo lance que for declarado VENCEDOR e cujo valor ofertado for igual ou superior a R$ 50.000,00 (LIMITE_PLD_FT) deve ser sinalizado.
  - O relatÃ³rio deve ser filtrado por data de inÃ­cio e fim da oferta (`dataOferta`) contemplando apenas lances integralizados (vencedores).
  - O acesso Ã© estritamente restrito aos perfis `ROLE_ADMIN` e `ROLE_AUDITOR`.
- **Acceptance Criteria**:
  - **Given** um usuÃ¡rio com perfil `ROLE_AUDITOR`
  - **When** solicitar o relatÃ³rio de PLD/FT informando as datas do mÃªs atual
  - **Then** deve receber a lista de lances vencedores maiores ou iguais a R$ 50.000,00 contendo dados do consorciado (CPF/CNPJ, Nome), valor da oferta, grupo e cota.

### REQ-RELATORIOS-002: Balancete ContÃ¡bil (Doc 4110)
- **DescriÃ§Ã£o**: GeraÃ§Ã£o do balancete consolidado das contas COSIF vinculadas ao grupo de consÃ³rcio.
- **Regras**:
  - Exibir a lista de contas contÃ¡beis mapeadas no grupo com seus respectivos saldos apurados em uma `dataReferencia`.
  - O saldo deve refletir os dÃ©bitos e crÃ©ditos registrados atÃ© as 23:59:59 da data solicitada.
  - O acesso Ã© restrito aos perfis `ROLE_ADMIN` e `ROLE_AUDITOR`.
- **Acceptance Criteria**:
  - **Given** um grupo com histÃ³rico de arrecadaÃ§Ã£o e contemplaÃ§Ãµes
  - **When** um `ROLE_ADMIN` solicita o balancete de uma data de referÃªncia
  - **Then** o sistema retorna o saldo apurado de cada conta COSIF (natureza devedora ou credora).

### REQ-RELATORIOS-003: EstatÃ­sticas do Grupo (Doc 2080)
- **DescriÃ§Ã£o**: Resumo consolidado das movimentaÃ§Ãµes do grupo no perÃ­odo para fins gerenciais.
- **Regras**:
  - Contabilizar o volume de adesÃµes (cotas ativas) e exclusÃµes (cotas canceladas).
  - Somar a quantidade de cotas inadimplentes no grupo (em atraso).
  - Listar o nÃºmero total de lances ofertados e lances vencedores.
  - Segmentar o volume de contemplaÃ§Ãµes por tipo (Sorteio vs Lance).
  - Somar o valor total de crÃ©ditos efetivamente liberados no perÃ­odo.
- **Acceptance Criteria**:
  - **Given** que o grupo teve assembleias no mÃªs passado
  - **When** extraÃ­das as estatÃ­sticas de primeiro a trinta do mÃªs
  - **Then** os totais de lances ofertados, vencedores, contemplaÃ§Ãµes geradas e cotas inadimplentes devem coincidir com os registros do sistema no perÃ­odo.
