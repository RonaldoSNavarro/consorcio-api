-- Script para inserir OSAMA BIN LADEN para testes de compliance (PLD/FT)
INSERT INTO clientes (nome, cpf_cnpj, email, telefone, status, nivel_risco, data_cadastro, renda_mensal, patrimonio, patrimonio_estimado)
VALUES ('OSAMA BIN LADEN', '00011122233', 'osama@teste.com', '11999999999', 'ATIVO', 'ALTO', CURRENT_DATE, 0, 0, 0)
ON CONFLICT (cpf_cnpj) DO NOTHING;
