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

## Frontend (`front_end_consorcio-api`)

- `[x]` **TASK-COMP-007 (Formulários de Cliente)**: Incluir no Zod schema e nos formulários UI os campos de renda mensal e patrimônio do cliente.
- `[x]` **TASK-COMP-008 (Painel de Compliance)**: Criar uma tela restrita ao `ROLE_COMPLIANCE` (menu lateral) contendo a tabela de alertas.
- `[x]` **TASK-COMP-009 (Ação de Deliberação)**: Implementar Modal no painel de compliance para alterar o status do alerta preenchendo justificativa textual obrigatória.
