# Especificação do Domínio: Reembolsos e Excluídos

## Visão Geral
Este módulo trata das cotas canceladas/excluídas do grupo e do gerenciamento das devoluções e reembolsos de valores, conforme as diretrizes do Banco Central do Brasil (BCB) referentes a consórcios.

## Regras de Negócio e BCB

### 1. Participação em Sorteios
- **Restituição Antecipada**: O consorciado excluído continua participando dos sorteios mensais das assembleias.
- **Sorteio da Cota Cancelada**: Caso a sua cota inativa (cancelada) seja contemplada no sorteio, ele não recebe uma carta de crédito, mas ganha o direito à **restituição dos valores pagos** (fundo comum), abatendo-se a multa contratual e as taxas administrativas.
- **Implementação Técnica (Motor de Apuração)**: O algoritmo de pedra-chave avalia os ativos primeiro. Quando uma cota ativa (versão 0) é contemplada, o motor realiza uma busca espelhada na mesma assembleia visando contemplar para restituição a respectiva cota inativa do mesmo número (versão 1 ou superior). Esse duplo processamento (ativo + excluído) unifica a esteira de contemplação.

### 2. Cálculo da Restituição (Conforme ADR 005)
- `Percentual Amortizado` = Total amortizado pela cota em relação ao Fundo Comum do bem referenciado.
- `Valor Base de Restituição (VBR)` = Percentual Amortizado aplicado sobre o valor atualizado do bem de referência vigente na data da Assembleia (AGO).
- `Deduções Aplicáveis` = Multa Contratual por Quebra de Acordo de 10% sobre o VBR + Taxas pendentes (se houver).
- `Total a Devolver (Líquido)` = Valor Base de Restituição - Deduções.

### 3. Fim do Grupo (Encerramento)
- Consorciados excluídos que **não foram sorteados** ao longo da vida do grupo têm direito ao recebimento de seus valores residuais num prazo de até **60 dias após o encerramento do grupo**, respeitando o caixa final.

## Integrações
- O processo é profundamente acoplado ao `MotorApuracaoService`, que foi alterado (na Wave 4) para permitir que cotas `CANCELADA` participem do globo da Loteria Federal.
- Acopla-se também à Contabilidade, para deduzir o valor da devolução do Fundo Comum do grupo.
