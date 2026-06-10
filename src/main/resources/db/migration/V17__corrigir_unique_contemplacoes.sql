-- FC-07 FIX: Remove a constraint UNIQUE de cota_id na tabela contemplacoes.
-- Uma mesma cota pode ser contemplada em cenários como:
--   1. Re-contemplação após reprovação de análise de crédito
--   2. Transferência de cota entre grupos
--   3. Cota cancelada contemplada por sorteio para restituição
ALTER TABLE contemplacoes DROP CONSTRAINT IF EXISTS contemplacoes_cota_id_key;
