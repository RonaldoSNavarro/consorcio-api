-- Versão 0 = cota em situação normal, Versão 1 = cota excluída
ALTER TABLE cotas ADD COLUMN versao INTEGER NOT NULL DEFAULT 0;

CREATE TABLE IF NOT EXISTS historico_versoes_cota (
    id              BIGSERIAL PRIMARY KEY,
    cota_id         BIGINT NOT NULL REFERENCES cotas(id),
    versao          INTEGER NOT NULL,
    status_anterior VARCHAR(50),
    status_novo     VARCHAR(50) NOT NULL,
    motivo          VARCHAR(500),
    data_transicao  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    usuario_id      BIGINT REFERENCES usuarios(id)
);

CREATE INDEX idx_hist_versao_cota ON historico_versoes_cota(cota_id);
