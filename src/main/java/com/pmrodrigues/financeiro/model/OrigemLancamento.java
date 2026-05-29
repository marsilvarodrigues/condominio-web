package com.pmrodrigues.financeiro.model;

/**
 * Business origin that generated a bank transaction entry.
 */
public enum OrigemLancamento {
    COTA_CONDOMINIO,
    RESERVA,
    DESPESA_ORDINARIA,
    DESPESA_EXTRAORDINARIA,
    TAXA_EXTRA,
    MULTA,
    JUROS,
    MANUAL,
    IMPORTACAO
}
