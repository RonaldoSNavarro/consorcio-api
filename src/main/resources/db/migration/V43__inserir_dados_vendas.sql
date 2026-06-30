-- População inicial de Categorias de Bens (Forçando ID para manter a integridade dos FKs)
INSERT INTO categorias_bem (id, nome, tipo_bacen, indice_reajuste_padrao) VALUES 
(1, 'Veículos Automotores', 'MOVEL_I', 'FIPE'),
(2, 'Imóveis', 'IMOVEL', 'INCC')
ON CONFLICT (id) DO NOTHING;

-- População inicial de Bens de Referência
INSERT INTO bens_referencia (id, categoria_bem_id, descricao, valor_atual, data_ultima_atualizacao, ativo) VALUES 
(1, 1, 'Carro Popular 1.0 (Ex: Onix, HB20)', 75000.00, CURRENT_DATE, true),
(2, 1, 'Sedan Premium (Ex: Corolla, Civic)', 150000.00, CURRENT_DATE, true),
(3, 2, 'Imóvel Residencial Padrão', 350000.00, CURRENT_DATE, true)
ON CONFLICT (id) DO NOTHING;

-- População inicial de Produtos de Consórcio
INSERT INTO produtos_consorcio (id, nome, bem_referencia_id, prazo_meses, taxa_administracao_perc, fundo_reserva_perc, ativo) VALUES 
(1, 'Plano Auto 100 meses - Popular', 1, 100, 15.00, 2.00, true),
(2, 'Plano Auto 120 meses - Premium', 2, 120, 12.00, 2.00, true),
(3, 'Plano Imóvel 240 meses', 3, 240, 20.00, 3.00, true)
ON CONFLICT (id) DO NOTHING;

-- População inicial de Tipos de Venda
INSERT INTO tipos_venda (id, nome, canal, percentual_comissao, exige_seguro, permite_reajuste, ativo, data_criacao) VALUES 
(1, 'Venda Direta', 'VENDA_DIRETA', 0.05, false, true, true, CURRENT_TIMESTAMP),
(2, 'Parceiro Comercial', 'PARCERIA_COMERCIAL', 0.10, true, true, true, CURRENT_TIMESTAMP),
(3, 'Digital', 'DIGITAL_SELF_SERVICE', 0.02, false, true, true, CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;
