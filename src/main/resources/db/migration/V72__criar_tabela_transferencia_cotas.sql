CREATE TABLE IF NOT EXISTS transferencia_cotas (
    id BIGSERIAL PRIMARY KEY,
    cota_id BIGINT NOT NULL REFERENCES cotas(id),
    cedente_id BIGINT NOT NULL REFERENCES clientes(id),
    cessionario_id BIGINT NOT NULL REFERENCES clientes(id),
    data_solicitacao TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    data_efetivacao TIMESTAMP,
    taxa_transferencia NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    status VARCHAR(50) NOT NULL DEFAULT 'SOLICITADA',
    motivo_recusa TEXT,
    observacao TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_transf_cota ON transferencia_cotas(cota_id);
CREATE INDEX IF NOT EXISTS idx_transf_status ON transferencia_cotas(status);