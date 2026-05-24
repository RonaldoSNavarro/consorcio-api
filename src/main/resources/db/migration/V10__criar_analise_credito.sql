CREATE TABLE analises_credito (
    id BIGSERIAL PRIMARY KEY,
    cota_id BIGINT NOT NULL,
    renda_comprovada NUMERIC(38, 2) NOT NULL,
    garantia_aprovada BOOLEAN NOT NULL DEFAULT FALSE,
    status VARCHAR(20) NOT NULL,
    data_analise DATE NOT NULL,
    observacao TEXT,
    versao INT NOT NULL DEFAULT 0,
    CONSTRAINT fk_analise_cota FOREIGN KEY (cota_id) REFERENCES cotas (id)
);

-- Atualiza restrição de check se existir para o status_cota, caso estivessemos usando CHECK. Como estamos usando VARCHAR com Enum no JPA, não há CHECK no banco padrão, mas se houvesse, colocaríamos aqui.
