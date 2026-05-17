CREATE TABLE IF NOT EXISTS usuarios (
    id     BIGSERIAL PRIMARY KEY,
    login  VARCHAR(255) NOT NULL UNIQUE,
    senha  VARCHAR(255) NOT NULL
);

CREATE TABLE IF NOT EXISTS clientes (
    id             BIGSERIAL PRIMARY KEY,
    nome           VARCHAR(100) NOT NULL,
    cpf_cnpj       VARCHAR(14)  NOT NULL UNIQUE,
    email          VARCHAR(100) NOT NULL UNIQUE,
    telefone       VARCHAR(20),
    data_cadastro  DATE
);

CREATE TABLE IF NOT EXISTS grupos (
    id                  BIGSERIAL PRIMARY KEY,
    codigo              VARCHAR(255) NOT NULL UNIQUE,
    valor_credito       NUMERIC(38,2) NOT NULL,
    prazo_meses         INTEGER NOT NULL,
    taxa_administracao  NUMERIC(38,2) NOT NULL,
    status              VARCHAR(50) NOT NULL,
    data_criacao        DATE NOT NULL,
    data_inauguracao    DATE
);

CREATE TABLE IF NOT EXISTS cotas (
    id          BIGSERIAL PRIMARY KEY,
    numero_cota INTEGER NOT NULL,
    cliente_id  BIGINT NOT NULL REFERENCES clientes(id),
    grupo_id    BIGINT NOT NULL REFERENCES grupos(id),
    status      VARCHAR(50) NOT NULL
);

CREATE TABLE IF NOT EXISTS parcelas (
    id                       BIGSERIAL PRIMARY KEY,
    cota_id                  BIGINT NOT NULL REFERENCES cotas(id),
    numero_parcela           INTEGER NOT NULL,
    valor_fundo_comum        NUMERIC(38,2) NOT NULL,
    valor_taxa_administracao NUMERIC(38,2) NOT NULL,
    valor_fundo_reserva      NUMERIC(38,2) NOT NULL,
    valor_parcela            NUMERIC(38,2) NOT NULL,
    valor_multa              NUMERIC(15,2) DEFAULT 0,
    valor_juros              NUMERIC(15,2) DEFAULT 0,
    valor_pago               NUMERIC(15,2),
    data_vencimento          DATE NOT NULL,
    data_pagamento           DATE,
    status                   VARCHAR(50) NOT NULL
);

CREATE TABLE IF NOT EXISTS assembleias (
    id              BIGSERIAL PRIMARY KEY,
    grupo_id        BIGINT NOT NULL REFERENCES grupos(id),
    data_assembleia DATE NOT NULL,
    tipo            VARCHAR(50) NOT NULL
);

CREATE TABLE IF NOT EXISTS contemplacoes (
    id                    BIGSERIAL PRIMARY KEY,
    cota_id               BIGINT NOT NULL UNIQUE REFERENCES cotas(id),
    assembleia_id         BIGINT NOT NULL REFERENCES assembleias(id),
    tipo_contemplacao     VARCHAR(50) NOT NULL,
    valor_lance           NUMERIC(15,2),
    data_contemplacao     DATE NOT NULL,
    is_lance_embutido     BOOLEAN NOT NULL DEFAULT FALSE,
    valor_credito_liberado NUMERIC(38,2)
);