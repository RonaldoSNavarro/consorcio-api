-- Repara bancos que já possuíam o histórico da V66 sem as colunas físicas.
-- A operação é segura para instalações novas, nas quais a V66 já as criou.
ALTER TABLE lances
    ADD COLUMN IF NOT EXISTS data_liquidacao TIMESTAMP,
    ADD COLUMN IF NOT EXISTS tipo_amortizacao VARCHAR(30),
    ADD COLUMN IF NOT EXISTS amortizacao_aplicada BOOLEAN NOT NULL DEFAULT FALSE;
