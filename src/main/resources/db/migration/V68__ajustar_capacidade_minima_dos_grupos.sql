-- A capacidade configurada nunca pode ser menor que as cotas físicas já criadas.
-- Preserva grupos legados que receberam o default histórico de capacidade.
UPDATE grupos g
SET quantidade_cotas = GREATEST(
    g.quantidade_cotas,
    COALESCE((SELECT COUNT(*)::integer FROM cotas c WHERE c.grupo_id = g.id), 0)
);
