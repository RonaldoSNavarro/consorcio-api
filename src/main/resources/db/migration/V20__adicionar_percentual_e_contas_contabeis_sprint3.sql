-- Migration Sprint 3: Adicionar contas contábeis do trânsito de crédito e reembolsos e coluna de percentual de amortização

-- Inserir novas contas contábeis COSIF oficiais
INSERT INTO contas_contabeis (codigo_cosif, nome, tipo, natureza)
SELECT '2.1.2.30.10-0', 'Créditos a Liberar (Bens/Serviços Contemplados a Liberar)', 'PASSIVO', 'CREDORA'
WHERE NOT EXISTS (SELECT 1 FROM contas_contabeis WHERE codigo_cosif = '2.1.2.30.10-0');

INSERT INTO contas_contabeis (codigo_cosif, nome, tipo, natureza)
SELECT '2.1.2.20.10-3', 'Recursos de Consorciados Excluídos a Devolver', 'PASSIVO', 'CREDORA'
WHERE NOT EXISTS (SELECT 1 FROM contas_contabeis WHERE codigo_cosif = '2.1.2.20.10-3');

-- Adicionar coluna percentual_fundo_comum na tabela parcelas
ALTER TABLE parcelas ADD COLUMN percentual_fundo_comum NUMERIC(10, 6);

-- Atualizar registros existentes calculando o percentual de amortização correspondente
UPDATE parcelas p
SET percentual_fundo_comum = ROUND(
    p.valor_fundo_comum / (
        SELECT g.valor_credito
        FROM cotas c
        JOIN grupos g ON c.grupo_id = g.id
        WHERE c.id = p.cota_id
    ), 6
);
