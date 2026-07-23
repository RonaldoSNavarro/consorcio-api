# 🔌 Contrato de API — Bens de Referência e Tabela FIPE

Base URL: `/api/bens-referencia`

---

## 📍 Endpoints

### 1. Listar Categorias de Bem
- **GET** `/categorias`
- **Permissão**: `VIEW_GRUPOS` | `MANAGE_GRUPOS`
- **Response HTTP 200 OK**:
```json
[
  {
    "id": 1,
    "nome": "Veículos Automotores",
    "tipoBacen": "BEM_MOVEL_I",
    "indiceReajustePadrao": "FIPE"
  },
  {
    "id": 2,
    "nome": "Imóveis",
    "tipoBacen": "BEM_IMOVEL",
    "indiceReajustePadrao": "INCC"
  }
]
```

### 2. Listar Bens de Referência (Paginado)
- **GET** `?categoriaId={id}&page=0&size=20`
- **Permissão**: `VIEW_GRUPOS` | `MANAGE_GRUPOS`
- **Response HTTP 200 OK**: Page<BemReferenciaResponseDTO>

### 3. Cadastrar Bem de Referência
- **POST** `/`
- **Permissão**: `MANAGE_GRUPOS`
- **Request Body**:
```json
{
  "categoriaBemId": 1,
  "descricao": "Sedan Premium 2.0 Flex",
  "valorAtual": 125000.00,
  "codigoFipe": "004001-8",
  "ativo": true
}
```
- **Response HTTP 201 Created**

### 4. Atualizar Bem de Referência
- **PUT** `/{id}?origemReajuste=FIPE`
- **Permissão**: `MANAGE_GRUPOS`
- **Response HTTP 200 OK**

### 5. Consultar Histórico de Preços
- **GET** `/{id}/historico`
- **Permissão**: `VIEW_GRUPOS` | `MANAGE_GRUPOS`
- **Response HTTP 200 OK**:
```json
[
  {
    "id": 1,
    "bemReferenciaId": 1,
    "descricaoBem": "Sedan Premium 2.0 Flex",
    "valorAnterior": 120000.00,
    "valorNovo": 125000.00,
    "origemReajuste": "FIPE",
    "codigoFipe": "004001-8",
    "dataAtualizacao": "2026-07-23T00:18:05"
  }
]
```

### 6. Endpoints FIPE
- **GET** `/fipe/marcas`
- **GET** `/fipe/marcas/{marcaId}/modelos`
- **GET** `/fipe/marcas/{marcaId}/modelos/{modeloId}/anos`
- **GET** `/fipe/consultar?marcaId={m}&modeloId={mod}&anoId={a}`
