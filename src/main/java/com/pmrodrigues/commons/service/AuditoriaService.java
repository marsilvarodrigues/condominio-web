package com.pmrodrigues.commons.service;

import com.pmrodrigues.commons.audit.CustomRevisionEntity;
import com.pmrodrigues.commons.dto.RevisaoDTO;
import com.pmrodrigues.commons.tenant.TenantContext;
import com.pmrodrigues.condominio.model.Apartamento;
import com.pmrodrigues.cobranca.model.Cobranca;
import com.pmrodrigues.financeiro.model.OrcamentoAnual;
import com.pmrodrigues.financeiro.model.RateioExecucao;
import io.micrometer.core.annotation.Timed;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.query.AuditEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Service that queries Hibernate Envers revision history for whitelisted entities.
 *
 * <p>Tenant isolation is enforced manually: the Envers {@link AuditReader} does not apply Hibernate
 * {@code @Filter} automatically, so every query adds an explicit {@code condominio_id} predicate.
 */
@Slf4j
@Service
public class AuditoriaService {

  /**
   * Whitelist of valid entity path segments to their entity classes. Never accept arbitrary class
   * names from the path — doing so would be a Class.forName injection vector.
   */
  private static final Map<String, Class<?>> ENTIDADES = Map.of(
      "orcamentos", OrcamentoAnual.class,
      "cobrancas", Cobranca.class,
      "apartamentos", Apartamento.class,
      "rateio-execucoes", RateioExecucao.class
  );

  private final EntityManager entityManager;

  /**
   * Constructs the service with the JPA {@link EntityManager}.
   *
   * @param entityManager JPA entity manager used to obtain the Envers {@link AuditReader}
   */
  public AuditoriaService(EntityManager entityManager) {
    this.entityManager = entityManager;
  }

  /**
   * Returns a paginated list of audit revisions for a whitelisted entity, sorted by revision number
   * descending (most recent first).
   *
   * @param entidade path segment identifying the entity type (must be in the whitelist)
   * @param entityId primary key of the entity
   * @param pageable pagination and sorting parameters
   * @return page of revisions sorted descending by revision number
   * @throws ResponseStatusException 400 if {@code entidade} is not in the whitelist
   */
  @Timed(value = "auditoria.service.historico", description = "Fetch audit history for entity")
  @Transactional(readOnly = true)
  public Page<RevisaoDTO> historico(String entidade, Long entityId, Pageable pageable) {
    log.info("historico() entidade={} id={} pageable={}", entidade, entityId, pageable);
    Class<?> entityClass = ENTIDADES.get(entidade);
    if (entityClass == null) {
      log.error("historico() entidade não suportada: {}", entidade);
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "entidade inválida: " + entidade);
    }
    Long condominioId = TenantContext.getCondominioId();
    AuditReader reader = AuditReaderFactory.get(entityManager);

    @SuppressWarnings("unchecked")
    List<Object[]> allRows = reader.createQuery()
        .forRevisionsOfEntityWithChanges(entityClass, true)
        .add(AuditEntity.id().eq(entityId))
        .add(AuditEntity.relatedId("condominio").eq(condominioId))
        .addOrder(AuditEntity.revisionNumber().desc())
        .getResultList();

    long total = allRows.size();
    var dtos = allRows.stream()
        .skip(pageable.getOffset())
        .limit(pageable.getPageSize())
        .map(this::toRevisaoDTO)
        .toList();

    log.info("historico() entidade={} id={} total={} page={}", entidade, entityId, total,
        pageable.getPageNumber());
    return new PageImpl<>(dtos, pageable, total);
  }

  @SuppressWarnings("unchecked")
  private RevisaoDTO toRevisaoDTO(Object[] row) {
    Object snapshot = row[0];
    var rev = (CustomRevisionEntity) row[1];
    var revType = (org.hibernate.envers.RevisionType) row[2];
    var camposAlterados = (Set<String>) row[3];
    return new RevisaoDTO(rev.getId(), revType.name(), rev.getTimestamp(), rev.getUsername(),
        snapshot, camposAlterados);
  }
}
