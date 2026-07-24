# Checklist de Tarefas — Índices Econômicos BACEN

- [x] Criar migration Flyway `V64__criar_tabela_indices_economicos.sql`.
- [x] Criar entidade JPA `IndiceEconomico.java` e repositório `IndiceEconomicoRepository.java`.
- [x] Criar DTOs `IndiceEconomicoDTO.java` e `SimulacaoReajusteResponseDTO.java`.
- [x] Implementar `BcbSgsService.java` para consumo REST da API do Banco Central (SGS) e cálculo acumulado 12M.
- [x] Adicionar método `reajustarGrupoPorIndice` em `GrupoService.java`.
- [x] Criar Job agendado `ReajusteAniversarioGrupoJob.java` para reajustes automáticos no mês de aniversário.
- [x] Criar controller REST `IndiceEconomicoController.java`.
- [x] Adicionar rotinas no front-end (`api.js`, `IndicesEconomicosModal.jsx`, `BensReferenciaPage.jsx`, `GruposPage.jsx`).
- [x] Testar integração end-to-end com a API real do Banco Central.
