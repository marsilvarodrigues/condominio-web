package com.pmrodrigues.cobranca.service;

import static com.pmrodrigues.commons.util.Exceptions.notFound;

import com.pmrodrigues.cobranca.dto.CancelarCobrancaDTO;
import com.pmrodrigues.cobranca.dto.CobrancaDTO;
import com.pmrodrigues.cobranca.dto.CobrancaFilterDTO;
import com.pmrodrigues.cobranca.dto.CobrancaResumoDTO;
import com.pmrodrigues.cobranca.dto.GerarCobrancasDTO;
import com.pmrodrigues.cobranca.dto.ResumoCobrancasDTO;
import com.pmrodrigues.cobranca.email.CobrancaEmailTemplate;
import com.pmrodrigues.cobranca.mapper.CobrancaMapper;
import com.pmrodrigues.cobranca.model.Cobranca;
import com.pmrodrigues.cobranca.model.CobrancaConfiguracao;
import com.pmrodrigues.cobranca.model.StatusCobranca;
import com.pmrodrigues.cobranca.repository.CobrancaConfiguracaoRepository;
import com.pmrodrigues.cobranca.repository.CobrancaRepository;
import com.pmrodrigues.cobranca.specification.CobrancaSpecification;
import com.pmrodrigues.commons.service.MailService;
import com.pmrodrigues.commons.tenant.TenantContext;
import com.pmrodrigues.financeiro.model.CotaRateio;
import com.pmrodrigues.financeiro.service.CotaRateioService;
import com.pmrodrigues.gateway.dto.AsaasEmissaoRequest;
import com.pmrodrigues.gateway.dto.AsaasWebhookPayload;
import com.pmrodrigues.gateway.service.AsaasGatewayService;
import com.pmrodrigues.morador.service.PessoaService;
import io.micrometer.core.annotation.Timed;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Application service for the Cobrança module.
 *
 * <p>Orchestrates charge generation from rateio executions, Asaas gateway calls, and billing email
 * dispatch. Cross-module data access follows the module boundary rule:
 *
 * <ul>
 *   <li>Financeiro data is read via {@link CotaRateioService} (never via its repositories directly)
 *   <li>Resident data is read via {@link PessoaService} (never via morador repositories directly)
 *   <li>Asaas gateway operations go through {@link AsaasGatewayService}
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CobrancaService {

  private final CobrancaRepository cobrancaRepository;
  private final CobrancaConfiguracaoRepository configuracaoRepo;
  private final AsaasGatewayService gatewayService;
  private final MailService mailService;
  private final CobrancaMapper mapper;
  private final CotaRateioService cotaRateioService;
  private final PessoaService pessoaService;

  /**
   * Generates charges for all apartment units of a rateio execution.
   *
   * <p>Units without a linked resident are silently skipped. Already-charged units (same {@code
   * cotaRateioId}) are idempotently skipped. One failure does not stop processing of the remaining
   * units — failed cotas are logged at ERROR level and excluded from the returned list.
   *
   * @param dto contains the execucaoId and target due date
   * @return list of successfully created {@link CobrancaDTO}s
   * @throws org.springframework.web.server.ResponseStatusException with 404 if the execution does
   *     not exist
   */
  @Timed(
      value = "cobranca.service.gerarCobrancas",
      description = "Generate charges from rateio execution")
  public List<CobrancaDTO> gerarCobrancas(GerarCobrancasDTO dto) {
    log.info("gerarCobrancas execucaoId={} vencimento={}", dto.execucaoId(), dto.vencimento());
    cotaRateioService.findExecucaoById(
        dto.execucaoId()); // validates existence, throws 404 if absent

    var cotas = cotaRateioService.findByExecucaoId(dto.execucaoId());
    log.info("gerarCobrancas: {} cotas found for execucaoId={}", cotas.size(), dto.execucaoId());

    return cotas.stream()
        .map(cota -> processarCotaSeguro(cota, dto.vencimento()))
        .filter(Optional::isPresent)
        .map(Optional::get)
        .toList();
  }

  /**
   * Processes one {@link CotaRateio}: resolves the resident, delegates charge creation and QR Code
   * generation to {@link AsaasGatewayService}, persists the entity and sends the billing email.
   * Runs in a new transaction so that a failure on one unit does not roll back others.
   *
   * @param cota quota record with apartment and value
   * @param vencimento target due date
   * @return the DTO of the created charge, or {@code null} if the unit was skipped (no resident or
   *     already charged)
   */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  @Timed(
      value = "cobranca.service.processarCota",
      description = "Process a single CotaRateio into a charge")
  public CobrancaDTO processarCota(CotaRateio cota, LocalDate vencimento) {
    var moradores = pessoaService.listarPorApartamento(cota.getApartamento().getId());
    if (moradores.isEmpty()) {
      log.warn(
          "No resident for apartamentoId={} cotaId={} — skipping",
          cota.getApartamento().getId(),
          cota.getId());
      return null;
    }

    if (cobrancaRepository.findByCotaRateioId(cota.getId()).isPresent()) {
      log.warn("Charge already exists for cotaRateioId={} — skipping (idempotent)", cota.getId());
      return null;
    }

    var morador = moradores.getFirst();
    var config = resolverConfig(TenantContext.getCondominioId());

    var emissao =
        gatewayService.emitir(
            new AsaasEmissaoRequest(
                morador.id(),
                morador.nome(),
                morador.cpf(),
                morador.email(),
                cota.getValor(),
                vencimento,
                config.getDescricaoPadrao(),
                config.getMultaPercent(),
                config.getJurosMoraPercent()));

    var apt = cota.getApartamento();
    var cobranca =
        Cobranca.builder()
            .apartamento(apt)
            .cotaRateioId(cota.getId())
            .moradorId(morador.id())
            .valor(cota.getValor())
            .vencimento(vencimento)
            .asaasId(emissao.asaasId())
            .asaasCustomerId(emissao.asaasCustomerId())
            .boletoUrl(emissao.boletoUrl())
            .boletoCodBarras(emissao.boletoCodBarras())
            .pixQrCodeBase64(emissao.pixQrCodeBase64())
            .pixCopiaCola(emissao.pixCopiaCola())
            .build();

    cobrancaRepository.save(cobranca);

    var aptDesc =
        "Apt "
            + apt.getNumero()
            + (apt.getBloco() != null ? " · Bloco " + apt.getBloco().getBloco() : "");
    mailService.sendEmail(
        morador.email(),
        new CobrancaEmailTemplate(
            morador.nome(),
            config.getDescricaoPadrao(),
            aptDesc,
            cobranca.getValor(),
            cobranca.getVencimento(),
            cobranca.getBoletoUrl(),
            cobranca.getBoletoCodBarras(),
            cobranca.getPixCopiaCola(),
            cobranca.getPixQrCodeBase64()));

    cobranca.setEmailEnviado(true);
    cobranca.setEmailEnviadoEm(LocalDateTime.now());
    cobrancaRepository.save(cobranca);

    log.info(
        "Charge created and email sent: cobrancaId={} moradorId={}",
        cobranca.getId(),
        morador.id());
    var dto = mapper.toDTO(cobranca);
    return new CobrancaDTO(
        dto.id(), dto.apartamentoId(), dto.apartamentoNumero(), dto.blocoNome(),
        dto.moradorId(), morador.nome(), morador.email(),
        dto.valor(), dto.vencimento(), dto.status(),
        dto.boletoUrl(), dto.boletoCodBarras(), dto.pixQrCodeBase64(), dto.pixCopiaCola(),
        dto.emailEnviado(), dto.emailEnviadoEm(), dto.pagoEm(), dto.criadaEm());
  }

  /**
   * Returns a paginated list of charges matching the supplied filter criteria.
   *
   * @param filter optional filter parameters; absent fields are ignored
   * @param pageable pagination and sort parameters
   * @return page of matching charge DTOs
   */
  @Transactional(readOnly = true)
  @Timed(value = "cobranca.service.filterBy", description = "List charges with filters")
  public Page<CobrancaDTO> filterBy(CobrancaFilterDTO filter, Pageable pageable) {
    log.info("filterBy filter={}", filter);
    return cobrancaRepository
        .findAll(
            Specification.allOf(
                CobrancaSpecification.hasApartamento(filter.apartamentoId()),
                CobrancaSpecification.hasStatus(filter.status()),
                CobrancaSpecification.vencimentoFrom(filter.vencimentoDe()),
                CobrancaSpecification.vencimentoTo(filter.vencimentoAte()),
                CobrancaSpecification.emailEnviado(filter.emailEnviado())),
            pageable)
        .map(mapper::toDTO)
        .map(this::enriquecerMorador);
  }

  /**
   * Looks up a single charge by its primary key.
   *
   * @param id charge primary key
   * @return the DTO for the found charge
   * @throws org.springframework.web.server.ResponseStatusException with 404 if not found
   */
  @Transactional(readOnly = true)
  @Timed(value = "cobranca.service.findById", description = "Find charge by ID")
  public CobrancaDTO findById(Long id) {
    log.info("findById id={}", id);
    return enriquecerMorador(
        mapper.toDTO(
            cobrancaRepository.findById(id).orElseThrow(() -> notFound("Cobranca", id))));
  }

  /**
   * Returns an aggregate summary of pending and overdue charges for the current tenant.
   *
   * @return count and sum of PENDENTE+ENVIADA charges, and count and sum of VENCIDA charges
   */
  @Transactional(readOnly = true)
  @Timed(value = "cobranca.service.resumo", description = "Aggregate pending/overdue charges")
  public ResumoCobrancasDTO resumo() {
    log.info("resumo cobrancas");
    var pendentes = List.of(StatusCobranca.PENDENTE, StatusCobranca.ENVIADA);
    var vencidas = List.of(StatusCobranca.VENCIDA);
    var result = new ResumoCobrancasDTO(
        cobrancaRepository.countByStatusIn(pendentes),
        cobrancaRepository.sumValorByStatusIn(pendentes),
        cobrancaRepository.countByStatusIn(vencidas),
        cobrancaRepository.sumValorByStatusIn(vencidas));
    log.info("resumo cobrancas: pendentes={} vencidas={}", result.quantidadePendente(), result.quantidadeVencida());
    return result;
  }

  /**
   * Returns all charges for a given apartment, ordered by creation date descending.
   *
   * @param apartamentoId apartment primary key
   * @param pageable pagination parameters
   * @return page of summary charge DTOs
   */
  @Transactional(readOnly = true)
  @Timed(value = "cobranca.service.porApartamento", description = "List charges by apartment")
  public Page<CobrancaResumoDTO> porApartamento(Long apartamentoId, Pageable pageable) {
    log.info("porApartamento apartamentoId={}", apartamentoId);
    return cobrancaRepository
        .findByApartamentoIdOrderByCreatedAtDesc(apartamentoId, pageable)
        .map(mapper::toResumoDTO);
  }

  /**
   * Cancels a charge: calls the Asaas gateway to cancel the external payment (if one exists), then
   * marks the local entity as CANCELADA.
   *
   * @param id charge primary key
   * @param dto cancellation payload (reason for logging purposes)
   * @return updated charge DTO
   * @throws org.springframework.web.server.ResponseStatusException with 404 if not found
   */
  @Transactional
  @Timed(value = "cobranca.service.cancelar", description = "Cancel a charge")
  public CobrancaDTO cancelar(Long id, CancelarCobrancaDTO dto) {
    log.info("cancelar id={} motivo={}", id, dto.motivo());
    var cobranca = cobrancaRepository.findById(id).orElseThrow(() -> notFound("Cobranca", id));
    if (cobranca.getAsaasId() != null) {
      gatewayService.cancelar(cobranca.getAsaasId());
    }
    cobranca.setStatus(StatusCobranca.CANCELADA);
    return enriquecerMorador(mapper.toDTO(cobrancaRepository.save(cobranca)));
  }

  /**
   * Re-sends the billing email for an existing charge. Updates the {@code emailEnviado} flag and
   * timestamp on success.
   *
   * @param id charge primary key
   * @throws org.springframework.web.server.ResponseStatusException with 404 if not found
   * @throws IllegalStateException if the charge has no linked resident
   */
  @Transactional
  @Timed(value = "cobranca.service.reenviarEmail", description = "Resend billing email")
  public void reenviarEmail(Long id) {
    log.info("reenviarEmail id={}", id);
    var cobranca = cobrancaRepository.findById(id).orElseThrow(() -> notFound("Cobranca", id));
    var moradores = pessoaService.listarPorApartamento(cobranca.getApartamento().getId());
    if (moradores.isEmpty()) {
      throw new IllegalStateException("No resident linked to apartamento for cobrancaId=" + id);
    }
    var morador = moradores.get(0);
    var apt = cobranca.getApartamento();
    var aptDesc =
        "Apt "
            + apt.getNumero()
            + (apt.getBloco() != null ? " · Bloco " + apt.getBloco().getBloco() : "");
    var config = resolverConfig(TenantContext.getCondominioId());
    mailService.sendEmail(
        morador.email(),
        new CobrancaEmailTemplate(
            morador.nome(),
            config.getDescricaoPadrao(),
            aptDesc,
            cobranca.getValor(),
            cobranca.getVencimento(),
            cobranca.getBoletoUrl(),
            cobranca.getBoletoCodBarras(),
            cobranca.getPixCopiaCola(),
            cobranca.getPixQrCodeBase64()));
    cobranca.setEmailEnviado(true);
    cobranca.setEmailEnviadoEm(LocalDateTime.now());
    cobrancaRepository.save(cobranca);
  }

  /**
   * Processes an Asaas payment webhook. Marks the charge as {@link StatusCobranca#PAGA} when the
   * event is {@code PAYMENT_RECEIVED} or {@code PAYMENT_CONFIRMED}. Idempotent — already-paid
   * charges are silently ignored.
   *
   * @param payload webhook body from Asaas
   */
  @Transactional
  @Timed(value = "cobranca.service.processarWebhook", description = "Process Asaas payment webhook")
  public void processarWebhook(AsaasWebhookPayload payload) {
    log.info("processarWebhook event={} asaasId={}", payload.event(), payload.payment().id());
    if (!isPaymentEvent(payload.event())) {
      log.info("Ignoring non-payment webhook event: {}", payload.event());
      return;
    }
    cobrancaRepository
        .findByAsaasIdNative(payload.payment().id())
        .ifPresentOrElse(
            cobranca -> {
              if (cobranca.getStatus() == StatusCobranca.PAGA) {
                log.info("Charge already paid cobrancaId={} — ignoring", cobranca.getId());
                return;
              }
              cobranca.setStatus(StatusCobranca.PAGA);
              cobranca.setPagoEm(LocalDateTime.now());
              cobrancaRepository.save(cobranca);
              log.info("Charge marked as PAID: cobrancaId={}", cobranca.getId());
            },
            () -> log.warn("No charge found for asaasId={}", payload.payment().id()));
  }

  // ── Private helpers ────────────────────────────────────────────────────────

  /**
   * Loads per-condominium billing configuration from the database. When no record exists the entity
   * {@link CobrancaConfiguracao} builder defaults apply (10 days, 1 % juros, 2 % multa, "Taxa
   * condominial").
   *
   * @param condominioId the current tenant identifier
   * @return effective configuration, never null
   */
  private CobrancaConfiguracao resolverConfig(Long condominioId) {
    if (condominioId != null) {
      return configuracaoRepo
          .findByCondominioId(condominioId)
          .orElseGet(() -> CobrancaConfiguracao.builder().condominioId(condominioId).build());
    }
    return CobrancaConfiguracao.builder().build();
  }

  private Optional<CobrancaDTO> processarCotaSeguro(CotaRateio cota, LocalDate vencimento) {
    try {
      return Optional.ofNullable(processarCota(cota, vencimento));
    } catch (Exception e) {
      log.error(
          "Failed to process cotaId={} apartamentoId={}: {}",
          cota.getId(),
          cota.getApartamento().getId(),
          e.getMessage(),
          e);
      return Optional.empty();
    }
  }

  /**
   * Soft-deletes all charges belonging to the given condominium. Called by {@code
   * CondominioService.delete()} during cascade soft-delete.
   *
   * @param condominioId condominium primary key
   */
  @Transactional
  @Timed(
      value = "cobranca.service.softDeleteByCondominioId",
      description = "Soft-delete cobrancas by condominio")
  public void softDeleteByCondominioId(Long condominioId) {
    log.info("softDeleteByCondominioId condominioId={}", condominioId);
    cobrancaRepository.softDeleteByCondominioId(condominioId);
  }

  /**
   * Creates a new {@link CobrancaDTO} with {@code moradorNome} and {@code moradorEmail} populated
   * from the pessoa record. Returns the original DTO unchanged when {@code moradorId} is null or the
   * lookup fails (e.g. soft-deleted resident).
   */
  private CobrancaDTO enriquecerMorador(CobrancaDTO dto) {
    if (dto.moradorId() == null) return dto;
    try {
      var pessoa = pessoaService.findById(dto.moradorId());
      return new CobrancaDTO(
          dto.id(), dto.apartamentoId(), dto.apartamentoNumero(), dto.blocoNome(),
          dto.moradorId(), pessoa.nome(), pessoa.email(),
          dto.valor(), dto.vencimento(), dto.status(),
          dto.boletoUrl(), dto.boletoCodBarras(), dto.pixQrCodeBase64(), dto.pixCopiaCola(),
          dto.emailEnviado(), dto.emailEnviadoEm(), dto.pagoEm(), dto.criadaEm());
    } catch (Exception e) {
      log.warn("Could not enrich moradorNome for cobrancaId={}: {}", dto.id(), e.getMessage());
      return dto;
    }
  }

  private static boolean isPaymentEvent(String event) {
    return "PAYMENT_RECEIVED".equals(event) || "PAYMENT_CONFIRMED".equals(event);
  }
}
