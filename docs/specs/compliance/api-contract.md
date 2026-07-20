# Contrato de API â€” Compliance (Listas Restritivas)

*   **Status**: IMPLEMENTED v1.0
VersÃ£o: v1.2

## 1. Endpoints

### 1.1. SincronizaÃ§Ã£o Manual de Listas
- **MÃ©todo**: `POST`
- **Rota**: `/api/compliance/sincronizar`
- **PermissÃ£o**: `ROLE_ADMIN`, `ROLE_COMPLIANCE`
- **DescriÃ§Ã£o**: Dispara a rotina assÃ­ncrona de ingestÃ£o das APIs externas e arquivos locais, executando em seguida o matching com os clientes.
- **Request Body**: `N/A`
- **Response**: `202 Accepted`
```json
{
  "ofacStatus": "ONLINE",
  "ofacRegistros": 8452,
  "pepRegistros": 15422,
  "onuRegistros": 1204,
  "ibgeRegistros": 0,
  "erros": []
}
```

### 1.2. Listar Alertas de Compliance
- **MÃ©todo**: `GET`
- **Rota**: `/api/compliance/alertas`
- **PermissÃ£o**: `ROLE_COMPLIANCE`, `ROLE_ADMIN`
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
- **MÃ©todo**: `PUT`
- **Rota**: `/api/compliance/alertas/{alertaId}/deliberar`
- **PermissÃ£o**: `ROLE_COMPLIANCE`, `ROLE_ADMIN`
- **Request Body**:
```json
{
  "novoStatus": "CONFIRMADO",
  "justificativa": "CoincidÃªncia exata de passaporte e data de nascimento na lista OFAC. Bloqueio cautelar acionado."
}
```
- **Response**: `200 OK`

### 1.4. Upload de Arquivo PEP (CSV)
- **MÃ©todo**: `POST`
- **Rota**: `/api/compliance/upload/pep`
- **PermissÃ£o**: `ROLE_ADMIN`, `ROLE_COMPLIANCE`
- **Multipart Form Data**:
  - `file`: Arquivo CSV contendo CPFs e nomes de Pessoas Expostas Politicamente.
- **Response**: `200 OK`
```json
{
  "mensagem": "Arquivo PEP processado com sucesso. [N] registros inseridos/atualizados."
}
```

### 1.5. Upload de Arquivo ONU (XML)
- **MÃ©todo**: `POST`
- **Rota**: `/api/compliance/upload/onu`
- **PermissÃ£o**: `ROLE_ADMIN`, `ROLE_COMPLIANCE`
- **Multipart Form Data**:
  - `file`: Arquivo XML contendo indivÃ­duos e entidades sancionados pela ONU.
- **Response**: `200 OK`
```json
{
  "mensagem": "Arquivo ONU processado com sucesso. [N] registros inseridos/atualizados."
}
```

### 1.6. Upload de Arquivo IBGE (XLS)
- **MÃ©todo**: `POST`
- **Rota**: `/api/compliance/upload/ibge`
- **PermissÃ£o**: `ROLE_ADMIN`, `ROLE_COMPLIANCE`
- **Multipart Form Data**:
  - `file`: Arquivo XLS de MunicÃ­pios de Faixa de Fronteira e Cidades GÃªmeas do IBGE.
- **Response**: `200 OK`
```json
{
  "mensagem": "Arquivo IBGE processado com sucesso. [N] municÃ­pios de fronteira indexados."
}
```

### 1.7. Obter ConfiguraÃ§Ã£o de Agendamento (Cron)
- **MÃ©todo**: `GET`
- **Rota**: `/api/compliance/config`
- **PermissÃ£o**: `ROLE_ADMIN`, `ROLE_COMPLIANCE`
- **Response**: `200 OK`
```json
{
  "cronExpression": "0 0 3 * * *",
  "frequencia": "DIARIO",
  "horario": "03:00",
  "dataAtualizacao": "2026-06-22T23:00:00"
}
```

### 1.8. Atualizar ConfiguraÃ§Ã£o de Agendamento (Cron)
- **MÃ©todo**: `PUT`
- **Rota**: `/api/compliance/config`
- **PermissÃ£o**: `ROLE_ADMIN`, `ROLE_COMPLIANCE`
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

### 1.9. Listar ExecuÃ§Ãµes de SincronizaÃ§Ã£o (Logs)
- **MÃ©todo**: `GET`
- **Rota**: `/api/compliance/execucoes`
- **PermissÃ£o**: `ROLE_ADMIN`, `ROLE_COMPLIANCE`
- **Response**: `200 OK`
```json
[
  {
    "id": 15,
    "dataExecucao": "2026-06-25T01:30:00",
    "triggerExecucao": "CRON",
    "ofacStatus": "ONLINE",
    "pepRegistros": 15422,
    "onuRegistros": 1204,
    "ibgeRegistros": 0,
    "ofacRegistros": 8452,
    "duracaoMs": 4500,
    "erros": "[]"
  }
]
```
