package com.pmrodrigues.gateway.dto;

import java.math.BigDecimal;

/**
 * Fine (multa) configuration included in an Asaas charge request.
 *
 * @param value fine amount or percentage
 * @param type  {@code "PERCENTAGE"} or {@code "FIXED"}
 */
public record AsaasFineRequest(BigDecimal value, String type) {}
