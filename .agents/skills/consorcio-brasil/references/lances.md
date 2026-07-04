# Referência: Lances — Tipos, Apuração e Desempate

## 1. Regra Fundamental (Lei)

> Art. 8º, Circular BACEN 3.432/2009 (replicado na Resolução BCB 285):
> **"A contemplação por lance somente pode ocorrer após a contemplação por sorteio
> ou se essa não for realizada por insuficiência de recursos."**

Isso significa que lances são **sempre processados depois** do sorteio de ativos e do sorteio
de excluídos, e só ocorrem se houver saldo disponível no fundo comum.

---

## 2. Tipos de Lance

### 2.1 Lance Livre
- Consorciado define livremente o valor ou percentual a ofertar
- Não há limite máximo (exceto o valor total da carta)
- **Vence o maior percentual** em relação ao valor do crédito do grupo
- Todos os lances são apresentados ao mesmo tempo (leilão fechado)
- Se vencedor não pagar no prazo → desclassificado → próximo da fila assume

**Cálculo do percentual:**
```
percentual_lance = (valor_absoluto_lance / valor_credito_grupo) × 100
```

**Importante:** Em grupos onde consorciados têm cartas de valores diferentes,
o critério é o **percentual**, não o valor absoluto, para garantir isonomia.

### 2.2 Lance Fixo
- A administradora define um percentual pré-estabelecido (ex: 30% da carta)
- Todos que oferecem exatamente esse percentual participam em igualdade
- Não faz sentido oferecer mais; não contempla quem oferecer menos
- **Desempate obrigatório** (ver seção 4)
- Elimina a concorrência de valores; mais simples para planejamento do consorciado

### 2.3 Lance Embutido
- O consorciado usa **parte da própria carta de crédito** como lance
- Não exige recursos financeiros externos no momento do lance
- **O crédito disponível é reduzido pelo valor do lance:**

```
credito_disponivel = valor_credito_total - valor_lance_embutido

Exemplo:
  Carta = R$ 300.000
  Lance embutido = 25% = R$ 75.000
  Crédito disponível ao contemplado = R$ 225.000
```

- O valor do lance embutido deve ser deduzido do crédito e contabilizado em conta específica
  (Art. 9º, Circular 3432 / Resolução BCB 285)
- Útil para quem quer antecipar contemplação sem reserva financeira

### 2.4 Lance FGTS
- Usa saldo da conta vinculada do FGTS
- Regido pelas normas do Conselho Curador do FGTS e da Caixa Econômica Federal
- Disponível apenas para consórcio de imóveis (moradia própria)
- Processamento pode ter prazo mais longo (verificação junto à CEF)
- O valor é tratado como lance livre em termos de apuração percentual

---

## 3. Apuração do Vencedor

### 3.1 Lance Livre — Regra geral

```python
def apurar_vencedor_lance_livre(
    lances: list[Lance],
    saldo_disponivel: float,
    valor_credito: float
) -> list[Lance]:
    
    # 1. Calcular percentual de cada lance (normalizado pelo crédito do grupo)
    for lance in lances:
        lance.percentual = (lance.valor_absoluto / valor_credito) * 100
    
    # 2. Ordenar por percentual decrescente
    lances.sort(key=lambda l: l.percentual, reverse=True)
    
    # 3. Determinar quantas contemplações cabem
    max_contemplacoes = int(saldo_disponivel // valor_credito)
    
    # 4. Selecionar vencedores (sem empate por enquanto)
    vencedores = []
    for i, lance in enumerate(lances):
        if i >= max_contemplacoes:
            break
        # Verificar empate com próximo (necessita desempate)
        if i > 0 and lance.percentual == lances[i-1].percentual:
            # Já está em zona de desempate, tratar separadamente
            break
        vencedores.append(lance)
    
    return vencedores
```

### 3.2 Verificação de recursos antes dos lances

```python
saldo_apos_sorteios = saldo_fundo_comum - (contemplados_sorteio * valor_credito)
pode_haver_lance = saldo_apos_sorteios >= valor_credito

if not pode_haver_lance:
    # Registrar na ata: "Sem contemplação por lance — saldo insuficiente"
    return
```

---

## 4. Desempate de Lances

Quando dois ou mais consorciados ofertam **o mesmo percentual**, aplicar os critérios
definidos em contrato, nesta ordem típica:

### Critério 1 — Proximidade à Cota Sorteada ⭐ (Santander e mais comuns)

O número da cota sorteada na fase de ativos serve de referência. Contempla a cota empatada
cujo número esteja **mais próximo** da cota sorteada.

Em caso de mesma distância → **ordem crescente do número de cota**.

```python
def desempatar_por_proximidade(
    empatados: list[Lance],
    cota_sorteada: int
) -> list[Lance]:
    def distancia(lance: Lance) -> tuple:
        dist = abs(lance.numero_cota - cota_sorteada)
        return (dist, lance.numero_cota)  # desempate final: número menor primeiro
    
    return sorted(empatados, key=distancia)

# Exemplo:
# Cota sorteada = 482
# Empatados com lances iguais: cotas 480, 484, 490
# Distâncias: |480-482|=2, |484-482|=2, |490-482|=8
# Empate entre 480 e 484 → crescente → cota 480 vence
```

**Tratar caso sem sorteio de ativos:**
- Se não houve sorteio (recursos insuficientes), usar a pedra-chave calculada como referência
- Ou usar o menor número de cota como referência padrão (definir em contrato)

### Critério 2 — Maior Número de Parcelas Pagas

Entre os empatados, vence quem tiver pago mais parcelas até a data da AGO.

```python
def desempatar_por_parcelas(empatados: list[Lance]) -> list[Lance]:
    return sorted(empatados, key=lambda l: l.cota.total_parcelas_pagas, reverse=True)
```

### Critério 3 — Sorteio entre Empatados (mais usado no Lance Fixo)

Novo mini-sorteio usando a Loteria Federal, mas apenas entre os empatados.

```python
def desempatar_por_sorteio(empatados: list[Lance], primeiro_premio: int) -> Lance:
    # Numerar os empatados de 1 a N em ordem crescente de número de cota
    empatados_ordenados = sorted(empatados, key=lambda l: l.numero_cota)
    n = len(empatados_ordenados)
    
    # Calcular pedra-chave dentre os empatados
    pedra = pedra_chave_divisao(primeiro_premio, n)
    pedra = max(1, min(pedra, n))  # garantir dentro do range
    
    return empatados_ordenados[pedra - 1]  # índice base-1
```

### Critério 4 — Lance Adicional

Solicitar que os empatados ofertem um valor adicional; vence o maior.

---

## 5. Fluxo Completo de Apuração de Lance

```
1. Receber todos os lances para a AGO com status PENDENTE
2. Verificar saldo disponível após sorteios
3. Para cada lance: calcular percentual normalizado
4. Ordenar: maior percentual primeiro
5. Identificar quantas contemplações comporta o saldo
6. Identificar zona de empate no corte (se houver)
7. Aplicar critério de desempate (configurado em contrato)
8. Marcar vencedores como VENCEDOR (contemplação CONDICIONAL)
9. Marcar perdedores como PERDEDOR
10. Notificar vencedores do prazo de pagamento
11. Job de monitoramento:
    - Se pago no prazo → confirmar contemplação
    - Se não pago → desclassificar → promover próximo da fila
```

---

## 6. Confirmação e Homologação da Contemplação por Lance

- A contemplação por lance é **CONDICIONAL** até o pagamento efetivo
- Prazo típico: **24h a 72h** após a AGO (definido em contrato)
- Se o vencedor não pagar:
  1. Status → `CANCELADO`
  2. Próximo da fila (2º maior percentual) assume
  3. Se também não pagar → próximo, e assim por diante
  4. Se nenhum pagar → SEM contemplação por lance nessa AGO

**Base legal:** Art. 69 do Regulamento Santander (referência de mercado):
> "A Contemplação por Lance apenas será homologada após o efetivo recebimento pela
> Administradora do valor correspondente ao Lance."

---

## 7. Lance Embutido — Contabilidade Específica

Conforme Art. 9º, Circular 3432 / Resolução BCB 285:

O valor do lance embutido deve:
1. Ser **deduzido integralmente** do crédito a ser distribuído
2. Destinar-se ao **abatimento de prestações vincendas** (fundo comum + encargos)
3. Ser **contabilizado em conta específica** (separado do fundo comum)

```
Fluxo contábil:
  Crédito a distribuir: R$ 300.000
  Lance embutido vencedor: R$ 75.000 (25%)
  
  → Crédito disponível ao contemplado: R$ 225.000
  → R$ 75.000 entra em conta específica
  → Abate parcelas vincendas do contemplado (FC + encargos)
```

---

## 8. Regras de Isonomia (obrigatório)

- Todo consorciado no mesmo grupo concorre em igualdade de condições
- Consorciado que entrou no grupo em andamento **usa o saldo devedor do grupo**
  como base de cálculo, NÃO o saldo devedor da sua própria cota
- Todas as regras de lance e desempate devem estar expressas no contrato (Art. 5º, X, Circular 3432)
- A administradora nunca pode favorecer um consorciado específico

---

## 9. Caso: Lance e Seguro de Vida (Resolução BCB 285)

Novidade da Resolução BCB 285: em caso de **falecimento do consorciado titular** de cota
não contemplada com seguro vinculado:

> "O saldo devedor quitado pela seguradora será considerado como lance vencedor,
> desde que ocorra na primeira assembleia geral ordinária subsequente e com recursos
> suficientes para contemplação, se o montante da indenização for igual ou superior
> ao saldo devedor da cota."

Implementação: gerar um lance automático com `tipo = LANCE_SEGURO_OBITO` e processar
normalmente na fase de lances.
