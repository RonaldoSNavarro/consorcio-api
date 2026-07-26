ALTER TABLE lances
    ADD COLUMN data_liquidacao TIMESTAMP,
    ADD COLUMN tipo_amortizacao VARCHAR(30),
    ADD COLUMN amortizacao_aplicada BOOLEAN NOT NULL DEFAULT FALSE;
