package com.pmrodrigues.commons.config;

import io.micrometer.core.aop.TimedAspect;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Registers Micrometer aspects that enable {@code @Timed} method-level metrics. */
@Configuration
public class MetricsConfig {

  /**
   * Enables AOP-driven timing of methods annotated with {@code @Timed}.
   *
   * @return a {@link TimedAspect} bound to the application's {@link MeterRegistry}
   */
  @Bean
  public TimedAspect timedAspect(MeterRegistry registry) {
    return new TimedAspect(registry);
  }
}
