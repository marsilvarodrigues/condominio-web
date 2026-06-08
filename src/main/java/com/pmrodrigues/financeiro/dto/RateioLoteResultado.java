package com.pmrodrigues.financeiro.dto;

/**
 * Summary of a batch rateio execution over all pending expenses in a condominium.
 *
 * @param total number of expenses processed
 * @param sucesso number of expenses successfully rated
 * @param erro number of expenses that failed
 * @param duracaoMs wall-clock time in milliseconds
 */
public record RateioLoteResultado(int total, int sucesso, int erro, long duracaoMs) {}
