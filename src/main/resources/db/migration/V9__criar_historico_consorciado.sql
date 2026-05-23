CREATE TABLE IF NOT EXISTS historico_consorciado (
    id                  BIGSERIAL PRIMARY KEY,
    cliente_id          BIGINT NOT NULL REFERENCES clientes(id),
    cota_id             BIGINT REFERENCES cotas(id),
    grupo_id            BIGINT REFERENCES grupos(id),
    tipo_interacao      VARCHAR(50) NOT NULL,
    descricao           VARCHAR(1000),

    -- Snapshot financeiro no momento da interação
    valor_credito       NUMERIC(38,2),
    valor_fundo_comum   NUMERIC(38,2),
    valor_fundo_reserva NUMERIC(38,2),
    valor_seguro        NUMERIC(38,2),
    valor_categoria     NUMERIC(38,2),

    -- Dados do bem (carta de crédito)
    descricao_bem       VARCHAR(500),
    valor_bem           NUMERIC(38,2),

    -- Dados de parcela (quando aplicável)
    parcela_id          BIGINT REFERENCES parcelas(id),
    numero_parcela      INTEGER,
    valor_parcela       NUMERIC(38,2),

    data_interacao      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    usuario_id          BIGINT REFERENCES usuarios(id)
);

CREATE INDEX idx_hist_cons_cliente ON historico_consorciado(cliente_id);
CREATE INDEX idx_hist_cons_cota ON historico_consorciado(cota_id);
CREATE INDEX idx_hist_cons_tipo ON historico_consorciado(tipo_interacao);
