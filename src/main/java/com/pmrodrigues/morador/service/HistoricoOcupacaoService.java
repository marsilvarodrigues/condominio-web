package com.pmrodrigues.morador.service;

import com.pmrodrigues.commons.tenant.TenantContext;
import com.pmrodrigues.morador.dto.HistoricoOcupacaoDTO;
import com.pmrodrigues.morador.mapper.HistoricoOcupacaoMapper;
import com.pmrodrigues.morador.model.HistoricoOcupacao;
import com.pmrodrigues.morador.model.Morador;
import com.pmrodrigues.morador.model.Pessoa;
import com.pmrodrigues.morador.repository.HistoricoOcupacaoRepository;
import io.micrometer.core.annotation.Timed;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Application service responsible exclusively for persisting and querying occupancy history records.
 * Keeps the write logic (snapshot creation) isolated from the read logic (listing), and decoupled
 * from {@link PessoaService}.
 *
 * <p>Design notes:
 *
 * <ul>
 *   <li>This service never modifies the apartment or the condomínio — it only reads from Pessoa to
 *       build a snapshot before the caller clears the apartment reference.
 *   <li>condominioId is resolved via {@link TenantContext} because {@code Pessoa.condominio} has a
 *       suppressed getter.
 *   <li>dataEntrada is inferred from {@code Pessoa.createdAt}; falls back to today with a warning
 *       log if createdAt is null.
 *   <li>The record is immutable once persisted — no update or delete methods are exposed.
 *   <li>{@link #registrar} also blocks system access for the morador unless they hold a role that
 *       grants access independently of apartment assignment (ROLE_PROPRIETARIO or ROLE_SINDICO).
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HistoricoOcupacaoService {

  private final HistoricoOcupacaoRepository historicoRepository;
  private final HistoricoOcupacaoMapper historicoMapper;

  /**
   * Persists a snapshot of the morador's occupancy before they are removed from the apartment.
   * Must be called BEFORE clearing {@code Pessoa.apartamento}. Also disables system access for the
   * morador unless they hold an apartment-independent role.
   *
   * @param pessoa the morador being removed; must have {@code apartamento != null}
   * @param dataSaida the departure date (typically {@link LocalDate#now()})
   * @return the persisted snapshot as a DTO
   */
  @Transactional
  @Timed(value = "historico.ocupacao.service.registrar")
  public HistoricoOcupacaoDTO registrar(Pessoa pessoa, LocalDate dataSaida) {
    log.info(
        "Registering historico ocupacao for pessoa={} apartamento={}",
        pessoa.getId(),
        pessoa.getApartamento().getId());

    var entity =
        HistoricoOcupacao.builder()
            .condominioId(TenantContext.getCondominioId())
            .apartamentoId(pessoa.getApartamento().getId())
            .pessoaId(pessoa.getId())
            .nomeMorador(pessoa.getName())
            .emailMorador(pessoa.getEmail())
            .cpfMorador(resolveCpf(pessoa))
            .dataEntrada(resolveDataEntrada(pessoa))
            .dataSaida(dataSaida)
            .build();

    var saved = historicoRepository.save(entity);
    log.info("Historico ocupacao registered with id={}", saved.getId());

    bloquearSeApenasResidente(pessoa);

    return historicoMapper.toDTO(saved);
  }

  /**
   * Returns all history records for a given apartment ordered by most recent departure first.
   *
   * @param apartamentoId apartment primary key
   * @param condominioId tenant identifier (to scope the query)
   * @return ordered list of DTOs; empty list when no records exist
   */
  @Transactional(readOnly = true)
  @Timed(value = "historico.ocupacao.service.listar")
  public Page<HistoricoOcupacaoDTO> listarPorApartamento(Long apartamentoId, Long condominioId, Pageable page) {
    log.info("Listing historico ocupacao for apartamento={}", apartamentoId);
    var result =
        historicoRepository.findByApartamentoIdAndCondominioIdOrderByDataSaidaDesc(
            apartamentoId, condominioId, page).map(historicoMapper::toDTO);
    log.info(
        "Found {} historico records for apartamento={}", result.getTotalElements(), apartamentoId);
    return result;
  }

  // ── private helpers ───────────────────────────────────────────────────────────

  private LocalDate resolveDataEntrada(Pessoa pessoa) {
    if (pessoa.getCreatedAt() != null) {
      return pessoa.getCreatedAt().toLocalDate();
    }
    log.warn(
        "Pessoa {} has null createdAt; using today as dataEntrada", pessoa.getId());
    return LocalDate.now();
  }

  /** Extracts CPF if the Pessoa is a {@link Morador}; returns {@code null} otherwise. */
  private String resolveCpf(Pessoa pessoa) {
    if (pessoa instanceof Morador m) {
      return m.getCpf();
    }
    return null;
  }

  /**
   * Blocks system access for the given pessoa unless they hold a role that grants access
   * independently of apartment assignment.
   *
   * <p>Roles that exempt a pessoa from blocking:
   *
   * <ul>
   *   <li>{@code ROLE_PROPRIETARIO} — owns apartments, does not need to reside
   *   <li>{@code ROLE_SINDICO} — operational role, not linked to residency
   * </ul>
   *
   * <p>The {@code enabled} flag is set in memory on the {@code pessoa} object; the caller is
   * responsible for persisting it via the repository.
   *
   * @param pessoa the morador being archived; may hold additional roles
   */
  private void bloquearSeApenasResidente(Pessoa pessoa) {
    var roles = pessoa.getRoles();
    if (roles != null
        && (roles.contains("ROLE_PROPRIETARIO") || roles.contains("ROLE_SINDICO"))) {
      log.info(
          "Pessoa {} has independent role — skipping access block", pessoa.getId());
      return;
    }
    log.info(
        "Blocking system access for pessoa {} (morador removed from apartment)",
        pessoa.getId());
    pessoa.setEnabled(false);
  }
}
