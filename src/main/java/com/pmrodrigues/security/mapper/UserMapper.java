package com.pmrodrigues.security.mapper;

import com.pmrodrigues.condominio.model.Condominio;
import com.pmrodrigues.security.dto.CreateUserDTO;
import com.pmrodrigues.security.dto.UserDTO;
import com.pmrodrigues.security.model.User;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.mapstruct.NullValuePropertyMappingStrategy;

/**
 * MapStruct mapper between {@link User} entities and {@link UserDTO} records.
 *
 * <p>Condominio associations are managed by the service layer; mappers only read IDs for the DTO
 * output.
 */
@Mapper(
    componentModel = "spring",
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface UserMapper {

  /**
   * Converts a creation DTO to a new {@link User} entity, leaving security, audit, and condominio
   * fields unset.
   *
   * @return a new {@link User} populated from {@code dto}
   */
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "password", ignore = true)
  @Mapping(target = "activationToken", ignore = true)
  @Mapping(target = "activationTokenExpiry", ignore = true)
  @Mapping(target = "rawPassword", ignore = true)
  @Mapping(target = "deleted", ignore = true)
  @Mapping(target = "enabled", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  @Mapping(target = "condominios", ignore = true)
  User toEntity(CreateUserDTO dto);

  /**
   * Converts a full DTO to a new {@link User} entity, leaving security, audit, and condominio
   * fields unset.
   *
   * @return a new {@link User} populated from {@code dto}
   */
  @Mapping(target = "password", ignore = true)
  @Mapping(target = "activationToken", ignore = true)
  @Mapping(target = "activationTokenExpiry", ignore = true)
  @Mapping(target = "rawPassword", ignore = true)
  @Mapping(target = "deleted", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  @Mapping(target = "condominios", ignore = true)
  User toEntity(UserDTO dto);

  /**
   * Converts a {@link User} entity to its DTO representation, mapping condominio entities to their
   * IDs.
   *
   * @return the corresponding {@link UserDTO}
   */
  @Mapping(target = "condominioIds", source = "condominios", qualifiedByName = "condominiosToIds")
  UserDTO toDTO(User user);

  /**
   * Applies non-null DTO fields onto an existing entity, ignoring id, security credentials,
   * soft-delete flag, timestamps, and condominio associations (managed by service).
   */
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "password", ignore = true)
  @Mapping(target = "activationToken", ignore = true)
  @Mapping(target = "activationTokenExpiry", ignore = true)
  @Mapping(target = "rawPassword", ignore = true)
  @Mapping(target = "deleted", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  @Mapping(target = "condominios", ignore = true)
  void updateEntity(@MappingTarget User user, UserDTO dto);

  /** Maps a set of {@link Condominio} entities to a set of their primary key IDs. */
  @Named("condominiosToIds")
  default Set<Long> condominiosToIds(Set<Condominio> condominios) {
    if (condominios == null) return new HashSet<>();
    return condominios.stream().map(Condominio::getId).collect(Collectors.toSet());
  }
}
