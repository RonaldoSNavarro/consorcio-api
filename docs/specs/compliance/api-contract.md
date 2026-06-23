# Contrato de API — Compliance (Listas Restritivas)

Status: PATCHED
Versão: v1.1

## 1. Endpoints

### 1.1. Sincronização Manual de Listas
- **Método**: `POST`
- **Rota**: `/api/compliance/sincronizar`
- **Permissão**: `ROLE_ADMIN`, `ROLE_COMPLIANCE`
- **Descrição**: Dispara a rotina assíncrona de ingestão das APIs externas e arquivos locais, executando em seguida o matching com os clientes.
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
  - `origemLista` (opcional): `PEP`, `OFAC`, `ONU`, `IBGE`
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
- **Permissão**: `ROLE_COMPLIANCE`, `ROLE_ADMIN`
- **Request Body**:
```json
{
  "novoStatus": "CONFIRMADO",
  "justificativa": "Coincidência exata de passaporte e data de nascimento na lista OFAC. Bloqueio cautelar acionado."
}
```
- **Response**: `200 OK`

### 1.4. Upload de Arquivo PEP (CSV)
- **Método**: `POST`
- **Rota**: `/api/compliance/upload/pep`
- **Permissão**: `ROLE_ADMIN`, `ROLE_COMPLIANCE`
- **Multipart Form Data**:
  - `file`: Arquivo CSV contendo CPFs e nomes de Pessoas Expostas Politicamente.
- **Response**: `200 OK`
```json
{
  "mensagem": "Arquivo PEP processado com sucesso. [N] registros inseridos/atualizados."
}
```

### 1.5. Upload de Arquivo ONU (XML)
- **Método**: `POST`
- **Rota**: `/api/compliance/upload/onu`
- **Permissão**: `ROLE_ADMIN`, `ROLE_COMPLIANCE`
- **Multipart Form Data**:
  - `file`: Arquivo XML contendo indivíduos e entidades sancionados pela ONU.
- **Response**: `200 OK`
```json
{
  "mensagem": "Arquivo ONU processado com sucesso. [N] registros inseridos/atualizados."
}
```

### 1.6. Upload de Arquivo IBGE (XLS)
- **Método**: `POST`
- **Rota**: `/api/compliance/upload/ibge`
- **Permissão**: `ROLE_ADMIN`, `ROLE_COMPLIANCE`
- **Multipart Form Data**:
  - `file`: Arquivo XLS de Municípios de Faixa de Fronteira e Cidades Gêmeas do IBGE.
- **Response**: `200 OK`
```json
{
  "mensagem": "Arquivo IBGE processado com sucesso. [N] municípios de fronteira indexados."
}
```

### 1.7. Obter Configuração de Agendamento (Cron)
- **Método**: `GET`
- **Rota**: `/api/compliance/config`
- **Permissão**: `ROLE_ADMIN`, `ROLE_COMPLIANCE`
- **Response**: `200 OK`
```json
{
  "cronExpression": "0 0 3 * * *",
  "frequencia": "DIARIO",
  "horario": "03:00",
  "dataAtualizacao": "2026-06-22T23:00:00"
}
```

### 1.8. Atualizar Configuração de Agendamento (Cron)
- **Método**: `PUT`
- **Rota**: `/api/compliance/config`
- **Permissão**: `ROLE_ADMIN`, `ROLE_COMPLIANCE`
- **Request Body**:
```json
{
  "frequencia": "DIARIO",
  "horario": "02:30"
}
```
- **Response**: `200 OK`
```json
{
  "cronExpression": "0 30 2 * * *",
  "frequencia": "DIARIO",
  "horario": "02:30",
  "dataAtualizacao": "2026-06-22T23:15:00"
}
```

