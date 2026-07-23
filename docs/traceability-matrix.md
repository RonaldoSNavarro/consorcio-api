# 📊 Matriz de Rastreabilidade — REQ-ID → Código → Teste

*   **Última atualização**: 2026-06-16
*   **Gerada retroativamente** a partir do baseline v1.0

---

## auth — Autenticação e Sessão

| REQ-ID | Regra | Classe(s) Java | Controller/Endpoint | Teste |
|--------|-------|----------------|---------------------|-------|
| REQ-AUTH-001 | Autenticação Stateless e Emissão de Token | `AutenticacaoService`, `TokenService` | `AutenticacaoController` — `POST /api/login`, `POST /api/login/logout` | Testes Spring Security (auth suite) |
| REQ-AUTH-002 | Revalidação Ativa (F5-safety) | `AutenticacaoController.obterUsuarioLogado()` | `GET /api/login/me` | Testes de sessão + E2E |

---

## assembleia — Gestão de Assembleias

| REQ-ID | Regra | Classe(s) Java | Controller/Endpoint | Teste |
|--------|-------|----------------|---------------------|-------|
| REQ-ASM-001 | Estados da Assembleia | `AssembleiaService` | `AssembleiaController` — `POST /api/assembleias` | `AssembleiaServiceTest` |
| REQ-ASM-002 | Vinculação e Frequência | `AssembleiaService.salvar()` | `POST /api/assembleias` (validação duplicatas) | `AssembleiaServiceTest` |

---

## fundos — Composição de Fundos e Parcelas

| REQ-ID | Regra | Classe(s) Java | Controller/Endpoint | Teste |
|--------|-------|----------------|---------------------|-------|
| REQ-FUN-001 | Composição da Parcela Mensal | `ParcelaService`, `Parcela.java` | `ParcelaController` — `POST /api/parcelas` | `ParcelaServiceTest` |
| REQ-FUN-002 | Hook JPA de Consistência | `Parcela.java` (`@PrePersist/@PreUpdate`) | N/A (automático) | `ParcelaServiceTest` |
| REQ-FUN-003 | Segregação Contábil | `ContabilidadeService`, `LancamentoContabil` | N/A (interno) | `ContabilidadeServiceTest` |

---

## lances — Oferta de Lances

| REQ-ID | Regra | Classe(s) Java | Controller/Endpoint | Teste |
|--------|-------|----------------|---------------------|-------|
| REQ-LAN-001 | Elegibilidade para Lance | `LanceService` | N/A (via contemplação) | `LanceServiceTest` |
| REQ-LAN-002 | Lance Embutido | `LanceService`, `ContemplacaoService` | N/A (validação interna) | `LanceServiceTest` |
| REQ-LAN-003 | Motor de Amortização | `ParcelaService.amortizarPorReducaoDePrazo()`, `.amortizarPorDiluicao()` | `ParcelaController` — `POST /api/parcelas/cota/{id}/lance/reducao-prazo`, `POST .../diluicao` | `ParcelaServiceTest` |
| REQ-LAN-004 | Cadastro de Oferta de Lance Fixo | `LanceService`, `LanceController` | `LanceController` — `POST /api/lances` | `LanceServiceTest`, `LanceControllerTest` |

---

## contemplacao — Apuração e Contemplações

| REQ-ID | Regra | Classe(s) Java | Controller/Endpoint | Teste |
|--------|-------|----------------|---------------------|-------|
| REQ-CON-001 | Motor de Apuração | `MotorApuracaoService`, `ContemplacaoService` | `ContemplacaoController` — `POST /api/contemplacoes` | `MotorApuracaoServiceTest` |
| REQ-CON-002 | Checagem Saldo Ledger | `ContabilidadeService.obterSaldoFundoComum()` | `POST /api/contemplacoes` (validação interna) | `ContemplacaoServiceTest` |
| REQ-CON-003 | Homologação Lance Livre (ADR 004) | `ContemplacaoService.confirmarPagamentoLance()` | `ContemplacaoController` — `POST /api/contemplacoes/lances/{id}/integralizar` | `ContemplacaoServiceTest` |
| REQ-CON-004 | Lançamentos Contábeis | `ContabilidadeService` | `ContemplacaoController` — `POST /api/contemplacoes/{id}/pagamento-bem` | `ContabilidadeServiceTest` |
| REQ-CON-005 | Parametrização e Oferta de Lance Fixo | `LanceService` | N/A (validação interna) | `LanceServiceTest` |
| REQ-CON-006 | Motor de Apuração do Lance Fixo | `MotorApuracaoService`, `ContemplacaoService` | N/A (interno à assembleia) | `MotorApuracaoServiceTest` |

---

## inadimplencia — Mora e Inadimplência

| REQ-ID | Regra | Classe(s) Java | Controller/Endpoint | Teste |
|--------|-------|----------------|---------------------|-------|
| REQ-INA-001 | Encargos de Mora | `ParcelaService.pagar()` | `ParcelaController` — `PUT /api/parcelas/{id}/pagar` | `ParcelaServiceTest` |
| REQ-INA-002 | Destinação Legal | `ContabilidadeService` | `ParcelaController` — `POST /api/parcelas/{id}/estornar` | `ContabilidadeServiceTest` |
| REQ-INA-003 | Cálculo Volátil | `ParcelaService.obterInadimplenciaCota()` | `CotaController` — `GET /api/cotas/{id}/inadimplencia` | `ParcelaServiceTest` |

---

## seguros — Seguros

| REQ-ID | Regra | Classe(s) Java | Controller/Endpoint | Teste |
|--------|-------|----------------|---------------------|-------|
| REQ-SEG-001 | Cobrança do Prêmio | `Parcela.java` (campo `valorSeguro`) | `ParcelaController` — `POST /api/parcelas` | `ParcelaServiceTest` |
| REQ-SEG-002 | Repasse Contábil COSIF | `ContabilidadeService` | N/A (interno ao pagar) | `ContabilidadeServiceTest` |

---

## excluidos — Restituição de Excluídos

| REQ-ID | Regra | Classe(s) Java | Controller/Endpoint | Teste |
|--------|-------|----------------|---------------------|-------|
| REQ-EXC-001 | Elegibilidade | `CotaService.cancelarCota()` | `CotaController` — `POST /api/cotas/{id}/cancelar` | `CotaServiceTest` |
| REQ-EXC-002 | Memória de Cálculo (ADR 005) | `CotaService.reembolsarCota()` | `CotaController` — `POST /api/cotas/{id}/reembolsar` | `CotaServiceTest` |
| REQ-EXC-003 | Destinação Cláusula Penal | `CotaService.reembolsarCota()` | N/A (interno) | `CotaServiceTest` |
| REQ-EXC-004 | Fixação de Saldo | `CotaService.reembolsarCota()` | N/A (interno) | `CotaServiceTest` |
| REQ-EXC-005 | Contabilização COSIF | `ContabilidadeService` | N/A (interno) | `ContabilidadeServiceTest` |

---

## encerramento — Reajustes e Encerramento

| REQ-ID | Regra | Classe(s) Java | Controller/Endpoint | Teste |
|--------|-------|----------------|---------------------|-------|
| REQ-ENC-001 | Motor de Reajuste | `GrupoService.reajustarGrupo()` | `GrupoController` — `PUT /api/grupos/{id}/reajuste` | `GrupoServiceTest` |
| REQ-ENC-002 | Encerramento + PDD (ADR 006) | `GrupoService.encerrarGrupo()` | `GrupoController` — `POST /api/grupos/{id}/encerrar` | `GrupoServiceTest` |
| REQ-ENC-003 | Recursos Não Procurados (RNP) | `GrupoService.encerrarGrupo()` | `POST /api/grupos/{id}/encerrar` | `GrupoServiceTest` |

---

## bens-referencia — Bens de Referência e Tabela FIPE

| REQ-ID | Regra | Classe(s) Java | Controller/Endpoint | Teste |
|--------|-------|----------------|---------------------|-------|
| REQ-BEM-001 | Categorias Regulamentadas BACEN | `CategoriaBemDataLoader`, `BemReferenciaService` | `BemReferenciaController` — `GET /api/bens-referencia/categorias` | Testes de integração REST |
| REQ-BEM-002 | Integração FIPE (Parallelum API) | `FipeService` | `BemReferenciaController` — `GET /api/bens-referencia/fipe/*` | Testes de integração REST |
| REQ-BEM-003 | Auditoria de Histórico | `HistoricoValorBemReferencia`, `BemReferenciaService` | `BemReferenciaController` — `GET /api/bens-referencia/{id}/historico` | Testes de integração REST |

---

## ⚠️ SPEC DRIFT Pendente

| ID | Capability | Descrição | Status |
|----|-----------|-----------|--------|
| DRIFT-001 | contemplacao | `LANCE_FIXO` declarado no enum `TipoContemplacao` mas sem motor de apuração especificado no REQ-CON-001 | ✅ RESOLVIDO v1.1 |
