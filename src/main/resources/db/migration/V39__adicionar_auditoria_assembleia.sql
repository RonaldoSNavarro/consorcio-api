-- Adiciona colunas para auditoria de assembleia (GAP 11)
ALTER TABLE assembleias
ADD COLUMN numero_extracao_loteria VARCHAR(50),
ADD COLUMN algoritmo_usado VARCHAR(50),
ADD COLUMN pedra_chave_calculada INTEGER,
ADD COLUMN fallbacks_aplicados INTEGER DEFAULT 0;
