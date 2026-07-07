# BRIEFING — 2026-07-04T22:44:30-03:00

## Mission
Fix QA and Code Review findings in the consorcio-api project.

## 🔒 My Identity
- Archetype: Dev Full Stack Sênior
- Roles: implementer, qa, specialist
- Working directory: f:\Dev\Projetos\consorcio-api\.agents\worker_compliance_2
- Original parent: fd55759c-ae7b-4205-9348-602fa0166937
- Milestone: Compliance and Performance Optimizations

## 🔒 Key Constraints
- CODE_ONLY network mode: no external HTTP requests or network-based lookups.
- Minimal change principle.
- No git commits without explicit user request.
- Ensure all unit/integration tests compile and pass via `.\mvnw.cmd test`.

## Current Parent
- Conversation ID: fd55759c-ae7b-4205-9348-602fa0166937
- Updated: not yet

## Task Summary
- **What to build**: Add StatusCota.BLOQUEADA_COMPLIANCE, update ComplianceController to block cotas for ONU/OFAC, restrict payout of compliance blocked cotas in ContemplacaoService, optimize 10% group limit count, fix N+1 query issues in RelatorioService and CotaService, and optimize MatchComplianceService Jaro-Winkler comparison and memory efficiency.
- **Success criteria**: All specified fixes implemented, all tests compiling and passing.
- **Interface contracts**: f:\Dev\Projetos\consorcio-api\docs\specs\compliance\api-contract.md
- **Code layout**: f:\Dev\Projetos\consorcio-api\src\

## Key Decisions Made
- [TBD]

## Artifact Index
- f:\Dev\Projetos\consorcio-api\.agents\worker_compliance_2\handoff.md — Handoff report

## Change Tracker
- **Files modified**: None
- **Build status**: TBD
- **Pending issues**: None

## Quality Status
- **Build/test result**: TBD
- **Lint status**: TBD
- **Tests added/modified**: TBD

## Loaded Skills
- **Source**: f:\Dev\Projetos\consorcio-api\.agents\skills\consorcio-sdd\SKILL.md
  - **Local copy**: f:\Dev\Projetos\consorcio-api\.agents\skills\consorcio-sdd\SKILL.md
  - **Core methodology**: Spec-Driven Development orchestration.
- **Source**: f:\Dev\Projetos\consorcio-api\.agents\skills\consorcio-brasil\SKILL.md
  - **Local copy**: f:\Dev\Projetos\consorcio-api\.agents\skills\consorcio-brasil\SKILL.md
  - **Core methodology**: Brazilian consorcio system rules and algorithms.
- **Source**: f:\Dev\Projetos\consorcio-api\.agents\skills\java-springboot\SKILL.md
  - **Local copy**: f:\Dev\Projetos\consorcio-api\.agents\skills\java-springboot\SKILL.md
  - **Core methodology**: Spring Boot development best practices.
- **Source**: f:\Dev\Projetos\consorcio-api\.agents\skills\java-junit\SKILL.md
  - **Local copy**: f:\Dev\Projetos\consorcio-api\.agents\skills\java-junit\SKILL.md
  - **Core methodology**: JUnit 5 unit testing best practices.
- **Source**: f:\Dev\Projetos\consorcio-api\.agents\skills\java-docs\SKILL.md
  - **Local copy**: f:\Dev\Projetos\consorcio-api\.agents\skills\java-docs\SKILL.md
  - **Core methodology**: Javadoc documentation guidelines.
