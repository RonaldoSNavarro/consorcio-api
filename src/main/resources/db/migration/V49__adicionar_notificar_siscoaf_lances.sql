-- Adicionar coluna notificar_siscoaf na tabela lances
ALTER TABLE lances ADD COLUMN notificar_siscoaf BOOLEAN DEFAULT FALSE NOT NULL;
