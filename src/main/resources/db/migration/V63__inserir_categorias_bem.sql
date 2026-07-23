-- Garantir existência de todas as 4 categorias de bem exigidas pelo BACEN
INSERT INTO categorias_bem (id, nome, tipo_bacen, indice_reajuste_padrao) VALUES 
(1, 'Veículos Automotores', 'BEM_MOVEL_I', 'FIPE'),
(2, 'Imóveis', 'BEM_IMOVEL', 'INCC'),
(3, 'Serviços', 'SERVICO', 'IPCA'),
(4, 'Outros Bens Móveis', 'BEM_MOVEL_II', 'IPCA')
ON CONFLICT (id) DO UPDATE SET 
    nome = EXCLUDED.nome,
    tipo_bacen = EXCLUDED.tipo_bacen,
    indice_reajuste_padrao = EXCLUDED.indice_reajuste_padrao;
