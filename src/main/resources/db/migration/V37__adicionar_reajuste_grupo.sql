-- Adiciona colunas para regra de reajuste do grupo (GAP 9)
ALTER TABLE grupos 
ADD COLUMN indice_reajuste VARCHAR(20),
ADD COLUMN mes_reajuste INTEGER;
