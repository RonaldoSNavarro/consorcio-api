# 📋 Decomposição de Tarefas — Apuração e Contemplações (contemplacao)

*   **Capability**: contemplacao
*   **Spec de referência**: [spec.md](spec.md)
*   **Total de tarefas**: 11
*   **REQ-IDs cobertos**: 4/4

---

## Tarefas

### [BACKEND] REQ-CON-001: Motor de Apuração e Ordenação
- [x] Criar entidade `Contemplacao.java` com cota, assembleia, tipo, valorLance, status
- [x] Criar `MotorApuracaoService.java` — ordenação por percentual ofertado e desempate
- [x] Criar enum `TipoContemplacao` — SORTEIO, LANCE_LIVRE, LANCE_FIXO
- [x] Criar enum `CriterioDesempateLance` no Grupo

### [BACKEND] REQ-CON-002: Checagem de Saldo em Tempo Real (Ledger Contábil)
- [x] Implementar `ContabilidadeService.obterSaldoFundoComum()` — consulta ao Ledger por conta COSIF
- [x] Implementar validação de saldo em `ContemplacaoService.registrar()`

### [BACKEND] REQ-CON-003: Fluxo de Homologação de Lance Livre (ADR 004)
- [x] Implementar status intermediário `PENDENTE_INTEGRALIZACAO` em `StatusCota`
- [x] Criar `ContemplacaoController.confirmarIntegralizacao()` — `POST /lances/{id}/integralizar`

### [BACKEND] REQ-CON-004: Lançamentos Contábeis do Ciclo de Contemplação
- [x] Implementar lançamentos de compensação (oferta) em `ContabilidadeService`
- [x] Implementar lançamentos de integralização e homologação
- [x] Criar `ContemplacaoController.pagarBem()` — `POST /{id}/pagamento-bem` (desembolso efetivo)

### [BACKEND] REQ-CON-005 e REQ-CON-006: Motor de Apuração do Lance Fixo e Parametrização
- [x] Implementar no `MotorApuracaoService` a segregação e apuração de lances na modalidade `FIXO`
- [x] Implementar regra de desempate do Lance Fixo baseada na cota mais próxima da sorteada (Loteria Federal)
- [x] Garantir que o valor liberado e lançamentos contábeis de integralização para o Lance Fixo sigam o fluxo contábil de transição no Razão
- [x] Adicionar testes de integração no `MotorApuracaoServiceTest.java` validando apuração e desempate de Lance Fixo

