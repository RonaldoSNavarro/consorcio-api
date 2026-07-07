# Referência: Grupos e Cotas — Ciclo de Vida e Regras

## 1. Formação de um Grupo

### Requisitos para a 1ª AGO (Resolução BCB 285)

1. Quantidade mínima de consorciados definida pelo regulamento
2. Perspectiva de contemplação de **todos os participantes** no prazo do plano
3. Contrato padrão aprovado e disponível no site da administradora (sem necessidade de cartório)
4. Cada participante com sua capacidade de pagamento avaliada

### Elementos obrigatórios no contrato (Art. 5º, Circular 3432 / Res. BCB 285)

- Valor do crédito (carta) e índice de reajuste
- Prazo do grupo (em meses)
- Número máximo de cotas
- Taxa de administração (% fixo)
- Fundo de reserva (se previsto) e percentual de devolução
- Periodicidade das assembleias
- Critérios e método de sorteio
- Regras de lance (livre, fixo, embutido, FGTS) e critério de desempate
- Condições para participar de sorteios e lances
- Regras de exclusão e restituição
- Discriminação em tabela dos componentes da parcela inicial

---

## 2. Status da Cota (`StatusCota` - 14 Estados)

O ciclo de vida da cota é complexo e mapeado em 14 estados no sistema:

- **AGUARDANDO_PAGAMENTO:** Cota criada via contrato de adesão, aguarda pagamento da 1ª parcela.
- **ATIVA:** Adimplente e apta a sorteios.
- **INADIMPLENTE:** Flag/status secundário indicando atraso leve.
- **SUSPENSA:** 1 a 2 parcelas atrasadas. Bloqueada para sorteios.
- **EXCLUIDA:** ≥ 3 parcelas atrasadas (não contemplada). Participa de sorteio de excluídos.
- **CANCELADA:** Cota encerrada antes da constituição do grupo ou cancelada por infração. Diferente de EXCLUÍDA (que tem direito a restituição legal).
- **CONTEMPLADA:** Sorteada, mas ainda no ciclo de avaliação de crédito/uso.
- **PENDENTE_INTEGRALIZACAO:** Venceu lance livre, aguarda pagamento (ADR 004).
- **AGUARDANDO_ANALISE:** Cota contemplada, aguardando aprovação de garantias/renda.
- **APROVADO:** Análise de crédito aprovada, crédito liberado.
- **EM_EXECUCAO:** Cota contemplada inadimplente (≥ 3 atrasos). Busca e apreensão do bem.
- **JURIDICO:** Processo judicial ativo (revisão de juros, reintegração).
- **QUITADA:** Todas as parcelas e obrigações pagas 100%. Carta nula de alienação fiduciária.
- **NAO_COMERCIALIZADA:** Vaga disponível no pool do grupo.
- **AGUARDANDO_INAUGURACAO:** Cota vendida em um grupo `EM_FORMACAO`, aguardando a 1ª AGO.

### ATIVA (Detalhes)
- Parcelas em dia (adimplente)
- Participa de sorteios de ativos e pode ofertar lances
- Condição: 0 vencimentos em atraso

### SUSPENSA (Inadimplente não excluída)
- 1 ou 2 vencimentos consecutivos em atraso
- **Não** participa de sorteios de ativos
- **Não** pode ofertar lances
- Pode regularizar e voltar ao status ATIVA
- Na data da **última AGO**: se tiver até 2 vencimentos em atraso → passa para EXCLUIDA

### EXCLUIDA
Ocorre em 3 situações (Resolução BCB 285, Art. 32):

1. **Manifestação de desistência** inequívoca e comprovável
2. **≥ 3 vencimentos consecutivos** inadimplentes durante o grupo
3. **≤ 2 vencimentos** inadimplentes na data da **última** AGO do grupo

Consequências:
- Não recebe a carta de crédito normalmente
- Concorre ao **sorteio de excluídos** para fins de restituição
- Recebe restituição proporcional baseada no **percentual amortizado sobre o valor do bem atualizado** (ADR 005 - Art. 30, Lei 11.795/08), descontada a multa rescisória (cláusula penal).
- Pode ser readmitida pelo Art. 31-A (ver seção 6)

### CONTEMPLADA
- Já recebeu (ou tem direito a) a carta de crédito
- Continua obrigada a pagar parcelas até o fim do plano
- Se ficar inadimplente após contemplação:
  - Se não usou o crédito: AGO pode deliberar pelo cancelamento (Art. 10, Circular 3432)
  - Se já usou o crédito: medidas judiciais e extrajudiciais

### NÃO_COMERCIALIZADA
- Vaga do grupo ainda não vendida
- Não participa de nenhum sorteio ou lance
- A administradora pode vendê-la como "cota de reposição"

---

## 3. Limite de Cotas por Consorciado (Resolução BCB 285)

**Regra atual (dinâmica):**
> Um único consorciado e seu cônjuge/companheiro não podem deter mais de **10%** das cotas
> vendidas **até a data da compra**.

**Cálculo:**
```python
def limite_cotas_consorciado(cotas_vendidas_ate_agora: int) -> int:
    return max(1, int(cotas_vendidas_ate_agora * 0.10))

# Exemplo: grupo com 1.000 cotas, já vendidas 400
# Limite = 400 × 10% = 40 cotas para o consorciado + cônjuge
```

**Diferença vs. regra anterior (Circular 3432):**
- Antes: 10% do **total de cotas ativas** (estático)
- Agora: 10% das **cotas vendidas até a data** (dinâmico, mais restritivo no início)

**Objetivo:** Evitar risco de inadimplência concentrada e controle indevido do grupo.

---

## 4. Assembleias

### Assembleia Geral Ordinária (AGO)

- Periodicidade: **mensal** (ou conforme contrato)
- Data fixada no contrato; deve ser consistente
- Pode ser presencial, virtual ou híbrida (Resolução BCB 285, Art. 44)
- Instalada com **qualquer número de consorciados presentes**
- Votações: maioria simples dos presentes; apenas **adimplentes** têm direito a voto
- O que acontece: sorteio de ativos → sorteio de excluídos → lances → deliberações

### Assembleia Geral Extraordinária (AGE)

- Convocada quando necessário (sem data fixa)
- Prazo máximo de convocação: **5 dias úteis** após o evento que a motivou
- Temas típicos:
  - Substituição da administradora
  - Fusão de grupos
  - Dilação do prazo do grupo
  - Substituição do bem (ex: descontinuação de fabricação)
  - Outros assuntos não resolvidos na AGO

### Ata das Assembleias

- Obrigatória para cada AGO e AGE
- Deve conter: contemplados, dados da extração, resultado de lances, deliberações
- Disponibilizada a todos os consorciados (Circular 3432, Art. 11)
- Mantida por **mínimo de 5 anos** após encerramento do grupo (Resolução BCB 285)
- A falta de ata no prazo legal: consorciado pode solicitar por escrito

---

## 5. Transferência de Cota

- Permitida conforme condições do contrato
- O novo titular assume todos os direitos e obrigações da cota
- Avaliação de capacidade de pagamento do novo titular obrigatória
- Documentação deve ser arquivada por 5 anos (Resolução BCB 285)
- Em caso de falecimento do titular: herdeiros podem assumir a cota

---

## 6. Exclusão e Readmissão

### Processo de Exclusão

```
1. Consorciado atinge ≥ 3 parcelas consecutivas em atraso
2. Administradora notifica (tentativas de cobrança obrigatórias)
3. Cota passa para EXCLUIDA
4. Consorciado perde direito à carta de crédito normal
5. Entra na fila de sorteio de excluídos para restituição
```

### Readmissão (Art. 31-A, Circular 3432 / Resolução BCB 285)

A administradora pode readmitir o excluído não contemplado, se:
1. Manifestação expressa e inequívoca do interessado
2. Quitação das parcelas em atraso (negociação com a administradora)
3. Verificação de capacidade de pagamento
4. Disponibilidade de cota no grupo (ou reincorporação da mesma cota)

---

## 7. Contemplação e Uso do Crédito

Após a contemplação (por sorteio ou lance), o consorciado pode usar a carta para:

1. **Adquirir o bem ou serviço** previsto em contrato (na rede de fornecedores de sua escolha)
2. **Quitar financiamento** da mesma categoria (imóvel financia imóvel, veículo financia veículo)
3. **Receber o crédito em espécie** — apenas após **180 dias** da contemplação,
   se todas as obrigações estiverem quitadas (Art. 5º, c, Circular 3432)

**Disponibilização do crédito:**
- Administradora deve colocar o crédito à disposição até o **3º dia útil** após a contemplação
  (Art. 11, Circular 3432)

**Análise documental pós-contemplação:**
- A administradora analisa documentação do contemplado e avalia o bem
- Prazo típico: 5 a 15 dias úteis para liberação efetiva
- Documentação de análise de capacidade de pagamento mantida por 5 anos (Res. BCB 285)

---

## 8. Representantes dos Consorciados

- Eleitos na AGO para representar o grupo perante a administradora
- Mandato não remunerado
- Impedidos: funcionários, sócios, gerentes, diretores da administradora
- Substituídos se: renúncia, contemplação, exclusão, ou outro impedimento
- Nova eleição na próxima AGO após o impedimento

---

## 9. Encerramento do Grupo

Causas de encerramento (Art. 35, Circular 3432):
- Todos os consorciados contemplados
- Deliberação em AGE por irregularidades graves
- Inadimplência em número que comprometa as contemplações futuras
- Descontinuidade do bem

**Na dissolução:**
0. Status do Grupo transita para: `AGENDADO` → `EM_LIQUIDACAO` → `ENCERRADO_CONTABIL` → `ENCERRADO_DEFINITIVO` (eventualmente `EXPURGADO` via LGPD após 10 anos).
1. Contemplados continuam pagando parcelas vincendas (exceto parcela de fundo de reserva)
2. Excluídos recebem restituição em até 60 dias da última AGO
3. Saldo do fundo de reserva distribuído proporcionalmente
4. Administradora não pode transferir os recursos do grupo para outra administradora
   durante o período entre a última AGO e o encerramento (Art. 29, Circular 3432)
