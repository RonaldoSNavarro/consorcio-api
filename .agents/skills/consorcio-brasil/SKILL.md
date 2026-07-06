---
name: consorcio-brasil
description: >
  Conhecimento completo e atualizado sobre o Sistema de Consórcios brasileiro regulamentado pelo
  Banco Central do Brasil. Use esta skill sempre que o usuário perguntar sobre ou precisar
  implementar qualquer aspecto de consórcio: regras de sorteio/contemplação por Loteria Federal,
  algoritmos de pedra-chave, tipos e apuração de lances, desempate, sorteio de excluídos,
  composição de parcelas, reajustes (INCC/IPCA/FIPE), fundo comum, taxa de administração, fundo de
  reserva, inadimplência, exclusão, restituição, assembleias (AGO/AGE), transferência de cotas,
  regras do BACEN (Circular 3432/2009 e Resolução BCB 285/2023), modelagem de dados,
  desenvolvimento de APIs ou qualquer outro tema relacionado a consórcios no Brasil.
  Deve ser usada SEMPRE que o usuário mencionar palavras como "consórcio", "contemplação",
  "assembleia", "cota", "carta de crédito", "fundo comum", "lance", "sorteio", "BACEN consórcio",
  "administradora de consórcio" ou qualquer variante, mesmo que a pergunta pareça simples.
---

# Skill: Sistema de Consórcios Brasileiro

Guia técnico e regulatório completo para desenvolvimento de sistemas de gestão de consórcios
no Brasil. Atualizado com a **Resolução BCB nº 285/2023** (vigente desde 01/07/2024).

---

## Quando Usar Esta Skill

Sempre que o trabalho envolver:
- Modelagem de dados de grupos, cotas, assembleias, lances, contemplações
- Implementação de algoritmos de sorteio (Loteria Federal)
- Regras de lance (livre, fixo, embutido, FGTS) e desempate
- Cálculo de parcelas, reajustes, fundo comum, restituição de excluídos
- Compliance com regulamentação do BACEN
- Dúvidas sobre qualquer aspecto operacional ou legal de consórcios

---

## Estrutura desta Skill

Leia o arquivo principal abaixo para contexto geral. Para implementações específicas, carregue o
arquivo de referência correspondente:

| Tópico | Arquivo |
|--------|---------|
| Legislação e regulamentação | `references/legislacao.md` |
| Sorteio e contemplação (algoritmos) | `references/sorteio_contemplacao.md` |
| Lances: tipos, apuração e desempate | `references/lances.md` |
| Financeiro: parcelas, reajustes, índices | `references/financeiro.md` |
| Grupos e cotas: ciclo de vida e status | `references/grupos_cotas.md` |

---

## Visão Geral do Sistema de Consórcios

### O que é

Consórcio é uma modalidade de crédito coletivo tipicamente brasileira, criada nos anos 1960.
Pessoas físicas ou jurídicas se unem em grupos para formar um fundo comum, usado para contemplar
cada membro com uma **carta de crédito** (sem juros) para adquirir bens ou serviços.

- **Regulador e fiscalizador:** Banco Central do Brasil (BACEN)
- **Associação do setor:** ABAC (Associação Brasileira de Administradoras de Consórcios)
- **Mercado atual:** >12 milhões de participantes ativos (2026)

### Diferença fundamental de um financiamento

| Aspecto | Consórcio | Financiamento |
|---------|-----------|---------------|
| Juros | Não há | Sim (CET) |
| Custo | Taxa de administração (% do crédito) | Juros mensais |
| Certeza de quando receber | Não (depende de sorteio/lance) | Imediato |
| Adequado para | Planejamento de médio/longo prazo | Necessidade imediata |

### Participantes do sistema

- **Administradora:** Empresa autorizada pelo BACEN que organiza e opera os grupos
- **Consorciado ativo:** Participante com parcelas em dia; concorre a sorteios e lances
- **Consorciado excluído:** Foi removido do grupo por inadimplência (≥3 parcelas consecutivas);
  ainda concorre a sorteios para fins de restituição
- **Consorciado contemplado:** Já recebeu a carta de crédito; continua pagando parcelas

---

## Componentes da Parcela Mensal

Toda parcela é composta por até 4 componentes (discriminados em tabela no contrato, conforme
Resolução BCB 285):

```
Parcela = Fundo Comum + Taxa de Administração + Fundo de Reserva [+ Seguro]
```

| Componente | Descrição | Obrigatoriedade |
|------------|-----------|-----------------|
| **Fundo Comum** | Principal arrecadação; financia as contemplações | Obrigatório |
| **Taxa de Administração** | Remuneração da administradora; % fixo do crédito | Obrigatório |
| **Fundo de Reserva** | Colchão para inadimplência e despesas extras; devolvido parcialmente ao final | Obrigatório se previsto |
| **Seguro** | Seguro prestamista opcional (vida, desemprego etc.) | Opcional |

### Cálculo da parcela base (fundo comum)

```
Parcela_FC = Valor_Credito_Atual / Numero_Meses_Restantes
```

O valor do crédito é reajustado periodicamente (ver `references/financeiro.md`).

---

## Fluxo Macro de uma AGO (Assembleia Geral Ordinária)

A ordem é **legalmente obrigatória** (Art. 8º, Circular 3432 / Resolução BCB 285):

```
FASE 0 — Verificações prévias
  ├── Saldo do fundo comum ≥ valor do crédito?
  ├── Extração da Loteria Federal do período disponível?
  └── Lances encerrados (não aceitar lances durante/após sorteio)

FASE 1 — Sorteio de Cotas Ativas
  └── Calcula pedra-chave → busca cota apta → fallback se necessário

FASE 2 — Sorteio de Cotas Excluídas (Restituição)
  └── Usa 2º prêmio ou milhar do 1º prêmio → restituição ao excluído contemplado

FASE 3 — Contemplação por Lance
  └── Ordena lances → determina vencedor(es) → contemplação condicional ao pagamento

FASE 4 — Registro em Ata
  └── Documenta tudo: extração usada, pedra-chave, cotas contempladas, lances apurados
```

> **Regra crítica:** lance só ocorre APÓS o sorteio. Se não houver recursos para sorteio,
> também não há lance (salvo exceção contratual explícita).

---

## Status de uma Cota (ciclo de vida)

```
NÃO_COMERCIALIZADA → ATIVA → CONTEMPLADA
                          ↓         ↓
                      SUSPENSA  (continua pagando)
                          ↓
                      EXCLUIDA → (sorteio de excluídos para restituição)
```

- **ATIVA:** Adimplente; participa de sorteios e pode ofertar lances
- **SUSPENSA:** Inadimplente mas ainda não atingiu 3 parcelas; NÃO participa de sorteios de ativos
- **EXCLUIDA:** ≥3 parcelas consecutivas inadimplentes; participa de sorteio de excluídos para restituição
- **CONTEMPLADA:** Recebeu a carta de crédito; continua obrigada a pagar parcelas
- **NÃO_COMERCIALIZADA:** Vaga ainda não vendida; não participa de nenhum sorteio

---

## Tipos de Contemplação

1. **Sorteio Regular** — Todos os ativos têm chance igual; usa Loteria Federal
2. **Lance Livre** — Consorciado oferta o valor que quiser; maior % do crédito vence
3. **Lance Fixo** — Administradora define % fixo; empate → sorteio entre empatados
4. **Lance Embutido** — Usa parte da própria carta de crédito como lance; reduz crédito disponível
5. **Lance FGTS** — Usa saldo da conta vinculada do FGTS (regras da CEF/Conselho Curador)
6. **Sorteio de Excluídos** — Restituição de valores pagos ao fundo comum

---

## Para Implementação: Carregue os Arquivos de Referência

- **Algoritmos de sorteio detalhados** → `references/sorteio_contemplacao.md`
- **Regras de lance e desempate** → `references/lances.md`
- **Reajustes e cálculos financeiros** → `references/financeiro.md`
- **Ciclo de vida de grupos e cotas** → `references/grupos_cotas.md`
- **Base legal (Circular 3432, Resolução 285, Lei 11795)** → `references/legislacao.md`
