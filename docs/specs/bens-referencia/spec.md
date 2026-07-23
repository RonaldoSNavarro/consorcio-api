# 📄 Especificação — Bens de Referência e Tabela FIPE

## 🎯 Objetivo
Prover a gestão completa do catálogo de Bens de Referência (Veículos Automotores, Imóveis, Serviços e Outros Bens Móveis), servindo de baseline imutável para a composição de créditos das cotas, parametrização de produtos e cálculo de reajustes por índices (FIPE, INCC, IPCA).

---

## 📋 Regras de Negócio

### RN-BEM-001: Categorias Regulamentadas pelo BACEN
O sistema deve manter 4 categorias de bens de referência pré-definidas conforme regulamentação do Banco Central do Brasil:
1. **Veículos Automotores** (`BEM_MOVEL_I` — Índice padrão: **FIPE**)
2. **Imóveis** (`BEM_IMOVEL` — Índice padrão: **INCC**)
3. **Serviços** (`SERVICO` — Índice padrão: **IPCA**)
4. **Outros Bens Móveis** (`BEM_MOVEL_II` — Índice padrão: **IPCA**)

### RN-BEM-002: Integração com Tabela FIPE
Para bens da categoria *Veículos Automotores*, o sistema deve oferecer integração via API REST pública da Parallelum (`parallelum.com.br/fipe/api/v1/`), permitindo:
- Consulta encadeada de **Marcas → Modelos → Anos**.
- Obtenção do **Valor Oficial FIPE** e do **Código FIPE** de referência.
- Preenchimento automático do formulário de cadastro/reajuste do bem.

### RN-BEM-003: Auditoria e Histórico de Preços
Toda alteração efetuada no `valorAtual` de um Bem de Referência deve gerar automaticamente um registro na tabela `historico_valores_bem_referencia`, persistindo:
- `valorAnterior`
- `valorNovo`
- `origemReajuste` (`FIPE`, `INCC`, `MANUAL`, `CADASTRO_INICIAL`)
- `codigoFipe` (quando aplicável)
- `dataAtualizacao` (timestamp)

### RN-BEM-004: Controle de Acesso e Segurança (RBAC)
- Leitura (`GET /api/bens-referencia`, `/categorias`, `/historico`, `/fipe/*`): Exige permissão `VIEW_GRUPOS` ou `MANAGE_GRUPOS`.
- Modificação (`POST`, `PUT`): Exige permissão `MANAGE_GRUPOS`.

---

## 🎨 Especificação de Interface (UI)
- O módulo **Bens de Referência** deve ser acessível via menu lateral na rota `/bens-referencia`, posicionado entre **Grupos Adm** e **Cotas**.
- A listagem deve dispor de busca por descrição/código FIPE, filtro por Categoria e colunas alinhadas com clareza.
- O formulário de edição/cadastro deve disponibilizar a aba interativa **"Consulta Tabela FIPE"**.
- A visualização de histórico deve ser apresentada em modal dedicado com badges indicativas de origem (`FIPE Official`, `INCC / IGPM`, `Manual`).
