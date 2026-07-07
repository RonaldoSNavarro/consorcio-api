# Auditoria, Histórico e Concorrência

Devido à natureza financeira da aplicação, o sistema emprega mecanismos rigorosos de auditoria de ações e tratamento de concorrência em transações simultâneas.

---

## 1. Trilha de Auditoria (`HistoricoConsorciado`)

O sistema mantém um registro imutável de todas as interações e eventos sistêmicos que ocorrem no ciclo de vida do consorciado e de sua cota. Essa trilha é essencial para compliance com o BACEN e resolução de disputas.

### Campos Obrigatórios na Auditoria
Todo evento gravado no histórico contém:
- **`usuarioId`**: Identificação de quem disparou a ação (sistema, cliente via app, ou operador backoffice).
- **`timestamp`**: Data e hora exata do servidor.
- **`valores`**: Valores financeiros envolvidos na transação, se houver.
- **`descricao` / `payload`**: Contexto do evento.

### Enum `TipoInteracao` (Eventos Mapeados)
- `GERACAO_PARCELAS`, `PAGAMENTO_PARCELA`, `AMORTIZACAO`, `REEMBOLSO` (Financeiro)
- `CONTEMPLACAO`, `CARTA_CREDITO`, `PAGAMENTO_BEM` (Fase de Contemplação)
- `CESSAO_DIREITOS`, `CANCELAMENTO_COTA`, `ATUALIZACAO_CADASTRAL` (Lifecycle de Cota)
- `REAJUSTE_CREDITO` (Indexadores)
- `ANALISE_CREDITO` (Aprovação Pós-contemplação)
- `COMUNICADO`, `OUTROS`

---

## 2. Histórico de Versão (`HistoricoVersaoCota`)

Além da trilha de eventos, alterações sensíveis (como uma Cessão de Direitos - transferência de titularidade da cota) geram um *snapshot* na entidade `HistoricoVersaoCota`, permitindo reconstruir a linha do tempo de propriedade da cota.

---

## 3. Concorrência Otimista (`@Version`)

A arquitetura do sistema utiliza Concorrência Otimista (Optimistic Locking) do JPA para evitar condição de corrida (Race Condition) em eventos simultâneos, como:
- Dois operadores tentando aprovar lances para a mesma cota ao mesmo tempo.
- Pagamento de parcela recebido via webhook concorrendo com fechamento de assembleia.

A anotação `@Version` nas entidades críticas (`Cota`, `Grupo`, etc.) garante que, se duas threads tentarem persistir atualizações divergentes na mesma entidade, a segunda thread receberá uma exceção `OptimisticLockException` e sua transação sofrerá *rollback*, protegendo a integridade financeira do grupo.

---

## 4. Ata de Assembleia

Na conclusão do `AssembleiaService`, os eventos consolidados geram a entidade `AtaAssembleia`. Este documento oficializa as contemplações (por sorteio e lance) e registra as exclusões que participaram do sorteio de restituição, servindo como documento com validade jurídica de auditoria do evento da AGO/AGE.
