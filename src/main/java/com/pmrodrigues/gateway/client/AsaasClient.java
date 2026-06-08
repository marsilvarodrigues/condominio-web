package com.pmrodrigues.gateway.client;

import com.pmrodrigues.gateway.config.AsaasProperties;
import com.pmrodrigues.gateway.dto.AsaasCobrancaRequest;
import com.pmrodrigues.gateway.dto.AsaasCobrancaResponse;
import com.pmrodrigues.gateway.dto.AsaasCustomerRequest;
import com.pmrodrigues.gateway.dto.AsaasCustomerResponse;
import io.micrometer.core.annotation.Timed;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;

/**
 * HTTP client for the Asaas payment gateway API.
 *
 * <p>All methods call {@code .block()} because they are invoked from virtual-thread
 * worker contexts — blocking is safe and avoids reactive context propagation overhead.
 *
 * <p>The {@code access_token} header is injected on every request via a default header
 * configured at construction time.
 */
@Slf4j
@Component
public class AsaasClient {

    private final WebClient webClient;

    /**
     * Builds a {@link WebClient} pre-configured with the Asaas base URL and API key.
     *
     * @param props Asaas gateway connection properties
     */
    public AsaasClient(AsaasProperties props) {
        this.webClient = WebClient.builder()
                .baseUrl(props.baseUrl())
                .defaultHeader("access_token", props.apiKey())
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    /**
     * Creates a customer in Asaas and returns their generated customer ID.
     *
     * @param request customer data (name, CPF/CNPJ, email, phone)
     * @return Asaas customer ID (e.g. {@code cus_000123456789})
     * @throws IllegalStateException if the API key is invalid (HTTP 401)
     * @throws ResponseStatusException for other HTTP 4xx/5xx errors
     */
    @Timed(value = "asaas.client.criarCustomer", description = "Create Asaas customer")
    public String criarCustomer(AsaasCustomerRequest request) {
        log.info("Creating Asaas customer: name={}", request.name());
        AsaasCustomerResponse response = webClient.post()
                .uri("/customers")
                .bodyValue(request)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, resp ->
                        resp.bodyToMono(String.class).map(body -> {
                            if (resp.statusCode() == HttpStatus.UNAUTHORIZED) {
                                log.error("Asaas API key invalid");
                                return new IllegalStateException("Asaas API key inválida");
                            }
                            return new ResponseStatusException(HttpStatus.BAD_REQUEST, body);
                        }))
                .onStatus(HttpStatusCode::is5xxServerError, resp ->
                        resp.bodyToMono(String.class).map(body ->
                                new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Asaas error: " + body)))
                .bodyToMono(AsaasCustomerResponse.class)
                .block();
        log.info("Asaas customer created: customerId={}", response.id());
        return response.id();
    }

    /**
     * Creates a BOLETO_PIX charge in Asaas (both payment methods simultaneously).
     *
     * @param request charge parameters (customer, value, dueDate, billing type, fine, interest)
     * @return full Asaas charge response with ID, boleto URL, and Pix payload
     * @throws IllegalStateException if the API key is invalid (HTTP 401)
     * @throws ResponseStatusException for other HTTP 4xx/5xx errors
     */
    @Timed(value = "asaas.client.criarCobranca", description = "Create Asaas charge")
    public AsaasCobrancaResponse criarCobranca(AsaasCobrancaRequest request) {
        log.info("Creating Asaas charge: customer={} value={} dueDate={}",
                request.customer(), request.value(), request.dueDate());
        return webClient.post()
                .uri("/payments")
                .bodyValue(request)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, resp ->
                        resp.bodyToMono(String.class).map(body -> {
                            if (resp.statusCode() == HttpStatus.UNAUTHORIZED) {
                                log.error("Asaas API key invalid");
                                return new IllegalStateException("Asaas API key inválida");
                            }
                            return new ResponseStatusException(HttpStatus.BAD_REQUEST, body);
                        }))
                .onStatus(HttpStatusCode::is5xxServerError, resp ->
                        resp.bodyToMono(String.class).map(body ->
                                new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Asaas error: " + body)))
                .bodyToMono(AsaasCobrancaResponse.class)
                .block();
    }

    /**
     * Cancels a charge in Asaas by its external payment ID.
     *
     * @param asaasId the Asaas payment ID (e.g. {@code pay_000123456789})
     * @throws ResponseStatusException for HTTP 4xx/5xx errors
     */
    @Timed(value = "asaas.client.cancelarCobranca", description = "Cancel Asaas charge")
    public void cancelarCobranca(String asaasId) {
        log.info("Cancelling Asaas charge: asaasId={}", asaasId);
        webClient.delete()
                .uri("/payments/{id}", asaasId)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, resp ->
                        resp.bodyToMono(String.class).map(body ->
                                new ResponseStatusException(HttpStatus.BAD_REQUEST, body)))
                .onStatus(HttpStatusCode::is5xxServerError, resp ->
                        resp.bodyToMono(String.class).map(body ->
                                new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Asaas error: " + body)))
                .bodyToMono(Void.class)
                .block();
        log.info("Asaas charge cancelled: asaasId={}", asaasId);
    }

    /**
     * Fetches the current state of a charge from Asaas.
     *
     * @param asaasId the Asaas payment ID
     * @return full charge response reflecting the current Asaas state
     * @throws ResponseStatusException for HTTP 4xx/5xx errors
     */
    @Timed(value = "asaas.client.buscarCobranca", description = "Fetch Asaas charge")
    public AsaasCobrancaResponse buscarCobranca(String asaasId) {
        log.info("Fetching Asaas charge: asaasId={}", asaasId);
        return webClient.get()
                .uri("/payments/{id}", asaasId)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, resp ->
                        resp.bodyToMono(String.class).map(body ->
                                new ResponseStatusException(HttpStatus.BAD_REQUEST, body)))
                .onStatus(HttpStatusCode::is5xxServerError, resp ->
                        resp.bodyToMono(String.class).map(body ->
                                new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Asaas error: " + body)))
                .bodyToMono(AsaasCobrancaResponse.class)
                .block();
    }
}
