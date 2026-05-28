package com.pmrodrigues.security.mapper;

import com.pmrodrigues.security.dto.CreateUserDTO;
import com.pmrodrigues.security.dto.UserDTO;
import com.pmrodrigues.security.model.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

/**
 * MapStruct mapper between {@link User} entities and {@link UserDTO} records.
 */
@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface UserMapper {

    /**
     * Converts a creation DTO to a new {@link User} entity, leaving security and audit fields unset.
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
    User toEntity(CreateUserDTO dto);

    /**
     * Converts a full DTO to a new {@link User} entity, leaving security and audit fields unset.
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
    User toEntity(UserDTO dto);

    /**
     * Converts a {@link User} entity to its DTO representation.
     *
     * @return the corresponding {@link UserDTO}
     */
    UserDTO toDTO(User user);

    /**
     * Applies non-null DTO fields onto an existing entity, ignoring id, security credentials, soft-delete flag, and timestamps.
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "activationToken", ignore = true)
    @Mapping(target = "activationTokenExpiry", ignore = true)
    @Mapping(target = "rawPassword", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(@MappingTarget User user, UserDTO dto);
}