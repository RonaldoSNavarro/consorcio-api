# Contrato de API — Compliance (Listas Restritivas)

Status: DRAFT
Versão: v1.0

## 1. Endpoints

### 1.1. Sincronização Manual de Listas
- **Método**: `POST`
- **Rota**: `/api/compliance/sincronizar`
- **Permissão**: `ROLE_ADMIN`, `ROLE_COMPLIANCE`
- **Descrição**: Dispara a rotina assíncrona de ingestão das APIs do Portal da Transparência, OFAC e ONU.
- **Request Body**: `N/A`
- **Response**: `202 Accepted`
```json
{
  "mensagem": "Sincronização de listas restritivas iniciada em background.",
  "dataHora": "2026-06-19T10:00:00"
}
```

### 1.2. Listar Alertas de Compliance
- **Método**: `GET`
- **Rota**: `/api/compliance/alertas`
- **Permissão**: `ROLE_COMPLIANCE`, `ROLE_ADMIN`
- **Query Params**:
  - `status` (opcional): `PENDENTE_ANALISE`, `FALSO_POSITIVO`, `CONFIRMADO`
  - `origemLista` (opcional): `PEP`, `OFAC`, `ONU`
- **Response**: `200 OK`
```json
[
  {
    "alertaId": 1054,
    "clienteId": 89,
    "nomeCliente": "JOHN DOE",
    "cpfCnpj": "111.222.333-44",
    "origemLista": "OFAC",
    "nomeEncontradoLista": "JOHN DOE",
    "scoreSimilaridade": 1.0,
    "status": "PENDENTE_ANALISE",
    "dataDeteccao": "2026-06-19T11:00:00"
  }
]
```

### 1.3. Deliberar sobre Alerta
- **Método**: `PUT`
- **Rota**: `/api/compliance/alertas/{alertaId}/deliberar`
- **Permissão**: `ROLE_COMPLIANCE`
- **Request Body**:
```json
{
  "novoStatus": "CONFIRMADO",
  "justificativa": "Coincidência exata de passaporte e data de nascimento na lista OFAC. Bloqueio cautelar acionado."
}
```
- **Response**: `200 OK`
