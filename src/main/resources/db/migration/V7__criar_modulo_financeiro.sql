CREATE TABLE IF NOT EXISTS movimentos_financeiros (
    id                  BIGSERIAL PRIMARY KEY,
    grupo_id            BIGINT NOT NULL REFERENCES grupos(id),
    cota_id             BIGINT REFERENCES cotas(id),
    parcela_id          BIGINT REFERENCES parcelas(id),
    contemplacao_id     BIGINT REFERENCES contemplacoes(id),
    tipo_movimento      VARCHAR(50) NOT NULL,
    natureza            VARCHAR(10) NOT NULL,
    valor               NUMERIC(38,2) NOT NULL,
    saldo_anterior      NUMERIC(38,2),
    saldo_posterior     NUMERIC(38,2),
    descricao           VARCHAR(500),
    data_movimento      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    data_referencia     DATE,
    usuario_id          BIGINT REFERENCES usuarios(id)
);

CREATE INDEX idx_mov_fin_grupo ON movimentos_financeiros(grupo_id);
CREATE INDEX idx_mov_fin_cota ON movimentos_financeiros(cota_id);
CREATE INDEX idx_mov_fin_tipo ON movimentos_financeiros(tipo_movimento);
CREATE INDEX idx_mov_fin_data ON movimentos_financeiros(data_movimento);
