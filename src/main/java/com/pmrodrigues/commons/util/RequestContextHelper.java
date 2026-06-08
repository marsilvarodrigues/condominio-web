package com.pmrodrigues.commons.util;

import com.pmrodrigues.commons.interceptor.RequestIdInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;
import org.springframework.lang.Nullable;

/**
 * Utility methods for extracting values set by interceptors from the current {@link
 * HttpServletRequest}.
 */
public final class RequestContextHelper {

  private RequestContextHelper() {}

  /**
   * Extracts the correlation UUID stored by {@code RequestIdInterceptor}, or {@code null} if
   * absent.
   *
   * @param request the current HTTP request
   * @return the request UUID, or {@code null}
   */
  @Nullable
  public static UUID requestId(HttpServletRequest request) {
    Object attr = request.getAttribute(RequestIdInterceptor.REQUEST_ID_ATTRIBUTE);
    return attr instanceof UUID id ? id : null;
  }
}
