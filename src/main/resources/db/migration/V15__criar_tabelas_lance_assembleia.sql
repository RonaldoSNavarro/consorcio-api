ALTER TABLE assembleias ADD COLUMN data_inicio_captacao TIMESTAMP;
ALTER TABLE assembleias ADD COLUMN data_fim_captacao TIMESTAMP;
ALTER TABLE assembleias ADD COLUMN status VARCHAR(50) DEFAULT 'AGENDADA' NOT NULL;
ALTER TABLE assembleias ADD COLUMN version BIGINT DEFAULT 0 NOT NULL;

CREATE TABLE lances (
    id BIGSERIAL PRIMARY KEY,
    cota_id BIGINT NOT NULL REFERENCES cotas(id),
    assembleia_id BIGINT NOT NULL REFERENCES assembleias(id),
    tipo VARCHAR(50) NOT NULL,
    valor_oferta NUMERIC(19,4) NOT NULL,
    data_oferta TIMESTAMP NOT NULL,
    status_apuracao VARCHAR(50) NOT NULL,
    version BIGINT DEFAULT 0 NOT NULL,
    CONSTRAINT uc_cota_assembleia UNIQUE (cota_id, assembleia_id)
);
