package com.pmrodrigues.bdd;

import org.junit.jupiter.api.Tag;
import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectClasspathResource;
import org.junit.platform.suite.api.Suite;

import static io.cucumber.junit.platform.engine.Constants.*;

/**
 * Runner JUnit Platform para os testes BDD com Cucumber.
 *
 * <p>O Surefire descobre esta classe pelo sufixo {@code Test} e a executa
 * via JUnit Platform Suite Engine. O Cucumber Engine então:
 * <ol>
 *   <li>Carrega os feature files de {@code classpath:features/}</li>
 *   <li>Encontra os step definitions no pacote {@code com.pmrodrigues.bdd}</li>
 *   <li>Usa o contexto Spring configurado em {@link CucumberSpringConfiguration}</li>
 * </ol>
 *
 * <p>Para executar somente os testes BDD:
 * <pre>
 *   mvn test -Dtest=CucumberTest
 * </pre>
 */
@Tag("e2e")
@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "com.pmrodrigues.bdd")
@ConfigurationParameter(key = PLUGIN_PROPERTY_NAME,
        value = "pretty, html:target/cucumber-reports/index.html, json:target/cucumber-reports/results.json")
@ConfigurationParameter(key = PLUGIN_PUBLISH_QUIET_PROPERTY_NAME, value = "true")
public class CucumberTest {
}
