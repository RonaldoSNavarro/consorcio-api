# Implementation & Verification Plan — Compliance and PLD/FT

This plan details the steps to audit, test, correct, and evolve the Compliance and PLD/FT module in `consorcio-api`, following the SDD pipeline.

## Milestones

### Milestone 1: Deep Exploration & Codebase Audit [DONE]
- **Objective**: Use `teamwork_preview_explorer` to inspect existing code, verify behavior of `MatchComplianceService`, `ContemplacaoService`, `CotaService`, `LanceService`, `RelatorioService`, and identify potential bugs (like database enum mismatches in `V40__inserir_mock_pld.sql`).
- **Input**: Current codebase and tests.
- **Output**: Detailed audit report listing all logic/contract gaps.

### Milestone 2: Code Implementation & Corrections [DONE]
- **Objective**: Implement all required compliance rules and database migrations.
- **Tasks**:
  1. Add Flyway migration for `notificarSiscoaf` (boolean flag) on `lances` table, and map it to `Lance` model.
  2. Update `MatchComplianceService` to ensure correctness of Jaro-Winkler logic (limiar >= 0.90) and PEP centralized CPF logic.
  3. Integrate blocking rules (block operations if client has `PENDENTE_ANALISE` or `CONFIRMADO` alerts):
     - In `ContemplacaoService.registrar()` (contemplation approval).
     - In `CotaService.transferirCota()` (cota transfer for both current titular and new client).
     - In `PropostaAdesaoService.criarProposta()` / `aprovarProposta()`.
  4. Implement Siscoaf trigger for lances: set `notificarSiscoaf` on `Lance` when a bid is marked as a winner (in `MotorApuracaoService` or `ContemplacaoService`) if the value is >= R$ 50.000,00 and the type is `FIRME` (own resources).

### Milestone 2.1: Compliance Refinements & Optimizations [IN_PROGRESS]
- **Objective**: Refine status mappings, block payment loop-holes, optimize query performance (N+1 queries), and fix compilation errors in tests.
- **Tasks**:
  1. Add `BLOQUEADA_COMPLIANCE` enum to `StatusCota.java`.
  2. Add `List<Cota> findByClienteId(Long clienteId);` to `CotaRepository.java`.
  3. Add `long countByGrupoIdAndClienteIdAndStatus(Long grupoId, Long clienteId, StatusCota status);` to `CotaRepository.java`.
  4. Update `ComplianceController.deliberarSobreAlerta()` to set client cotas to `StatusCota.BLOQUEADA_COMPLIANCE` upon confirming (CONFIRMADO) a Terrorism alert (ONU/OFAC).
  5. Update `CotaService.java`:
     - Block refunds (`reembolsarCota()`) if client has active/confirmed alerts or cota is blocked.
     - Optimize 10% group concentration limit checks by using the DB-level count query instead of in-memory streaming.
  6. Update `ContemplacaoService.java`:
     - Block payouts (`pagarBem()`) if client has active/confirmed alerts or cota is blocked.
  7. Optimize N+1 queries in `RelatorioService.gerarAlertaPldFt()` by using a JOIN FETCH query in `LanceRepository`.
  8. Fix Jaro-Winkler empty-name matching edge cases in `MatchComplianceService.java` to prevent false positive matches on empty names.
  9. Clean up and verify compilation of `ComplianceChallengerTest.java` and all other tests, ensuring 100% pass rate.

### Milestone 3: Review and Verification (QA/Reviewer/Challenger/Auditor) [PLANNED]
- **Objective**: Independent review, challenger validation, and forensic audit of all implementations including refinements.

### Milestone 4: Final Sign-off & Report [PLANNED]
- **Objective**: Deliver results and documentation to Sentinel.

## Iteration Status
- Current iteration: 1 / 32
