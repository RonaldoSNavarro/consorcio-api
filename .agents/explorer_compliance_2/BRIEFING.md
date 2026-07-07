# BRIEFING — 2026-07-04T18:32:00-03:00

## Mission
Investigate compliance module Jaro-Winkler logic, blocking rules integration, Lance entity and schema change for Siscoaf notification, and SQL mock file validation in consorcio-api.

## 🔒 My Identity
- Archetype: Explorer
- Roles: Investigator, Auditor
- Working directory: f:\Dev\Projetos\consorcio-api\.agents\explorer_compliance_2
- Original parent: 358af94b-f988-4231-a18a-1143f525ef90
- Milestone: Compliance Audit

## 🔒 Key Constraints
- Read-only investigation — do NOT implement
- Operational compliance audit only

## Current Parent
- Conversation ID: 358af94b-f988-4231-a18a-1143f525ef90
- Updated: not yet

## Investigation State
- **Explored paths**:
  - `MatchComplianceService.java`
  - `PropostaAdesaoService.java`
  - `ContemplacaoService.java`
  - `CotaService.java`
  - `Lance.java` & `TipoLance.java` & `ModalidadeLance.java`
  - `V40__inserir_mock_pld.sql` & SQL migrations
  - `ComplianceServiceTest.java`
- **Key findings**:
  - **Jaro-Winkler**: Similarity threshold is hardcoded (`0.90`), not configurable. Normalization converts to UPPERCASE and strips diacritics using NFD. Potential NPE in `clientCityState` construction if `localidade` or `uf` is null (since `trim()` is called on them). PEP matching has a bug: if the PEP record has an 11-digit CPF (unmasked), `obterDigitosCentraisCpfPep` returns `null` because it only handles 6-digit CPFs.
  - **Blocking Rules**:
    - `PropostaAdesaoService.java`: Checks alerts during `criarProposta` but NOT during `aprovarProposta` or `efetivarContrato`.
    - `ContemplacaoService.java`: Completely lacks any PLD/FT alerts check. A blocked client can be contemplado.
    - `CotaService.java`: Checks alerts during `salvar` (creation) and `transferirCota` (destination client). But it does NOT check alerts during `readmitirCota`.
  - **Lance Entity**: We can add `notificarSiscoaf` (boolean) to `Lance` entity and schema. Best place to set to true is either in `@PreUpdate`/`@PrePersist` of `Lance` or in `MotorApuracaoService` when classifying as `VENCEDOR`.
  - **SQL Mock Validation**: `V40__inserir_mock_pld.sql` uses invalid enum values `'LANCE_LIVRE'` (for `tipo`) and `'FINANCEIRO'` (for `modalidade`). This will crash any JPA operation loading this entity. Correct values are `'FIRME'` and `'LIVRE'`.
- **Unexplored areas**: None (all investigated).

## Key Decisions Made
- Audited all requested services and SQL files.
- Executed clean compile to fix MapStruct class loader issues.

## Artifact Index
- f:\Dev\Projetos\consorcio-api\.agents\explorer_compliance_2\ORIGINAL_REQUEST.md — Original request
- f:\Dev\Projetos\consorcio-api\.agents\explorer_compliance_2\BRIEFING.md — Working briefing index
