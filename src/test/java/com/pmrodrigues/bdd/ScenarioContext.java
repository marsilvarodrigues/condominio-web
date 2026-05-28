package com.pmrodrigues.bdd;

import io.cucumber.spring.ScenarioScope;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.springframework.stereotype.Component;

/**
 * Estado compartilhado entre os step definitions de um cenário Cucumber.
 *
 * <p>{@code @ScenarioScope} garante que uma nova instância seja criada para
 * cada cenário — equivalente ao "World Object" do Cucumber. Sem isso, o estado
 * de um cenário vazaria para o próximo, causando falsos positivos ou negativos.
 *
 * <p>Cada step definition injecta esta classe via {@code @Autowired ScenarioContext ctx}
 * e acumula/lê o estado:
 * <ul>
 *   <li>{@code accessToken} / {@code refreshToken} — token do usuário atual</li>
 *   <li>{@code lastStatus} — status HTTP da última resposta</li>
 *   <li>{@code lastResponseBody} — body da última resposta como String JSON</li>
 *   <li>{@code lastCreatedId} — id extraído do último POST bem-sucedido</li>
 *   <li>{@code testCondominioId} — id do condomínio criado no @Before do cenário</li>
 *   <li>{@code testBlocoId} — id do bloco criado em cenários de apartamento</li>
 * </ul>
 */
@Component
@ScenarioScope
@Getter
@Setter
@Accessors(chain = true)
public class ScenarioContext {

    private String accessToken;
    private String refreshToken;
    private int lastStatus;
    private String lastResponseBody;
    private Long lastCreatedId;
    private Long testCondominioId;
    private Long testEstadoId;
    private Long testBlocoId;
    private String lastCreatedEmail;
    private Long masterUserId;
    private Long regularUserId;
}
