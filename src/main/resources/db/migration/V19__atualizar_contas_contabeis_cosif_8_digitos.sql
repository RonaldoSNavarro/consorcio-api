-- Migration para atualizar o plano de contas simplificado para as contas de 8 dígitos oficiais do COSIF (ADR 001)

-- ATIVOS
UPDATE contas_contabeis SET codigo_cosif = '1.1.1.10.00-2', nome = 'Bancos - Recursos de Grupos (Disponibilidades)' WHERE codigo_cosif = '1.1.0.00';
UPDATE contas_contabeis SET codigo_cosif = '1.2.1.10.00-8', nome = 'Valores a Receber de Consorciados' WHERE codigo_cosif = '1.2.0.00';

-- PASSIVOS (OBRIGAÇÕES COM O GRUPO / ADMINISTRADORA)
UPDATE contas_contabeis SET codigo_cosif = '2.1.2.10.10-6', nome = 'Fundo Comum de Grupos' WHERE codigo_cosif = '2.1.0.01';
UPDATE contas_contabeis SET codigo_cosif = '2.1.2.10.20-9', nome = 'Fundo de Reserva de Grupos' WHERE codigo_cosif = '2.1.0.02';
UPDATE contas_contabeis SET codigo_cosif = '2.1.2.10.30-2', nome = 'Taxa de Administração a Repassar' WHERE codigo_cosif = '2.1.0.03';
UPDATE contas_contabeis SET codigo_cosif = '2.1.2.10.40-5', nome = 'Seguros a Repassar' WHERE codigo_cosif = '2.1.0.04';

-- RECEITAS RECLASSIFICADAS COMO PASSIVO EXIGÍVEL DO GRUPO (RENDIMENTOS DO GRUPO)
UPDATE contas_contabeis SET codigo_cosif = '2.1.2.10.50-8', nome = 'Rendimentos de Aplicações Financeiras', tipo = 'PASSIVO' WHERE codigo_cosif = '3.1.0.01';
