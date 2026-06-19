# 📋 Decomposição de Tarefas — Restituição de Excluídos (excluidos)

*   **Capability**: excluidos
*   **Spec de referência**: [spec.md](spec.md)
*   **Total de tarefas**: 9
*   **REQ-IDs cobertos**: 5/5

---

## Tarefas

### [BACKEND] REQ-EXC-001: Elegibilidade e Contemplação de Excluídos
- [x] Implementar `CotaService.cancelarCota()` — transição para CANCELADA com auditoria
- [x] Criar `CotaController.cancelar()` — endpoint `POST /api/cotas/{id}/cancelar`

### [BACKEND] REQ-EXC-002: Memória de Cálculo da Restituição (ADR 005)
- [x] Implementar cálculo PAFC (percentual amortizado) em `CotaService.reembolsarCota()`
- [x] Implementar cálculo VBD (valor bruto sobre bem atualizado)
- [x] Implementar cálculo VLD (valor líquido com multa de 10%)

### [BACKEND] REQ-EXC-003: Destinação da Cláusula Penal
- [x] Implementar lógica parametrizável de destinação da multa (grupo vs. administradora)

### [BACKEND] REQ-EXC-004: Ajuste Monotônico e Fixação de Saldo
- [x] Implementar fixação do saldo no passivo após sorteio (sem reajuste posterior)

### [BACKEND] REQ-EXC-005: Contabilização do Sorteio e Desembolso (COSIF)
- [x] Implementar lançamentos contábeis em `ContabilidadeService` — débito FC, crédito Excluídos a Devolver
- [x] Criar `CotaController.reembolsar()` — endpoint `POST /api/cotas/{id}/reembolsar`
