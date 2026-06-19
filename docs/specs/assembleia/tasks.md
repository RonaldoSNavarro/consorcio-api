# 📋 Decomposição de Tarefas — Gestão de Assembleias (assembleia)

*   **Capability**: assembleia
*   **Spec de referência**: [spec.md](spec.md)
*   **Total de tarefas**: 7
*   **REQ-IDs cobertos**: 2/2

---

## Tarefas

### [BACKEND] REQ-ASM-001: Estados da Assembleia
- [x] Criar enum `StatusAssembleia` — CAPTANDO, REALIZADA, FECHADA
- [x] Criar enum `TipoAssembleia` — ORDINARIA, EXTRAORDINARIA
- [x] Criar entidade `Assembleia.java` com grupo, dataAssembleia, tipo, status
- [x] Criar `AssembleiaService.java` — validação de status e transições
- [x] Criar `AssembleiaController.java` — endpoints POST e GET

### [BACKEND] REQ-ASM-002: Vinculação e Frequência
- [x] Implementar validação no `AssembleiaService` — impedir assembleias duplicadas por grupo
- [x] Criar DTOs: `AssembleiaRequestDTO`, `AssembleiaResponseDTO`
