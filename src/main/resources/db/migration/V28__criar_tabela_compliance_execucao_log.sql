CREATE TABLE compliance_execucao_log (
    id BIGSERIAL PRIMARY KEY,
    data_execucao TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    trigger_execucao VARCHAR(20) NOT NULL,
    ofac_status VARCHAR(20),
    pep_registros INTEGER,
    onu_registros INTEGER,
    ibge_registros INTEGER,
    ofac_registros INTEGER,
    duracao_ms BIGINT,
    erros TEXT
);
