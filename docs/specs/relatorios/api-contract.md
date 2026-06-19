# Contract API — Relatórios e PLD/FT (Capability: relatorios)

Status: LOCKED
Versão: v1.1
Aprovações: CTO [✅] (retroativo com ajustes regulatórios)

**Atenção**: Este contrato foi atualizado na versão 1.1 para refletir as correções regulatórias apontadas pelos Especialistas de Domínio (monitoramento efetivo de lances pagos e métricas de inadimplência).

## Visão Geral

Endpoints para a geração de relatórios de Balancete COSIF, Estatísticas do Grupo e alertas para monitoramento de lavagem de dinheiro (PLD/FT).

**Base URL:** `/api/relatorios`

**Segurança Global:**
- Header obrigatório: `Authorization: Bearer <JWT>`
- O endpoint PLD/FT e Balancete 4110 são restritos a usuários com os papéis `ROLE_ADMIN` ou `ROLE_AUDITOR`. O papel `ROLE_CONSORCIADO` recebe `403 Forbidden`.

---

## Endpoints

### 1. Alerta PLD/FT (Monitoramento)
Lista lances registrados acima de R$ 50.000,00 para um período específico, com foco em Prevenção à Lavagem de Dinheiro / Financiamento ao Terrorismo (PLD/FT).

- **Método:** `GET`
- **Path:** `/pld-ft`
- **Permissões:** `ROLE_ADMIN`, `ROLE_AUDITOR`

**Query Parameters:**
| Parâmetro | Tipo | Obrigatório | Formato | Descrição |
|:---|:---|:---:|:---|:---|
| `dataInicio` | `string` | Sim | `ISO-8601 (YYYY-MM-DDThh:mm:ss)` | Data/hora inicial da busca. |
| `dataFim` | `string` | Sim | `ISO-8601 (YYYY-MM-DDThh:mm:ss)` | Data/hora final da busca. |

**Resposta Sucesso (200 OK):**
```json
[
  {
    "lanceId": 1054,
    "cotaId": 98,
    "nomeConsorciado": "João da Silva",
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

### 2. Balancete Contábil (Doc 4110)
Gera o balancete de um grupo consolidando as contas COSIF e avaliando a quadratura de saldos.

- **Método:** `GET`
- **Path:** `/balancete/{grupoId}`
- **Permissões:** `ROLE_ADMIN`, `ROLE_AUDITOR`

**Path Variables:**
| Variável | Tipo | Descrição |
|:---|:---|:---|
| `grupoId` | `integer` | ID do grupo do consórcio. |

**Query Parameters:**
| Parâmetro | Tipo | Obrigatório | Formato | Descrição |
|:---|:---|:---:|:---|:---|
| `dataReferencia` | `string` | Não | `YYYY-MM-DD` | Data base para os saldos. Se não informado, assume data atual. |

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

### 3. Estatísticas do Grupo (Doc 2080)
Consolida adesões, exclusões, lances e contemplações em um determinado período para análise gerencial.

- **Método:** `GET`
- **Path:** `/estatisticas/{grupoId}`

**Path Variables:**
| Variável | Tipo | Descrição |
|:---|:---|:---|
| `grupoId` | `integer` | ID do grupo do consórcio. |

**Query Parameters:**
| Parâmetro | Tipo | Obrigatório | Formato | Descrição |
|:---|:---|:---:|:---|:---|
| `dataInicio` | `string` | Sim | `YYYY-MM-DD` | Data de início do período. |
| `dataFim` | `string` | Sim | `YYYY-MM-DD` | Data de fim do período. |

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

- `400 Bad Request`: Faltam query parameters obrigatórios (ex: `dataInicio` ou `dataFim`) ou a formatação das datas está incorreta.
- `401 Unauthorized`: Token JWT ausente, expirado ou inválido.
- `403 Forbidden`: O usuário autenticado (ex: Consorciado) não tem a permissão `ROLE_ADMIN` ou `ROLE_AUDITOR` necessária.
- `404 Not Found`: `grupoId` não foi localizado no sistema.
- `500 Internal Server Error`: Falha interna durante o processamento.
