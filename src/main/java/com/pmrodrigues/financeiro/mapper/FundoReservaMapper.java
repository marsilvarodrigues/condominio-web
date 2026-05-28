package com.pmrodrigues.financeiro.mapper;

import com.pmrodrigues.financeiro.dto.CreateFundoReservaDTO;
import com.pmrodrigues.financeiro.dto.FundoReservaDTO;
import com.pmrodrigues.financeiro.dto.FundoReservaMovimentacaoDTO;
import com.pmrodrigues.financeiro.model.FundoReserva;
import com.pmrodrigues.financeiro.model.FundoReservaMovimentacao;
import org.mapstruct.*;

/**
 * MapStruct mapper for FundoReserva and its movements.
 */
@Mapper(componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface FundoReservaMapper {

    /**
     * Maps FundoReserva to its DTO.
     */
    FundoReservaDTO toDTO(FundoReserva entity);

    /**
     * Maps a creation payload to a new FundoReserva entity.
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "saldoAtual", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    FundoReserva toEntity(CreateFundoReservaDTO dto);

    /**
     * Maps a FundoReservaMovimentacao entity to its DTO.
     */
    FundoReservaMovimentacaoDTO toMovimentacaoDTO(FundoReservaMovimentacao entity);
}
