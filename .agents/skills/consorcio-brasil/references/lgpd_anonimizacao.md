# LGPD e Anonimização

Este documento cobre as regras de tratamento de dados sensíveis e o processo de expurgo de grupos encerrados no contexto da Lei Geral de Proteção de Dados (LGPD) e das regulações do Banco Central.

---

## 1. Conflito LGPD vs BACEN

A LGPD (Lei 13.709/2018) estabelece o direito ao esquecimento e a eliminação de dados pessoais. No entanto, o **Banco Central exige a retenção** dos registros de transações financeiras e documentos de consórcio por longos períodos para fins de auditoria e PLD/FT (Prevenção à Lavagem de Dinheiro).

- **Prazo de Retenção (BACEN):** 10 anos contados a partir do efetivo encerramento definitivo do grupo de consórcio.
- Durante esse período, solicitações de "esquecimento" (exclusão de dados) por parte de clientes em grupos encerrados são bloqueadas com justificativa de base legalizatória ("Cumprimento de Obrigação Legal ou Regulatória").

---

## 2. Processo de Anonimização (Expurgo)

Passado o prazo legal de retenção de 10 anos, os dados pessoais perdem a justificativa regulatória e devem ser eliminados ou anonimizados.

O sistema processa essa tarefa através do **`LgpdAnonymizationJob`**.

### Regras do LgpdAnonymizationJob
- **Gatilho:** Executado diariamente (ex: às 2h da manhã).
- **Alvo:** Grupos que atingiram exatamente o marco temporal de > 10 anos desde a data que assumiram o status `ENCERRADO_DEFINITIVO`.
- **Efeito no Grupo:** O status do grupo é alterado de `ENCERRADO` para `EXPURGADO`.

### Máscaras de Anonimização nos Clientes
A anonimização substitui chaves identificadoras para tornar impossível a reversão (desidentificação absoluta):
- `nome`: Alterado para `"ANONIMIZADO-{clienteId}"`.
- `cpf / cnpj`: Preenchido com zeros (ex: `"00000000000"`).
- `email`: Alterado para formato genérico irreversível (ex: `"expurgado-{id}@anonimizado.local"`).
- `endereco` / telefones: Conteúdo substituído pela string `"EXPURGADO"`.

*Nota: Os lançamentos contábeis e movimentações financeiras agregadas no COSIF são mantidos integralmente para fins estatísticos, já que não contêm dados pessoais identificáveis (PII).*
