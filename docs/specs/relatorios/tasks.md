# Tarefas de Implementação — Relatórios e PLD/FT

Spec ref: `docs/specs/relatorios/spec.md` (v1.1)

## Backend (`consorcio-api`)

- `[x]` **TASK-REL-001 (Refatoração)**: Ajustar `RelatorioService.gerarAlertaPldFt` para filtrar apenas lances com `statusApuracao = VENCEDOR` e `valorOferta >= 50.000,00` no período informado (REQ-RELATORIOS-001).
- `[x]` **TASK-REL-002 (DTO)**: Adicionar `long totalCotasInadimplentes` em `EstatisticasGrupoResponseDTO` e atualizá-lo no `RelatorioController` (REQ-RELATORIOS-003).
- `[x]` **TASK-REL-003 (Service)**: Atualizar `RelatorioService.gerarEstatisticas` para consultar no repositório a quantidade de cotas inadimplentes do grupo (> 3 parcelas em atraso) e popular no DTO.

## Frontend (`front_end_consorcio-api`)

- `[ ]` **TASK-REL-004**: Atualizar os schemas Zod e interfaces TypeScript (`EstatisticasGrupoResponseDTO`) para incluir o campo `totalCotasInadimplentes`.
- `[ ]` **TASK-REL-005**: Garantir que as chamadas para os endpoints de PLD/FT passem as datas corretas e tratem adequadamente o retorno atualizado.
