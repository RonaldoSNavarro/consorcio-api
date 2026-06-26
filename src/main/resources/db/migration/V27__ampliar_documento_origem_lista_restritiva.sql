-- Sprint 7 — Bug Fix: Ampliar coluna documento_origem de VARCHAR(50) para TEXT
-- Motivo: Strings compostas do IBGE ("IBGE:UF:MUNICIPIO_LONGO:GEMEA:CIDADE_GEMEA") e documentos ONU
-- podem exceder 50 caracteres, causando erro 'value too long for type character varying(50)'
ALTER TABLE listas_restritivas ALTER COLUMN documento_origem TYPE TEXT;
