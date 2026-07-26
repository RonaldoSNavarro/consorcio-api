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

### [BACKEND] P1: Fonte oficial e idempotência da apuração
- [x] Resolver o prêmio exclusivamente por extração persistida de `LoteriaFederal` elegível na data da assembleia.
- [x] Persistir e validar concurso, primeiro e segundo prêmios usados na assembleia.
- [x] Impedir reprocessamento de assembleia já `FECHADA`.

### [BACKEND] P2: Sorteio de excluídos e desempates
- [x] Executar o sorteio de cotas excluídas independentemente da existência de cota ativa sorteada.
- [x] Usar o segundo prêmio oficial para excluídos.
- [x] Implementar desempates por pedra-chave contratual, proximidade da cota ativa sorteada e Fundo Comum acumulado até a data da apuração.
- [x] Cobrir fonte oficial, divergência de prêmio e idempotência com testes unitários do motor.
