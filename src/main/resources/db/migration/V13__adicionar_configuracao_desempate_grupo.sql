ALTER TABLE grupos
ADD COLUMN criterio_desempate_lance VARCHAR(50) NOT NULL DEFAULT 'LOTERIA_FEDERAL';

-- Comentário para documentar que pode ser configurável (ex: LOTERIA_FEDERAL, ORDEM_OFERTA, MAIOR_LANCE_ACUMULADO)
COMMENT ON COLUMN grupos.criterio_desempate_lance IS 'Define a regra de desempate para lances de mesmo percentual';
