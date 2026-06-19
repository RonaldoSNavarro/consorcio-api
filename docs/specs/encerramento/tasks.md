# 📋 Decomposição de Tarefas — Reajustes e Encerramento (encerramento)

*   **Capability**: encerramento
*   **Spec de referência**: [spec.md](spec.md)
*   **Total de tarefas**: 8
*   **REQ-IDs cobertos**: 3/3

---

## Tarefas

### [BACKEND] REQ-ENC-001: Motor de Reajuste do Bem de Referência
- [x] Implementar `GrupoService.reajustarGrupo()` — cálculo do fator e atualização proporcional de parcelas
- [x] Implementar lançamentos contábeis de ajuste patrimonial no Ledger
- [x] Criar `GrupoController.reajustar()` — endpoint `PUT /api/grupos/{id}/reajuste`

### [BACKEND] REQ-ENC-002: Encerramento de Grupo e Baixa de Inadimplência (ADR 006)
- [x] Implementar `GrupoService.encerrarGrupo()` — baixa de parcelas inadimplentes para PDD
- [x] Implementar lançamentos contábeis: Débito PDD, Crédito Contas a Receber
- [x] Criar `GrupoController.encerrar()` — endpoint `POST /api/grupos/{id}/encerrar`

### [BACKEND] REQ-ENC-003: Recursos Não Procurados (RNP)
- [x] Implementar transferência de saldos não reclamados para conta `2.1.2.40.30-2`
- [x] Criar DTO `GrupoEncerrarResponseDTO` com métricas de PDD e RNP
