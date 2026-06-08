package com.pmrodrigues.gateway.dto;

import java.math.BigDecimal;

/**
 * Monthly interest (juros mora) configuration included in an Asaas charge request.
 *
 * @param value monthly interest percentage
 */
public record AsaasInterestRequest(BigDecimal value) {}
