# Compliance e Listas Restritivas (PLD/FT)

*   **Status**: IMPLEMENTED v1.0
VersÃ£o: v1.2
AprovaÃ§Ãµes: Pendentes
Ãšltima alteraÃ§Ã£o: AdiÃ§Ã£o de logs de execuÃ§Ã£o, feedback detalhado na sincronizaÃ§Ã£o manual e processamento em batch (Wave 3).

## 1. VisÃ£o Geral (O que Ã©?)
Esta capability introduz o motor ativo de Compliance (PrevenÃ§Ã£o Ã  Lavagem de Dinheiro e Financiamento ao Terrorismo - PLD/FT). O sistema substituirÃ¡ a carga manual de arquivos CSV/XML (modelo Newcon) por uma **integraÃ§Ã£o automatizada via APIs externas** (Portal da TransparÃªncia, OFAC e ONU). O motor farÃ¡ o cruzamento contÃ­nuo dos CPFs/Nomes dos consorciados ativos contra essas listas, garantindo conformidade com a Circular BCB 3.978/2020.

Adicionalmente, o sistema permite uploads manuais das listas PEP (CSV), ONU (XML) e MunicÃ­pios de Fronteira/Cidades GÃªmeas do IBGE (XLS), alÃ©m de possibilitar ao administrador e compliance configurar dinamicamente o horÃ¡rio e frequÃªncia de execuÃ§Ã£o do Job automÃ¡tico.

## 2. Requisitos Funcionais

1. **REQ-COMP-001**: O sistema deve consultar diariamente (via job agendado) APIs governamentais abertas para alimentar/atualizar a base de Listas Restritivas.
   - **Lista PEP (Pessoas Expostas Politicamente)**: Portal da TransparÃªncia API.
   - **Lista SDN/Terroristas (OFAC/Tesouro Americano)**: Extrator OFAC API.
   - **Lista CSNU (ONU)**: Extrator CSNU API.
2. **REQ-COMP-002**: O sistema deve expor um endpoint `/api/compliance/sincronizar` para disparo manual sob demanda dessa sincronizaÃ§Ã£o.
3. **REQ-COMP-003**: O sistema deve cruzar (matching) o `cpfCnpj` e o `nome` dos clientes cadastrados contra as listas atualizadas.
4. **REQ-COMP-004**: Havendo correspondÃªncia (`match >= 90%` para nomes ou `match exato` para CPF), o sistema deve gerar uma entidade de `AlertaCompliance` atrelada ao cliente, com status `PENDENTE_ANALISE`.
5. **REQ-COMP-005**: O sistema deve fornecer endpoint `/api/compliance/alertas` acessÃ­vel estritamente para roles `ROLE_COMPLIANCE` e `ROLE_ADMIN` para visualizaÃ§Ã£o e deliberaÃ§Ã£o (Falso Positivo, Suspeita Confirmada) dos alertas.
6. **REQ-COMP-006**: O sistema deve permitir ao administrador e analista de compliance fazer o upload de trÃªs arquivos de referÃªncia:
   - **PEP (CSV)**: Delimitado por ponto e vÃ­rgula `;`, com CPFs mascarados (`***.531.324-**`) e nomes.
   - **ONU (XML)**: Lista consolidada de indivÃ­duos e entidades do Conselho de SeguranÃ§a da ONU.
   - **IBGE (XLS)**: Planilha de MunicÃ­pios da Faixa de Fronteira e Cidades GÃªmeas.
7. **REQ-COMP-007**: O sistema deve integrar-se diretamente com a API do OFAC Sanctions List Service (`https://sanctionslistservice.ofac.treas.gov/api/download/SDN.XML` ou `CONSOLIDATED.XML`) para ingestÃ£o da base americana de sanÃ§Ãµes.
8. **REQ-COMP-008**: O sistema deve permitir configurar o agendamento (cron expression) de sincronizaÃ§Ã£o e matching via tela, armazenando a frequÃªncia e horÃ¡rio e atualizando o trigger do agendamento sem reiniciar a aplicaÃ§Ã£o.
9. **REQ-COMP-009**: O sistema deve manter um histÃ³rico de execuÃ§Ãµes das sincronizaÃ§Ãµes (seja manual ou por agendamento automÃ¡tico) e prover um endpoint `/api/compliance/execucoes` para visualizaÃ§Ã£o dos Ãºltimos logs.
10. **REQ-COMP-010**: O endpoint de sincronizaÃ§Ã£o manual `/api/compliance/sincronizar` deve retornar informaÃ§Ãµes em tempo real sobre a disponibilidade das APIs externas (como a OFAC) e a contagem de registros processados.
11. **REQ-COMP-011**: O processamento de arquivos em massa (como a Lista PEP) deve utilizar processamento em lote (batching) e mitigaÃ§Ã£o de N+1 queries para evitar sobrecarga no banco de dados e garantir escalabilidade.
12. **REQ-COMP-012**: A busca e matching textual de nomes deve ser delegada ao banco de dados PostgreSQL utilizando a extensÃ£o `pg_trgm`, Ã­ndices de expressÃ£o `GIN` e o operador de similaridade (`%`) para assegurar alta performance.

## 3. Regras de NegÃ³cio (InegociÃ¡veis)

- **RN-COMP-001 (Match SensÃ­vel)**: A comparaÃ§Ã£o de nomes deve utilizar algoritmos avanÃ§ados para tratar variaÃ§Ãµes de grafia em nomes estrangeiros oriundos da ONU/OFAC, processados exclusivamente no nÃ­vel do banco de dados para garantir performance em grandes volumes.
- **RN-COMP-002 (Bloqueio Cautelar)**: Clientes com alerta de Terrorismo (ONU/OFAC) ativo e confirmado pelo compliance devem ter o pagamento de lances, sorteios e restituiÃ§Ãµes **bloqueados** via `CotaStatus.BLOQUEADA_COMPLIANCE`. 
- **RN-COMP-003 (Sigilo)**: UsuÃ¡rios comuns (`ROLE_ATENDIMENTO`) nÃ£o podem visualizar alertas de compliance na ficha do cliente para evitar *tipping-off* (aviso ao suspeito).
- **RN-COMP-004 (Matching PEP Mascarado)**: Como a lista de PEP contÃ©m CPFs mascarados no formato `***.531.324-**` (somente os 6 dÃ­gitos centrais visÃ­veis), a validaÃ§Ã£o por documento contra a lista PEP deve extrair os 6 dÃ­gitos centrais do CPF do cliente (caracteres de Ã­ndice 3 a 8 do CPF numÃ©rico limpo) e comparÃ¡-los com os 6 dÃ­gitos expostos da lista. Havendo correspondÃªncia de CPF E similaridade do nome (`pg_trgm` >= limite de similaridade do SGBD), o alerta Ã© gerado.
- **RN-COMP-005 (Fronteira e Cidades GÃªmeas)**: Clientes que residam em municÃ­pios da faixa de fronteira ou cidades gÃªmeas (cruzando `localidade` e `uf` do cliente contra a lista IBGE normalizada) devem ter sua classificaÃ§Ã£o de risco marcada ou gerar alertas especÃ­ficos de atenÃ§Ã£o `IBGE`.
- **RN-COMP-006 (OFAC Resilience)**: Caso a API do OFAC esteja inacessÃ­vel ou falhe na requisiÃ§Ã£o de download do XML, o sistema deve registrar o erro no log e prosseguir com a execuÃ§Ã£o das demais listas locais.

## 4. CritÃ©rios de Aceite (QA)

- [ ] A sincronizaÃ§Ã£o das listas via endpoint preenche a tabela `ListaRestritiva` com milhares de registros.
- [ ] Um cliente recÃ©m-cadastrado cujo CPF coincida com a lista PEP gera automaticamente um `AlertaCompliance`.
- [ ] UsuÃ¡rios sem a role `COMPLIANCE` ou `ADMIN` recebem HTTP 403 Forbidden ao tentar acessar ou configurar os alertas/agendamento.
- [ ] O upload do arquivo PEP de 16MB Ã© processado sem estourar o limite de tamanho do Spring Boot.
- [ ] O upload do arquivo IBGE (XLS) indexa os municÃ­pios de fronteira, e clientes localizados em Alta Floresta D'Oeste (RO) geram alertas sob a origem `IBGE`.
- [ ] Alterar o agendamento cron para rodar a cada minuto (ex: `0 */1 * * * *`) ativa imediatamente o job sem precisar reiniciar o servidor.

