package com.pmrodrigues.security.repository;

import com.pmrodrigues.security.model.PasswordHistory;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data JPA repository for {@link PasswordHistory} entities. */
public interface PasswordHistoryRepository extends JpaRepository<PasswordHistory, Long> {

  /**
   * Returns the most recent password history entries for a user, newest first.
   *
   * @param userId the user's primary key
   * @param pageable limits the number of records returned
   * @return ordered list of recent password history entries
   */
  List<PasswordHistory> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
}
