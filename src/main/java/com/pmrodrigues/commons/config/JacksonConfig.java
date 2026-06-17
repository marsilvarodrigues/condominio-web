package com.pmrodrigues.commons.config;

import com.fasterxml.jackson.datatype.hibernate6.Hibernate6Module;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registers Jackson's Hibernate 6 module so uninitialized lazy associations serialize as
 * {@code null} instead of throwing {@code LazyInitializationException} once the session that
 * loaded them has closed — relevant for {@code AuditoriaService}, which returns raw Envers entity
 * snapshots that may carry unfetched lazy relations.
 */
@Configuration
public class JacksonConfig {

  /**
   * Builds the Hibernate6 Jackson module, auto-registered by Spring Boot's Jackson
   * autoconfiguration since it implements {@code com.fasterxml.jackson.databind.Module}.
   *
   * @return a configured {@link Hibernate6Module}
   */
  @Bean
  public Hibernate6Module hibernate6Module() {
    var module = new Hibernate6Module();
    module.enable(Hibernate6Module.Feature.SERIALIZE_IDENTIFIER_FOR_LAZY_NOT_LOADED_OBJECTS);
    return module;
  }
}
