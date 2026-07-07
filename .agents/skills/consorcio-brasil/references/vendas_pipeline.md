# Funil de Vendas e Comercial

Este documento detalha o processo comercial de venda de cotas, desde a prospecção até a alocação do consorciado em um grupo de consórcio.

---

## 1. O Funil de Vendas (Pipeline)

A jornada do cliente antes de se tornar um consorciado ativo segue um pipeline rigoroso para garantir compliance e viabilidade financeira.

```
Produto Consórcio → Simulação → Proposta de Adesão → Contrato de Adesão → Pagamento 1ª Parcela → Cota Ativa
```

### Entidades do Catálogo
- **CategoriaBem**: Tipo primário (IMOVEL, VEICULO_AUTOMOTOR, OUTROS_BENS_MOVEIS, SERVICO). Mapeia para os tipos do BACEN.
- **BemReferencia**: O bem específico sendo referenciado (ex: "Gol 1.0", "Carta de Imóvel 200k").
- **ProdutoConsorcio**: O plano comercial empacotado, definindo prazo (ex: 120 meses), taxa de administração, fundo de reserva, tipos de lances permitidos e seguros obrigatórios.

---

## 2. Proposta de Adesão

A Proposta de Adesão é o documento formal (físico ou digital) onde o interessado manifesta a intenção de entrar no grupo.

### Status da Proposta (`StatusProposta`)
1. `EM_ANALISE`: Cliente submeteu a proposta. Passa por validações KYC/AML e birô de crédito.
2. `APROVADA`: Proposta passou nas análises de compliance e crédito.
3. `RECUSADA`: Proposta bloqueada por compliance (ex: CPF em lista restritiva) ou restrição de crédito (conforme regra RN-VND-001).
4. `CANCELADA`: Cliente desistiu ou prazo expirou.

### Validações de Compliance (KYC/AML)
Toda proposta passa pelo subsistema de PLD/FT (Prevenção à Lavagem de Dinheiro):
- Validação de CPF/CNPJ.
- Verificação de PEP (Pessoa Exposta Politicamente).
- Consulta a listas restritivas (OFAC, ONU).
- *Se houver match (score Jaro-Winkler ≥ 0.90), a proposta é suspensa para análise manual.*

---

## 3. Contrato de Adesão

Após a aprovação da proposta, é gerado o Contrato de Adesão.

### Status do Contrato (`StatusContrato`)
1. `PENDENTE_PAGAMENTO`: Contrato assinado, aguardando compensação da primeira parcela (adesão).
2. `EFETIVADO`: Pagamento confirmado.
3. `CANCELADO`: Não pagamento no prazo legal (geralmente 7 dias - CDC) ou desistência.
4. `TRANSFERIDO`: Contrato originou uma cota que posteriormente foi cedida a terceiros.

### A Primeira Parcela (Adesão)
- **Regra RN-VND-003**: A primeira parcela cobrada no momento da adesão é composta de **100% Antecipação de Taxa de Administração + Fundo de Reserva**.
- Não há cobrança de Fundo Comum na parcela inaugural (ou é mínima), pois o fundo só será formado de fato após a constituição do grupo.

---

## 4. Alocação Inteligente de Cota

Quando o contrato é efetivado, o sistema executa a **Alocação Inteligente**:
1. Busca um grupo `EM_FORMACAO` ou `ATIVO` que tenha a mesma `CategoriaBem` e `ProdutoConsorcio`.
2. Verifica se o grupo tem vagas disponíveis (não atingiu limite máximo).
3. Se não houver grupo compatível com vagas, o sistema auto-instancia um novo grupo `EM_FORMACAO`.
4. Uma `Cota` é alocada ao cliente, inicialmente com status `AGUARDANDO_INAUGURACAO` (se grupo em formação) ou `ATIVA` (se grupo já ativo).

### Regra dos 10% (Limite de Concentração)
- O BACEN proíbe que um único cliente detenha mais de 10% das cotas ativas de um mesmo grupo. A alocação deve validar isso por CPF/CNPJ.

---

## 5. Canais de Venda e Comissionamento

### Canais (`TipoVendaEnum`)
- `VENDA_DIRETA`: Vendedor interno da administradora.
- `CORRESPONDENTE_BANCARIO`: Parceiro credenciado.
- `DIGITAL_SELF_SERVICE`: Venda online no e-commerce/app.
- `PARCERIA_COMERCIAL`: Corretores e concessionárias.

### Comissionamento
- Calculado sobre o `valorCredito × percentualComissao`.
- O gatilho de pagamento da comissão ao corretor/parceiro ocorre na **Efetivação do Contrato** (pagamento da 1ª parcela) ou, dependendo da política, de forma diluída ao longo das parcelas.
