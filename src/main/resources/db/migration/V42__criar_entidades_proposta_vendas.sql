CREATE TABLE categorias_bem (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nome VARCHAR(255) NOT NULL,
    tipo_bacen VARCHAR(50) NOT NULL,
    indice_reajuste_padrao VARCHAR(20) NOT NULL
);

CREATE TABLE bens_referencia (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    categoria_bem_id BIGINT NOT NULL,
    descricao VARCHAR(255) NOT NULL,
    valor_atual DECIMAL(15, 2) NOT NULL,
    data_ultima_atualizacao DATE,
    ativo BOOLEAN DEFAULT TRUE,
    FOREIGN KEY (categoria_bem_id) REFERENCES categorias_bem(id)
);

CREATE TABLE produtos_consorcio (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nome VARCHAR(255) NOT NULL,
    bem_referencia_id BIGINT NOT NULL,
    prazo_meses INT NOT NULL,
    taxa_administracao_perc DECIMAL(5, 2) NOT NULL,
    fundo_reserva_perc DECIMAL(5, 2) NOT NULL,
    ativo BOOLEAN DEFAULT TRUE,
    FOREIGN KEY (bem_referencia_id) REFERENCES bens_referencia(id)
);

CREATE TABLE propostas_adesao (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    numero_proposta VARCHAR(50) UNIQUE NOT NULL,
    cliente_id BIGINT NOT NULL,
    produto_id BIGINT NOT NULL,
    tipo_venda_id BIGINT NOT NULL,
    valor_credito_solicitado DECIMAL(15, 2) NOT NULL,
    status VARCHAR(50) NOT NULL, -- EM_ANALISE, APROVADA, RECUSADA, CANCELADA
    data_proposta TIMESTAMP NOT NULL,
    data_atualizacao TIMESTAMP,
    FOREIGN KEY (cliente_id) REFERENCES clientes(id),
    FOREIGN KEY (produto_id) REFERENCES produtos_consorcio(id),
    FOREIGN KEY (tipo_venda_id) REFERENCES tipos_venda(id)
);

CREATE TABLE contratos_adesao (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    numero_contrato VARCHAR(50) UNIQUE NOT NULL,
    proposta_id BIGINT NOT NULL UNIQUE,
    data_assinatura TIMESTAMP,
    ip_assinatura VARCHAR(45),
    status VARCHAR(50) NOT NULL, -- PENDENTE_PAGAMENTO, EFETIVADO, CANCELADO, TRANSFERIDO
    FOREIGN KEY (proposta_id) REFERENCES propostas_adesao(id)
);

ALTER TABLE cotas
ADD COLUMN contrato_adesao_id BIGINT UNIQUE,
ADD FOREIGN KEY (contrato_adesao_id) REFERENCES contratos_adesao(id);
