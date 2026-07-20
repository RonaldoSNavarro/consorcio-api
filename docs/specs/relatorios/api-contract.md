# Contract API â€” RelatÃ³rios e PLD/FT (Capability: relatorios)

*   **Status**: IMPLEMENTED v1.0
VersÃ£o: v1.1
AprovaÃ§Ãµes: CTO [âœ…] (retroativo com ajustes regulatÃ³rios)

**AtenÃ§Ã£o**: Este contrato foi atualizado na versÃ£o 1.1 para refletir as correÃ§Ãµes regulatÃ³rias apontadas pelos Especialistas de DomÃ­nio (monitoramento efetivo de lances pagos e mÃ©tricas de inadimplÃªncia).

## VisÃ£o Geral

Endpoints para a geraÃ§Ã£o de relatÃ³rios de Balancete COSIF, EstatÃ­sticas do Grupo e alertas para monitoramento de lavagem de dinheiro (PLD/FT).

**Base URL:** `/api/relatorios`

**SeguranÃ§a Global:**
- Header obrigatÃ³rio: `Authorization: Bearer <JWT>`
- O endpoint PLD/FT e Balancete 4110 sÃ£o restritos a usuÃ¡rios com os papÃ©is `ROLE_ADMIN` ou `ROLE_AUDITOR`. O papel `ROLE_CONSORCIADO` recebe `403 Forbidden`.

---

## Endpoints

### 1. Alerta PLD/FT (Monitoramento)
Lista lances registrados acima de R$ 50.000,00 para um perÃ­odo especÃ­fico, com foco em PrevenÃ§Ã£o Ã  Lavagem de Dinheiro / Financiamento ao Terrorismo (PLD/FT).

- **MÃ©todo:** `GET`
- **Path:** `/pld-ft`
- **PermissÃµes:** `ROLE_ADMIN`, `ROLE_AUDITOR`

**Query Parameters:**
| ParÃ¢metro | Tipo | ObrigatÃ³rio | Formato | DescriÃ§Ã£o |
|:---|:---|:---:|:---|:---|
| `dataInicio` | `string` | Sim | `ISO-8601 (YYYY-MM-DDThh:mm:ss)` | Data/hora inicial da busca. |
| `dataFim` | `string` | Sim | `ISO-8601 (YYYY-MM-DDThh:mm:ss)` | Data/hora final da busca. |

**Resposta Sucesso (200 OK):**
```json
[
  {
    "lanceId": 1054,
    "cotaId": 98,
    "nomeConsorciado": "JoÃ£o da Silva",
    "cpfCnpj": "123.456.789-00",
    "valorOferta": 55000.00,
    "tipoLance": "LIVRE",
    "dataOferta": "2026-06-18T14:30:00",
    "grupoId": 12,
    "codigoGrupo": "GRUPO-XYZ"
  }
]
```

---

### 2. Balancete ContÃ¡bil (Doc 4110)
Gera o balancete de um grupo consolidando as contas COSIF e avaliando a quadratura de saldos.

- **MÃ©todo:** `GET`
- **Path:** `/balancete/{grupoId}`
- **PermissÃµes:** `ROLE_ADMIN`, `ROLE_AUDITOR`

**Path Variables:**
| VariÃ¡vel | Tipo | DescriÃ§Ã£o |
|:---|:---|:---|
| `grupoId` | `integer` | ID do grupo do consÃ³rcio. |

**Query Parameters:**
| ParÃ¢metro | Tipo | ObrigatÃ³rio | Formato | DescriÃ§Ã£o |
|:---|:---|:---:|:---|:---|
| `dataReferencia` | `string` | NÃ£o | `YYYY-MM-DD` | Data base para os saldos. Se nÃ£o informado, assume data atual. |

**Resposta Sucesso (200 OK):**
```json
{
  "grupoId": 12,
  "codigoGrupo": "GRUPO-XYZ",
  "dataReferencia": "2026-06-19",
  "contas": [
    {
      "codigoCosif": "2.1.2.10.10-6",
      "nome": "Disponibilidades",
      "natureza": "DEVEDORA",
      "saldo": 150000.00
    }
  ]
}
```

---

### 3. EstatÃ­sticas do Grupo (Doc 2080)
Consolida adesÃµes, exclusÃµes, lances e contemplaÃ§Ãµes em um determinado perÃ­odo para anÃ¡lise gerencial.

- **MÃ©todo:** `GET`
- **Path:** `/estatisticas/{grupoId}`

**Path Variables:**
| VariÃ¡vel | Tipo | DescriÃ§Ã£o |
|:---|:---|:---|
| `grupoId` | `integer` | ID do grupo do consÃ³rcio. |

**Query Parameters:**
| ParÃ¢metro | Tipo | ObrigatÃ³rio | Formato | DescriÃ§Ã£o |
|:---|:---|:---:|:---|:---|
| `dataInicio` | `string` | Sim | `YYYY-MM-DD` | Data de inÃ­cio do perÃ­odo. |
| `dataFim` | `string` | Sim | `YYYY-MM-DD` | Data de fim do perÃ­odo. |

**Resposta Sucesso (200 OK):**
```json
{
  "grupoId": 12,
  "codigoGrupo": "GRUPO-XYZ",
  "dataInicio": "2026-05-01",
  "dataFim": "2026-05-31",
  "totalAdesoes": 5,
  "totalExclusoes": 1,
  "totalLancesOfertados": 15,
  "totalLancesVencedores": 2,
  "totalContemplacoesSorteio": 1,
  "totalContemplacoesLance": 2,
  "totalCotasInadimplentes": 3,
  "valorTotalCreditosLiberados": 120000.00
}
```

---

## Tratamento de Erros Comuns

O backend utiliza um `@ControllerAdvice` global e padroniza os erros seguindo a RFC 7807 (Problem Details).

- `400 Bad Request`: Faltam query parameters obrigatÃ³rios (ex: `dataInicio` ou `dataFim`) ou a formataÃ§Ã£o das datas estÃ¡ incorreta.
- `401 Unauthorized`: Token JWT ausente, expirado ou invÃ¡lido.
- `403 Forbidden`: O usuÃ¡rio autenticado (ex: Consorciado) nÃ£o tem a permissÃ£o `ROLE_ADMIN` ou `ROLE_AUDITOR` necessÃ¡ria.
- `404 Not Found`: `grupoId` nÃ£o foi localizado no sistema.
- `500 Internal Server Error`: Falha interna durante o processamento.
