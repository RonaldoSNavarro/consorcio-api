# Handoff Report — Compliance Code Review

## 1. Observation

During the review of the compliance changes implemented in the `consorcio-api` repository, the following observations were made:

### Maven Test Execution Output
Running the command `.\mvnw.cmd test` resulted in a **BUILD FAILURE** due to compile-time errors in the test suite:
```
[INFO] --- compiler:3.13.0:testCompile (default-testCompile) @ consorcio-api ---
[INFO] Recompiling the module because of changed source code.
[INFO] Compiling 27 source files with javac [debug parameters release 21] to target\test-classes
[INFO] -------------------------------------------------------------
[ERROR] COMPILATION ERROR : 
[INFO] -------------------------------------------------------------
[ERROR] /F:/Dev/Projetos/consorcio-api/src/test/java/br/com/estudo/consorcio/service/ComplianceChallengerTest.java:[169,51] constructor ContemplacaoService in class br.com.estudo.consorcio.service.ContemplacaoService cannot be applied to given types;
  required: br.com.estudo.consorcio.domain.repository.ContemplacaoRepository,br.com.estudo.consorcio.domain.repository.AssembleiaRepository,br.com.estudo.consorcio.domain.repository.CotaRepository,br.com.estudo.consorcio.domain.repository.ParcelaRepository,br.com.estudo.consorcio.domain.mapper.ContemplacaoMapper,br.com.estudo.consorcio.service.ContabilidadeService,br.com.estudo.consorcio.service.CotaService,br.com.estudo.consorcio.service.HistoricoConsorciadoService,br.com.estudo.consorcio.domain.repository.LanceRepository,br.com.estudo.consorcio.domain.mapper.CotaMapper,br.com.estudo.consorcio.domain.repository.AlertaComplianceRepository
  found:    br.com.estudo.consorcio.domain.repository.ContemplacaoRepository,br.com.estudo.consorcio.domain.repository.CotaRepository,br.com.estudo.consorcio.domain.repository.AssembleiaRepository,br.com.estudo.consorcio.domain.repository.ParcelaRepository,br.com.estudo.consorcio.domain.mapper.ContemplacaoMapper,br.com.estudo.consorcio.service.HistoricoConsorciadoService,br.com.estudo.consorcio.domain.repository.HistoricoVersaoCotaRepository,br.com.estudo.consorcio.service.MovimentoFinanceiroService,br.com.estudo.consorcio.service.ContabilidadeService,br.com.estudo.consorcio.domain.repository.AlertaComplianceRepository
  reason: actual and formal argument lists differ in length
[ERROR] /F:/Dev/Projetos/consorcio-api/src/test/java/br/com/estudo/consorcio/service/ComplianceChallengerTest.java:[189,66] incompatible types: java.time.LocalDateTime cannot be converted to java.time.LocalDate
[ERROR] /F:/Dev/Projetos/consorcio-api/src/test/java/br/com/estudo/consorcio/service/ComplianceChallengerTest.java:[209,9] cannot find symbol
  symbol:   class CotaMapper
  location: class br.com.estudo.consorcio.service.ComplianceChallengerTest
[ERROR] /F:/Dev/Projetos/consorcio-api/src/test/java/br/com/estudo/consorcio/service/ComplianceChallengerTest.java:[209,34] cannot find symbol
  symbol:   class CotaMapper
  location: class br.com.estudo.consorcio.service.ComplianceChallengerTest
[INFO] 4 errors 
```

### Source Code Analysis
1. **MatchComplianceService.java**:
   - Similarity threshold is loaded dynamically via `@Value("${compliance.similarity.threshold:0.90}")` (line 23-24).
   - Jaro-Winkler logic uses `org.apache.commons.text.similarity.JaroWinklerSimilarity` (lines 11, 47, 68, 76).
   - PEP masked CPF checks (`obterDigitosCentraisCpfPep`, lines 108-118) correctly extract 6 digits if length is 6 (e.g. from `***.531.324-**` to `531324`) or extract index 3 to 9 (e.g. `substring(3, 9)`) if length is 11.
   - Null-safety checks: `obterDigitosCentraisCpf` (lines 99-106) and `normalizar` (lines 120-124) handle null inputs safely.

2. **ContemplacaoService.java, CotaService.java, PropostaAdesaoService.java**:
   - Blocking rules for clients with alerts `PENDENTE_ANALISE` or `CONFIRMADO` were implemented using `alertaComplianceRepository.existsByClienteIdAndStatusIn(...)`.
   - `ContemplacaoService.java` blocks contemplations at line 78-84.
   - `CotaService.java` blocks creation/saving at line 133, transfer (both origin and destination) at lines 433 and 441, and readmission at line 514.
   - `PropostaAdesaoService.java` blocks proposal creation at line 52, approval at line 89, and contract execution at line 124.

3. **Lance.java & LanceService.java**:
   - `Lance` entity uses `@PrePersist` and `@PreUpdate` hooks to evaluate the `notificarSiscoaf` flag via `atualizarNotificarSiscoaf()`:
     ```java
     this.notificarSiscoaf = this.statusApuracao == StatusApuracaoLance.VENCEDOR
             && this.tipo == TipoLance.FIRME
             && this.valorOferta != null
             && this.valorOferta.compareTo(new BigDecimal("50000.00")) >= 0;
     ```
   - Own-resource (FIRME) winning bids >= 50,000.00 are correctly targeted.

4. **Database Migrations**:
   - `V48__corrigir_mock_lances.sql` fixes legacy mock data: `UPDATE lances SET tipo = 'FIRME', modalidade = 'LIVRE' WHERE tipo = 'LANCE_LIVRE' AND modalidade = 'FINANCEIRO';`.
   - `V49__adicionar_notificar_siscoaf_lances.sql` adds column: `ALTER TABLE lances ADD COLUMN notificar_siscoaf BOOLEAN DEFAULT FALSE NOT NULL;`.

5. **Spring standards, JPA practices, Lombok**:
   - Lombok annotations `@Data`, `@NoArgsConstructor`, `@AllArgsConstructor` are used on model classes.
   - `Lance.java` has a custom 9-parameter compatibility constructor (lines 54-65) to support old MapStruct / test calls that do not supply `notificarSiscoaf`.
   - **In-memory filtering of cotas**: `CotaService.java` loads all cotas of a group into memory using `cotaRepository.findByGrupoId(...)` (lines 144, 453, 531) to count active cotas for a specific client (checking the 10% limit).

---

## 2. Logic Chain

1. **Compilation Failure**:
   - The test file `ComplianceChallengerTest.java` is instantiating `ContemplacaoService` and `CotaService` using their old constructors (10 arguments), but the actual implementation constructors were refactored to require 11 arguments (including `AlertaComplianceRepository`, `CotaMapper`, etc.).
   - Furthermore, `Assembleia.dataAssembleia` was changed from `LocalDateTime` to `LocalDate`, but the test is still calling `LocalDateTime.now().plusDays(1)`.
   - These constructor/type mismatches and a missing import for `CotaMapper` prevent compilation of the test package.
   - *Conclusion*: The test suite cannot run or pass, necessitating a verdict of `REQUEST_CHANGES`.

2. **In-Memory Filtering Risk (JPA Anti-pattern)**:
   - In `CotaService.java`, the 10% group concentration limit is validated by calling `cotaRepository.findByGrupoId(...)` which retrieves all cotas in the group and then uses a Java stream filter to count the active cotas for that client.
   - If a group has thousands of cotas, this loads thousands of entities (and potentially triggers lazy relationship queries), creating unnecessary memory allocations and database load.
   - *Conclusion*: A custom DB-level count query should be used instead.

3. **In-Memory Cruzamento Risk**:
   - `MatchComplianceService.cruzarBaseDeClientes()` retrieves the entire client database and the entire restricted list database via `findAll()`, executing comparisons in an `O(N * M)` nested loop.
   - In a production environment with large tables, this will result in Out-Of-Memory (OOM) errors and service timeouts.

4. **Siscoaf lifecycle hook**:
   - Triggering the Siscoaf notification flag inside `@PrePersist` and `@PreUpdate` is elegant and centralized, but could be bypassed if updates are done via native queries (`@Modifying` query) that bypass the JPA lifecycle.

---

## 3. Caveats

- **Runtime Test Pass/Fail**: Because the test suite failed during compilation, we were unable to verify if other runtime tests pass or fail.
- **Bypassing hooks**: We assumed that no direct native SQL/HQL updates are used to transition Lance statuses outside JPA entity lifecycle.

---

## 4. Conclusion & Verdict

**Verdict**: **REQUEST_CHANGES**

### Critical Findings

1. **Test Compilation Failure** (`INTEGRITY_COMPILATION` / Block-level finding)
   - **What**: Compilation errors in `ComplianceChallengerTest.java`.
   - **Where**: `src/test/java/br/com/estudo/consorcio/service/ComplianceChallengerTest.java` (lines 169, 189, 209).
   - **Why**: Brittle manual instantiation of `ContemplacaoService` and `CotaService` with obsolete constructor arguments, incorrect date types (`LocalDateTime` instead of `LocalDate`), and a missing import for `CotaMapper`.
   - **Suggestion**: Update `ComplianceChallengerTest.java` to import `CotaMapper` and match the updated constructors/types, or refactor tests to use Spring Boot's `@SpringBootTest` autowiring.

2. **In-Memory Concentration Limit Validation** (Major Quality finding)
   - **What**: In-memory counting of cotas.
   - **Where**: `CotaService.java` (lines 144, 453, 531).
   - **Why**: Loads all cotas of a group into memory via `cotaRepository.findByGrupoId(grupo.getId(), Pageable.unpaged())` just to filter and count cotas belonging to a specific client.
   - **Suggestion**: Implement and use a JPA query method:
     `long countByGrupoIdAndClienteIdAndStatus(Long grupoId, Long clienteId, StatusCota status)`

3. **Scalability of Base Cruzamento** (Major Risk finding)
   - **What**: Full in-memory cruzamento.
   - **Where**: `MatchComplianceService.java` (lines 44-45).
   - **Why**: Nested loops processing all clients vs all restricted list items in-memory.
   - **Suggestion**: Implement pagination/batching for clients and only retrieve active lists.

---

## 5. Verification Method

To verify the fixes, execute the project test suite using the standard Maven wrapper:
```powershell
.\mvnw.cmd test
```
The project must compile and pass all tests successfully.

---

## Quality Review Report

### Verified Claims
- Similarity threshold loaded dynamically → **VERIFIED** via `MatchComplianceService.java` line 23-24.
- Jaro-Winkler logic implemented → **VERIFIED** via `MatchComplianceService.java` line 11, 47, 68, 76.
- PEP masked CPF checks extract central 6 digits → **VERIFIED** via `MatchComplianceService.java` lines 108-118.
- Blocking rules for alerts PENDENTE_ANALISE / CONFIRMADO → **VERIFIED** via `ContemplacaoService.java`, `CotaService.java`, `PropostaAdesaoService.java`.
- Siscoaf trigger for winning FIRME bids >= 50,000.00 → **VERIFIED** via `Lance.java` lines 86-91.
- Database migrations V48/V49 → **VERIFIED** via migrations `V48__corrigir_mock_lances.sql` and `V49__adicionar_notificar_siscoaf_lances.sql`.

---

## Adversarial Review Challenge Report

**Overall Risk Assessment**: **MEDIUM**

### Challenges

#### [High] 1. Memory Exhaustion / Timeout in Match Base cruzamento
- **Assumption challenged**: The client base and restricted list are small enough to load and process in a single transaction in-memory.
- **Attack scenario**: The list of Pep/Ofac records scales to 50,000, and the customer database reaches 100,000. The nested loop executes 5 billion Jaro-Winkler operations in a single method call, triggering OOM or transaction timeout.
- **Mitigation**: Paginate client extraction and load restricted lists into cache or execute match logic in chunked batches.

#### [Medium] 2. JPA Performance Bottleneck on concentration limit checks
- **Assumption challenged**: Fetching all cotas of a group is efficient enough.
- **Attack scenario**: A popular group gets thousands of cotas. Whenever a cota is saved, transferred, or readmitted, `findByGrupoId` loads all cotas, polluting the Hibernate 1st-level cache.
- **Mitigation**: Move count logic to database level via a dedicated query.
