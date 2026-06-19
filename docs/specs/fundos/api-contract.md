# 📋 Contrato de API — Composição de Fundos e Parcelas (fundos)

*   **Capability**: fundos
*   **Versão**: v1.0 (Baseline Retroativo)
*   **Spec de referência**: [spec.md](spec.md)
*   **Última alteração**: Geração retroativa baseada no código implementado.

---

## 🔐 Autenticação

Todos os endpoints requerem cookie `HttpOnly` com JWT válido.

---

## 📡 Endpoints

### POST `/api/parcelas`

| Item | Valor |
|---|---|
| **Descrição** | Gera uma nova parcela (cobrança) vinculada a uma cota |
| **Auth** | 🔒 Autenticado |
| **REQ-IDs** | REQ-FUN-001, REQ-FUN-002 |

**Request Body** (JSON):
```json
{
  "cotaId": "Long — ID da cota (obrigatório, positivo)",
  "numeroParcela": "Integer — número sequencial (obrigatório, positivo)",
  "valorFundoComum": "BigDecimal — componente FC (obrigatório, positivo)",
  "valorTaxaAdministracao": "BigDecimal — componente TA (obrigatório, positivo)",
  "valorFundoReserva": "BigDecimal — componente FR (obrigatório, positivo)",
  "valorSeguro": "BigDecimal — componente SEG (obrigatório, ≥ 0)",
  "dataVencimento": "LocalDate — vencimento (obrigatório, futuro ou presente)"
}
```

**Response `201 Created`** (JSON):
```json
{
  "id": "Long",
  "cotaId": "Long",
  "numeroParcela": "Integer",
  "valorFundoComum": "BigDecimal",
  "percentualFundoComum": "BigDecimal",
  "valorTaxaAdministracao": "BigDecimal",
  "valorFundoReserva": "BigDecimal",
  "valorSeguro": "BigDecimal",
  "valorParcela": "BigDecimal — soma calculada pelo hook JPA",
  "valorMulta": "BigDecimal",
  "valorJuros": "BigDecimal",
  "valorPago": "BigDecimal",
  "dataVencimento": "LocalDate",
  "dataPagamento": "LocalDate | null",
  "status": "StatusParcela — PENDENTE"
}
```

---

### GET `/api/parcelas/cota/{cotaId}`

| Item | Valor |
|---|---|
| **Descrição** | Lista todas as parcelas de uma cota |
| **Auth** | 🔒 Autenticado |
| **REQ-IDs** | REQ-FUN-001 |

**Path Parameters**: `cotaId` (Long)

**Response `200 OK`**: JSON Array de `ParcelaResponseDTO`.

---

## 📐 DTOs de Referência

### Request: `ParcelaRequestDTO`
```java
public record ParcelaRequestDTO(
    @NotNull @Positive Long cotaId,
    @NotNull @Positive Integer numeroParcela,
    @NotNull @Positive BigDecimal valorFundoComum,
    @NotNull @Positive BigDecimal valorTaxaAdministracao,
    @NotNull @Positive BigDecimal valorFundoReserva,
    @NotNull @PositiveOrZero BigDecimal valorSeguro,
    @NotNull @FutureOrPresent LocalDate dataVencimento
) {}
```

### Response: `ParcelaResponseDTO`
```java
public record ParcelaResponseDTO(
    Long id, Long cotaId, Integer numeroParcela,
    BigDecimal valorFundoComum, BigDecimal percentualFundoComum,
    BigDecimal valorTaxaAdministracao, BigDecimal valorFundoReserva,
    BigDecimal valorSeguro, BigDecimal valorParcela,
    BigDecimal valorMulta, BigDecimal valorJuros, BigDecimal valorPago,
    LocalDate dataVencimento, LocalDate dataPagamento, StatusParcela status
) {}
```
