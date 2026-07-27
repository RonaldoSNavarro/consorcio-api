# 📋 Decomposição de Tarefas — Composição de Fundos e Parcelas (fundos)

*   **Capability**: fundos
*   **Spec de referência**: [spec.md](spec.md) v1.2
*   **Total de tarefas**: 10
*   **REQ-IDs cobertos**: 4/4

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

### [BACKEND] REQ-FUN-004: Efetivação após pagamento da adesão
- [x] Promover `ContratoAdesao` e `Cota` dentro da transação de `ParcelaService.pagar()`
- [x] Cobrir a transição com teste unitário para a parcela de adesão e preservar o fluxo normal das parcelas posteriores

### [BACKEND/QA] CR-FIN-VND-003: Reversão e amortização consistentes
- [x] Reverter contrato, cota e comissão no estorno da primeira parcela.
- [x] Impedir que amortização altere parcelas para `PAGA`.
- [x] Cobrir regressões de estorno e de cota aguardando pagamento.

### [BACKEND/QA] BUG-FUN-009: Provisionamento COSIF de pagamento
- [x] Provisionar de modo idempotente as 12 contas COSIF padronizadas na inicialização e dentro das transações contábeis.
- [x] Rejeitar códigos COSIF fora do conjunto operacional com erro de negócio, sem persistência parcial.
- [x] Cobrir o provisionamento completo e sua repetição idempotente em `ContabilidadeServiceTest`.
