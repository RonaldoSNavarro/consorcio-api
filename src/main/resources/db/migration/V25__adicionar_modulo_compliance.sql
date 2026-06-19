-- Sprint 6: Módulo de Compliance e PLD/FT
-- Adiciona campos de renda e patrimônio no Cliente
ALTER TABLE clientes ADD COLUMN renda_mensal_declarada DECIMAL(19, 2);
ALTER TABLE clientes ADD COLUMN patrimonio_estimado DECIMAL(19, 2);

-- Tabela para listas de sanções (OFAC, ONU, PEP)
CREATE TABLE listas_restritivas (
    id BIGSERIAL PRIMARY KEY,
    nome VARCHAR(255) NOT NULL,
    documento_origem VARCHAR(50),
    origem VARCHAR(50) NOT NULL,
    data_inclusao TIMESTAMP NOT NULL
);

-- Tabela para alertas gerados no cruzamento de dados
CREATE TABLE alertas_compliance (
    id BIGSERIAL PRIMARY KEY,
    cliente_id BIGINT NOT NULL,
    lista_id BIGINT NOT NULL,
    score DECIMAL(5, 4) NOT NULL,
    status VARCHAR(50) NOT NULL,
    justificativa TEXT,
    data_deteccao TIMESTAMP NOT NULL,
    CONSTRAINT fk_alerta_cliente FOREIGN KEY (cliente_id) REFERENCES clientes (id),
    CONSTRAINT fk_alerta_lista FOREIGN KEY (lista_id) REFERENCES listas_restritivas (id)
);
