package com.pmrodrigues.security.dto;

import java.util.List;

/** JWT claim payload for a proprietário user. */
public record ProprietarioClaims(Long id, List<Long> apartamentoIds) {}
