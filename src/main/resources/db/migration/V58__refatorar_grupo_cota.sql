-- Refatoração de Grupo
ALTER TABLE grupos DROP COLUMN valor_credito;
ALTER TABLE grupos DROP COLUMN prazo_meses;

ALTER TABLE grupos ADD COLUMN quantidade_cotas INTEGER NOT NULL DEFAULT 100;
ALTER TABLE grupos ADD COLUMN dia_base_assembleias INTEGER NOT NULL DEFAULT 10;
ALTER TABLE grupos ADD COLUMN dias_antecedencia_vencimento INTEGER NOT NULL DEFAULT 5;
ALTER TABLE grupos ADD COLUMN prazo_maximo_meses INTEGER NOT NULL DEFAULT 60;

-- Tabelas de coleções do Grupo
CREATE TABLE grupo_prazos_permitidos (
    grupo_id BIGINT NOT NULL REFERENCES grupos(id),
    prazo_meses INTEGER NOT NULL
);

CREATE TABLE grupo_bens_permitidos (
    grupo_id BIGINT NOT NULL REFERENCES grupos(id),
    bem_referencia_id BIGINT NOT NULL REFERENCES bens_referencia(id),
    PRIMARY KEY (grupo_id, bem_referencia_id)
);

-- Refatoração de Cota
ALTER TABLE cotas ALTER COLUMN cliente_id DROP NOT NULL;

-- Contrato de adesão não existe na cota se não foi vendida
ALTER TABLE cotas ALTER COLUMN contrato_adesao_id DROP NOT NULL;

ALTER TABLE cotas ADD COLUMN bem_referencia_id BIGINT REFERENCES bens_referencia(id);
ALTER TABLE cotas ADD COLUMN prazo_meses INTEGER;
