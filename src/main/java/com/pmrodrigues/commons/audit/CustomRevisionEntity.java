package com.pmrodrigues.commons.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.envers.RevisionEntity;
import org.hibernate.envers.RevisionNumber;
import org.hibernate.envers.RevisionTimestamp;

/**
 * Custom Hibernate Envers revision entity stored in the {@code audit.revinfo} table. Extends the
 * standard revision information with the username and request ID of the change author.
 */
@Getter
@Setter
@Entity
@Table(name = "revinfo", schema = "audit")
@RevisionEntity(AuditRevisionListener.class)
public class CustomRevisionEntity implements Serializable {

  @Serial private static final long serialVersionUID = 1L;

  /** Surrogate revision number — auto-incremented by the database. */
  @Id
  @RevisionNumber
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "rev")
  private int id;

  /** Unix epoch milliseconds when the revision was created. */
  @RevisionTimestamp
  @Column(name = "revtstmp")
  private long timestamp;

  /** Username extracted from the Spring Security context at revision time. */
  @Column(name = "username", length = 255)
  private String username;

  /**
   * HTTP request ID from the {@code X-Request-Id} header, correlates with application logs.
   */
  @Column(name = "request_id", length = 36)
  private String requestId;

  /**
   * Returns the revision date as a {@link Date} from the stored epoch-millisecond timestamp.
   *
   * @return date of the revision
   */
  public Date getRevisionDate() {
    return new Date(timestamp);
  }
}
