# Spec: Gestão de Inadimplência e Mora

## 1. Visão Geral
Este módulo gerencia o fluxo de atrasos no pagamento de parcelas do consórcio, aplicando os encargos legais (multa e juros moratórios) e disparando eventos de exclusão de cotas inadimplentes ou execução de garantias, conforme a Lei 11.795/08.

## 2. Regras de Negócio e Cálculos

### 2.1 Multa e Juros Moratórios
Sempre que uma parcela passar do seu vencimento (`dataVencimento < dataAtual` e `status == A_VENCER`), seu status muda para `VENCIDA`.
No momento da cobrança (pagamento em atraso):
- **Multa Moratória**: Fixa de 2% (dois por cento) sobre o valor da parcela atualizada.
- **Juros Moratórios**: 1% (um por cento) ao mês cobrados *pro-rata die* (Juros Simples).
  - Fórmula: `Juros = (1% / 30) * dias_de_atraso * valor_atualizado`
  - *Exemplo:* Atraso de 15 dias -> Juros = (0.01 / 30) * 15 = 0.5% sobre o valor.

### 2.2 Destinação Contábil
- O valor arrecadado a título de Multa e Juros reverte **100% para o Fundo Comum (FC)** do respectivo grupo, uma vez que o grupo é a entidade que sofre o desfalque financeiro pelo atraso.

### 2.3 Gatilho de Inadimplência Crítica (3 Parcelas)
A administradora deve rodar diariamente um Job/Rotina (`InadimplenciaJob`) que avalia o número de parcelas `VENCIDA` de cada cota.
- **Trigger**: Se a Cota possuir >= 3 parcelas `VENCIDA` (consecutivas ou não).
- **Ação para Cota ATIVA (Não Contemplada)**:
  - O status da Cota é alterado para `EXCLUIDA`.
  - Ela perde o direito de ofertar lances e participar dos sorteios normais.
  - Entra na lista de Sorteios de Excluídos para futura restituição.
- **Ação para Cota CONTEMPLADA**:
  - O status da Cota é alterado para `EM_EXECUCAO`.
  - A cota NÃO é removida do grupo (pois já utilizou o crédito), mas a inadimplência gera um alerta grave para a Administradora acionar o processo jurídico de Alienação Fiduciária (apreensão do bem) e negativação nos órgãos de crédito.

## 3. Entidades e Status Envolvidos
- **StatusParcela**: Novo estado `VENCIDA`.
- **StatusCota**: Novos estados `EXCLUIDA` e `EM_EXECUCAO`.
- **Lançamentos Contábeis**: Novas naturezas para entrada de Juros Moratórios e Multas no COSIF (Fundo Comum).


## 5. Status da Implementa��o
- **Status**: Implementado e validado.
- **QA**: Valida��o E2E visual via UI confirmando exclus�es, execu��o de contempladas e encargos aplicados com sucesso.
- **Artefato**: elatorio_qa_consorcio.md no brain do agente.
