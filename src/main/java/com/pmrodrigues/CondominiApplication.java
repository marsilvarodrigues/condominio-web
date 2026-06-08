package com.pmrodrigues;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Spring Boot entry point for the condominium management application. */
@SpringBootApplication
public class CondominiApplication {
  /**
   * Bootstraps the Spring application context and starts the embedded server.
   *
   * @param args command-line arguments passed to {@link SpringApplication}
   */
  public static void main(String[] args) {
    SpringApplication.run(CondominiApplication.class, args);
  }
}
