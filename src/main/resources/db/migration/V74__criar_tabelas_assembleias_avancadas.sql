-- Tabela de Credenciamento Prévio de Lances
CREATE TABLE IF NOT EXISTS credenciamentos_lances (
    id BIGSERIAL PRIMARY KEY,
    cota_id BIGINT NOT NULL REFERENCES cotas(id),
    assembleia_id BIGINT NOT NULL REFERENCES assembleias(id),
    tipo_lance VARCHAR(30) NOT NULL,
    modalidade VARCHAR(30) NOT NULL,
    valor_lance NUMERIC(15, 2) NOT NULL,
    percentual_lance NUMERIC(8, 4),
    valor_fgts NUMERIC(15, 2) DEFAULT 0.00,
    valor_embutido NUMERIC(15, 2) DEFAULT 0.00,
    valor_proprio NUMERIC(15, 2) DEFAULT 0.00,
    status VARCHAR(30) NOT NULL DEFAULT 'ATIVO',
    hash_assinatura VARCHAR(256),
    ip_origem VARCHAR(50),
    termos_aceitos BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_credenciamentos_cota ON credenciamentos_lances(cota_id);
CREATE INDEX IF NOT EXISTS idx_credenciamentos_assembleia ON credenciamentos_lances(assembleia_id);
CREATE INDEX IF NOT EXISTS idx_credenciamentos_status ON credenciamentos_lances(status);

-- Adição de colunas para auditoria criptográfica e múltiplos sorteios
ALTER TABLE assembleias ADD COLUMN IF NOT EXISTS tipo_assembleia VARCHAR(30) DEFAULT 'ORDINARIA';
ALTER TABLE assembleias ADD COLUMN IF NOT EXISTS hash_apuracao_sha256 VARCHAR(64);
ALTER TABLE assembleias ADD COLUMN IF NOT EXISTS quorum_presente NUMERIC(8, 4);

ALTER TABLE grupos ADD COLUMN IF NOT EXISTS max_sorteios_ativos INTEGER DEFAULT 1;
ALTER TABLE grupos ADD COLUMN IF NOT EXISTS percentual_saldo_excluidos NUMERIC(5, 2) DEFAULT 10.00;
ALTER TABLE grupos ADD COLUMN IF NOT EXISTS quorum_minimo_instalacao NUMERIC(5, 2) DEFAULT 50.00;