CREATE TABLE indices_economicos_historico (
    id BIGSERIAL PRIMARY KEY,
    tipo_indice VARCHAR(20) NOT NULL,
    data_referencia DATE NOT NULL,
    valor_percentual NUMERIC(10, 4) NOT NULL,
    data_captura TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_tipo_data_referencia UNIQUE (tipo_indice, data_referencia)
);

CREATE INDEX idx_indices_tipo_data ON indices_economicos_historico(tipo_indice, data_referencia DESC);
