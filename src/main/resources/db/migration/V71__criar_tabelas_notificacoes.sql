CREATE TABLE IF NOT EXISTS notificacoes (
    id BIGSERIAL PRIMARY KEY,
    tipo VARCHAR(50) NOT NULL,
    canal VARCHAR(20) NOT NULL DEFAULT 'EMAIL',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDENTE',
    destinatario_email VARCHAR(150),
    cliente_id BIGINT REFERENCES clientes(id),
    cota_id BIGINT REFERENCES cotas(id),
    titulo VARCHAR(200) NOT NULL,
    mensagem TEXT NOT NULL,
    data_envio TIMESTAMP,
    data_leitura TIMESTAMP,
    retentativas INT NOT NULL DEFAULT 0,
    erro_mensagem TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_notificacoes_cliente_status ON notificacoes(cliente_id, status);
CREATE INDEX IF NOT EXISTS idx_notificacoes_status_data ON notificacoes(status, data_envio);

CREATE TABLE IF NOT EXISTS preferencias_notificacao (
    id BIGSERIAL PRIMARY KEY,
    cliente_id BIGINT NOT NULL REFERENCES clientes(id),
    categoria VARCHAR(50) NOT NULL,
    habilitado BOOLEAN NOT NULL DEFAULT TRUE,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_cliente_categoria UNIQUE (cliente_id, categoria)
);