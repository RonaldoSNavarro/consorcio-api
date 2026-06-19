# 📋 Decomposição de Tarefas — Composição de Fundos e Parcelas (fundos)

*   **Capability**: fundos
*   **Spec de referência**: [spec.md](spec.md)
*   **Total de tarefas**: 8
*   **REQ-IDs cobertos**: 3/3

---

## Tarefas

### [BACKEND] REQ-FUN-001: Composição e Cálculo da Parcela Mensal
- [x] Criar entidade `Parcela.java` com campos FC, TA, FR, SEG, valorParcela
- [x] Criar `ParcelaService.java` — lógica de geração e listagem
- [x] Criar `ParcelaController.java` — endpoints POST e GET
- [x] Criar DTOs: `ParcelaRequestDTO`, `ParcelaResponseDTO`

### [BACKEND] REQ-FUN-002: Hook de Consistência e Arredondamento
- [x] Implementar hook JPA `@PrePersist` / `@PreUpdate` em `Parcela.java` para soma automática dos componentes

### [BACKEND] REQ-FUN-003: Segregação Contábil (Patrimônio de Afetação)
- [x] Integrar `ContabilidadeService.java` — lançamentos de partida dobrada ao pagar parcela
- [x] Mapear contas COSIF: FC → `2.1.2.10.10-6`, TA → `2.1.2.10.30-2`, FR → `2.1.2.10.20-9`
- [x] Criar entidades `ContaContabil.java` e `LancamentoContabil.java`
