## 2026-07-04T22:44:30Z
🎭 Acting as: Dev Full Stack Sênior.
Your working directory is f:\Dev\Projetos\consorcio-api\.agents\worker_compliance_2.
Your task is to fix the QA and Code Review findings in f:\Dev\Projetos\consorcio-api.

Please apply the following changes:

1. **StatusCota.java**: Add the enum value `BLOQUEADA_COMPLIANCE` to the `StatusCota` enum.
2. **ComplianceController.java**: Inject `CotaRepository` and update the `deliberarSobreAlerta` method:
   - If the request status is `CONFIRMADO` and the alert's restriction list has origin `ONU` or `OFAC`, fetch all cotas of the client (using `cotaRepository.findByClienteId(alerta.getCliente().getId())`) and set their status to `StatusCota.BLOQUEADA_COMPLIANCE`. Save these cotas.
3. **ContemplacaoService.java**: In the `pagarBem` method (payout flow), add a validation check:
   - If the cota has status `StatusCota.BLOQUEADA_COMPLIANCE`, throw a `RegraDeNegocioException("Pagamento de bem bloqueado por compliance.")`.
4. **CotaRepository.java & CotaService.java (10% Group Limit Optimization)**:
   - In `CotaRepository.java`, add method: `long countByGrupoIdAndClienteIdAndStatus(Long grupoId, Long clienteId, StatusCota status);`
   - In `CotaService.java`, replace the in-memory stream counting of `cotaRepository.findByGrupoId(grupo.getId(), Pageable.unpaged())` at lines 144, 453, and 531 with the database-level query `cotaRepository.countByGrupoIdAndClienteIdAndStatus(grupoId, clienteId, StatusCota.ATIVA)`.
5. **RelatorioService.java & LanceRepository.java (N+1 Query Optimization)**:
   - In `LanceRepository.java`, add `@Query` fetch joins to eagerly load cota, client, assembleia, and group associations for:
     `List<Lance> findByStatusApuracaoAndValorOfertaGreaterThanEqualAndDataOfertaBetween(StatusApuracaoLance status, BigDecimal valorMinimo, LocalDateTime dataInicio, LocalDateTime dataFim);`
6. **CotaService.java:listarPendentesReembolso() (N+1 Query Optimization)**:
   - Eagerly fetch or pre-fetch/load contemplations and paid installments in bulk to avoid executing repository queries inside the mapping loop over cancelled cotas.
7. **MatchComplianceService.java (Matching Scale & Safety Optimization)**:
   - Paginate client loading in `cruzarBaseDeClientes` (e.g., retrieve clients in pages of 1,000 using `clienteRepository.findAll(Pageable)`) and clear the entity manager (`entityManager.clear()`) after each batch to avoid heap bloat.
   - Add name guard checks: if the normalized client name or list name is empty/blank, skip Jaro-Winkler comparison (treating it as non-matching) to prevent false-positives from empty strings.

MANDATORY INTEGRITY WARNING:
> DO NOT CHEAT. All implementations must be genuine. DO NOT
> hardcode test results, create dummy/facade implementations, or
> circumvent the intended task. A Forensic Auditor will independently
> verify your work. Integrity violations WILL be detected and your
> work WILL be rejected.

Ensure all unit/integration tests compile and pass via `.\mvnw.cmd test`.
Write a detailed handoff report to f:\Dev\Projetos\consorcio-api\.agents\worker_compliance_2\handoff.md.
Keep progress.md updated for liveness.
