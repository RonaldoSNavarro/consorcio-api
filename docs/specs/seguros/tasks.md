# 📋 Decomposição de Tarefas — Seguros (seguros)

*   **Capability**: seguros
*   **Spec de referência**: [spec.md](spec.md)
*   **Total de tarefas**: 4
*   **REQ-IDs cobertos**: 2/2

---

## Tarefas

### [BACKEND] REQ-SEG-001: Cobrança do Prêmio de Seguro
- [x] Adicionar campo `valorSeguro` na entidade `Parcela.java`
- [x] Incluir `valorSeguro` no `ParcelaRequestDTO` com validação `@PositiveOrZero`
- [x] Incluir `valorSeguro` na soma automática do hook JPA `@PrePersist`

### [BACKEND] REQ-SEG-002: Lançamento Contábil de Repasse (COSIF)
- [x] Implementar no `ContabilidadeService` — destinar `valorSeguro` para conta `2.1.2.10.40-5` (Prêmios de Seguros a Recolher) ao processar pagamento
