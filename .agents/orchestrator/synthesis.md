# Synthesis — Compliance and PLD/FT Codebase Audit

This document consolidates and reconciles the findings from the Explorer subagents (`explorer_compliance_2` and `explorer_compliance_3`).

## Consensus
Both Explorer 2 and 3 identified the following critical gaps and issues:
1. **Jaro-Winkler Matching Threshold**: The similarity threshold (`0.90`) is currently hardcoded in `MatchComplianceService.java` (lines 59, 66). It needs to be configurable via properties.
2. **Missing Transactional Compliance Blocks**:
   - `PropostaAdesaoService.java`: Missing blocks in `aprovarProposta` and `efetivarContrato`.
   - `ContemplacaoService.java`: Completely missing blocks in `registrar` (contemplation approval).
   - `CotaService.java`: Missing blocks in `transferirCota` for the current cota owner (cedente).
3. **Lance Siscoaf Flag**: The `notificarSiscoaf` boolean flag must be added to the `lances` table and `Lance` entity to mark winning bids >= R$ 50.000,00 funded via own resources (`FIRME`).
4. **Mock Data Corruptions in V40**:
   - `V40__inserir_mock_pld.sql` inserts `'LANCE_LIVRE'` for `tipo` (mapped to `TipoLance` enum) and `'FINANCEIRO'` for `modalidade` (mapped to `ModalidadeLance` enum). These cause database read operations to fail due to deserialization issues (unknown enum values).

## Resolved Conflicts and Unique Findings
- **CPF Digits Central Extractor (Unique to Explorer 2)**:
  - In `MatchComplianceService.java`, the method `obterDigitosCentraisCpfPep()` only handles 6-digit masked CPFs (`***.531.324-**`). If the list has a full 11-digit unmasked CPF, matching fails because it returns `null`.
  - **Resolution**: Refactor the method to handle both 6-digit and 11-digit CPFs.
- **NPE Risk in Normalization (Unique to Explorer 2)**:
  - In `MatchComplianceService.java` at line 43, `cliente.getLocalidade().trim()` and `cliente.getUf().trim()` will throw a `NullPointerException` if either address field is null.
  - **Resolution**: Implement null-safety logic before calling `.trim()`.
- **Readmission Blocking (Unique to Explorer 2)**:
  - In `CotaService.java`, `readmitirCota()` lacks compliance checking.
  - **Resolution**: Add compliance check to block readmission if the client has alerts in `PENDENTE_ANALISE` or `CONFIRMADO`.

## Gaps
None. The coverage is complete.

## Unified Implementation Tasks (to be assigned to Worker)
1. **Flyway Migration V48**: Fix database mock values in `lances` from `V40`.
2. **Flyway Migration V49**: Add `notificar_siscoaf` column to `lances` table.
3. **Entity `Lance.java` Update**: Map `notificarSiscoaf` field and implement evaluation logic using `@PrePersist` and `@PreUpdate` callbacks.
4. **Normalization & Matching Configuration**:
   - Inject `@Value("${compliance.similarity.threshold:0.90}")` into `MatchComplianceService.java`.
   - Update address normalizations to be null-safe.
   - Refactor `obterDigitosCentraisCpfPep` to support 11-digit CPFs.
5. **Transactional Blocks**:
   - Add checks in `ContemplacaoService.registrar()`.
   - Add checks in `CotaService.transferirCota()` (for the current owner) and `readmitirCota()`.
   - Add checks in `PropostaAdesaoService.aprovarProposta()` and `efetivarContrato()`.
