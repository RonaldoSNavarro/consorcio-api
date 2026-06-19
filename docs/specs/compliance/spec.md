# Compliance e Listas Restritivas (PLD/FT)

Status: DRAFT
Versão: v1.0
Aprovações: Pendentes
Última alteração: Criação da especificação para integração com listas externas via API.

## 1. Visão Geral (O que é?)
Esta capability introduz o motor ativo de Compliance (Prevenção à Lavagem de Dinheiro e Financiamento ao Terrorismo - PLD/FT). O sistema substituirá a carga manual de arquivos CSV/XML (modelo Newcon) por uma **integração automatizada via APIs externas** (Portal da Transparência, OFAC e ONU). O motor fará o cruzamento contínuo dos CPFs/Nomes dos consorciados ativos contra essas listas, garantindo conformidade com a Circular BCB 3.978/2020.

## 2. Requisitos Funcionais

1. **REQ-COMP-001**: O sistema deve consultar diariamente (via job agendado) APIs governamentais abertas para alimentar/atualizar a base de Listas Restritivas.
   - **Lista PEP (Pessoas Expostas Politicamente)**: Portal da Transparência API.
   - **Lista SDN/Terroristas (OFAC/Tesouro Americano)**: Extrator OFAC API.
   - **Lista CSNU (ONU)**: Extrator CSNU API.
2. **REQ-COMP-002**: O sistema deve expor um endpoint `/api/compliance/sincronizar` para disparo manual sob demanda dessa sincronização.
3. **REQ-COMP-003**: O sistema deve cruzar (matching) o `cpfCnpj` e o `nome` dos clientes cadastrados contra as listas atualizadas.
4. **REQ-COMP-004**: Havendo correspondência (`match >= 90%` para nomes ou `match exato` para CPF), o sistema deve gerar uma entidade de `AlertaCompliance` atrelada ao cliente, com status `PENDENTE_ANALISE`.
5. **REQ-COMP-005**: O sistema deve fornecer endpoint `/api/compliance/alertas` acessível estritamente para roles `ROLE_COMPLIANCE` e `ROLE_ADMIN` para visualização e deliberação (Falso Positivo, Suspeita Confirmada) dos alertas.

## 3. Regras de Negócio (Inegociáveis)

- **RN-COMP-001 (Match Sensível)**: A comparação de nomes deve utilizar algoritmos de Similaridade/Distância de Levenshtein ou Jaro-Winkler para tratar variações de grafia em nomes estrangeiros oriundos da ONU/OFAC.
- **RN-COMP-002 (Bloqueio Cautelar)**: Clientes com alerta de Terrorismo (ONU/OFAC) ativo e confirmado pelo compliance devem ter o pagamento de lances, sorteios e restituições **bloqueados** via `CotaStatus.BLOQUEADA_COMPLIANCE`. 
- **RN-COMP-003 (Sigilo)**: Usuários comuns (`ROLE_ATENDIMENTO`) não podem visualizar alertas de compliance na ficha do cliente para evitar *tipping-off* (aviso ao suspeito).

## 4. Critérios de Aceite (QA)

- [ ] A sincronização das listas via endpoint preenche a tabela `ListaRestritiva` com milhares de registros.
- [ ] Um cliente recém-cadastrado cujo CPF coincida com a lista PEP gera automaticamente um `AlertaCompliance`.
- [ ] Usuários sem a role `COMPLIANCE` recebem HTTP 403 Forbidden ao tentar ler os alertas.
