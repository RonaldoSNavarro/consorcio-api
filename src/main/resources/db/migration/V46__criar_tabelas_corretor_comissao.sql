CREATE TABLE corretores (
    id BIGSERIAL PRIMARY KEY,
    nome VARCHAR(100) NOT NULL,
    documento VARCHAR(14) NOT NULL UNIQUE,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    data_criacao TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE comissoes_venda (
    id BIGSERIAL PRIMARY KEY,
    corretor_id BIGINT NOT NULL,
    contrato_id BIGINT NOT NULL,
    valor_total_comissao DECIMAL(12, 2) NOT NULL,
    status VARCHAR(20) NOT NULL,
    data_geracao TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_comissao_corretor FOREIGN KEY (corretor_id) REFERENCES corretores(id),
    CONSTRAINT fk_comissao_contrato FOREIGN KEY (contrato_id) REFERENCES contratos_adesao(id)
);

ALTER TABLE propostas_adesao ADD COLUMN corretor_id BIGINT;
ALTER TABLE propostas_adesao ADD CONSTRAINT fk_proposta_corretor FOREIGN KEY (corretor_id) REFERENCES corretores(id);
