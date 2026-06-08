package com.pmrodrigues.commons.versioning;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares the API version handled by a controller class or individual handler method.
 *
 * <p>Spring routes requests to the controller whose version matches the {@code X-API-Version}
 * request header. When the header is absent the latest (highest) version is used automatically. A
 * method-level annotation overrides a class-level one.
 *
 * <pre>{@code
 * @ApiVersion("1")
 * @RestController
 * @RequestMapping("/condominios")
 * public class CondominioController { ... }
 * }</pre>
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ApiVersion {

  /**
   * The version string this controller or method handles (e.g. {@code "1"}, {@code "2"}).
   *
   * @return the version identifier
   */
  String value();
}
