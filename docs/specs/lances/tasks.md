# 📋 Decomposição de Tarefas — Oferta de Lances (lances)

*   **Capability**: lances
*   **Spec de referência**: [spec.md](spec.md)
*   **Total de tarefas**: 9
*   **REQ-IDs cobertos**: 3/3

---

## Tarefas

### [BACKEND] REQ-LAN-001: Elegibilidade para Oferta de Lance
- [x] Criar entidade `Lance.java` com cota, assembleia, tipo, valorOferta, statusApuracao
- [x] Criar `LanceService.java` — validação de elegibilidade (status ATIVA, adimplência, assembleia CAPTANDO)
- [x] Criar DTOs: `LanceRequestDTO`, `LanceResponseDTO`

### [BACKEND] REQ-LAN-002: Modalidade de Lance Embutido
- [x] Implementar validação no `LanceService` — teto de `percentualLanceEmbutidoMaximo` sobre valorCredito
- [x] Criar enum `TipoLance` — LIVRE, EMBUTIDO

### [BACKEND] REQ-LAN-003: Motor de Amortização de Lances
- [x] Implementar `ParcelaService.amortizarPorReducaoDePrazo()` — quita parcelas de trás para frente com diluição de TA/FR
- [x] Implementar `ParcelaService.amortizarPorDiluicao()` — reduz uniformemente cada parcela restante
- [x] Implementar regra do centavo perdido (ajuste de arredondamento na última parcela)
- [x] Criar endpoints em `ParcelaController` — `POST /cota/{cotaId}/lance/reducao-prazo` e `POST /cota/{cotaId}/lance/diluicao`

### [BACKEND] REQ-LAN-004: Cadastro de Oferta de Lance Fixo
- [x] Criar migration Flyway para adicionar `percentual_lance_fixo` na tabela `grupos` e `modalidade` na tabela `lances`
- [x] Atualizar entidade `Grupo.java` com o atributo `percentualLanceFixo` (default 0.2000)
- [x] Criar enum `ModalidadeLance` (LIVRE, FIXO) e atualizar a entidade `Lance.java` com o atributo `modalidade`
- [x] Atualizar `LanceRequestDTO` e `LanceResponseDTO` com o campo `modalidade` e o correspondente mapper `LanceMapper.java`
- [x] Implementar regra no `LanceService.registrarLance` para calcular automaticamente o `valorOferta` se `modalidade` for `FIXO` e torná-lo imutável
- [x] Criar classe `LanceController.java` expondo o endpoint `POST /api/lances` integrado com `LanceService`
- [x] Escrever testes de integração em `LanceControllerTest.java` validando segurança e requisições no endpoint `POST /api/lances`
- [x] Escrever testes de unidade em `LanceServiceTest.java` para o fluxo de cadastro e validação de Lance Fixo


