package com.pmrodrigues.financeiro.mapper;

import com.pmrodrigues.financeiro.dto.CreateLancamentoBancarioDTO;
import com.pmrodrigues.financeiro.dto.LancamentoBancarioDTO;
import com.pmrodrigues.financeiro.dto.UpdateLancamentoBancarioDTO;
import com.pmrodrigues.financeiro.model.LancamentoBancario;
import org.mapstruct.*;

/**
 * MapStruct mapper for {@link LancamentoBancario} entities and their DTOs.
 */
@Mapper(componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface LancamentoBancarioMapper {

    /**
     * Converts a {@link LancamentoBancario} entity to its DTO, flattening the conta bancaria reference.
     */
    @Mapping(target = "contaBancariaId", source = "contaBancaria.id")
    @Mapping(target = "contaBancariaDescricao", source = "contaBancaria.descricao")
    LancamentoBancarioDTO toDTO(LancamentoBancario entity);

    /**
     * Creates a new {@link LancamentoBancario} from a creation payload.
     * The {@code contaBancaria} and {@code condominio} associations are set by the service.
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "contaBancaria", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    LancamentoBancario toEntity(CreateLancamentoBancarioDTO dto);

    /**
     * Updates mutable fields of an existing {@link LancamentoBancario}.
     * Tipo, origem, contaBancaria, and status cannot be changed via update.
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "contaBancaria", ignore = true)
    @Mapping(target = "tipo", ignore = true)
    @Mapping(target = "origem", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    void updateEntity(@MappingTarget LancamentoBancario entity, UpdateLancamentoBancarioDTO dto);
}
