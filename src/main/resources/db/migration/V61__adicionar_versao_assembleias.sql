-- Migration para garantir que a coluna versao (JPA @Version Optimistic Lock) existe na tabela assembleias
ALTER TABLE assembleias ADD COLUMN IF NOT EXISTS versao BIGINT NOT NULL DEFAULT 0;
