-- Provisiona e normaliza idempotentemente as 12 contas usadas pelo domínio.
INSERT INTO contas_contabeis (codigo_cosif, nome, tipo, natureza, ativa)
VALUES
    ('1.1.1.10.00-2', 'Bancos - Recursos de Grupos (Disponibilidades)', 'ATIVO', 'DEVEDORA', TRUE),
    ('1.2.1.10.00-8', 'Valores a Receber de Consorciados', 'ATIVO', 'DEVEDORA', TRUE),
    ('1.6.9.10.00-5', 'Provisao para Creditos de Liquidacao Duvidosa (PDD)', 'ATIVO', 'CREDORA', TRUE),
    ('2.1.2.10.10-6', 'Fundo Comum de Grupos', 'PASSIVO', 'CREDORA', TRUE),
    ('2.1.2.10.20-9', 'Fundo de Reserva de Grupos', 'PASSIVO', 'CREDORA', TRUE),
    ('2.1.2.10.30-2', 'Taxa de Administracao a Repassar', 'PASSIVO', 'CREDORA', TRUE),
    ('2.1.2.10.40-5', 'Seguros a Repassar', 'PASSIVO', 'CREDORA', TRUE),
    ('2.1.2.10.50-8', 'Rendimentos de Aplicacoes Financeiras', 'PASSIVO', 'CREDORA', TRUE),
    ('2.1.2.20.10-3', 'Recursos de Consorciados Excluidos a Devolver', 'PASSIVO', 'CREDORA', TRUE),
    ('2.1.2.30.10-0', 'Creditos a Liberar - Bens e Servicos', 'PASSIVO', 'CREDORA', TRUE),
    ('2.1.2.90.10-8', 'Recursos Nao Procurados (RNP)', 'PASSIVO', 'CREDORA', TRUE),
    ('3.1.8.10.00-1', 'Despesas de Provisao para Devedores Duvidosos', 'DESPESA', 'DEVEDORA', TRUE)
ON CONFLICT (codigo_cosif) DO UPDATE
SET nome = EXCLUDED.nome,
    tipo = EXCLUDED.tipo,
    natureza = EXCLUDED.natureza,
    ativa = TRUE;
