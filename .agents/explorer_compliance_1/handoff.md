# Handoff Report — Compliance & PLD/FT Module Audit

This report presents the findings from the codebase audit of the Compliance/PLD/FT module of the `consorcio-api` and outlines the implementation strategy for the worker.

---

## 1. Observation

### 1.1 MatchComplianceService.java & Configurable Threshold
* **Exact Path**: `src/main/java/br/com/estudo/consorcio/service/MatchComplianceService.java`
* **Findings**: 
  - The Jaro-Winkler similarity score threshold is hardcoded to `0.90` at lines 59 and 66:
    ```java
    59:                     if (cpfMatch && scoreNome >= 0.90) {
    ...
    66:                     if (scoreNome >= 0.90) {
    ```
  - The normalization method at lines 106-110 is:
    ```java
    106:     private String normalizar(String str) {
    107:         if (str == null) return "";
    108:         String normalized = Normalizer.normalize(str, Normalizer.Form.NFD);
    109:         return normalized.replaceAll("\\p{M}", "").toUpperCase();
    110:     }
    ```
  - Central digits extraction for CPFs at lines 88-104 correctly isolates the 6 central digits (indices 3 to 9 for raw 11-digit CPF, and checks for length 6 for masked PEP CPF from Portal da Transparência, e.g., `***.531.324-**` resulting in `531324`).

### 1.2 Blocking Rules Gaps
* **PropostaAdesaoService.java**:
  - Exact Path: `src/main/java/br/com/estudo/consorcio/domain/service/PropostaAdesaoService.java`
  - Current Status: Compliance alert check is present in `criarProposta()` (lines 52-58) but is **completely missing** from `aprovarProposta()` (lines 81-105).
* **ContemplacaoService.java**:
  - Exact Path: `src/main/java/br/com/estudo/consorcio/service/ContemplacaoService.java`
  - Current Status: **No compliance alert checks** are integrated into this service. `AlertaComplianceRepository` is not injected.
* **CotaService.java**:
  - Exact Path: `src/main/java/br/com/estudo/consorcio/service/CotaService.java`
  - Current Status: Checked in `salvar()` (lines 133-137) and `transferirCota()` (lines 433-437) for the new recipient client, but **missing** in `readmitirCota()` (lines 486-541).

### 1.3 Lance Entity, Schema, & Siscoaf Notification Flag
* **Lance.java**:
  - Exact Path: `src/main/java/br/com/estudo/consorcio/domain/model/Lance.java`
  - Current Status: The entity contains no flag `notificarSiscoaf`.
* **Database Schema**:
  - The `lances` table has no column `notificar_siscoaf` in the existing Flyway migration files (`V15__criar_tabelas_lance_assembleia.sql`, `V24__adicionar_percentual_e_modalidade_lance_fixo.sql`).
* **RelatorioService.java**:
  - Exact Path: `src/main/java/br/com/estudo/consorcio/service/RelatorioService.java`
  - Current Status: `gerarAlertaPldFt()` (lines 115-131) queries the database for all winning bids with value >= R$ 50.000,00 dynamically, without filtering by `notificarSiscoaf` or funding type.

### 1.4 V40__inserir_mock_pld.sql Mismatches
* **Exact Path**: `src/main/resources/db/migration/V40__inserir_mock_pld.sql`
* **Verbatim Code (Line 22-28)**:
  ```sql
  22: INSERT INTO lances (cota_id, assembleia_id, tipo, modalidade, valor_oferta, data_oferta, status_apuracao)
  23: SELECT co.id, a.id, 'LANCE_LIVRE', 'FINANCEIRO', 80000.00, CURRENT_DATE - 6, 'VENCEDOR'
  ```
* **Findings**:
  - It inserts `'LANCE_LIVRE'` into the `tipo` column, which maps to the `TipoLance` enum. `TipoLance` contains only: `EMBUTIDO`, `FIRME`, `MISTO`, `FGTS`, `SEGURO_OBITO`.
  - It inserts `'FINANCEIRO'` into the `modalidade` column, which maps to the `ModalidadeLance` enum. `ModalidadeLance` contains only: `LIVRE`, `FIXO`.

---

## 2. Logic Chain

1. **Jaro-Winkler Logic**: Because the threshold `0.90` is hardcoded, it cannot be adjusted without code compilation. Moving it to `application.properties` (or `compliance_config` table) is required for flexibility.
   The normalization is correct because it splits accent characters from base letters (`Form.NFD`), strips the combining accents (`\\p{M}`), and enforces uppercase.
2. **Blocking Rules**: Since background compliance runs can flag a client *after* a proposal is created but *before* approval, leaving `aprovarProposta()` unchecked allows flagged clients to obtain contracts. Similarly, checking must be enforced at contemplation registration (`ContemplacaoService.registrar()`) and cota readmission (`CotaService.readmitirCota()`) to completely isolate restricted clients.
3. **Lance Siscoaf Flag**: Bids are marked as winners during the draw execution inside `MotorApuracaoService.java` when they qualify contigent on the group's free balance. Setting the flag during this state change is the cleanest and most centralized approach.
4. **V40 Schema Mismatch**: Hibernate attempts to map string values from the database to Java enums when loading entities. Because `'LANCE_LIVRE'` and `'FINANCEIRO'` do not match any values in `TipoLance` and `ModalidadeLance`, loading these mock records fails with `IllegalArgumentException`. Modifying `V40__inserir_mock_pld.sql` to insert `'FIRME'` and `'LIVRE'` solves this.

---

## 3. Caveats

- **No code changes were made** to the main codebase during this audit. The current analysis is read-only.
- Local tests connected to a running PostgreSQL database at `localhost:5432` with database `consorcio_db` (using credentials configured in `application.properties`).

---

## 4. Conclusion

The audit identifies four key areas of improvement:
1. Make similarity threshold configurable in `MatchComplianceService`.
2. Close three compliance checking gaps in `PropostaAdesaoService`, `ContemplacaoService`, and `CotaService`.
3. Add `notificarSiscoaf` database column/field to `Lance` and set it in `MotorApuracaoService`. Update `RelatorioService` to query this field.
4. Correct the enum string mismatches in `V40__inserir_mock_pld.sql`.

---

## 5. Implementation Strategy & Recommendations for the Worker

### Task 1: Make Jaro-Winkler Similarity Threshold Configurable
1. Add property to `src/main/resources/application.properties`:
   ```properties
   compliance.similarity.threshold=0.90
   ```
2. In `MatchComplianceService.java`, inject it using `@Value`:
   ```java
   @Value("${compliance.similarity.threshold:0.90}")
   private double similarityThreshold;
   ```
3. Replace hardcoded `0.90` references with `similarityThreshold`.

### Task 2: Implement Missing Blocking Rules
1. **PropostaAdesaoService.java**:
   - In `aprovarProposta(Long propostaId)`, check if the client has restricted alerts before changing the status to `APROVADA`.
2. **ContemplacaoService.java**:
   - Inject `AlertaComplianceRepository`.
   - In `registrar()`, retrieve the client (`cota.getCliente()`) and block the operation if restricted alerts exist.
3. **CotaService.java**:
   - In `readmitirCota(Long cotaId)`, perform the check on the cota's client before executing the transition to `ATIVA`.

### Task 3: Implement Siscoaf Notification Flag
1. **Flyway Migration**: Create `src/main/resources/db/migration/V48__add_notificar_siscoaf_to_lances.sql`:
   ```sql
   ALTER TABLE lances ADD COLUMN notificar_siscoaf BOOLEAN NOT NULL DEFAULT FALSE;
   ```
2. **Lance Entity**: Add field to `Lance.java`:
   ```java
   @Column(name = "notificar_siscoaf", nullable = false)
   private Boolean notificarSiscoaf = false;
   ```
   Add initialization fallback `if (this.notificarSiscoaf == null) { this.notificarSiscoaf = false; }` inside `@PrePersist` method `onCreate()`.
3. **Lance DTO / Mapper**: Add `Boolean notificarSiscoaf` to `LanceResponseDTO.java` record.
4. **MotorApuracaoService.java**:
   - Inside `apurarLances()`, where `lance.setStatusApuracao(StatusApuracaoLance.VENCEDOR)` is called (both for free and fixed bids), set the flag:
     ```java
     if (lance.getTipo() == TipoLance.FIRME && lance.getValorOferta().compareTo(new BigDecimal("50000.00")) >= 0) {
         lance.setNotificarSiscoaf(true);
     }
     ```
5. **RelatorioService.java**:
   - Update `gerarAlertaPldFt()` to filter directly by the new column:
     ```java
     List<Lance> lancesSuspeitos = lanceRepository.findByNotificarSiscoafTrueAndDataOfertaBetween(dataInicio, dataFim);
     ```

### Task 4: Fix V40 Enum Mismatches
1. Edit `src/main/resources/db/migration/V40__inserir_mock_pld.sql` directly:
   - Line 23: Replace `'LANCE_LIVRE'` with `'FIRME'` (valid `TipoLance`).
   - Line 23: Replace `'FINANCEIRO'` with `'LIVRE'` (valid `ModalidadeLance`).
   - If tests are run locally on a persistent DB that already migrated V40, run `flyway repair` or recreate the schema.

---

## 6. Verification Method

- **Tests Command**: Run the test suite:
  ```powershell
  .\mvnw.cmd test
  ```
- **Specific Files to Verify**:
  - `src/main/resources/db/migration/V40__inserir_mock_pld.sql`
  - `src/main/java/br/com/estudo/consorcio/service/MatchComplianceService.java`
  - `src/main/java/br/com/estudo/consorcio/domain/service/PropostaAdesaoService.java`
  - `src/main/java/br/com/estudo/consorcio/service/ContemplacaoService.java`
  - `src/main/java/br/com/estudo/consorcio/service/CotaService.java`
  - `src/main/java/br/com/estudo/consorcio/domain/model/Lance.java`
  - `src/main/java/br/com/estudo/consorcio/service/MotorApuracaoService.java`
  - `src/main/java/br/com/estudo/consorcio/service/RelatorioService.java`
- **Invalidation Condition**: If any `@SpringBootTest` fails to load the application context, or if `NoClassDefFound` errors persist, check local Postgres connection configuration and classpath caching.
