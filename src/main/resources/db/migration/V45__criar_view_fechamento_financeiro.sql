CREATE MATERIALIZED VIEW vw_fechamento_grupo AS
SELECT g.id as grupo_id, 
       COALESCE(SUM(p.valor_fundo_comum), 0) as total_fundo_comum,
       COALESCE(SUM(p.valor_fundo_reserva), 0) as total_fundo_reserva,
       COALESCE(SUM(p.valor_taxa_administracao), 0) as total_taxa_adm
FROM grupos g
LEFT JOIN cotas c ON c.grupo_id = g.id
LEFT JOIN parcelas p ON p.cota_id = c.id AND p.status = 'PAGA'
GROUP BY g.id;

-- Criação de um índice único para permitir o comando REFRESH MATERIALIZED VIEW CONCURRENTLY no futuro
CREATE UNIQUE INDEX idx_vw_fechamento_grupo_id ON vw_fechamento_grupo (grupo_id);
