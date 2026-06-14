-- Migration Sprint 4: Contas contábeis para encerramento de grupo (ADR 006) e RNP

-- Conta de Provisão para Créditos de Liquidação Duvidosa (PDD) — Retificadora de Ativo
INSERT INTO contas_contabeis (codigo_cosif, nome, tipo, natureza)
SELECT '1.6.9.10.00-5', '(-) Provisão para Créditos de Liquidação Duvidosa (PDD)', 'ATIVO', 'CREDORA'
WHERE NOT EXISTS (SELECT 1 FROM contas_contabeis WHERE codigo_cosif = '1.6.9.10.00-5');

-- Conta de Despesa de Provisão PDD — Resultado (Despesa)
INSERT INTO contas_contabeis (codigo_cosif, nome, tipo, natureza)
SELECT '3.1.8.10.00-1', 'Despesas de Provisão para Devedores Duvidosos', 'DESPESA', 'DEVEDORA'
WHERE NOT EXISTS (SELECT 1 FROM contas_contabeis WHERE codigo_cosif = '3.1.8.10.00-1');

-- Conta de Recursos Não Procurados (RNP) — Passivo
INSERT INTO contas_contabeis (codigo_cosif, nome, tipo, natureza)
SELECT '2.1.2.90.10-8', 'Recursos Não Procurados (RNP)', 'PASSIVO', 'CREDORA'
WHERE NOT EXISTS (SELECT 1 FROM contas_contabeis WHERE codigo_cosif = '2.1.2.90.10-8');
