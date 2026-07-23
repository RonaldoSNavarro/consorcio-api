ALTER TABLE bens_referencia ADD COLUMN IF NOT EXISTS codigo_fipe VARCHAR(20);

CREATE TABLE IF NOT EXISTS historico_valores_bem_referencia (
    id BIGSERIAL PRIMARY KEY,
    bem_referencia_id BIGINT NOT NULL,
    valor_anterior NUMERIC(15, 2) NOT NULL,
    valor_novo NUMERIC(15, 2) NOT NULL,
    origem_reajuste VARCHAR(30) NOT NULL,
    codigo_fipe VARCHAR(20),
    data_atualizacao TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_historico_bem_referencia FOREIGN KEY (bem_referencia_id) REFERENCES bens_referencia(id) ON DELETE CASCADE
);
