package com.pmrodrigues.commons.audit;

import com.pmrodrigues.commons.interceptor.RequestIdInterceptor;
import org.hibernate.envers.RevisionListener;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Hibernate Envers {@link RevisionListener} that enriches each revision with the authenticated
 * username and the HTTP request ID for audit trail correlation.
 *
 * <p>Called by Envers immediately before committing a revision. Instantiated by Envers (not Spring),
 * so it must not have constructor dependencies — read context from thread-local holders only.
 */
public class AuditRevisionListener implements RevisionListener {

  /**
   * Populates {@link CustomRevisionEntity#setUsername(String)} and
   * {@link CustomRevisionEntity#setRequestId(String)} from the current thread's security context
   * and HTTP request attributes.
   *
   * @param revisionEntity the revision entity to be persisted; cast to {@link CustomRevisionEntity}
   */
  @Override
  public void newRevision(Object revisionEntity) {
    var rev = (CustomRevisionEntity) revisionEntity;
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
      rev.setUsername(auth.getName());
    }
    var attrs = RequestContextHolder.getRequestAttributes();
    if (attrs instanceof ServletRequestAttributes servletAttrs) {
      String requestId = servletAttrs.getRequest().getHeader(RequestIdInterceptor.REQUEST_ID_HEADER);
      rev.setRequestId(requestId);
    }
  }
}
