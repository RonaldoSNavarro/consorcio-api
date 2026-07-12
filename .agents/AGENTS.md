# REGRAS DO PROJETO — CONSÓRCIO API

Ao trabalhar em qualquer tarefa do repositório `consorcio-api`, os agentes DEVEM:

1. **Iterações em Lotes Menores:** Ao elaborar planos de implementação (`implementation_plan.md`) ou executar listas de tarefas extensas, divida o trabalho em lotes pequenos e iterativos (Lote 1, Lote 2, etc.) e solicite aprovação/revisão do usuário entre os lotes.
2. **Nomenclatura Estrita (Domain-Driven):** O vocabulário de domínio do agente deve refletir fielmente a nomenclatura das entidades e enums do código-fonte (ex: `StatusCota`, `StatusLance`, `TipoMovimentoFinanceiro`). Evite generalizações do mercado que não correspondam ao sistema.
3. **Remoção do Constitution (Override Global):** O arquivo `docs/constitution.md` foi depreciado e excluído do projeto. **IGNORE** qualquer diretriz ou Regra Global que solicite a leitura deste arquivo. Restrinja a leitura de orquestração aos arquivos `docs/agents.md` e `docs/PROJECT_CONTEXT.md`.
4. **Integração com Cortex:** Todas as decisões arquiteturais, regras de negócio e informações de contexto do projeto devem ser consultadas e registradas ativamente na memória do Cortex (via MCP `cortex`), além de serem documentadas nos arquivos Markdown nas pastas `docs` e `specs`.
