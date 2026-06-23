# Planejamento de Execução — Compliance (PLD/FT)

## Backend (`consorcio-api`)

- `[x]` **TASK-COMP-001 (Domínio e Flyway)**: Criar entidades `ListaRestritiva` (campos: nome, documentoOrigem, origem(Enum), dataInclusao) e `AlertaCompliance` (campos: clienteId, listaId, score, status, justificativa, dataDeteccao). Criar migration correspondente.
- `[x]` **TASK-COMP-002 (Cliente DTOs)**: Adicionar os novos campos `rendaMensalDeclarada` e `patrimonioEstimado` ao `ClienteResponseDTO` e forms de cadastro/alteração, refletindo o `REQUIREMENTS.md`.
- `[x]` **TASK-COMP-003 (Integração Feign/RestTemplate)**: Implementar clientes HTTP para consumo das APIs externas:
  - Portal Transparência (PEP)
  - OFAC Sanctions / ONU Consolidated
- `[x]` **TASK-COMP-004 (Sincronização)**: Criar o `ComplianceSincronizacaoService` com anotação `@Scheduled(cron = "0 0 3 * * *")` (Roda às 3h da manhã) e o respectivo endpoint REST `/api/compliance/sincronizar`.
- `[x]` **TASK-COMP-005 (Algoritmo de Match)**: Implementar a rotina de cruzamento de dados. Quando um cliente for inserido/alterado (via evento) ou na rotina noturna, comparar o nome/documento com a tabela `ListaRestritiva`. Usar Apache Commons Text (Jaro-Winkler) para nomes.
- `[x]` **TASK-COMP-006 (Endpoints de Auditoria)**: Criar o Controller `/api/compliance/alertas` para listagem e deliberação (REQ-COMP-005).
- `[ ]` **TASK-COMP-010 (Banco e Configuração)**: Criar a tabela `compliance_config` via Flyway e a respectiva entidade/repositório JPA para armazenar as expressões cron, frequência e horários configurados.
- `[ ]` **TASK-COMP-011 (Dynamic Scheduler Configurer)**: Implementar um agendador dinâmico (`SchedulingConfigurer`) que carrega e aplica a expressão cron da tabela `compliance_config` a cada agendamento sem necessidade de reboot.
- `[ ]` **TASK-COMP-012 (Uploads de Arquivos)**: Criar endpoints multipart em `ComplianceController` para receber arquivos PEP (CSV), ONU (XML) e IBGE (XLS) e passá-los para serviços de processamento em lote.
- `[ ]` **TASK-COMP-013 (Parsers e Persistência)**: Implementar os parsers:
  - PEP CSV: processa linha a linha, extrai nomes e os 6 dígitos centrais de CPFs, salvando em `ListaRestritiva` (PEP).
  - ONU XML: lê o arquivo XML consolidado via DOM/StAX, extrai nomes de indivíduos e entidades e salva na `ListaRestritiva` (ONU).
  - IBGE XLS: adiciona dependência Apache POI, lê a aba de Faixa de Fronteira / Cidades Gêmeas e indexa em `ListaRestritiva` (IBGE).
- `[ ]` **TASK-COMP-014 (Integração Real com OFAC)**: Implementar HttpClient real apontando para `https://sanctionslistservice.ofac.treas.gov/api/download/CONS_ADVANCED.XML` ou `SDN.XML`. Criar rotina resiliente que loga erros e prossegue caso a API esteja inacessível.
- `[ ]` **TASK-COMP-015 (Regras de Match Avançado)**:
  - Comparação PEP: dígitos centrais do CPF + nome JW >= 90%.
  - Comparação IBGE: cidade e UF do cliente coincidem com os municípios cadastrados do IBGE (alertas de origem `IBGE`).

## Frontend (`front_end_consorcio-api`)

- `[x]` **TASK-COMP-007 (Formulários de Cliente)**: Incluir no Zod schema e nos formulários UI os campos de renda mensal e patrimônio do cliente.
- `[x]` **TASK-COMP-008 (Painel de Compliance)**: Criar uma tela restrita ao `ROLE_COMPLIANCE` (menu lateral) contendo a tabela de alertas.
- `[x]` **TASK-COMP-009 (Ação de Deliberação)**: Implementar Modal no painel de compliance para alterar o status do alerta preenchendo justificativa textual obrigatória.
- `[ ]` **TASK-COMP-016 (Upload de Arquivos na UI)**: Criar aba de uploads no painel de compliance com drag-and-drop para os arquivos PEP, ONU e IBGE, incluindo barras de progresso ou loaders.
- `[ ]` **TASK-COMP-017 (Configuração de Agendamento UI)**: Criar formulário na UI para alterar a frequência (Diária, Semanal, Mensal) e o horário de execução do Job de Compliance.

