# 📋 Contrato de API — Seguros (seguros)

*   **Capability**: seguros
*   **Versão**: v1.0 (Baseline Retroativo)
*   **Spec de referência**: [spec.md](spec.md)
*   **Última alteração**: Geração retroativa baseada no código implementado.

---

## 🔐 Autenticação

Todos os endpoints requerem cookie `HttpOnly` com JWT válido.

---

## 📡 Endpoints

> **Nota arquitetural**: A capability de Seguros não possui controller dedicado. O valor do seguro é integrado como componente da parcela mensal (`valorSeguro` no `ParcelaRequestDTO`) e seu lançamento contábil é processado automaticamente pelo `ContabilidadeService` no momento do pagamento da parcela. A cobrança é uma fração da parcela conforme REQ-SEG-001, e o repasse contábil segue REQ-SEG-002.

### Endpoints Compartilhados (via `ParcelaController`)

| Endpoint | Relação com Seguros |
|---|---|
| `POST /api/parcelas` | O campo `valorSeguro` define o prêmio cobrado na parcela |
| `PUT /api/parcelas/{id}/pagar` | Ao pagar, o `ContabilidadeService` destina `valorSeguro` para `2.1.2.10.40-5` |
| `GET /api/parcelas/cota/{cotaId}` | Retorna parcelas com o detalhe `valorSeguro` para cada título |

---

## 📐 DTOs de Referência

O campo de seguro está presente nos DTOs de parcela:

```java
// No ParcelaRequestDTO
@NotNull @PositiveOrZero BigDecimal valorSeguro

// No ParcelaResponseDTO
BigDecimal valorSeguro
```
