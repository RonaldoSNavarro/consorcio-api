## 2026-07-04T21:35:50Z
You are the Worker subagent (Identity: worker_compliance).
Your working directory is f:\Dev\Projetos\consorcio-api\.agents\worker_compliance.
Your task is to implement the modifications detailed in the synthesis file: f:\Dev\Projetos\consorcio-api\.agents\orchestrator\synthesis.md.

MANDATORY INTEGRITY WARNING:
> DO NOT CHEAT. All implementations must be genuine. DO NOT
> hardcode test results, create dummy/facade implementations, or
> circumvent the intended task. A Forensic Auditor will independently
> verify your work. Integrity violations WILL be detected and your
> work WILL be rejected.

Please execute the following implementation tasks:
1. **Flyway Migration V48**: Fix invalid enum values in the `lances` table from V40 (change 'LANCE_LIVRE' to 'FIRME' and 'FINANCEIRO' to 'LIVRE').
2. **Flyway Migration V49**: Add column `notificar_siscoaf` (BOOLEAN, DEFAULT FALSE, NOT NULL) to the `lances` table.
3. **Lance Entity Updates**: Map the new column as `notificarSiscoaf` (boolean) in `Lance.java`. Encapsulate the Siscoaf notification logic: set it to true if the status is VENCEDOR, the type is FIRME (own resources), and the valorOferta is >= R$ 50.000,00. You can use JPA lifecycle callbacks (@PrePersist and @PreUpdate) inside Lance.java.
4. **Configurable Jaro-Winkler & Safe Normalization**:
   - Inject the threshold in `MatchComplianceService.java` using `@Value("${compliance.similarity.threshold:0.90}")`. Add a fallback to 0.90 if property is missing.
   - Inject this property value (e.g. `compliance.similarity.threshold=0.90`) in `application.properties` (and make sure to update test configurations if needed).
   - Fix null-safety bugs in address normalizations inside `MatchComplianceService.java` (lines 43-44) to prevent NullPointerExceptions.
   - Update `obterDigitosCentraisCpfPep()` in `MatchComplianceService.java` to handle both 6-digit (masked) and 11-digit (unmasked) CPFs.
5. **Enforce Transaction Blocks**:
   - Inject `AlertaComplianceRepository` into `ContemplacaoService.java` and block contemplations in `registrar()` if the client has alerts in status PENDENTE_ANALISE or CONFIRMADO.
   - Update `CotaService.transferirCota()` to check both the new receiver client AND the current owner (cedente) for compliance alerts, and block if either is flagged.
   - Update `CotaService.readmitirCota()` to check the client, and block if they have restricted compliance alerts.
   - Update `PropostaAdesaoService.java` to block proposal approvals and contract effectiveness triggers if the client has restricted compliance alerts.

6. **Add Automated Tests**:
   - Write a `@ParameterizedTest` in a test class (such as `ComplianceServiceTest.java`) to test Jaro-Winkler logic on various phonetic/typographic similar names.
   - Write an automated integration or unit test verifying that a client with a PENDENTE_ANALISE alert is blocked when attempting to perform a Cota Transfer (TransferenciaCota) or receive a Contemplation.
   - Write a test verifying that the Siscoaf flag is triggered and set to true for lances >= R$ 50.000,00 of type FIRME, and is false otherwise.

Please compile the project, run all tests, verify they pass, and output your handoff report to handoff.md in your working directory.
Refer to consorcio-sdd and consorcio-brasil skills for development guidelines.
