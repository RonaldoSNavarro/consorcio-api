# Compliance e PLD/FT

Este documento detalha o subsistema de Compliance e Prevenção à Lavagem de Dinheiro e Financiamento do Terrorismo (PLD/FT), conforme obrigações da Circular BCB 3.978/2020.

---

## 1. Listas Restritivas e Monitoramento

A administradora é obrigada a monitorar clientes, propostas e movimentações financeiras cruzando dados com listas de sanções e PEPs (Pessoas Expostas Politicamente).

### Origens de Dados Monitoradas (`OrigemListaRestritiva`)
1. **OFAC (Office of Foreign Assets Control):** Download e processamento de listas de sanções dos EUA (SDN List).
2. **ONU (Organização das Nações Unidas):** Parsing XML de sanções contra indivíduos e entidades.
3. **PEP (Pessoas Expostas Politicamente):** Processamento de CSV do Portal da Transparência do Brasil (matching por nome e CPF mascarado/dígito central).
4. **IBGE:** Cruzamento de cidades de domicílio para identificar "Cidades Gêmeas" e faixa de fronteira (áreas de maior risco de contrabando/lavagem).

---

## 2. Motor de Matching (Score Jaro-Winkler)

O `MatchComplianceService` não exige coincidência exata de nomes, pois as listas internacionais muitas vezes não têm o CPF do brasileiro.
- Utiliza-se a distância de **Jaro-Winkler** para realizar *fuzzy matching* fonético e tipográfico.
- **Limiar (Threshold):** Score `≥ 0.90` dispara um `AlertaCompliance`.
- Parâmetros configuráveis ficam em `ComplianceConfig`.

---

## 3. Alertas e Bloqueios (`AlertaCompliance`)

### Status do Alerta (`StatusAlertaCompliance`)
- `PENDENTE_ANALISE`: O sistema encontrou um match (score ≥ 0.90) e aguarda revisão humana de um analista de compliance.
- `CONFIRMADO`: Analista confirmou que o cliente é de fato a entidade listada.
- `FALSO_POSITIVO`: Homônimo ou falso positivo descartado.

### Regras de Bloqueio (Blocking Rules)
Se um cliente possui qualquer alerta `PENDENTE_ANALISE` ou `CONFIRMADO`:
- É **proibido** criar novas `PropostaAdesao`.
- É **proibido** aprovar contemplações.
- É **proibido** realizar `TransferenciaCota` (Cessão de Direitos).
- Toda transação associada fica congelada até o descarte do alerta.

---

## 4. Limites de Alçada Regulatórios

- **Lances Suspeitos:** Conforme regras da Circular 3.978/2020 e manuais do COAF, toda contemplação por lance que utilize recursos próprios cujo valor seja **≥ R$ 50.000,00** é flaggada no módulo de relatórios para notificação especial ao regulador (DOC 2080 / Siscoaf).

---

## 5. Destinação de Multas Rescisórias
O subsistema de compliance também audita se a `DestinacaoMultaRescisoria` dos inadimplentes excluídos (configurada por grupo: `FUNDO_RESERVA` ou `TAXA_ADMINISTRACAO`) está sendo corretamente aplicada e se não configura apropriação indébita.
