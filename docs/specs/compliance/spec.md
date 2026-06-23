# Compliance e Listas Restritivas (PLD/FT)

Status: PATCHED
Versão: v1.1
Aprovações: Pendentes
Última alteração: Adição de uploads manuais (PEP, ONU, IBGE), integração real com OFAC API e agendamento dinâmico via cron.

## 1. Visão Geral (O que é?)
Esta capability introduz o motor ativo de Compliance (Prevenção à Lavagem de Dinheiro e Financiamento ao Terrorismo - PLD/FT). O sistema substituirá a carga manual de arquivos CSV/XML (modelo Newcon) por uma **integração automatizada via APIs externas** (Portal da Transparência, OFAC e ONU). O motor fará o cruzamento contínuo dos CPFs/Nomes dos consorciados ativos contra essas listas, garantindo conformidade com a Circular BCB 3.978/2020.

Adicionalmente, o sistema permite uploads manuais das listas PEP (CSV), ONU (XML) e Municípios de Fronteira/Cidades Gêmeas do IBGE (XLS), além de possibilitar ao administrador e compliance configurar dinamicamente o horário e frequência de execução do Job automático.

## 2. Requisitos Funcionais

1. **REQ-COMP-001**: O sistema deve consultar diariamente (via job agendado) APIs governamentais abertas para alimentar/atualizar a base de Listas Restritivas.
   - **Lista PEP (Pessoas Expostas Politicamente)**: Portal da Transparência API.
   - **Lista SDN/Terroristas (OFAC/Tesouro Americano)**: Extrator OFAC API.
   - **Lista CSNU (ONU)**: Extrator CSNU API.
2. **REQ-COMP-002**: O sistema deve expor um endpoint `/api/compliance/sincronizar` para disparo manual sob demanda dessa sincronização.
3. **REQ-COMP-003**: O sistema deve cruzar (matching) o `cpfCnpj` e o `nome` dos clientes cadastrados contra as listas atualizadas.
4. **REQ-COMP-004**: Havendo correspondência (`match >= 90%` para nomes ou `match exato` para CPF), o sistema deve gerar uma entidade de `AlertaCompliance` atrelada ao cliente, com status `PENDENTE_ANALISE`.
5. **REQ-COMP-005**: O sistema deve fornecer endpoint `/api/compliance/alertas` acessível estritamente para roles `ROLE_COMPLIANCE` e `ROLE_ADMIN` para visualização e deliberação (Falso Positivo, Suspeita Confirmada) dos alertas.
6. **REQ-COMP-006**: O sistema deve permitir ao administrador e analista de compliance fazer o upload de três arquivos de referência:
   - **PEP (CSV)**: Delimitado por ponto e vírgula `;`, com CPFs mascarados (`***.531.324-**`) e nomes.
   - **ONU (XML)**: Lista consolidada de indivíduos e entidades do Conselho de Segurança da ONU.
   - **IBGE (XLS)**: Planilha de Municípios da Faixa de Fronteira e Cidades Gêmeas.
7. **REQ-COMP-007**: O sistema deve integrar-se diretamente com a API do OFAC Sanctions List Service (`https://sanctionslistservice.ofac.treas.gov/api/download/SDN.XML` ou `CONSOLIDATED.XML`) para ingestão da base americana de sanções.
8. **REQ-COMP-008**: O sistema deve permitir configurar o agendamento (cron expression) de sincronização e matching via tela, armazenando a frequência e horário e atualizando o trigger do agendamento sem reiniciar a aplicação.

## 3. Regras de Negócio (Inegociáveis)

- **RN-COMP-001 (Match Sensível)**: A comparação de nomes deve utilizar algoritmos de Similaridade/Distância de Levenshtein ou Jaro-Winkler para tratar variações de grafia em nomes estrangeiros oriundos da ONU/OFAC.
- **RN-COMP-002 (Bloqueio Cautelar)**: Clientes com alerta de Terrorismo (ONU/OFAC) ativo e confirmado pelo compliance devem ter o pagamento de lances, sorteios e restituições **bloqueados** via `CotaStatus.BLOQUEADA_COMPLIANCE`. 
- **RN-COMP-003 (Sigilo)**: Usuários comuns (`ROLE_ATENDIMENTO`) não podem visualizar alertas de compliance na ficha do cliente para evitar *tipping-off* (aviso ao suspeito).
- **RN-COMP-004 (Matching PEP Mascarado)**: Como a lista de PEP contém CPFs mascarados no formato `***.531.324-**` (somente os 6 dígitos centrais visíveis), a validação por documento contra a lista PEP deve extrair os 6 dígitos centrais do CPF do cliente (caracteres de índice 3 a 8 do CPF numérico limpo) e compará-los com os 6 dígitos expostos da lista. Havendo correspondência de CPF E Jaro-Winkler do nome >= 90%, o alerta é gerado.
- **RN-COMP-005 (Fronteira e Cidades Gêmeas)**: Clientes que residam em municípios da faixa de fronteira ou cidades gêmeas (cruzando `localidade` e `uf` do cliente contra a lista IBGE normalizada) devem ter sua classificação de risco marcada ou gerar alertas específicos de atenção `IBGE`.
- **RN-COMP-006 (OFAC Resilience)**: Caso a API do OFAC esteja inacessível ou falhe na requisição de download do XML, o sistema deve registrar o erro no log e prosseguir com a execução das demais listas locais.

## 4. Critérios de Aceite (QA)

- [ ] A sincronização das listas via endpoint preenche a tabela `ListaRestritiva` com milhares de registros.
- [ ] Um cliente recém-cadastrado cujo CPF coincida com a lista PEP gera automaticamente um `AlertaCompliance`.
- [ ] Usuários sem a role `COMPLIANCE` ou `ADMIN` recebem HTTP 403 Forbidden ao tentar acessar ou configurar os alertas/agendamento.
- [ ] O upload do arquivo PEP de 16MB é processado sem estourar o limite de tamanho do Spring Boot.
- [ ] O upload do arquivo IBGE (XLS) indexa os municípios de fronteira, e clientes localizados em Alta Floresta D'Oeste (RO) geram alertas sob a origem `IBGE`.
- [ ] Alterar o agendamento cron para rodar a cada minuto (ex: `0 */1 * * * *`) ativa imediatamente o job sem precisar reiniciar o servidor.

