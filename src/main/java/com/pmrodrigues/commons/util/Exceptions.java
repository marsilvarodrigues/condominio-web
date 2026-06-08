package com.pmrodrigues.commons.util;

import static org.springframework.http.HttpStatus.NOT_FOUND;

import org.springframework.web.server.ResponseStatusException;

/**
 * Factory methods for common {@link ResponseStatusException} instances used across service layers.
 */
public final class Exceptions {

  private Exceptions() {}

  /**
   * Creates a 404 {@link ResponseStatusException} for a missing entity.
   *
   * @param entity simple class or domain name of the missing resource
   * @param id identifier that was not found
   * @return a ready-to-throw {@link ResponseStatusException} with HTTP 404
   */
  public static ResponseStatusException notFound(String entity, Object id) {
    return new ResponseStatusException(NOT_FOUND, entity + " not found: " + id);
  }
}
