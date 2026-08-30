# Especificação do Domínio: Assembleia e Apuração

## Visão Geral
O módulo de Assembleia gerencia as reuniões onde ocorrem as contemplações de cotas, seja por Sorteio ou por Lance (Livre/Fixo). A apuração obedece regras rigorosas de saúde financeira (Fundo Comum) e parâmetros externos de loteria.

## Regras de Apuração (Motor de Apuração)

### P1 — Fonte oficial e reprodutibilidade
- A apuração por sorteio usa exclusivamente o primeiro prêmio de uma extração persistida de `LoteriaFederal`, elegível na data da assembleia.
- O motor não pode gerar números aleatórios nem aceitar número avulso como fonte de sorteio.
- A assembleia persiste concurso e prêmio usado antes do encerramento, permitindo reprodução da pedra-chave e da ata.
- Ausência ou prêmio inválido da extração oficial interrompe a apuração antes de qualquer contemplação.

### P2 — Excluídos e desempates
- O sorteio de excluídos é independente da existência de contemplado ativo e usa o segundo prêmio da extração oficial.
- `LOTERIA_FEDERAL` calcula a pedra pelo algoritmo contratual do grupo; `PROXIMIDADE_COTA_SORTEADA` usa a mesma referência apenas quando não houver uma cota ativa sorteada registrada.
- `MAIOR_LANCE_ACUMULADO` ordena pelo Fundo Comum efetivamente pago pela cota até a data da apuração; em empate, aplica código de cota crescente.

## Ciclo de captacao
- O grupo possui assembleias ordinarias previamente agendadas. A abertura de captacao transita uma sessao `AGENDADA` para `CAPTANDO` e registra `dataInicioCaptacao`.
- A abertura e idempotente para uma sessao ja `CAPTANDO`: nao reinicia a janela ou produz novo efeito; registros legados sem data inicial recebem apenas a marca temporal faltante.
- Assembleias `REALIZADA` ou `FECHADA` nao podem ser reabertas. O recebimento de lances ocorre exclusivamente no status `CAPTANDO`.
- A listagem de assembleias deve expor `status`, para que o credenciamento apresente somente a sessao efetivamente `CAPTANDO` do grupo selecionado.

## Consulta operacional da Central AGO
- A Central AGO consulta assembleias por grupo e status no servidor. A agenda `AGENDADA` usa páginas de cinco registros; sessões `CAPTANDO`, `REALIZADA` e atas `FECHADA` são exibidas em áreas operacionais distintas.
- Uma sessão `CAPTANDO` permite apenas o recebimento ou encerramento de lances. A apuração é disponibilizada exclusivamente após o encerramento, em `REALIZADA`, preservando a ordem regulamentar do sorteio.

## Ata visual de apuração
- Para uma assembleia `FECHADA`, a Central AGO exibe uma ata somente de leitura com a extração oficial, os prêmios utilizados, a pedra-chave, o algoritmo, os fallbacks e as contemplações vinculadas.
- A ata não permite alterar o resultado, registrar contemplação manual ou liquidar lance. Lances externos vencedores seguem exclusivamente para a tela de Integralização.
- Quando não houver contemplação, a ata informa explicitamente que a apuração foi concluída sem resultados, sem mascarar o estado da assembleia.

## Execução do sorteio oficial
- Após registrar a extração da Loteria Federal, a captação deve ser encerrada para transitar a assembleia a `REALIZADA`.
- A ação `Apurar` resolve exclusivamente a extração oficial mais recente com data menor ou igual à data da assembleia, persiste concurso e prêmios usados e executa sorteio antes da apuração de lances.
- O operador não informa dezena, pedra-chave ou resultado aleatório; a ausência de extração elegível bloqueia a apuração.

### 1. Sorteio (Loteria Federal / Pedra Chave)
De acordo com o BCB, os sorteios em consórcios frequentemente se apoiam nos resultados da Loteria Federal.
- **Fonte do sorteio**: o motor seleciona ou valida exclusivamente uma extração oficial persistida em `LoteriaFederal`, elegível na data da assembleia. A interface não informa dezena ou pedra-chave avulsa.
- **Elegibilidade**: Todas as cotas com status `ATIVA` que não tenham sido contempladas anteriormente.

### 2. Participação de Cotas Canceladas no Sorteio
Por exigência do Banco Central do Brasil:
- **Regra**: Cotas com status `CANCELADA` ou inativas continuam compondo o universo (o globo) do sorteio.
- **Motivo**: Caso uma cota cancelada seja sorteada, ela "reativa" sua posição unicamente para fins de **Restituição de Valores** pagos no Fundo Comum, descontadas multas contratuais. Ela NÃO recebe o crédito integral para compra de bem.

### 3. Empate em Lances Livres
- **Critério de Desempate**: Quando há empate de valores percentuais ofertados em Lances Livres, o sistema utiliza o mesmo critério configurado para o Grupo (ex: Loteria Federal) para desempatar, contemplando a cota que for mais próxima à dezena ou pedra chave sorteada na assembleia.

### 4. Impacto no Fundo Comum
- A apuração de lances (fixos ou livres) e sorteios só pode contemplar se houver **saldo disponível** no Fundo Comum.
- `Impacto Caixa = Valor do Crédito - Valor Ofertado no Lance (se houver)`.
- Se o impacto superar o saldo da conta do grupo, a cota não pode ser contemplada.
