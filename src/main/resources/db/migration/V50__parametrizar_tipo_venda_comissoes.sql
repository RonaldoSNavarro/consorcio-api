ALTER TABLE tipos_venda ADD COLUMN parcela_um_zero_fundo_comum BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE tipos_venda ADD COLUMN liberacao_comissao_imediata BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE tipos_venda ADD COLUMN meses_garantia_comissao INTEGER NOT NULL DEFAULT 0;
ALTER TABLE tipos_venda ADD COLUMN percentual_estorno NUMERIC(5,4) NOT NULL DEFAULT 0.0000;

ALTER TABLE corretores ADD COLUMN saldo_devedor NUMERIC(12,2) NOT NULL DEFAULT 0.00;
