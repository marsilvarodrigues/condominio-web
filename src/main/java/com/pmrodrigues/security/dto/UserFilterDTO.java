package com.pmrodrigues.security.dto;

/** Filter criteria for user queries. All fields are optional; absent fields are ignored. */
public record UserFilterDTO(String nome, String email, Boolean enabled, String role) {}
