-- Migration to create indexes for ledger aggregation performance
-- Aimed to optimize queries like: sum(valor) grouped by grupo_id and accounts

CREATE INDEX IF NOT EXISTS idx_lanc_contabil_grupo_credito 
    ON lancamentos_contabeis (grupo_id, conta_credito_id) 
    INCLUDE (valor);

CREATE INDEX IF NOT EXISTS idx_lanc_contabil_grupo_debito 
    ON lancamentos_contabeis (grupo_id, conta_debito_id) 
    INCLUDE (valor);
