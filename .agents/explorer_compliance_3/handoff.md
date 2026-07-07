# Handoff Report - Compliance Module Audit

## 1. Observation
Below are the exact file references, line numbers, and code blocks observed during the investigation.

### 1.1 MatchComplianceService.java
* **Path**: `src/main/java/br/com/estudo/consorcio/service/MatchComplianceService.java`
* **Hardcoded Similarity Threshold**:
  * Line 59: `if (cpfMatch && scoreNome >= 0.90) {`
  * Line 66: `if (scoreNome >= 0.90) {`
* **Normalization Logic** (lines 106-110):
  ```java
  private String normalizar(String str) {
      if (str == null) return "";
      String normalized = Normalizer.normalize(str, Normalizer.Form.NFD);
      return normalized.replaceAll("\\p{M}", "").toUpperCase();
  }
  ```

### 1.2 PropostaAdesaoService.java
* **Path**: `src/main/java/br/com/estudo/consorcio/domain/service/PropostaAdesaoService.java`
* **Creation Check** (lines 52-58):
  ```java
  boolean hasRestrictedAlerts = alertaComplianceRepository.existsByClienteIdAndStatusIn(
          cliente.getId(), 
          List.of(StatusAlertaCompliance.PENDENTE_ANALISE, StatusAlertaCompliance.CONFIRMADO)
  );
  if (hasRestrictedAlerts) {
      throw new RegraDeNegocioException("Venda bloqueada por PLD/FT: Cliente possui alertas restritivos.");
  }
  ```
* **Approval Check (GAP)**: There are no checks for compliance alerts in `aprovarProposta` (lines 81-105) or `efetivarContrato` (lines 107-120).

### 1.3 ContemplacaoService.java
* **Path**: `src/main/java/br/com/estudo/consorcio/service/ContemplacaoService.java`
* **Registration Check (GAP)**: There are no references to `AlertaComplianceRepository` or any checks for compliance alerts in the `registrar` method (lines 65-235).

### 1.4 CotaService.java
* **Path**: `src/main/java/br/com/estudo/consorcio/service/CotaService.java`
* **Creation Check** (lines 133-137):
  ```java
  boolean hasAlertaRestritivo = alertaComplianceRepository.existsByClienteIdAndStatusIn(
          cliente.getId(), List.of(StatusAlertaCompliance.PENDENTE_ANALISE, StatusAlertaCompliance.CONFIRMADO));
  if (hasAlertaRestritivo) {
      throw new RegraDeNegocioException("Operação bloqueada pelo Compliance. Cliente possui alertas restritivos (PLD/FT).");
  }
  ```
* **Transfer Check** (lines 433-437):
  ```java
  boolean hasAlertaRestritivo = alertaComplianceRepository.existsByClienteIdAndStatusIn(
          novoCliente.getId(), List.of(StatusAlertaCompliance.PENDENTE_ANALISE, StatusAlertaCompliance.CONFIRMADO));
  if (hasAlertaRestritivo) {
      throw new RegraDeNegocioException("Transferência bloqueada pelo Compliance. Cliente destino possui alertas restritivos (PLD/FT).");
  }
  ```
* **Transfer Current Owner Check (GAP)**: The transfer only checks the target client (`novoCliente`), leaving the current owner (`cota.getCliente()`) unchecked.

### 1.5 V40__inserir_mock_pld.sql
* **Path**: `src/main/resources/db/migration/V40__inserir_mock_pld.sql`
* **Invalid SQL Insert** (lines 22-28):
  ```sql
  INSERT INTO lances (cota_id, assembleia_id, tipo, modalidade, valor_oferta, data_oferta, status_apuracao)
  SELECT co.id, a.id, 'LANCE_LIVRE', 'FINANCEIRO', 80000.00, CURRENT_DATE - 6, 'VENCEDOR'
  ...
  ```
  * `'LANCE_LIVRE'` is inserted into the `tipo` column (mapped to `TipoLance` enum).
  * `'FINANCEIRO'` is inserted into the `modalidade` column (mapped to `ModalidadeLance` enum).

---

## 2. Logic Chain

### 2.1 Jaro-Winkler Matching & Config
1. The Jaro-Winkler score thresholds are hardcoded (`0.90`) in `MatchComplianceService.java`. The `ComplianceConfig` entity only manages cron settings (`cronExpression`, `frequencia`, `horario`). Therefore, similarity matching threshold is completely **non-configurable** from the database or properties.
2. The normalization strips accents (NFD form and replacement of `\p{M}`) and capitalizes names. However, because it does not apply `.trim()` or strip duplicate whitespaces, it is susceptible to minor formatting differences that artificially lower Jaro-Winkler matching scores.

### 2.2 Blocking Rules Integration
1. Circular BCB 3.978/2020 dictates that clients flagged under restrictive lists (PEP, OFAC, ONU) with `PENDENTE_ANALISE` or `CONFIRMADO` must be prevented from committing transactions.
2. The audit showed that `ContemplacaoService.registrar()` does not execute any compliance check. If a client is flag-matched before contemplation, their contemplation is not blocked.
3. Similarly, if a client is flagged after creating a proposal but before approval or contract activation, `PropostaAdesaoService` will allow the proposal approval to proceed.
4. During cota transfer in `CotaService.transferirCota()`, only the destination client is checked for alerts. The current owner is ignored, exposing a vector where a blacklisted client could offload assets.

### 2.3 Lance Entity and Siscoaf Notification
1. Adding a `notificarSiscoaf` flag requires mapping a new table column to the JPA entity `Lance`.
2. Setting this flag to `true` depends on:
   - status is `VENCEDOR`
   - value is `>= 50,000.00`
   - type is `FIRME` (own resources)
3. Incorporating this in the domain entity's setters (encapsulated domain logic) ensures that updates from any source keep the flag synchronized, maintaining schema and status consistency.

### 2.4 SQL Mock Data Enum Collision
1. The `TipoLance` Java enum contains `EMBUTIDO, FIRME, MISTO, FGTS, SEGURO_OBITO`. The string `'LANCE_LIVRE'` is not a valid enum value.
2. The `ModalidadeLance` Java enum contains `LIVRE, FIXO`. The string `'FINANCEIRO'` is not a valid enum value.
3. When JPA repository methods load the rows created by `V40__inserir_mock_pld.sql`, Hibernate throws an `IllegalArgumentException` attempting to map those database values to Java enums.

---

## 3. Caveats
- No other service layer actions (such as `AnaliseCreditoService` or `Cobrança`) were audited for compliance blocks as the prompt restricted focus to `PropostaAdesaoService`, `ContemplacaoService`, and `CotaService`.

---

## 4. Conclusion
The compliance module is mostly functioning (120/120 unit/integration tests passing), but contains:
1. Critical GAPs in transaction blocking (lack of checks in proposal approval, cota contemplations, and source client in cota transfers).
2. Hardcoded thresholds and lacks whitespace trimming in matching normalization.
3. Invalid mock data inserts in `V40__inserir_mock_pld.sql` that break JPA entity instantiation.
4. Lack of automated flags for Siscoaf notifications on bids >= R$ 50k using own resources.

---

## 5. Verification Method

### 5.1 Verification Commands
The project test suite is verified using:
```powershell
.\mvnw.cmd test
```

### 5.2 Implementation Strategy for Worker (Task Plan)

#### Step 1: Database Corrections and Additions (Flyway Migrations)
1. Create a migration file `V48__corrigir_valores_mock_pld.sql` to fix invalid enums in the database from V40:
   ```sql
   UPDATE lances 
   SET tipo = 'FIRME', modalidade = 'LIVRE' 
   WHERE tipo = 'LANCE_LIVRE' AND modalidade = 'FINANCEIRO';
   ```
2. Create a migration file `V49__adicionar_notificar_siscoaf_lances.sql` to add the required column to the `lances` table:
   ```sql
   ALTER TABLE lances ADD COLUMN notificar_siscoaf BOOLEAN NOT NULL DEFAULT FALSE;
   ```

#### Step 2: Update the `Lance` Entity and Implement Flag Logic
1. Add the `notificarSiscoaf` boolean field in `Lance.java`:
   ```java
   @Column(name = "notificar_siscoaf", nullable = false)
   private Boolean notificarSiscoaf = false;
   ```
2. Encapsulate the Siscoaf notification rule inside `Lance.java` by updating setters or using a helper method:
   ```java
   public void setStatusApuracao(StatusApuracaoLance statusApuracao) {
       this.statusApuracao = statusApuracao;
       this.atualizarFlagSiscoaf();
   }

   public void setTipo(TipoLance tipo) {
       this.tipo = tipo;
       this.atualizarFlagSiscoaf();
   }

   public void setValorOferta(BigDecimal valorOferta) {
       this.valorOferta = valorOferta;
       this.atualizarFlagSiscoaf();
   }

   private void atualizarFlagSiscoaf() {
       this.notificarSiscoaf = this.statusApuracao == StatusApuracaoLance.VENCEDOR
               && this.valorOferta != null
               && this.valorOferta.compareTo(new BigDecimal("50000.00")) >= 0
               && this.tipo == TipoLance.FIRME;
   }
   ```

#### Step 3: Implement Configurable Jaro-Winkler Threshold & Improve Normalization
1. Add `similarityThreshold` property in `application.properties`:
   ```properties
   compliance.similarity.threshold=0.90
   ```
2. Inject it into `MatchComplianceService.java` using `@Value("${compliance.similarity.threshold:0.90}")`.
3. Enhance `normalizar()` to strip leading/trailing and duplicate whitespace:
   ```java
   private String normalizar(String str) {
       if (str == null) return "";
       String normalized = Normalizer.normalize(str, Normalizer.Form.NFD);
       return normalized.replaceAll("\\p{M}", "")
                        .replaceAll("\\s+", " ")
                        .trim()
                        .toUpperCase();
   }
   ```

#### Step 4: Fix Blocking Rule GAPs
1. **PropostaAdesaoService.java**: Add the compliance check before approving the proposal in `aprovarProposta` and activating the contract in `efetivarContrato`.
2. **ContemplacaoService.java**: Inject `AlertaComplianceRepository` and add a compliance check in `registrar()` before saving the contemplation:
   ```java
   boolean hasRestrictedAlerts = alertaComplianceRepository.existsByClienteIdAndStatusIn(
           cota.getCliente().getId(),
           List.of(StatusAlertaCompliance.PENDENTE_ANALISE, StatusAlertaCompliance.CONFIRMADO)
   );
   if (hasRestrictedAlerts) {
       throw new RegraDeNegocioException("Contemplação bloqueada pelo Compliance. Cliente possui alertas restritivos (PLD/FT).");
   }
   ```
3. **CotaService.java**: In `transferirCota()`, check both the `novoCliente` (receiver) and `cota.getCliente()` (current owner) for compliance alerts:
   ```java
   boolean hasAlertaRestritivoAnterior = alertaComplianceRepository.existsByClienteIdAndStatusIn(
           cota.getCliente().getId(), List.of(StatusAlertaCompliance.PENDENTE_ANALISE, StatusAlertaCompliance.CONFIRMADO));
   if (hasAlertaRestritivoAnterior) {
       throw new RegraDeNegocioException("Transferência bloqueada pelo Compliance. Cedente possui alertas restritivos (PLD/FT).");
   }
   ```
