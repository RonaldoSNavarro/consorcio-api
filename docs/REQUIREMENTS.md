# 📋 Requisitos do Sistema — Consórcio API

Este documento detalha os requisitos funcionais, regras de negócio e a modelagem do sistema de gestão de consórcios da administradora, em total conformidade com a Lei Federal nº 11.795/08 e as normativas do Banco Central do Brasil (BCB), em especial a Resolução BCB nº 285/2023.

---

## 🏗️ 1. Modelo de Dados e Entidades (Domain/Model)

O sistema de consórcio é baseado nas seguintes entidades centrais e seus respectivos papéis de negócio:

### A. Entidades do Consórcio
1. **`Cliente`:** Cadastro do participante. Pode ser pessoa física ou jurídica.
   - Atributos principais: `cpfCnpj`, `nome`, `email`, `status` (ATIVO, INATIVO).
2. **`Grupo`:** Pool coletivo autônomo com patrimônio de afetação segregado da administradora.
   - Atributos principais: `codigo`, `valorCredito`, `prazoMeses`, `taxaAdministracao`, `fundoReserva`, `percentualLanceEmbutidoMaximo`, `status` (EM_FORMACAO, EM_ANDAMENTO, ENCERRADO), `criterioDesempateLance`.
3. **`Cota`:** Contrato individualizado de participação dentro de um grupo.
   - Atributos principais: `numeroCota`, vínculo a `Cliente` e `Grupo`, `status` (ATIVA, CANCELADA, AGUARDANDO_ANALISE, APROVADO), `valorReembolsado`, flag `reembolsada`, `version` (controle de concorrência JPA `@Version`).
4. **`Parcela`:** Títulos de faturamento mensal gerados para cada cota. Composta obrigatoriamente por 4 componentes segregados:
   - `valorFundoComum` (FC)
   - `valorTaxaAdministracao` (TA)
   - `valorFundoReserva` (FR)
   - `valorSeguro` (SEG)
   - Adicionais de mora: `valorMulta` (2%), `valorJuros` (1% a.m. *pro rata die*).
   - Metadados: `dataVencimento`, `dataPagamento`, `status` (PENDENTE, PAGA, ATRASADA).
5. **`Assembleia`:** Assembleia Geral Ordinária (AGO) do grupo onde ocorrem as apurações e homologações de contemplações.
   - Atributos principais: `dataAssembleia`, `status` (CAPTANDO, REALIZADA, FECHADA).
6. **`Lance`:** Propostas de antecipação de saldo feitas pelos consorciados para a AGO.
   - Atributos principais: `tipo` (LIVRE, EMBUTIDO), `valorOferta`, `dataOferta`, `statusApuracao` (PENDENTE, VENCEDOR, PERDEDOR).
7. **`Contemplacao`:** Registro do direito creditório atribuído à cota.
   - Atributos principais: `tipoContemplacao` (SORTEIO, LANCE_LIVRE, LANCE_FIXO), `valorCreditoLiberado`, `valorLance`, flag `lanceEmbutido`, `dataContemplacao`.

### B. Entidades Contábeis (Double-Entry Ledger)
1. **`ContaContabil`:** Contas do Plano de Contas estruturado sob as diretrizes do COSIF (Banco Central).
   - Atributos principais: `codigoContabil` (padrão de 8 dígitos do BCB), `descricao`, `natureza` (DEVEDORA, CREDORA), `tipo`.
2. **`LancamentoContabil`:** Lançamentos contábeis de partida dobrada (Double-entry) associando obrigatoriamente uma conta de débito e uma de crédito pelo mesmo valor financeiro, garantindo auditoria e consistência $O(1)$ de saldos.

### C. Entidades de Auditoria
1. **`HistoricoVersaoCota`:** Registro histórico e imutável de transições de status da cota, apontando usuário logado e justificativa.

---

## 🧮 2. Regras de Negócio e Motores de Cálculo

### A. Composição da Parcela
A prestação mensal cobrada do consorciado é a soma de quatro componentes básicos calculados proporcionalmente sobre o valor do crédito do grupo:
$$\text{Valor Parcela} = \text{Fundo Comum} + \text{Taxa de Administração} + \text{Fundo de Reserva} + \text{Seguro}$$
- **Fundo Comum (FC):** Destinado exclusivamente a formar o saldo de caixa para contemplações do grupo.
- **Taxa de Administração (TA):** Remuneração de prestação de serviços da administradora (reconhecida por regime de competência).
- **Fundo de Reserva (FR):** Fundo de emergência para cobrir insuficiências de caixa pontuais ou inadimplência de contemplados.
- **Seguro:** Coberturas contratadas (prestamista ou quebra de garantia).
- *Hook de Banco:* A entidade `Parcela` executa a soma automatizada em métodos JPA `@PrePersist` e `@PreUpdate` para evitar divergências de arredondamento e gravação de valores parciais inválidos.

### B. Mora e Inadimplência
No caso de pagamento de parcelas após a data de vencimento, o sistema aplica encargos conforme limite legal do CDC e normativas do BCB:
1. **Multa de Mora:** Fator de **2,00%** fixado sobre o valor total da prestação atrasada.
2. **Juros de Mora:** Fator de **1,00% ao mês**, calculado *pro rata die* baseado nos dias úteis de atraso:
   $$\text{Juros} = \text{Valor Parcela} \times \left(\frac{0,01}{30}\right) \times \text{Dias de Atraso}$$
3. **Destinação dos Encargos:** Multa e juros moratórios **devem reverter 100% para o Fundo Comum do grupo** (Lei 11.795/08, Art. 25).
4. *Cálculo Volátil:* Ao consultar inadimplência em tempo de execução, os juros/multas são simulados em memória sem forçar flush indevido do JPA. O valor é consolidado e gravado somente no pagamento (`pagar`).

### C. Amortização de Lances
O saldo de lances pagos pode ser amortizado de duas formas pelo consorciado:
1. **Redução de Prazo:** O valor do lance quita as parcelas de trás para frente (ordem decrescente de vencimento).
   - As taxas de administração e fundo de reserva das parcelas excluídas devem ser redistribuídas (diluídas) sobre as parcelas remanescentes na ordem cronológica para que a administradora não perca a receita correspondente.
2. **Diluição de Valor:** O valor amortizado é dividido uniformemente entre todas as parcelas em aberto cronologicamente.
   - Aplica-se a **Regra do Centavo Perdido** para compensar dízimas e arredondamentos de centavos na última parcela pendente do cronograma.

### D. Oferta e Validação de Lances
1. **Elegibilidade:** Somente cotas ATIVAS pertencentes ao respectivo grupo da assembleia e que estejam **100% adimplentes** na data da assembleia podem cadastrar lances.
2. **Lance Embutido:** O valor do lance pode ser descontado diretamente do crédito a receber pelo consorciado, limitado ao percentual teto do grupo (`percentualLanceEmbutidoMaximo` do grupo sobre o `valorCredito`).
3. **Lance Livre:** Oferta de valor em espécie pelo consorciado, que deve ser integralizado fisicamente (recursos próprios) em caso de contemplação.

### E. Apuração de Assembleia e Contemplações
1. **Classificação:** Lances são ordenados pelo percentual ofertado em relação ao saldo devedor da cota (maior para menor). Em caso de empate, aplica-se o critério de desempate configurado no grupo (ex: número da cota mais próximo do número sorteado).
2. **Checagem de Caixa no Ledger:** O motor de apuração consulta em tempo real o saldo disponível no Fundo Comum do grupo via Ledger Contábil. O impacto líquido de caixa de cada contemplação é apurado como:
   $$\text{Impacto Caixa} = \text{Crédito do Grupo} - \text{Valor do Lance}$$
   - **Lançamentos Contábeis (Ledger COSIF):**
     * *Na Oferta e Homologação Inicial (Pendente):* Sem impacto patrimonial imediato nas contas do grupo. Lançamento opcional em contas de compensação: Débito em Lances a Integralizar (`9.1.1.10.00-3`) e Crédito em Lances Ofertados (`9.9.1.10.00-1`).
     * *Na Integralização do Lance (Compensação do boleto):*
       * **Débito:** `1.1.1.10.00-2` - Bancos - Recursos de Grupos (Disponibilidades)
       * **Crédito:** `2.1.2.10.10-6` - Fundo Comum de Grupos
     * *Na Homologação Definitiva da Contemplação (Trânsito de Crédito):*
       * **Débito:** `2.1.2.10.10-6` - Fundo Comum de Grupos (Redução da obrigação coletiva)
       * **Crédito:** `2.1.2.30.10-0` - Créditos a Liberar (Bens/Serviços Contemplados a Liberar)
       * *Valor:* Valor do crédito líquido liberado (Crédito do grupo menos lance embutido, se aplicável).
     * *No Pagamento Efetivo do Bem ao Fornecedor:*
       * **Débito:** `2.1.2.30.10-0` - Créditos a Liberar
       * **Crédito:** `1.1.1.10.00-2` - Bancos - Recursos de Grupos (Saída física de caixa)

5. **ADR 005 — Reajuste e Restituição Legal de Excluídos com Base no Valor do Bem Atualizado:**
   - **Regra de Negócio (Compliance: Art. 30 da Lei nº 11.795/08):** A devolução de valores pagos ao fundo comum por consorciados cancelados ou excluídos deve obrigatoriamente ocorrer por meio de contemplação exclusivamente por Sorteio na AGO do grupo. O cálculo baseia-se no percentual amortizado do valor do bem de referência reajustado vigente na data da assembleia de contemplação (sorteio), deduzida a multa compensatória (cláusula penal) acordada em contrato.
   - **Prazos e Comunicação:** Após a contemplação por sorteio da cota excluída, a administradora deve notificar o consorciado formalmente em até 5 dias úteis e disponibilizar um Demonstrativo de Prestação de Contas detalhando a memória de cálculo.
   - **Fórmula de Cálculo da Restituição:**
     1. **Percentual Amortizado do Fundo Comum (PAFC):**
        $$PAFC = \sum \left( \frac{\text{Valor Efetivamente Pago ao Fundo Comum de cada parcela}}{\text{Valor total do Crédito/Bem de referência na data do respectivo pagamento}} \right)$$
     2. **Valor Bruto da Devolução (VBD):**
        $$VBD = PAFC \times \text{Valor do Bem de Referência Atualizado na data da AGO de Contemplação do Excluído}$$
     3. **Valor Líquido da Devolução (VLD) após Cláusula Penal (Multa Rescisória de 10%):**
        $$VLD = VBD \times (1 - 0.10) = VBD \times 0.90$$
   - **Destinação da Cláusula Penal (Multa Rescisória):**
     * *Alternativa 1 (Reversão ao Fundo Comum do Grupo):* Destinada a recompor o fluxo de caixa do grupo. Apenas o valor líquido (90%) é retirado do fundo comum e segregado na conta de devolução de excluídos, mantendo a multa no patrimônio coletivo.
     * *Alternativa 2 (Reversão à Administradora):* Destinada a cobrir despesas de gestão de quebra de contrato. O fundo comum é debitado pelo valor bruto (100%), destinando 90% para devolução do excluído e 10% para as obrigações com a administradora.
   - **Ajuste Monotônico e Fixação de Saldo (Compliance):**
     * *Excluídos NÃO sorteados:* O valor pago compõe o Fundo Comum geral do grupo. Não há reajuste contábil periódico. A atualização é puramente sistêmica (em memória) e computada somente na data em que a cota for sorteada na assembleia.
     * *Excluídos JÁ sorteados:* Uma vez contemplada por sorteio, o valor apurado é transferido para a conta de passivo de excluídos. O saldo a devolver torna-se nominal e fixo. Ele **não sofre reajustes adicionais** decorrentes de variações posteriores do bem de referência do grupo. Em caso de atraso na disponibilização física dos recursos pela administradora após o sorteio, incide correção monetária por índice inflacionário e juros ordinários previstos contratualmente.
   - **Lançamentos Contábeis (Ledger COSIF):**
     * *Na Contemplação por Sorteio (Com Multa Revertida ao Grupo - Padrão):*
       * **Débito:** `2.1.2.10.10-6` - Fundo Comum de Grupos (Pelo valor líquido - 90%)
       * **Crédito:** `2.1.2.20.10-3` - Recursos de Consorciados Excluídos a Devolver (Pelo valor líquido - 90%)
     * *Na Contemplação por Sorteio (Com Multa Revertida à Administradora):*
       * **Débito:** `2.1.2.10.10-6` - Fundo Comum de Grupos (Pelo valor bruto - 100%)
       * **Crédito:** `2.1.2.20.10-3` - Recursos de Consorciados Excluídos a Devolver (Pelo valor líquido - 90%)
       * **Crédito:** `2.1.2.10.30-2` - Taxa de Administração a Repassar (Pelo valor da multa - 10%)
     * *No Efetivo Desembolso Físico ao Excluído Contemplado:*
       * **Débito:** `2.1.2.20.10-3` - Recursos de Consorciados Excluídos a Devolver (Valor líquido - 90%)
       * **Crédito:** `1.1.1.10.00-2` - Bancos - Recursos de Grupos (Disponibilidades)

### F. Reajustes e Encerramento de Grupo
1. **Reajuste:** Atualização anual ou semestral do bem de referência pelo índice parametrizado (ex: IPCA, FIPE).
   - Calcula-se o fator de reajuste:
     $$\text{Fator} = \frac{\text{Novo Valor Crédito}}{\text{Valor Crédito Antigo}}$$
   - Todas as parcelas em aberto (`PENDENTE` e `ATRASADA`) têm seu `valorFundoComum` multiplicado pelo fator. Os ganchos JPA recalculam o valor total do boleto e registram os ajustes contábeis de débito/crédito correspondentes.
2. **Encerramento de Grupo:**
   - Em conformidade com as regras do BCB, o encerramento do grupo deve ser processado formalmente no prazo máximo de **120 dias após a última AGO**.
   - Eventuais inadimplências residuais ativas de cotas contempladas ou pendentes devem ser baixadas do balancete consolidado (gerando provisão de perdas) e enviadas para cobrança judicial extracontábil. O grupo não pode ser bloqueado de encerrar no sistema por causa de pendências de inadimplentes.
   - Saldos residuais de consorciados credores não procurados devem ser transferidos para a conta de **Recursos Não Procurados (RNP)** no COSIF.

---

## 🔒 3. Requisitos de Segurança, Concorrência e Auditoria

1. **Optimistic Locking:** Uso de controle de concorrência otimista nas tabelas `cotas`, `grupos` e `parcelas` via coluna `@Version` do JPA para mitigar falhas em atualizações concorrentes paralelas de transações financeiras.
2. **Dupla Entrada (Partidas Dobradas):** Toda transação financeira gera necessariamente um lançamento contábil ligando uma conta de débito e uma conta de crédito do COSIF pelo mesmo valor. O saldo das contas é obtido de forma indexada no PostgreSQL filtrando pela natureza devedora ou credora da conta.
3. **Cookies HttpOnly & SameSite:** Armazenamento do token de autenticação JWT via cookies HTTP seguros (`HttpOnly = true` e `SameSite = Strict`) para proteger contra roubos por scripts XSS ou ataques CSRF. Mantém fallback por cabeçalho Bearer em endpoints de integração.
4. **Proteção contra IDOR:** Validação rigorosa na camada de serviço para conferir se o cliente autenticado no contexto do Spring Security é de fato o proprietário do recurso solicitado ou se possui credencial `ROLE_ADMIN`.
5. **Proxy ViaCEP:** Chamadas de validação de endereços consumindo a API externa ViaCEP encapsuladas na classe de serviço REST Client interna da API, prevenindo vazamentos de cabeçalhos e tokens sensíveis.

---

## 📈 4. Requisitos de Reports ao Banco Central (BCB)

Para auditoria legal e conformidade com o BCB, o sistema deve fornecer motores de exportação e consulta para as seguintes obrigações:
- **Documento 4110:** Balancete e Demonstração de Recursos de Grupos de Consórcio (dados consolidados mensalmente das contas do ativo e passivo de cada grupo).
- **Documento 2080:** Informações Estatísticas do Consórcio (relatório de adesões, exclusões, lances e contemplações no período).
- **Relatório PLD/FT:** Monitoramento automatizado de lavagem de dinheiro, sinalizando lances pagos acima de R$ 50.000,00 ou quitações em espécie.
