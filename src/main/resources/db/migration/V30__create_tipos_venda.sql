-- Sprint Wave 4 — Módulo de Vendas de Proposta
-- Cria a tabela de configuração de Tipos de Venda (canal, comissão, seguro, etc.)
CREATE TABLE tipos_venda (
    id BIGSERIAL PRIMARY KEY,
    nome VARCHAR(100) NOT NULL UNIQUE,
    descricao VARCHAR(500),
    canal VARCHAR(50) NOT NULL,
    percentual_comissao NUMERIC(5, 4) NOT NULL DEFAULT 0.05,
    exige_seguro BOOLEAN NOT NULL DEFAULT FALSE,
    permite_reajuste BOOLEAN NOT NULL DEFAULT TRUE,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    data_criacao TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Carga inicial com tipos padrão do mercado
INSERT INTO tipos_venda (nome, descricao, canal, percentual_comissao, exige_seguro, permite_reajuste) VALUES
('Venda Direta', 'Venda realizada diretamente pelo representante da administradora', 'VENDA_DIRETA', 0.0500, false, true),
('Correspondente Bancário', 'Venda via correspondente bancário credenciado pelo BACEN', 'CORRESPONDENTE_BANCARIO', 0.0300, false, true),
('Portal Digital', 'Adesão online pelo portal self-service sem intermediário humano', 'DIGITAL_SELF_SERVICE', 0.0100, false, true),
('Parceria Comercial', 'Venda via parceiro (concessionária, imobiliária, loja)', 'PARCERIA_COMERCIAL', 0.0400, true, true);
