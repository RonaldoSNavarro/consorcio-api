# Referência: Sorteio e Contemplação por Loteria Federal

## 1. A Loteria Federal como Referência

A Loteria Federal é usada pelas administradoras por ser:
- Pública, auditável e não manipulável pela administradora
- Divulgada 2×/semana (quartas e sábados), permitindo assembleias em qualquer dia da semana
- Números entre **00001 e 99999** (5 dígitos, sempre com zero à esquerda)

### Vinculação da extração ao dia da assembleia

| Dia da Assembleia (AGO) | Extração Utilizada |
|-------------------------|--------------------|
| Segunda, Terça ou Quarta | Sábado imediatamente anterior |
| Quinta ou Sexta | Quarta imediatamente anterior |
| Sábado | Próprio sábado (se resultado já disponível) |

> A administradora deve validar que o resultado da extração já foi publicado antes de executar o sorteio.

---

## 2. Três Algoritmos de Pedra-Chave

A "pedra-chave" é o número calculado a partir do resultado da Loteria que determina qual cota
será contemplada. O algoritmo é definido em contrato. Há três variantes principais:

---

### ALGORITMO A — Três Últimos Dígitos (Centena) ⭐ Mais comum

**Regra:** Os 3 últimos dígitos do 1º prêmio = pedra-chave.

```
1º prêmio = 75.457  →  pedra-chave = 457  →  busca cota 457
1º prêmio = 20.032  →  pedra-chave = 032  →  busca cota 32
1º prêmio = 41.000  →  pedra-chave = 000  →  tratar como última cota do grupo
```

**Adaptação por tamanho do grupo:**
- Grupo ≤ 99 cotas: usar **2 últimos dígitos** (dezena)
- Grupo 100–999 cotas: usar **3 últimos dígitos** (centena) ← padrão
- Grupo ≥ 1000 cotas: usar **4 últimos dígitos** (milhar)

**Pseudocódigo:**
```python
def pedra_chave_tres_digitos(primeiro_premio: int, total_cotas: int) -> int:
    if total_cotas <= 99:
        pedra = primeiro_premio % 100
    elif total_cotas <= 999:
        pedra = primeiro_premio % 1000
    else:
        pedra = primeiro_premio % 10000
    
    # Tratar zero: cota 000 geralmente = última cota ativa
    if pedra == 0:
        pedra = total_cotas  # ou conforme definido em contrato
    return pedra
```

---

### ALGORITMO B — Divisão por Total (Cálculo Proporcional)

**Regra:** Divide o 1º prêmio pelo total de cotas ativas; usa a fração decimal.

```
Fórmula:
  resultado = primeiro_premio / total_cotas_ativas
  parte_decimal = resultado - floor(resultado)
  pedra_raw = parte_decimal * total_cotas_ativas
  pedra_final = round(pedra_raw)  # ≥0.5 arredonda para CIMA

Exemplo:
  1º prêmio = 20.282   total = 600
  20282 / 600 = 33.80333...
  parte_decimal = 0.80333
  0.80333 × 600 = 481.998  →  pedra = 482  (pois 0.998 ≥ 0.5)

Caso especial: resultado == 0 → usar 1 como pedra
```

**Pseudocódigo:**
```python
import math

def pedra_chave_divisao(primeiro_premio: int, total_cotas_ativas: int) -> int:
    resultado = primeiro_premio / total_cotas_ativas
    parte_inteira = math.floor(resultado)
    parte_decimal = resultado - parte_inteira
    pedra_raw = parte_decimal * total_cotas_ativas
    
    # Arredondamento: ≥0.5 sobe, <0.5 desce
    fracao = pedra_raw - math.floor(pedra_raw)
    pedra = math.ceil(pedra_raw) if fracao >= 0.5 else math.floor(pedra_raw)
    
    if pedra == 0:
        pedra = 1  # caso especial: incrementar 1 e recalcular se necessário
    return pedra
```

> Esta pedra-chave é **compartilhada** entre todos os subgrupos com o mesmo número de participantes.

---

### ALGORITMO C — Divisão por 1000 (Centena do Prêmio)

**Regra:** Extrai a centena do 1º prêmio e divide por 1000 para obter o fator de escala.

```
Fórmula (Newcon Software):
  centena = floor(primeiro_premio / 100) % 10   ← dígito das centenas
  fator = centena / 1000
  pedra_raw = fator * numero_maior_cota_ativa
  pedra = round(pedra_raw)  # regra ≥0.5

Exemplo:
  1º prêmio = 75.457  →  centena = 4
  fator = 4/1000 = 0.004
  maior cota = 600
  0.004 × 600 = 2.4  →  pedra = 2
```

> Usado em sistemas ERP de consórcio como o Newcon. Definir em contrato.

---

## 3. Fallback: Cota Não Apta

Quando a cota da pedra-chave **não está apta** (excluída, suspensa, não comercializada ou já
contemplada), buscar a próxima cota apta na direção configurada em contrato:

### Direções disponíveis

| Configuração | Comportamento |
|---|---|
| `ACIMA_DEPOIS_ABAIXO` | → vai subindo; se chegar ao fim, volta do início descendo |
| `ABAIXO_DEPOIS_ACIMA` | → vai descendo; se chegar ao início, volta do fim subindo |
| `SO_ACIMA` | → só sobe, sem circular |
| `SO_ABAIXO` | → só desce, sem circular |

**Exemplo (ACIMA_DEPOIS_ABAIXO, pedra = 482, grupo de 600 cotas):**
```
Tenta 482 → não apta → tenta 483 → não apta → tenta 484 → APTA → contempla 484
```

**Pseudocódigo:**
```python
def buscar_cota_apta(pedra: int, cotas_elegiveis: list[int], direcao: str) -> int | None:
    n = len(cotas_elegiveis)
    cotas_set = set(cotas_elegiveis)
    
    if direcao == "ACIMA_DEPOIS_ABAIXO":
        # Tenta de pedra até max, depois de min até pedra-1
        candidatas = list(range(pedra, max(cotas_elegiveis) + 1)) + \
                     list(range(min(cotas_elegiveis), pedra))
    elif direcao == "ABAIXO_DEPOIS_ACIMA":
        candidatas = list(range(pedra, min(cotas_elegiveis) - 1, -1)) + \
                     list(range(max(cotas_elegiveis), pedra, -1))
    
    for c in candidatas:
        if c in cotas_set:
            return c
    return None  # sem contemplação nesta AGO
```

### Fallback de extração

Se configurado (`usar_premio_anterior_se_nao_contemple = True`):
1. Tenta com prêmios da extração atual (1º, 2º, 3º...)
2. Se nenhum resultar em cota apta → tenta a extração anterior
3. Se ainda assim não encontrar → **SEM CONTEMPLAÇÃO** nesta AGO

---

## 4. Sorteio de Cotas Excluídas (Restituição)

**Base legal:** Art. 30, Lei 11.795/2008; Art. 26, Circular 3432/2009.

### Quando ocorre
- APÓS o sorteio de ativos (Fase 2 da AGO)
- Apenas se houver saldo remanescente no fundo comum após contemplação dos ativos
- Participam: cotas com status `EXCLUIDA` que nunca foram contempladas

### Prêmio da Loteria para excluídos
- Administradoras usam tipicamente o **2º prêmio** ou o **milhar do 1º prêmio** (definir em contrato)
- Santander usa: centena ou milhar do 1º prêmio aplicado à tabela de excluídos

### Regras de NÃO-contemplação de excluídos
```
1. O número sorteado corresponde a uma cota excluída JÁ CONTEMPLADA → SEM SORTEIO
2. O número sorteado ultrapassa o total de cotas excluídas → SEM SORTEIO
3. Saldo insuficiente após fase 1 → SEM SORTEIO de excluídos
```

### Desempate entre excluídos com mesmo radical
Cotas do tipo `001.1`, `001.2`, `001.3`:
- **Prioridade para o MENOR sufixo:** `001.1` > `001.2` > `001.3`

### O que o excluído contemplado recebe (não é a carta de crédito)
```
Restituição = percentual_amortizado × valor_bem_atual + rendimentos_aplicação_fundo
```
Onde `percentual_amortizado` = (meses pagos / prazo total).

### Prazo para restituição
- Na contemplação por sorteio, OU
- Em até **60 dias** após a última AGO do grupo (art. 26, Circular 3432)

---

## 5. Elegibilidade — Resumo

| Status da Cota | Sorteio de Ativos | Sorteio de Excluídos | Lance |
|---|---|---|---|
| ATIVA (adimplente) | ✅ | ❌ | ✅ |
| SUSPENSA (inadimplente < 3 parc.) | ❌ | ❌ | ❌ |
| EXCLUIDA | ❌ | ✅ (para restituição) | ❌ |
| CONTEMPLADA | ❌ | ❌ | ❌ |
| NÃO_COMERCIALIZADA | ❌ | ❌ | ❌ |

---

## 6. Casos Especiais

### Nenhuma cota apta encontrada
- Registrar na ata: "Não houve contemplação por sorteio nesta AGO por ausência de cota apta"
- Recursos passam para a próxima AGO

### Múltiplos sorteios na mesma AGO
- Grupos maiores podem ter 2+ contemplações por sorteio por AGO
- Usar prêmios subsequentes da mesma extração (2º prêmio para 2ª contemplação, etc.)
- Ou extrações de datas diferentes, conforme contrato

### Grupo com múltiplos subgrupos
- A pedra-chave calculada vale para TODOS os subgrupos com o mesmo número de participantes
- Cada subgrupo tem seu próprio pool de cotas elegíveis

### Cotas com numeração não sequencial
- Ao usar o algoritmo de divisão, `total_cotas_ativas` = contagem real de cotas ativas
- A pedra-chave é um índice posicional: buscar a N-ésima cota apta na lista ordenada
- Ou usar o número de cota diretamente, conforme o contrato definir

---

## 7. Registro de Auditoria (obrigatório na ata)

A ata da AGO deve conter:
```
- Número e data da assembleia
- Número do concurso da Loteria Federal utilizado
- Data da extração usada
- Resultado do 1º prêmio (e demais usados)
- Algoritmo aplicado (A, B ou C) e parâmetros
- Pedra-chave calculada
- Cota sorteada (primária)
- Fallbacks aplicados (se houver) e motivo
- Cotas contempladas por sorteio de excluídos
- Saldo do fundo comum antes e depois
- Motivo de não contemplação (se aplicável)
```
