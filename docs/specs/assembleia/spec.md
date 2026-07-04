# Especificação do Domínio: Assembleia e Apuração

## Visão Geral
O módulo de Assembleia gerencia as reuniões onde ocorrem as contemplações de cotas, seja por Sorteio ou por Lance (Livre/Fixo). A apuração obedece regras rigorosas de saúde financeira (Fundo Comum) e parâmetros externos de loteria.

## Regras de Apuração (Motor de Apuração)

### 1. Sorteio (Loteria Federal / Pedra Chave)
De acordo com o BCB, os sorteios em consórcios frequentemente se apoiam nos resultados da Loteria Federal.
- **Dezena de Sorteio**: O usuário (via UI) pode informar uma "Dezena da Loteria Federal" ou uma "Pedra Chave". Esta dezena deve ser usada como "semente" ou parâmetro para a localização da cota contemplada.
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
