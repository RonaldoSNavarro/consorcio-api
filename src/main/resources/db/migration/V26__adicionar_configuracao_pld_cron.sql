-- Sprint 6: Módulo de Compliance e PLD/FT
-- Tabela de configuração do job de compliance (cron dinâmico)
CREATE TABLE compliance_config (
    id BIGINT PRIMARY KEY,
    cron_expression VARCHAR(100) NOT NULL,
    frequencia VARCHAR(50) NOT NULL,
    horario VARCHAR(10) NOT NULL,
    data_atualizacao TIMESTAMP NOT NULL
);

INSERT INTO compliance_config (id, cron_expression, frequencia, horario, data_atualizacao)
VALUES (1, '0 0 3 * * *', 'DIARIO', '03:00', CURRENT_TIMESTAMP);
