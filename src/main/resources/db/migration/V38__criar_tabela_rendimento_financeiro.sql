CREATE TABLE rendimentos_financeiros (
    id BIGSERIAL PRIMARY KEY,
    grupo_id BIGINT NOT NULL REFERENCES grupos(id),
    valor_rendimento NUMERIC(19, 2) NOT NULL,
    data_rendimento DATE NOT NULL,
    descricao VARCHAR(255)
);
