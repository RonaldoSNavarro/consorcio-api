# Contrato de API — Índices Econômicos BACEN

## Endpoints REST (`/api/indices-economicos`)

### 1. Obter Variação dos Últimos 12 Meses
`GET /api/indices-economicos/{tipoIndice}/ultimos-12-meses`

**Parâmetros de Path**:
- `tipoIndice`: Enum (`INCC`, `IPCA`, `IGP_M`)

**Resposta (200 OK)**:
```json
[
  {
    "tipoIndice": "INCC",
    "dataReferencia": "2025-07-01",
    "valorPercentual": 0.91
  },
  ...
]
```

---

### 2. Simular Reajuste Acumulado 12M
`GET /api/indices-economicos/simular?tipoIndice=INCC&valorAtual=100000`

**Resposta (200 OK)**:
```json
{
  "tipoIndice": "INCC",
  "percentualAcumulado12Meses": 6.7768,
  "fatorReajuste": 1.067768,
  "valorOriginal": 100000,
  "novoValorCalculado": 106776.80,
  "historico12Meses": [...]
}
```

---

### 3. Reajustar Grupo por Índice BACEN
`POST /api/indices-economicos/grupos/{grupoId}/reajustar?tipoIndice=INCC`

**Resposta (200 OK)**:
Retorna `GrupoResponseDTO` atualizado e registra movimentos de ajuste financeiro e histórico das cotas.
