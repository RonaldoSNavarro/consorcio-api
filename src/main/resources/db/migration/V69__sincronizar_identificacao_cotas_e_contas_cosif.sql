-- Operational identifiers keep direct query fields while the foreign keys remain authoritative.
ALTER TABLE cotas ADD COLUMN IF NOT EXISTS codigo_grupo VARCHAR(255);
ALTER TABLE cotas ADD COLUMN IF NOT EXISTS nome_cliente VARCHAR(255);
ALTER TABLE cotas ADD COLUMN IF NOT EXISTS cpf_cliente VARCHAR(20);

UPDATE cotas c
SET codigo_grupo = g.codigo_grupo
FROM grupos g
WHERE c.grupo_id = g.id
  AND c.codigo_grupo IS DISTINCT FROM g.codigo_grupo;

UPDATE cotas c
SET nome_cliente = cli.nome,
    cpf_cliente = cli.cpf_cnpj
FROM clientes cli
WHERE c.cliente_id = cli.id
  AND (c.nome_cliente IS DISTINCT FROM cli.nome OR c.cpf_cliente IS DISTINCT FROM cli.cpf_cnpj);

ALTER TABLE cotas ALTER COLUMN codigo_grupo SET NOT NULL;
CREATE INDEX IF NOT EXISTS idx_cotas_codigo_grupo_codigo_cota ON cotas (codigo_grupo, codigo_cota);
CREATE INDEX IF NOT EXISTS idx_cotas_cpf_cliente ON cotas (cpf_cliente);

CREATE OR REPLACE FUNCTION sincronizar_referencias_cota()
RETURNS TRIGGER AS $$
BEGIN
    SELECT codigo_grupo INTO NEW.codigo_grupo FROM grupos WHERE id = NEW.grupo_id;

    IF NEW.cliente_id IS NULL THEN
        NEW.nome_cliente := NULL;
        NEW.cpf_cliente := NULL;
    ELSE
        SELECT nome, cpf_cnpj INTO NEW.nome_cliente, NEW.cpf_cliente FROM clientes WHERE id = NEW.cliente_id;
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_cotas_sincronizar_referencias ON cotas;
CREATE TRIGGER trg_cotas_sincronizar_referencias
BEFORE INSERT OR UPDATE OF grupo_id, cliente_id ON cotas
FOR EACH ROW EXECUTE FUNCTION sincronizar_referencias_cota();

CREATE OR REPLACE FUNCTION propagar_codigo_grupo_para_cotas()
RETURNS TRIGGER AS $$
BEGIN
    UPDATE cotas SET codigo_grupo = NEW.codigo_grupo WHERE grupo_id = NEW.id;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_grupos_propagam_codigo_para_cotas ON grupos;
CREATE TRIGGER trg_grupos_propagam_codigo_para_cotas
AFTER UPDATE OF codigo_grupo ON grupos
FOR EACH ROW EXECUTE FUNCTION propagar_codigo_grupo_para_cotas();

CREATE OR REPLACE FUNCTION propagar_cliente_para_cotas()
RETURNS TRIGGER AS $$
BEGIN
    UPDATE cotas SET nome_cliente = NEW.nome, cpf_cliente = NEW.cpf_cnpj WHERE cliente_id = NEW.id;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_clientes_propagam_identificacao_para_cotas ON clientes;
CREATE TRIGGER trg_clientes_propagam_identificacao_para_cotas
AFTER UPDATE OF nome, cpf_cnpj ON clientes
FOR EACH ROW EXECUTE FUNCTION propagar_cliente_para_cotas();

-- The intended capacity default is 1,000. Normalize only historical implicit defaults.
ALTER TABLE grupos ALTER COLUMN quantidade_cotas SET DEFAULT 1000;
UPDATE grupos
SET quantidade_cotas = 1000
WHERE quantidade_cotas IN (100, 120);

-- Idempotent seed of every account used by ParcelaService.pagar().
INSERT INTO contas_contabeis (codigo_cosif, nome, tipo, natureza)
VALUES
    ('1.1.1.10.00-2', 'Bancos - Recursos de Grupos (Disponibilidades)', 'ATIVO', 'DEVEDORA'),
    ('1.2.1.10.00-8', 'Valores a Receber de Consorciados', 'ATIVO', 'DEVEDORA'),
    ('2.1.2.10.10-6', 'Fundo Comum de Grupos', 'PASSIVO', 'CREDORA'),
    ('2.1.2.10.20-9', 'Fundo de Reserva de Grupos', 'PASSIVO', 'CREDORA'),
    ('2.1.2.10.30-2', 'Taxa de Administracao a Repassar', 'PASSIVO', 'CREDORA'),
    ('2.1.2.10.40-5', 'Seguros a Repassar', 'PASSIVO', 'CREDORA')
ON CONFLICT (codigo_cosif) DO NOTHING;
