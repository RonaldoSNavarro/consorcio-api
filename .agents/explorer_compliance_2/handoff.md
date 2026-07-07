# Handoff Report: Compliance Module Audit (explorer_compliance_2)

This report details the audit findings and implementation proposals for the compliance and PLD/FT module of the consorcio-api.

---

## 1. Observation

### A. Jaro-Winkler Matching Logic (`MatchComplianceService.java`)
- **Hardcoded Threshold**: In `src/main/java/br/com/estudo/consorcio/service/MatchComplianceService.java`:
  - Line 59: `if (cpfMatch && scoreNome >= 0.90)`
  - Line 66: `if (scoreNome >= 0.90)`
  The threshold is hardcoded to `0.90`. There are no configuration properties loaded from `application.properties` or database parameters.
- **Normalization NPE Risk**: At line 43:
  - `String clientCityState = normalizar(cliente.getLocalidade().trim() + " - " + cliente.getUf().trim());`
  If `cliente.getLocalidade()` or `cliente.getUf()` is null, this will throw a `NullPointerException` due to the direct invocation of `.trim()`.
- **PEP Central Digits CPF Logic**: At line 97:
  ```java
  private String obterDigitosCentraisCpfPep(String pepCpf) {
      if (pepCpf == null) return null;
      String apenasDigitos = pepCpf.replaceAll("\\D", "");
      if (apenasDigitos.length() == 6) {
          return apenasDigitos;
      }
      return null;
  }
  ```
  If a PEP record contains a full 11-digit CPF (unmasked), `apenasDigitos.length()` will be 11, causing it to return `null`. Thus, unmasked PEP records fail to match.

### B. Blocking Rules Integration
- **`PropostaAdesaoService.java`**:
  - Checks for blocked clients on proposal creation (`criarProposta`, lines 52-58) using `alertaComplianceRepository.existsByClienteIdAndStatusIn`.
  - **Missing**: No checks are executed during proposal approval (`aprovarProposta`, lines 81-105) or contract effective trigger (`efetivarContrato`, lines 107-126).
- **`ContemplacaoService.java`**:
  - **Missing**: There are no checks against `alertaComplianceRepository` in `registrar` (lines 65-235). A client with `PENDENTE_ANALISE` or `CONFIRMADO` status can be contemplado.
- **`CotaService.java`**:
  - Checks for blocked clients on cota creation (`salvar`, lines 132-137) and cota transfer (`transferirCota`, lines 432-437) for the destination client (`novoCliente`).
  - **Missing**: No checks are executed during cota readmission (`readmitirCota`, lines 486-541).

### C. Lance Entity & Database Schema
- **Database Schema**: The `lances` table (defined in `V15` and modified in `V24`) has no `notificar_siscoaf` flag.
- **Entity**: `Lance.java` has no `notificarSiscoaf` field.

### D. Flyway Migration `V40__inserir_mock_pld.sql`
- **Invalid Data**: In `src/main/resources/db/migration/V40__inserir_mock_pld.sql` (line 23):
  ```sql
  SELECT co.id, a.id, 'LANCE_LIVRE', 'FINANCEIRO', 80000.00, CURRENT_DATE - 6, 'VENCEDOR'
  ```
  It inserts `'LANCE_LIVRE'` for the `tipo` column (mapped to `TipoLance` enum) and `'FINANCEIRO'` for the `modalidade` column (mapped to `ModalidadeLance` enum).
- **Enums**:
  - `TipoLance.java`: `EMBUTIDO`, `FIRME`, `MISTO`, `FGTS`, `SEGURO_OBITO`
  - `ModalidadeLance.java`: `LIVRE`, `FIXO`

---

## 2. Logic Chain

### A. Jaro-Winkler Logic
1. The similarity score comparison threshold (`0.90`) is specified as a literal double in two conditional statements, meaning any requirement change would necessitate a codebase change and redeployment.
2. Direct calls to `.trim()` on nullable strings within `clientCityState` will result in application crash (NPE) if the database holds invalid/null address values (even if database columns are non-nullable, in-memory object manipulation or test stubbing is vulnerable).
3. The PEP CPF parser returns `null` for 11-character numeric strings. Since a full CPF has 11 digits, full CPFs entered in the PEP list will never match the customer's central digits, leading to a false negative gap.

### B. Blocking Rules Integration
1. Clients can transition to blocked status between the creation of a proposal and its final approval/effectiveness.
2. Contemplation registration allows blocked clients to obtain credit allocations, bypassing compliance holds.
3. Excluded cotas can be readmitted (reactivated) for blocked clients because readmission does not check compliance alerts.

### C. Lance Siscoaf Flag
1. A Flyway migration is required to add `notificar_siscoaf` to the SQL schema, initialized to `FALSE`.
2. To set this flag automatically without duplicating logic, JPA Lifecycle callbacks `@PrePersist` and `@PreUpdate` can evaluate the business conditions: status is `VENCEDOR`, value is `>= 50,000.00`, and type is `FIRME`. Alternatively, this can be explicitly coded in `MotorApuracaoService` where these states are evaluated.

### D. V40 Mock SQL Issue
1. When JPA attempts to fetch a `Lance` record, Hibernate deserializes VARCHAR values from the database into the Java enums.
2. Since `'LANCE_LIVRE'` is not in `TipoLance` and `'FINANCEIRO'` is not in `ModalidadeLance`, Hibernate throws an `IllegalArgumentException` / `ConversionFailedException`.
3. Consequently, any query loading this mock record (directly or via relations) will fail.

---

## 3. Caveats
- Only read-only investigation was conducted; no changes were made to source files or DB migrations.
- Unit and integration tests passed because none of the existing tests retrieve the specific mock record inserted in V40 (they either use mock repositories or do not fetch the `lances` table records).

---

## 4. Conclusion

The compliance module has several critical security/compliance gaps and a database serialization bug:
1. Similarity threshold is not configurable, and PEP matching fails to resolve full CPFs.
2. Key transactional endpoints (proposal approval, contract effectiveness, contemplation, cota readmission) fail to enforce compliance blocks.
3. The `lances` table mock data in V40 is corrupted and will cause runtime errors on database reads of that record.

### Implementation Strategy for the Implementer

#### Step 1: Fix Jaro-Winkler Logic (`MatchComplianceService.java`)
- Define similarity threshold in `application.properties` (e.g. `compliance.similarity.threshold=0.90`) and inject it using `@Value("${compliance.similarity.threshold:0.90}")`.
- Add null-safety check inside `clientCityState` construction:
  ```java
  String localidade = cliente.getLocalidade() != null ? cliente.getLocalidade().trim() : "";
  String uf = cliente.getUf() != null ? cliente.getUf().trim() : "";
  String clientCityState = normalizar(localidade + " - " + uf);
  ```
- Refactor `obterDigitosCentraisCpfPep` to handle both 6-digit (masked) and 11-digit (unmasked) CPFs:
  ```java
  private String obterDigitosCentraisCpfPep(String pepCpf) {
      if (pepCpf == null) return null;
      String apenasDigitos = pepCpf.replaceAll("\\D", "");
      if (apenasDigitos.length() == 6) {
          return apenasDigitos;
      } else if (apenasDigitos.length() == 11) {
          return apenasDigitos.substring(3, 9);
      }
      return null;
  }
  ```

#### Step 2: Implement Missing Compliance Blocks
- **`PropostaAdesaoService.java`**:
  - Add compliance check in `aprovarProposta` and `efetivarContrato`:
    ```java
    boolean hasRestrictedAlerts = alertaComplianceRepository.existsByClienteIdAndStatusIn(
            proposta.getCliente().getId(), 
            List.of(StatusAlertaCompliance.PENDENTE_ANALISE, StatusAlertaCompliance.CONFIRMADO)
    );
    if (hasRestrictedAlerts) {
        throw new RegraDeNegocioException("Operação bloqueada pelo Compliance. Cliente possui alertas restritivos (PLD/FT).");
    }
    ```
- **`ContemplacaoService.java`**:
  - Inject `AlertaComplianceRepository`.
  - Add compliance check in `registrar`:
    ```java
    boolean hasRestrictedAlerts = alertaComplianceRepository.existsByClienteIdAndStatusIn(
            cota.getCliente().getId(),
            List.of(StatusAlertaCompliance.PENDENTE_ANALISE, StatusAlertaCompliance.CONFIRMADO)
    );
    if (hasRestrictedAlerts) {
        throw new RegraDeNegocioException("Contemplação bloqueada pelo Compliance: Cliente possui alertas restritivos (PLD/FT).");
    }
    ```
- **`CotaService.java`**:
  - Add compliance check in `readmitirCota`:
    ```java
    boolean hasRestrictedAlerts = alertaComplianceRepository.existsByClienteIdAndStatusIn(
            cota.getCliente().getId(),
            List.of(StatusAlertaCompliance.PENDENTE_ANALISE, StatusAlertaCompliance.CONFIRMADO)
    );
    if (hasRestrictedAlerts) {
        throw new RegraDeNegocioException("Readmissão bloqueada pelo Compliance. Cliente possui alertas restritivos (PLD/FT).");
    }
    ```

#### Step 3: Implement `notificarSiscoaf` in Lance Entity and Schema
- Create migration `src/main/resources/db/migration/V48__adicionar_flag_siscoaf_lances.sql`:
  ```sql
  ALTER TABLE lances ADD COLUMN notificar_siscoaf BOOLEAN NOT NULL DEFAULT FALSE;
  ```
- Update `Lance.java`:
  - Add field:
    ```java
    @Column(name = "notificar_siscoaf", nullable = false)
    private boolean notificarSiscoaf = false;
    ```
- Integrate logic to set `notificarSiscoaf` to true when:
  - Bid status is `VENCEDOR`.
  - Bid value is `>= 50,000.00`.
  - Bid type is `FIRME` (own resources).
  - Implementation can be placed in `MotorApuracaoService` where `VENCEDOR` is set (lines 203 & 225) OR as JPA `@PrePersist` and `@PreUpdate` callback inside `Lance.java`:
    ```java
    @PrePersist
    @PreUpdate
    protected void checkSiscoaf() {
        if (this.dataOferta == null) {
            this.dataOferta = LocalDateTime.now();
        }
        if (this.statusApuracao == null) {
            this.statusApuracao = StatusApuracaoLance.CADASTRADO;
        }
        if (this.modalidade == null) {
            this.modalidade = ModalidadeLance.LIVRE;
        }
        this.notificarSiscoaf = StatusApuracaoLance.VENCEDOR.equals(this.statusApuracao)
                && TipoLance.FIRME.equals(this.tipo)
                && this.valorOferta != null
                && this.valorOferta.compareTo(new BigDecimal("50000.00")) >= 0;
    }
    ```

#### Step 4: Correct V40 SQL Migration
- In `src/main/resources/db/migration/V40__inserir_mock_pld.sql` (line 23):
  Change `'LANCE_LIVRE'` to `'FIRME'` and `'FINANCEIRO'` to `'LIVRE'`.
  ```sql
  INSERT INTO lances (cota_id, assembleia_id, tipo, modalidade, valor_oferta, data_oferta, status_apuracao)
  SELECT co.id, a.id, 'FIRME', 'LIVRE', 80000.00, CURRENT_DATE - 6, 'VENCEDOR'
  FROM cotas co
  ...
  ```

---

## 5. Verification Method

To verify compilation, DB schema migrations, and unit tests:
1. **Compilation**: Run `.\mvnw.cmd clean test-compile` to ensure no MapStruct or compilation errors exist.
2. **Unit & Integration Tests**: Run `.\mvnw.cmd test` to ensure all tests pass (current baseline has 120 passing tests, 0 failures, 0 errors, 2 skipped).
3. **Database Migration Verification**: Invalidation condition: if Flyway fails during boot or tests fail loading the schema due to `IllegalArgumentException` on the Lance enums, then the SQL mock correction in V40 was not applied correctly.
