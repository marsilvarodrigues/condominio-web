package com.pmrodrigues.commons.config;

import com.pmrodrigues.commons.versioning.ApiVersionHandlerMapping;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcRegistrations;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/**
 * Registers {@link ApiVersionHandlerMapping} as the primary request-mapping handler, replacing
 * Spring Boot's default so that {@code @ApiVersion} annotations on controllers are evaluated during
 * handler selection.
 */
@Configuration
public class ApiVersioningConfig implements WebMvcRegistrations {

  /**
   * Provides the custom handler mapping that enables per-controller API versioning via the {@code
   * X-API-Version} request header.
   *
   * @return the custom handler mapping instance
   */
  @Override
  public RequestMappingHandlerMapping getRequestMappingHandlerMapping() {
    return new ApiVersionHandlerMapping();
  }
}
