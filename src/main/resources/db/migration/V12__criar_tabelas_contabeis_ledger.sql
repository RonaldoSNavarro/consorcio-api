CREATE TABLE IF NOT EXISTS contas_contabeis (
    id              BIGSERIAL PRIMARY KEY,
    codigo_cosif    VARCHAR(30) UNIQUE NOT NULL,
    nome            VARCHAR(100) NOT NULL,
    tipo            VARCHAR(20) NOT NULL, -- ATIVO, PASSIVO, RECEITA, DESPESA
    natureza        VARCHAR(10) NOT NULL, -- DEVEDORA, CREDORA
    ativa           BOOLEAN NOT NULL DEFAULT TRUE,
    data_criacao    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS lancamentos_contabeis (
    id                  BIGSERIAL PRIMARY KEY,
    grupo_id            BIGINT NOT NULL REFERENCES grupos(id),
    cota_id             BIGINT REFERENCES cotas(id),
    parcela_id          BIGINT REFERENCES parcelas(id),
    
    conta_debito_id     BIGINT NOT NULL REFERENCES contas_contabeis(id),
    conta_credito_id    BIGINT NOT NULL REFERENCES contas_contabeis(id),
    
    valor               NUMERIC(38,2) NOT NULL,
    data_competencia    DATE NOT NULL,     -- Data em que o fato gerador ocorreu (Regime de Competencia)
    data_lancamento     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, -- Data real do registro no sistema
    
    tipo_operacao       VARCHAR(50) NOT NULL, -- PROVISAO, BAIXA, RENDIMENTO, ESTORNO
    historico           VARCHAR(500) NOT NULL,
    
    usuario_id          BIGINT REFERENCES usuarios(id)
);

CREATE INDEX idx_lanc_contabil_grupo ON lancamentos_contabeis(grupo_id);
CREATE INDEX idx_lanc_contabil_cota ON lancamentos_contabeis(cota_id);
CREATE INDEX idx_lanc_contabil_competencia ON lancamentos_contabeis(data_competencia);
CREATE INDEX idx_lanc_contabil_operacao ON lancamentos_contabeis(tipo_operacao);
