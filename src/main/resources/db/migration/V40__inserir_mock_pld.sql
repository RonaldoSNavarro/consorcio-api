-- Script para inserir dados falsos de PLD/FT
INSERT INTO grupos (codigo_grupo, taxa_administracao, prazo_meses, valor_credito, status, data_criacao)
VALUES ('GRUPO_PLD_FT', 15.00, 120, 200000.00, 'EM_ANDAMENTO', CURRENT_DATE - 30)
ON CONFLICT DO NOTHING;

INSERT INTO clientes (nome, cpf_cnpj, email, telefone, status, nivel_risco, data_cadastro, renda_mensal, patrimonio, patrimonio_estimado)
VALUES ('CLIENTE PLD FT', '98765432100', 'pldft@teste.com', '11988888888', 'ATIVO', 'ALTO', CURRENT_DATE, 50000.00, 1000000.00, 1000000.00)
ON CONFLICT (cpf_cnpj) DO NOTHING;

INSERT INTO cotas (grupo_id, cliente_id, codigo_cota, status, versao)
SELECT g.id, c.id, 999, 'ATIVA', 0
FROM grupos g, clientes c
WHERE g.codigo_grupo = 'GRUPO_PLD_FT' AND c.cpf_cnpj = '98765432100'
ON CONFLICT DO NOTHING;

INSERT INTO assembleias (grupo_id, data_assembleia, tipo, status, versao)
SELECT g.id, CURRENT_DATE - 5, 'ORDINARIA', 'REALIZADA', 0
FROM grupos g
WHERE g.codigo_grupo = 'GRUPO_PLD_FT'
ON CONFLICT DO NOTHING;

INSERT INTO lances (cota_id, assembleia_id, tipo, modalidade, valor_oferta, data_oferta, status_apuracao)
SELECT co.id, a.id, 'LANCE_LIVRE', 'FINANCEIRO', 80000.00, CURRENT_DATE - 6, 'VENCEDOR'
FROM cotas co
JOIN assembleias a ON a.grupo_id = co.grupo_id
JOIN grupos g ON g.id = a.grupo_id
WHERE g.codigo_grupo = 'GRUPO_PLD_FT'
ON CONFLICT DO NOTHING;
