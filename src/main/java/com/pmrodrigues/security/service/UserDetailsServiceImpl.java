package com.pmrodrigues.security.service;

import com.pmrodrigues.security.repository.UserRepository;
import io.micrometer.core.annotation.Timed;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Spring Security {@link UserDetailsService} that loads a {@link
 * com.pmrodrigues.security.model.User} by email and converts it to a {@link
 * org.springframework.security.core.userdetails.UserDetails}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

  private final UserRepository userRepository;

  /**
   * Loads a user by email address and maps their roles to Spring Security authorities.
   *
   * @param email the user's email, used as the Spring Security username
   * @return the populated {@link UserDetails}
   * @throws UsernameNotFoundException if no active user exists with the given email
   */
  @Override
  @Timed(value = "user.details.loadByUsername", description = "Load user details by username")
  @Transactional(readOnly = true)
  public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
    log.info("Loading user by username: {}", email);

    var user =
        userRepository
            .findByEmail(email)
            .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));

    var authorities =
        user.getRoles().stream().map(SimpleGrantedAuthority::new).collect(Collectors.toSet());

    var userDetails =
        org.springframework.security.core.userdetails.User.builder()
            .username(user.getEmail())
            .password(user.getPassword())
            .authorities(authorities)
            .disabled(!user.isEnabled())
            .build();

    log.info("User loaded successfully: {}", email);
    return userDetails;
  }
}
