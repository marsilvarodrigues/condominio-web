package com.pmrodrigues.gateway.service;

import com.pmrodrigues.gateway.client.AsaasClient;
import com.pmrodrigues.gateway.client.PixQrCodeGenerator;
import com.pmrodrigues.gateway.dto.AsaasCobrancaRequest;
import com.pmrodrigues.gateway.dto.AsaasCustomerRequest;
import com.pmrodrigues.gateway.dto.AsaasEmissaoRequest;
import com.pmrodrigues.gateway.dto.AsaasEmissaoResult;
import com.pmrodrigues.gateway.dto.AsaasFineRequest;
import com.pmrodrigues.gateway.dto.AsaasInterestRequest;
import com.pmrodrigues.gateway.model.AsaasCustomer;
import com.pmrodrigues.gateway.repository.AsaasCustomerRepository;
import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Application service for Asaas gateway operations.
 *
 * <p>Orchestrates customer resolution, charge creation, and Pix QR Code generation into a single
 * operation ({@link #emitir}), shielding callers from the multi-step Asaas API flow.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AsaasGatewayService {

  private final AsaasClient asaasClient;
  private final PixQrCodeGenerator qrCodeGenerator;
  private final AsaasCustomerRepository asaasCustomerRepository;

  /**
   * Resolves or creates an Asaas customer, then emits a BOLETO_PIX charge with QR Code.
   *
   * <p>The Asaas customer lookup/creation is cached in {@code asaas_customers}. If a mapping
   * already exists for the given {@code pessoaId} it is reused; otherwise a new Asaas customer is
   * created and the mapping is persisted.
   *
   * @param req all data needed to issue the charge
   * @return charge identifiers and payment tokens ready for persistence and email delivery
   */
  @Timed(value = "gateway.service.emitir", description = "Emit Asaas BOLETO_PIX charge")
  @Transactional
  public AsaasEmissaoResult emitir(AsaasEmissaoRequest req) {
    log.info(
        "emitir pessoaId={} valor={} vencimento={}", req.pessoaId(), req.valor(), req.vencimento());

    String customerId = resolverCustomerId(req.pessoaId(), req.nome(), req.cpf(), req.email());

    var asaasRequest =
        new AsaasCobrancaRequest(
            customerId,
            req.valor(),
            req.vencimento(),
            "BOLETO_PIX",
            req.descricao(),
            new AsaasFineRequest(req.multaPercent(), "PERCENTAGE"),
            new AsaasInterestRequest(req.jurosMoraPercent()));

    var asaasResp = asaasClient.criarCobranca(asaasRequest);

    String qrCodeBase64 = null;
    String pixPayload = null;
    if (asaasResp.pix() != null) {
      pixPayload = asaasResp.pix().payload();
      if (pixPayload != null && !pixPayload.isBlank()) {
        qrCodeBase64 = qrCodeGenerator.gerarBase64(pixPayload);
      }
    }

    log.info("Asaas charge emitted: asaasId={} customerId={}", asaasResp.id(), customerId);
    return new AsaasEmissaoResult(
        asaasResp.id(),
        customerId,
        asaasResp.bankSlipUrl(),
        asaasResp.nossoNumero(),
        qrCodeBase64,
        pixPayload);
  }

  /**
   * Cancels a charge in Asaas.
   *
   * @param asaasId Asaas payment ID (e.g. {@code pay_000123456789})
   */
  @Timed(value = "gateway.service.cancelar", description = "Cancel Asaas charge")
  public void cancelar(String asaasId) {
    log.info("cancelar asaasId={}", asaasId);
    asaasClient.cancelarCobranca(asaasId);
  }

  // ── Private helpers ────────────────────────────────────────────────────────

  private String resolverCustomerId(Long pessoaId, String nome, String cpf, String email) {
    return asaasCustomerRepository
        .findByPessoaId(pessoaId)
        .map(AsaasCustomer::getCustomerId)
        .orElseGet(
            () -> {
              String customerId =
                  asaasClient.criarCustomer(new AsaasCustomerRequest(nome, cpf, email, null));
              asaasCustomerRepository.save(
                  AsaasCustomer.builder().pessoaId(pessoaId).customerId(customerId).build());
              log.info("Created Asaas customer pessoaId={} customerId={}", pessoaId, customerId);
              return customerId;
            });
  }
}
