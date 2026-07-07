# Project: Compliance and PLD/FT Evolution

## Architecture
- **Layer**: Service Layer / Web API / Persistence Layer.
- **Data Flow**:
  1. Manual or scheduled sync updates `ListaRestritiva` (PEP, OFAC, ONU, IBGE).
  2. `MatchComplianceService` compares client names/CPFs/residence against restrictions.
  3. Matches trigger `AlertaCompliance` in state `PENDENTE_ANALISE`.
  4. Core transaction flows (proposals, transfers, contemplations) check for pending/confirmed alerts and block operations accordingly.
  5. Bidding engine checks winner values and flags lances >= 50k using own resources for Siscoaf notification.

## Milestones
| # | Name | Scope | Dependencies | Status |
|---|---|---|---|---|
| 1 | Milestone 1: Exploration | Deep exploration of compliance, lances, contemplations, and reports | None | DONE |
| 2 | Milestone 2: Implementation | Implement Jaro-Winkler, blocks, and Siscoaf logic | M1 | DONE |
| 2.1 | Milestone 2.1: Refinement | Refine status mappings, payout block checks, and N+1 query optimizations | M2 | IN_PROGRESS |
| 3 | Milestone 3: Verification | Run QA, reviews, challenges, and forensic audit | M2.1 | PLANNED |
| 4 | Milestone 4: Handoff | Report completion to Sentinel | M3 | PLANNED |

## Interface Contracts
### `MatchComplianceService`
- `void cruzarBaseDeClientes()`: Compares clients against `ListaRestritiva` using Jaro-Winkler similarity (score >= 0.90 for name matches and PEP centralized CPF masking rules).

### `ContemplacaoService`
- `ContemplacaoResponseDTO registrar(ContemplacaoRequestDTO dto)`: Registers contemplations. Must block if the client has alerts `PENDENTE_ANALISE` or `CONFIRMADO`.

### `CotaService`
- `CotaResponseDTO transferirCota(Long cotaId, TransferirCotaRequestDTO dto)`: Transfers cota. Must block if current owner or new client has alerts `PENDENTE_ANALISE` or `CONFIRMADO`.

### `PropostaAdesaoService`
- `PropostaAdesao criarProposta(PropostaRequestDTO request)`: Creates proposals. Blocks if client has alerts `PENDENTE_ANALISE` or `CONFIRMADO`.

## Code Layout
- Controllers: `br.com.estudo.consorcio.controller.*`
- Models: `br.com.estudo.consorcio.domain.model.*`
- Repositories: `br.com.estudo.consorcio.domain.repository.*`
- Services: `br.com.estudo.consorcio.service.*`
- Migrations: `src/main/resources/db/migration/`
