# 📋 Decomposição de Tarefas — Mora e Inadimplência (inadimplencia)

*   **Capability**: inadimplencia
*   **Spec de referência**: [spec.md](spec.md)
*   **Total de tarefas**: 7
*   **REQ-IDs cobertos**: 3/3

---

## Tarefas

### [BACKEND] REQ-INA-001: Encargos de Mora
- [x] Implementar cálculo de multa (2%) em `ParcelaService.pagar()`
- [x] Implementar cálculo de juros pro rata die (1% a.m.) em `ParcelaService.pagar()`
- [x] Criar campos `valorMulta` e `valorJuros` na entidade `Parcela.java`

### [BACKEND] REQ-INA-002: Destinação Legal dos Recursos
- [x] Implementar lançamento contábil de encargos para conta `2.1.2.10.10-6` em `ContabilidadeService`
- [x] Implementar estorno de pagamento — `ParcelaService.estornar()` com lançamento inverso

### [BACKEND] REQ-INA-003: Cálculo Volátil e Dinâmico
- [x] Implementar `ParcelaService.obterInadimplenciaCota()` — simulação em memória sem flush
- [x] Criar `CotaController.obterInadimplencia()` — endpoint `GET /api/cotas/{id}/inadimplencia`
