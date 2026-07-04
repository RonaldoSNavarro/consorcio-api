# Referência: Financeiro — Parcelas, Reajustes e Índices

## 1. Composição da Parcela

Desde a **Resolução BCB 285** (vigente em 01/07/2024), a parcela inicial deve ser discriminada
em tabela no contrato, com valores nominais e percentuais de cada componente:

```
Parcela = Fundo Comum + Taxa de Administração + Fundo de Reserva [+ Seguro]
```

### 1.1 Fundo Comum (FC)

Principal componente; forma o caixa para contemplações.

```
FC_mensal = Valor_Credito_Atual / Número_Meses_Restantes_do_Grupo
```

- O valor do crédito é atualizado periodicamente (ver seção 2)
- Após reajuste, a parcela de FC é recalculada proporcionalmente
- Todos os consorciados (ativos e contemplados) pagam FC até o encerramento do grupo

### 1.2 Taxa de Administração (TA)

Remuneração da administradora pelo gerenciamento do grupo.

```
TA_total = percentual_ta × Valor_Credito_Original        (% fixo do crédito)
TA_mensal = TA_total / Número_Meses_do_Plano
```

**Regras (Resolução BCB 285):**
- Cobrada **proporcionalmente** ao longo dos meses do plano (percentual fixo)
- Incide sobre o valor do crédito **atualizado** (quando há reajuste, a base de cálculo da TA também sobe)
- Mercado: varia de **6,5% a 25%** do crédito total (depende da administradora, bem, prazo e valor)
- Em caso de exclusão: devolução pro-rata da TA que exceder o devido pelo tempo decorrido

**Exemplo:**
```
Carta de crédito: R$ 200.000
Taxa de administração: 18%
Prazo: 180 meses

TA_total = R$ 36.000
TA_mensal = R$ 200 (entra na parcela todo mês)
Parcela base = R$ 1.311 (≈ R$ 1.111 FC + R$ 200 TA)
```

### 1.3 Fundo de Reserva (FR)

Colchão de segurança para inadimplências e despesas imprevistas do grupo.

- Percentual definido em contrato (tipicamente 1% a 5% ao ano sobre o crédito)
- **Devolvido parcialmente ao final do grupo**, caso haja saldo positivo
- Devolução proporcional por participante (conforme percentual definido em contrato)
- Não há garantia de devolução integral (depende da saúde financeira do grupo)

### 1.4 Seguro Prestamista (opcional)

- Cobre obrigações em caso de morte ou invalidez do consorciado
- Valor varia conforme operadora e tipo de cobertura
- Cobrado mensalmente junto à parcela

---

## 2. Reajustes: Índices e Periodicidade

O reajuste atualiza tanto o **valor da carta de crédito** quanto as **parcelas mensais**,
preservando o poder de compra do consorciado ao longo do plano.

### 2.1 Índices por tipo de bem

| Tipo de Bem | Índice Principal | Alternativas |
|-------------|-----------------|-------------|
| Imóveis | **INCC** (Índice Nacional de Custo da Construção) | IGP-M, IPCA |
| Veículos (carros) | **Tabela FIPE** ou **IPCA** | IGP-M |
| Motocicletas | **Tabela FIPE** ou **IPCA** | — |
| Caminhões/Pesados | **Tabela do fabricante** ou IPCA | INCC |
| Serviços | **IPCA** | IGP-M, INPC |

> O índice deve estar previsto **expressamente no contrato**. A administradora não pode mudar
> o índice unilateralmente; requer deliberação em AGE.

### 2.2 Periodicidade padrão

- **Anual** (padrão): no mês de aniversário da cota ou do grupo
- Imóveis Porto Seguro: "no 14º mês a contar da data de constituição do grupo"
- Alguns contratos: semestral ou trimestral (menos comum)

### 2.3 Fórmula de reajuste

```
Valor_Atualizado = Valor_Base × (1 + índice_acumulado_período)

Exemplo IPCA:
  Carta = R$ 100.000
  IPCA acumulado = 5%
  Carta atualizada = R$ 105.000
  Parcelas sobem proporcionalmente

Exemplo INCC (imóvel):
  Carta = R$ 150.000  (janeiro 2023)
  INCC 7% ao ano
  Carta atualizada = R$ 160.500  (janeiro 2024)
```

### 2.4 Tipos de reajuste contratual

1. **Por índice econômico** (mais comum): INCC, IPCA, IGP-M — correção anual
2. **Por valor de referência do bem**: quando há parceria com montadora/fabricante;
   o reajuste ocorre sempre que o preço do bem sobe na tabela de fábrica
3. **Por valor nominal corrigido** (novidade Resolução BCB 285): crédito fixado em montante
   nominal, corrigido por índice de preço ou indicador definido em contrato

---

## 3. Saldo do Fundo Comum e Verificação de Recursos

### 3.1 Cálculo do saldo disponível para AGO

```python
def calcular_saldo_disponivel(grupo_id: str, data_ago: date) -> float:
    # Soma todas as contribuições ao FC recebidas até a AGO
    arrecadacao_fc = sum(pagamentos FC de todos os consorciados até data_ago)
    
    # Subtrai valores já distribuídos em AGOs anteriores
    distribuicoes_anteriores = sum(créditos distribuídos em AGOs anteriores)
    
    # Subtrai restituições a excluídos já realizadas
    restituicoes = sum(valores restituídos a excluídos contemplados)
    
    saldo_disponivel = arrecadacao_fc - distribuicoes_anteriores - restituicoes
    return saldo_disponivel
```

### 3.2 Condições para cada fase da AGO

```
Fase 1 (Sorteio Ativos):      saldo >= 1 × valor_credito
Fase 2 (Sorteio Excluídos):   saldo_restante >= valor_restituicao_excluido
Fase 3 (Lances):              saldo_restante >= 1 × valor_credito
```

### 3.3 Múltiplas contemplações

Se o saldo comportar mais de uma contemplação:
```
n_contemplacoes_sorteio = floor(saldo_disponivel / valor_credito_atual)
# Mas limitado pelo total de cotas elegíveis e pelo contrato do grupo
```

---

## 4. Taxa de Adesão

- Cobrada na entrada ou diluída nas primeiras parcelas
- Valor típico: até **1% do crédito**
- Cobre comissão do vendedor e custos de abertura de grupo
- Administradoras digitais geralmente não cobram
- Deve estar prevista em contrato e discriminada

---

## 5. Restituição ao Excluído

### Base de cálculo

```python
def calcular_restituicao_excluido(
    cota: Cota,
    valor_bem_atual: float,
    rendimentos_fundo: float
) -> float:
    # Percentual amortizado = quanto do plano foi cumprido
    percentual_amortizado = cota.total_parcelas_pagas / cota.prazo_total_meses
    
    # Valor da restituição do fundo comum
    valor_fc_restituir = percentual_amortizado * valor_bem_atual
    
    # Acrescido de rendimentos proporcionais
    restituicao_total = valor_fc_restituir + rendimentos_fundo
    
    # Deduzidas obrigações pendentes (parcelas em atraso + encargos)
    debitos = calcular_debitos_pendentes(cota)
    
    return max(0, restituicao_total - debitos)
```

### Prazo para restituição

1. Na contemplação por sorteio da cota excluída, OU
2. Em até **60 dias** da data da última AGO do grupo (se nunca for sorteada)

### Devolução da taxa de administração pro-rata (Resolução BCB 285)

Em caso de exclusão, a TA cobrada a título de antecipação além do devido deve ser
devolvida proporcionalmente ao tempo decorrido até a exclusão.

```
TA_devida = (meses_participados / prazo_total) × TA_total_contratada
TA_antecipada_cobrada = valor efetivamente cobrado de TA até a exclusão
Diferença a devolver = TA_antecipada_cobrada - TA_devida  (se positivo)
```

---

## 6. Antecipação de Parcelas

- O consorciado pode antecipar pagamentos
- **Não há desconto** por antecipação (o valor das parcelas é linear)
- Vantagem: parcelas antecipadas podem ser usadas como lance livre
- Parcelas antecipadas eliminam o risco de reajuste futuro naquelas parcelas
- Contemplado que quer quitar o restante multiplica linearmente as parcelas restantes

---

## 7. Custo Efetivo — Comparação com Financiamento

```
Custo total consórcio (sem lance) = Taxa de Administração Total
  Tipicamente: 6,5% a 25% do crédito total, diluídos no prazo

Custo financiamento (PRICE, por exemplo):
  Taxa de juros composta: geralmente 0,8% a 2,5% ao mês (9,6% a 34,5% ao ano)
  Custo total em 5 anos a 1,5%/mês: ≈ 143% do valor do bem em juros

O consórcio é mais barato no longo prazo; a desvantagem é a incerteza do momento
de contemplação.
```

---

## 8. Encerramento do Grupo

- Após a última AGO de contemplação, o grupo entra em fase de encerramento
- Prazo máximo de 60 dias para restituição de excluídos não contemplados
- Saldo positivo do fundo de reserva é devolvido proporcionalmente
- A administradora deve comunicar todos os consorciados sobre a última AGO

**Resolução BCB 285, Art. 36:** Comunicação obrigatória da última AGO a todos os ativos
e excluídos, com antecedência, incluindo dados de conta (Pix) para restituição.
