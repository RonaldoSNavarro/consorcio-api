-- Adiciona os novos campos de parâmetros de sorteio na tabela grupos
ALTER TABLE grupos ADD COLUMN algoritmo_pedra_chave VARCHAR(255) DEFAULT 'CENTENA' NOT NULL;
ALTER TABLE grupos ADD COLUMN direcao_fallback_sorteio VARCHAR(255) DEFAULT 'ACIMA_DEPOIS_ABAIXO' NOT NULL;
