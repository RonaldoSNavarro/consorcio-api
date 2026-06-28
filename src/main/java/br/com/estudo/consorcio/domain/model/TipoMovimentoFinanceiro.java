package br.com.estudo.consorcio.domain.model;

public enum TipoMovimentoFinanceiro {
    // --- Pagamento de Parcela ---
    PAGAMENTO_PARCELA,
    FUNDO_COMUM,
    TAXA_ADMINISTRACAO,
    FUNDO_RESERVA,
    SEGURO,                 // Novo componente

    // --- Inadimplência ---
    MULTA_ATRASO,
    JUROS_MORA,

    // --- Estorno (registro contábil inverso) ---
    ESTORNO_PAGAMENTO,

    // --- Contemplação ---
    LIBERACAO_CREDITO,
    LANCE_EMBUTIDO,
    PAGAMENTO_BEM,

    // --- Reembolso ---
    REEMBOLSO,
    MULTA_RESCISORIA,

    // --- Ajustes ---
    REAJUSTE_CREDITO,
    AMORTIZACAO,
    
    // --- Adicionais ---
    LANCAMENTO_MANUAL,
    TAXA_TRANSFERENCIA,
    RENDIMENTO_APLICACAO
}
