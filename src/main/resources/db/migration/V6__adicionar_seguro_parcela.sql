-- Novo componente: Seguros (obrigatório na composição da parcela)
ALTER TABLE parcelas ADD COLUMN valor_seguro NUMERIC(38,2) NOT NULL DEFAULT 0;
