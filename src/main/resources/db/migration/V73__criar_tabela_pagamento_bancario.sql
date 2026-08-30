CREATE TABLE IF NOT EXISTS pagamentos_bancarios (
    id BIGSERIAL PRIMARY KEY,
    cota_id BIGINT NOT NULL REFERENCES cotas(id),
    parcela_id BIGINT REFERENCES parcelas(id),
    codigo_barras VARCHAR(100),
    linha_digitavel VARCHAR(100),
    txid VARCHAR(100) UNIQUE,
    pix_copia_cola TEXT,
    qr_code_base64 TEXT,
    status VARCHAR(30) NOT NULL DEFAULT 'AGUARDANDO_PAGAMENTO',
    valor_cobrado NUMERIC(15, 2) NOT NULL,
    valor_pago NUMERIC(15, 2),
    taxa_bancaria NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    data_vencimento DATE NOT NULL,
    data_pagamento TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_pagamentos_cota ON pagamentos_bancarios(cota_id);
CREATE INDEX IF NOT EXISTS idx_pagamentos_parcela ON pagamentos_bancarios(parcela_id);
CREATE INDEX IF NOT EXISTS idx_pagamentos_txid ON pagamentos_bancarios(txid);
CREATE INDEX IF NOT EXISTS idx_pagamentos_status ON pagamentos_bancarios(status);