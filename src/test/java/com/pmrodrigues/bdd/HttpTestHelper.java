package com.pmrodrigues.bdd;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.spring.ScenarioScope;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.UUID;

/**
 * Helper de HTTP para steps Cucumber — usa {@link RestTemplate} contra o servidor real.
 *
 * <p>Diferença em relação ao MockMvc: as requisições percorrem toda a pilha HTTP —
 * filtros de servlet, interceptors, serializacao JSON, resolução de content-type —
 * tornando os testes mais próximos do comportamento em produção.
 *
 * <p>Comportamentos automáticos:
 * <ul>
 *   <li>Header {@code Authorization: Bearer {token}} quando
 *       {@link ScenarioContext#getAccessToken()} estiver preenchido</li>
 *   <li>Header {@code X-Request-ID} com UUID aleatório em cada requisição</li>
 *   <li>Nunca lança exceção para status 4xx/5xx — o status é armazenado em
 *       {@link ScenarioContext#getLastStatus()} para assertions nos steps</li>
 *   <li>Após cada POST: tenta extrair {@code $.data.id} e armazenar em
 *       {@link ScenarioContext#getLastCreatedId()}</li>
 * </ul>
 */
@Component
@ScenarioScope
public class HttpTestHelper {

    private static final String REQUEST_ID_HEADER = "X-Request-ID";

    private final ScenarioContext ctx;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;
    private final String baseUrl;

    public HttpTestHelper(ScenarioContext ctx,
                          ObjectMapper objectMapper,
                          @Value("${local.server.port}") int serverPort) {
        this.ctx = ctx;
        this.objectMapper = objectMapper;
        this.baseUrl = "http://localhost:" + serverPort + "/api";
        // HttpComponentsClientHttpRequestFactory enables PATCH support (HttpURLConnection does not).
        this.restTemplate = new RestTemplate(new HttpComponentsClientHttpRequestFactory());
        // Nunca lançar exceção — status 4xx/5xx são verificados explicitamente nos steps.
        this.restTemplate.setErrorHandler(new ResponseErrorHandler() {
            @Override public boolean hasError(ClientHttpResponse r) { return false; }
            @Override public void handleError(ClientHttpResponse r) {}
        });
    }

    public void get(String path) {
        execute(HttpMethod.GET, path, null);
    }

    public void post(String path, String body) {
        execute(HttpMethod.POST, path, body);
        tryExtractId();
    }

    public void put(String path, String body) {
        execute(HttpMethod.PUT, path, body);
    }

    public void patch(String path, String body) {
        execute(HttpMethod.PATCH, path, body);
    }

    public void delete(String path) {
        execute(HttpMethod.DELETE, path, null);
    }

    public void deleteWithBody(String path, String body) {
        execute(HttpMethod.DELETE, path, body);
    }

    private void execute(HttpMethod method, String path, String body) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(REQUEST_ID_HEADER, UUID.randomUUID().toString());
        if (ctx.getAccessToken() != null) {
            headers.setBearerAuth(ctx.getAccessToken());
        }
        if (body != null) {
            headers.setContentType(MediaType.APPLICATION_JSON);
        }

        var entity = new HttpEntity<>(body, headers);
        var uri = UriComponentsBuilder.fromUriString(baseUrl + path).build().encode().toUri();
        var response = restTemplate.exchange(uri, method, entity, String.class);

        ctx.setLastStatus(response.getStatusCode().value());
        ctx.setLastResponseBody(response.getBody() != null ? response.getBody() : "");
    }

    /** Tenta extrair {@code $.data.id} do body e armazenar em ctx.lastCreatedId. */
    private void tryExtractId() {
        try {
            String body = ctx.getLastResponseBody();
            if (body != null && !body.isBlank()) {
                JsonNode data = objectMapper.readTree(body).path("data");
                if (data.has("id")) {
                    ctx.setLastCreatedId(data.get("id").asLong());
                }
            }
        } catch (Exception ignored) {
        }
    }
}
