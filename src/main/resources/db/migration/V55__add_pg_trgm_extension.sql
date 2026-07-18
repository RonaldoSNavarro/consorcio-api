CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- Create trigram expression indexes to speed up the similarity search for UPPERCASE matching
CREATE INDEX IF NOT EXISTS idx_clientes_nome_trgm ON clientes USING GIN (UPPER(nome) gin_trgm_ops);
CREATE INDEX IF NOT EXISTS idx_listas_restritivas_nome_trgm ON listas_restritivas USING GIN (UPPER(nome) gin_trgm_ops);
