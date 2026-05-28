package com.pmrodrigues.financeiro.service;

import com.pmrodrigues.financeiro.dto.*;
import com.pmrodrigues.financeiro.mapper.FundoReservaMapper;
import com.pmrodrigues.financeiro.model.FundoReserva;
import com.pmrodrigues.financeiro.model.FundoReservaMovimentacao;
import com.pmrodrigues.financeiro.model.TipoMovimentacao;
import com.pmrodrigues.financeiro.repository.FundoReservaMovimentacaoRepository;
import com.pmrodrigues.financeiro.repository.FundoReservaRepository;
import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;

import static com.pmrodrigues.commons.util.Exceptions.notFound;

/**
 * Application service managing the reserve fund balance, including credit, debit and movement history.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FundoReservaService {

    static final String CACHE = "fundo-reserva";
    static final String TENANT_KEY = "T(com.pmrodrigues.commons.tenant.TenantContext).getCondominioId()";

    private final FundoReservaRepository repository;
    private final FundoReservaMovimentacaoRepository movimentacaoRepository;
    private final FundoReservaMapper mapper;

    /**
     * Returns the reserve fund for the current condominium.
     *
     * @return the fund DTO, or empty if none exists
     */
    @Transactional(readOnly = true)
    @Timed(value = "fundo.service.get", description = "Get fundo de reserva")
    @Cacheable(value = CACHE, key = TENANT_KEY)
    public FundoReservaDTO get() {
        log.info("Getting fundo de reserva");
        return repository.findFirstBy().map(mapper::toDTO).orElseThrow(() -> notFound("FundoReserva", "current"));
    }

    /**
     * Creates the reserve fund for the current condominium, refusing if one already exists.
     *
     * @throws ResponseStatusException 409 if a fund already exists
     */
    @Transactional
    @Timed(value = "fundo.service.create", description = "Create fundo de reserva")
    @CacheEvict(value = CACHE, allEntries = true)
    public FundoReservaDTO create(CreateFundoReservaDTO dto) {
        log.info("Creating fundo de reserva");
        if (repository.existsByDeletedFalse()) {
            log.error("FundoReserva already exists for this condominium");
            throw new ResponseStatusException(HttpStatus.CONFLICT, "FundoReserva already exists for this condominium");
        }
        var saved = repository.save(mapper.toEntity(dto));
        log.info("FundoReserva created with id: {}", saved.getId());
        return mapper.toDTO(saved);
    }

    /**
     * Updates the percentual and bank account destination of the existing fund.
     *
     * @throws ResponseStatusException 404 if no fund exists
     */
    @Transactional
    @Timed(value = "fundo.service.update", description = "Update fundo de reserva")
    @CacheEvict(value = CACHE, allEntries = true)
    public FundoReservaDTO update(UpdateFundoReservaDTO dto) {
        log.info("Updating fundo de reserva");
        var entity = repository.findFirstBy().orElseThrow(() -> notFound("FundoReserva", "current"));
        entity.setPercentualArrecadacao(dto.percentualArrecadacao());
        entity.setContaBancariaDestino(dto.contaBancariaDestino());
        var saved = repository.save(entity);
        log.info("FundoReserva updated: {}", saved.getId());
        return mapper.toDTO(saved);
    }

    /**
     * Credits an amount to the fund balance and records a movement.
     *
     * @throws ResponseStatusException 404 if no fund exists
     */
    @Transactional
    @Timed(value = "fundo.service.creditar", description = "Creditar fundo de reserva")
    @CacheEvict(value = CACHE, allEntries = true)
    public FundoReservaMovimentacaoDTO creditar(CreditarFundoDTO dto) {
        log.info("Creditando fundo de reserva: valor={}", dto.valor());
        var fundo = repository.findFirstBy().orElseThrow(() -> notFound("FundoReserva", "current"));
        fundo.setSaldoAtual(fundo.getSaldoAtual().add(dto.valor()));
        repository.save(fundo);

        var mov = buildMovimentacao(fundo, TipoMovimentacao.CREDITO, dto.valor(),
                dto.justificativa(), dto.dataMovimentacao());
        var saved = movimentacaoRepository.save(mov);
        log.info("FundoReserva credited: movimentacaoId={}", saved.getId());
        return mapper.toMovimentacaoDTO(saved);
    }

    /**
     * Debits an amount from the fund balance, refusing if balance would go negative.
     *
     * @throws ResponseStatusException 404 if no fund exists; 409 if balance insufficient
     */
    @Transactional
    @Timed(value = "fundo.service.debitar", description = "Debitar fundo de reserva")
    @CacheEvict(value = CACHE, allEntries = true)
    public FundoReservaMovimentacaoDTO debitar(DebitarFundoDTO dto) {
        log.info("Debitando fundo de reserva: valor={}", dto.valor());
        var fundo = repository.findFirstBy().orElseThrow(() -> notFound("FundoReserva", "current"));
        if (fundo.getSaldoAtual().compareTo(dto.valor()) < 0) {
            log.error("Saldo insuficiente: saldo={}, debit={}", fundo.getSaldoAtual(), dto.valor());
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Saldo insuficiente no fundo de reserva");
        }
        fundo.setSaldoAtual(fundo.getSaldoAtual().subtract(dto.valor()));
        repository.save(fundo);

        var mov = buildMovimentacao(fundo, TipoMovimentacao.DEBITO, dto.valor(),
                dto.justificativa(), dto.dataMovimentacao());
        var saved = movimentacaoRepository.save(mov);
        log.info("FundoReserva debited: movimentacaoId={}", saved.getId());
        return mapper.toMovimentacaoDTO(saved);
    }

    /**
     * Returns a paginated list of movements for the current fund.
     *
     * @throws ResponseStatusException 404 if no fund exists
     */
    @Transactional(readOnly = true)
    @Timed(value = "fundo.service.movimentacoes", description = "List movimentacoes do fundo de reserva")
    public Page<FundoReservaMovimentacaoDTO> listMovimentacoes(Pageable pageable) {
        log.info("Listing movimentacoes do fundo de reserva");
        var fundo = repository.findFirstBy().orElseThrow(() -> notFound("FundoReserva", "current"));
        return movimentacaoRepository
                .findByFundoReservaIdOrderByDataMovimentacaoDesc(fundo.getId(), pageable)
                .map(mapper::toMovimentacaoDTO);
    }

    private FundoReservaMovimentacao buildMovimentacao(FundoReserva fundo, TipoMovimentacao tipo,
                                                        java.math.BigDecimal valor,
                                                        String justificativa, LocalDate data) {
        return FundoReservaMovimentacao.builder()
                .fundoReserva(fundo)
                .tipo(tipo)
                .valor(valor)
                .justificativa(justificativa)
                .dataMovimentacao(data != null ? data : LocalDate.now())
                .build();
    }
}
