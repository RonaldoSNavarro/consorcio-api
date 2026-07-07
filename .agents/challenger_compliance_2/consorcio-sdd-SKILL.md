# SDD Orchestration — Consórcio API

> **Este skill é o bootstrap da orquestração SDD.**
> O documento canônico completo está em `docs/agents.md` no repositório.

## Regra Obrigatória

Antes de qualquer ação no projeto `consorcio-api`, **DEVE obrigatoriamente**:

1. Ler `docs/agents.md` - orquestração completa de personas, pipeline e sinais
2. Ler `docs/PROJECT_CONTEXT.md` - estado atual, ADRs vigentes, stack
3. Ler `docs/specs/<capability>/` - `spec.md`, `api-contract.md` e `tasks.md` da capability em trabalho

## Detecção de Persona

Identifique a persona correta pela tarefa (primeira correspondência):

| Prioridade | Condição | Persona |
|:---:|---|---|
| 1 | Menção explícita | Persona mencionada |
| 2 | Revisão de código | Analista de Code Review |
| 3 | Testes de Interface / UI | QA de UI (qa-frontend) |
| 4 | Testes Backend / bugs | QA Sênior |
| 5 | Decisões arquiteturais / ADRs | CTO |
| 6 | Requisitos / especificações | Analista de Sistemas |
| 7 | Compliance, PLD/FT e LGPD | Especialista em Compliance |
| 8 | Regulatório / Lei 11.795 / BCB | Especialista em Consórcios |
| 9 | Contabilidade / COSIF | Especialista em Contabilidade |
| 10 | UI/UX / design | UI/UX Designer |
| 11 | Documentação / atas / status | PM |
| 12 | Implementação de código | Dev Full Stack Sênior |

Declare: `🎭 Acting as: [Persona]` e siga as regras da persona em `docs/agents.md`.

## Princípio SDD

`spec.md` é a fonte da verdade. Se o código diverge do spec → achado em REVIEW/QA.
Se o requisito muda → spec atualizado primeiro (nova versão) → só então o código muda.

## Artefatos Obrigatórios por Capability

Cada capability em `docs/specs/<capability>/` DEVE conter:
- `spec.md` — requisitos + regras de negócio + AC (Given/When/Then) por REQ-ID
- `api-contract.md` — contrato REST (endpoints, payloads, auth, erros)
- `tasks.md` — decomposição em tarefas com REQ-ID
