-- Garantir que OSAMA BIN LADEN esteja na base para testes E2E
INSERT INTO clientes (nome, cpf_cnpj, email, telefone, status, nivel_risco, data_cadastro, renda_mensal, patrimonio, patrimonio_estimado)
VALUES ('OSAMA BIN LADEN', '00011122299', 'osama_' || extract(epoch from now())::int || '@teste.com', '11999999999', 'ATIVO', 'ALTO', CURRENT_DATE, 0, 0, 0)
ON CONFLICT (cpf_cnpj) DO NOTHING;
