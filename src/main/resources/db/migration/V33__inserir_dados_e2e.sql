-- Script para inserir um grupo e cota para o E2E
INSERT INTO grupos (codigo, taxa_administracao, prazo_meses, valor_credito, status, data_criacao)
VALUES ('E2E_GRUPO_50K', 10.00, 60, 50000.00, 'EM_FORMACAO', CURRENT_DATE)
ON CONFLICT DO NOTHING;

INSERT INTO clientes (nome, cpf_cnpj, email, telefone, status, nivel_risco, data_cadastro, renda_mensal, patrimonio, patrimonio_estimado)
VALUES ('CLIENTE COMUM E2E', '12345678909', 'e2e@teste.com', '11999999999', 'ATIVO', 'BAIXO', CURRENT_DATE, 5000.00, 100000.00, 100000.00)
ON CONFLICT (cpf_cnpj) DO NOTHING;
