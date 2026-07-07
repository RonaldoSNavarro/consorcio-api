-- Corrigir valores de enums invalidos inseridos no mock do PLD (V40)
UPDATE lances
SET tipo = 'FIRME', modalidade = 'LIVRE'
WHERE tipo = 'LANCE_LIVRE' AND modalidade = 'FINANCEIRO';
