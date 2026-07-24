ALTER TABLE propostas_adesao ADD COLUMN grupo_id BIGINT REFERENCES grupos(id);
ALTER TABLE propostas_adesao ADD COLUMN codigo_grupo VARCHAR(50);
