# Context Management — Compliance and PLD/FT Evolution

## Workspaces
- Project root: `f:\Dev\Projetos\consorcio-api`
- Orchestrator working directory: `f:\Dev\Projetos\consorcio-api\.agents\orchestrator`

## Key Files
- Spec: `f:\Dev\Projetos\consorcio-api\docs\specs\compliance\spec.md`
- Tasks: `f:\Dev\Projetos\consorcio-api\docs\specs\compliance\tasks.md`
- Services:
  - `MatchComplianceService.java`
  - `ContemplacaoService.java`
  - `CotaService.java`
  - `RelatorioService.java`
  - `LanceService.java`

## Enums and Fields
- `StatusAlertaCompliance`: `PENDENTE_ANALISE`, `FALSO_POSITIVO`, `CONFIRMADO`
- `TipoLance`: `EMBUTIDO`, `FIRME`, `MISTO`, `FGTS`, `SEGURO_OBITO`
- Siscoaf threshold: R$ 50.000,00 (`LIMITE_PLD_FT` in `RelatorioService.java`)
