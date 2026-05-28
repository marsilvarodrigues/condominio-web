package com.pmrodrigues.performance;

/*
 * ═══════════════════════════════════════════════════════════════════════════════
 *  Teste de Performance – Condomínio Web APIs
 *  Ferramenta: Apache JMeter 5.6.3 via API Java (modo headless, sem GUI)
 * ═══════════════════════════════════════════════════════════════════════════════
 *
 *  POR QUE JMETER VIA API JAVA?
 *  ─────────────────────────────
 *  O JMeter normalmente é usado pela interface gráfica ou pela linha de comando
 *  com arquivos .jmx. A API Java permite criar o mesmo plano de teste em código,
 *  com vantagens:
 *    • Versionado junto com o projeto (sem arquivos binários .jmx no Git)
 *    • Integrado ao Maven lifecycle (mvn test -Pperformance)
 *    • Parâmetros tipados e refatoráveis como qualquer código Java
 *    • Mais fácil de revisar em pull requests
 *
 *  COBERTURA: todas as APIs da aplicação
 *  ─────────────────────────────────────
 *
 *    Auth          POST /api/auth/login               → extrai jwt_token + refresh_token
 *                  POST /api/auth/refresh             → renova accessToken
 *                  POST /api/auth/logout              → invalida token (blacklist Redis)
 *
 *    Estados       GET  /api/estados                  → lista (cache L1 Caffeine)
 *                  GET  /api/estados/{id}             → por ID
 *
 *    Users         GET  /api/users                    → lista todos
 *                  POST /api/users         (ADMIN)    → cria → extrai created_user_id
 *                  GET  /api/users/{id}               → por ID
 *                  PUT  /api/users/{id}    (ADMIN)    → atualiza
 *                  DELETE /api/users/{id} (ADMIN)     → soft-delete
 *
 *    Condominios   GET  /api/condominios              → lista (Specification + cache)
 *                  POST /api/condominios   (ADMIN)    → cria → extrai created_condominio_id
 *                  GET  /api/condominios/{id}         → por ID
 *                  PUT  /api/condominios/{id} (ADMIN) → atualiza
 *                  DELETE /api/condominios/{id} (ADMIN) → soft-delete
 *
 *    Blocos        GET  /api/blocos                   → lista (Hibernate tenant filter)
 *                  POST /api/blocos        (ADMIN)    → cria → extrai created_bloco_id
 *                  GET  /api/blocos/{id}              → por ID
 *                  PUT  /api/blocos/{id}   (ADMIN)    → atualiza
 *                  DELETE /api/blocos/{id} (ADMIN)    → soft-delete
 *
 *    Apartamentos  GET  /api/apartamentos             → lista (JPA Specification)
 *                  POST /api/apartamentos  (ADMIN)    → cria → extrai created_apt_id
 *                  GET  /api/apartamentos/{id}        → por ID
 *                  PUT  /api/apartamentos/{id} (ADMIN)→ atualiza
 *                  DELETE /api/apartamentos/{id} (ADMIN) → soft-delete
 *
 *  ESTRUTURA DO PLANO DE TESTE
 *  ────────────────────────────
 *    TestPlan  (serialized=true → fases em sequência)
 *    │
 *    ├─ [Fase 1] ThreadGroup "Aquecimento" – 10 usuários, 30 s, ramp-up 10 s
 *    │   ├─ POST /api/auth/login           → ${jwt_token}, ${refresh_token}
 *    │   ├─ HeaderManager JWT              → Bearer ${jwt_token} em todos os samplers
 *    │   ├─ Estados: GET /estados, GET /estados/1
 *    │   ├─ Users:   GET all, POST, GET/{id}, PUT/{id}, DELETE/{id}
 *    │   ├─ Condominios: GET all, POST, GET/{id}, PUT/{id}, DELETE/{id}
 *    │   ├─ Blocos: GET all, POST, GET/{id}, PUT/{id}, DELETE/{id}
 *    │   ├─ Apartamentos: GET all, POST, GET/{id}, PUT/{id}, DELETE/{id}
 *    │   └─ Auth: POST /refresh, POST /logout
 *    │
 *    ├─ [Fase 2] ThreadGroup "Carga Máxima" – 50 usuários, 60 s, ramp-up 20 s
 *    │   └─ (mesmo cenário da Fase 1)
 *    │
 *    └─ ResultCollector → target/performance-results/results.jtl
 *
 *  UNICIDADE DOS DADOS
 *  ────────────────────
 *  Cada thread precisa criar recursos com identificadores únicos para evitar
 *  violações de constraints UNIQUE no banco (email, cnpj, etc.). O JMeter
 *  resolve ${...} em tempo de execução:
 *    ${__threadNum}    → número da thread (1 a N), constante durante a fase
 *    ${__counter(TRUE,)} → contador crescente por thread (1, 2, 3 ... por iteração)
 *  Exemplo de email: t1_1@perf.test, t1_2@perf.test, t2_1@perf.test ...
 *
 *  PRÉ-REQUISITOS
 *  ───────────────
 *  1. Aplicação rodando em localhost:8080
 *  2. Usuário admin existente com credenciais:
 *       email: admin@condominio.com  senha: admin123
 *     (sobrescrevível via -Dapp.user=... -Dapp.pass=...)
 *  3. Para POST /blocos e POST /apartamentos funcionarem: o usuário admin deve
 *     ter condominioId configurado em seu registro. Sem condominioId no JWT,
 *     o @PrePersist não consegue setar a FK e a criação retornará 500. As
 *     demais rotas não são afetadas.
 *
 *  COMO EXECUTAR
 *  ─────────────
 *    mvn test -Pperformance
 *
 *  Com parâmetros customizados:
 *    mvn test -Pperformance \
 *      -Dapp.host=meu-servidor.com \
 *      -Dapp.port=8080             \
 *      -Dapp.user=admin@x.com     \
 *      -Dapp.pass=minhasenha
 *
 *  ANALISANDO OS RESULTADOS
 *  ─────────────────────────
 *  Os resultados são salvos em: target/performance-results/results.jtl
 *
 *  Opção A – Relatório HTML (requer JMeter instalado localmente):
 *    jmeter -g target/performance-results/results.jtl \
 *           -o target/performance-results/html-report/
 *
 *  Opção B – Importar no JMeter GUI:
 *    File > Open Recent > results.jtl → adicionar "Summary Report" e
 *    "Response Time Graph" como listeners
 *
 *  Opção C – Grafana (se o stack Docker estiver rodando):
 *    http://localhost:3000 → dashboard "Condomínio Web" → painel JVM/HTTP/HikariCP
 */

import org.apache.jmeter.assertions.ResponseAssertion;
import org.apache.jmeter.control.LoopController;
import org.apache.jmeter.engine.StandardJMeterEngine;
import org.apache.jmeter.extractor.RegexExtractor;
import org.apache.jmeter.protocol.http.control.Header;
import org.apache.jmeter.protocol.http.control.HeaderManager;
import org.apache.jmeter.protocol.http.sampler.HTTPSamplerProxy;
import org.apache.jmeter.reporters.ResultCollector;
import org.apache.jmeter.reporters.Summariser;
import org.apache.jmeter.testelement.TestPlan;
import org.apache.jmeter.threads.ThreadGroup;
import org.apache.jmeter.util.JMeterUtils;
import org.apache.jorphan.collections.ListedHashTree;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * Plano de performance em dois estágios com cobertura completa de todas as APIs:
 * <ul>
 *   <li>Fase 1 – Aquecimento: {@value #WARMUP_USERS} usuários por {@value #WARMUP_DURATION_S} s</li>
 *   <li>Fase 2 – Pico:        {@value #PEAK_USERS}   usuários por {@value #PEAK_DURATION_S} s</li>
 * </ul>
 *
 * Executar com: {@code mvn test -Pperformance}
 */
@Tag("performance")   // JUnit 5: permite filtrar com -Dgroups=performance
class CondominioPerformanceTest {

    // ── Alvo do teste ─────────────────────────────────────────────────────────
    // Lidos de system properties passadas pelo Maven profile "performance".
    // Podem ser sobrescritos na linha de comando: -Dapp.host=192.168.0.1
    private static final String HOST = System.getProperty("app.host", "localhost");
    private static final int    PORT = Integer.parseInt(System.getProperty("app.port", "8080"));
    private static final String USER = System.getProperty("app.user", "admin@condominio.com");
    private static final String PASS = System.getProperty("app.pass", "admin123");

    // ── Parâmetros de carga ───────────────────────────────────────────────────

    /** Quantidade mínima de usuários virtuais (fase de aquecimento). */
    private static final int WARMUP_USERS = 10;

    /**
     * Tempo de ramp-up da fase 1 em segundos.
     * O JMeter distribuirá as threads igualmente neste intervalo:
     * um novo usuário virtual a cada (WARMUP_RAMPUP_S / WARMUP_USERS) segundos.
     */
    private static final int WARMUP_RAMPUP_S = 10;

    /** Duração total da fase de aquecimento em segundos. */
    private static final int WARMUP_DURATION_S = 30;

    /** Quantidade máxima de usuários virtuais (fase de carga de pico). */
    private static final int PEAK_USERS = 50;

    /** Tempo de ramp-up da fase 2: 50 usuários em 20 s → 2,5 novos usuários/s. */
    private static final int PEAK_RAMPUP_S = 20;

    /** Duração total da fase de pico em segundos. */
    private static final int PEAK_DURATION_S = 60;

    // ── Saída ─────────────────────────────────────────────────────────────────
    private static final Path RESULTS_DIR = Paths.get("target", "performance-results");
    private static final String JTL_FILE  = "results.jtl";

    // ─────────────────────────────────────────────────────────────────────────
    // INICIALIZAÇÃO DO JMETER
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * O JMeter exige que seu diretório "home" esteja configurado antes de qualquer
     * uso da API. Em modo headless (sem instalação local) criamos um diretório
     * temporário com um {@code jmeter.properties} mínimo — suficiente para
     * iniciar o engine e salvar resultados.
     */
    @BeforeAll
    static void inicializarJMeter() throws IOException {
        // Cria um diretório temporário que simula o JMETER_HOME
        Path jmeterHome = Files.createTempDirectory("jmeter-home");

        // O JMeter espera encontrar o arquivo de propriedades em JMETER_HOME/bin/
        Path binDir = jmeterHome.resolve("bin");
        Files.createDirectories(binDir);

        // Gera o jmeter.properties mínimo no diretório bin/
        File propsFile = binDir.resolve("jmeter.properties").toFile();
        escreverPropriedadesMinimas(propsFile);

        // Registra o home e carrega as propriedades no JMeter
        JMeterUtils.setJMeterHome(jmeterHome.toString());
        JMeterUtils.loadJMeterProperties(propsFile.getAbsolutePath());

        // Inicializa o locale (necessário para formatação de resultados)
        JMeterUtils.initLocale();

        // Garante que o diretório de resultados exista
        Files.createDirectories(RESULTS_DIR);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PLANO DE TESTE
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Performance: CRUD completo em todas as APIs")
    void executarTesteDeCarga() throws Exception {

        // ── 1. Engine ─────────────────────────────────────────────────────────
        // StandardJMeterEngine é o motor de execução local do JMeter.
        // Ele roda um único nó (single-machine), suficiente para até ~500 threads.
        // Para testes distribuídos em múltiplas máquinas usaríamos RemoteJMeterEngine.
        StandardJMeterEngine engine = new StandardJMeterEngine();

        // ── 2. HashTree — a estrutura de dados central do JMeter ──────────────
        // O JMeter modela o plano como uma árvore onde cada elemento pai
        // define o escopo dos elementos filho.
        //
        // ListedHashTree em vez de HashTree: o StandardJMeterEngine exige ListedHashTree
        // (preserva a ordem de inserção — crítico para as fases rodarem em sequência).
        // HashTree simples não garante ordem e gera ClassCastException no engine.
        ListedHashTree planoRaiz = new ListedHashTree();

        // ── 3. TestPlan — raiz de todo plano JMeter ───────────────────────────
        TestPlan testPlan = new TestPlan("Plano de Performance – Condomínio APIs");

        // setSerialized(true) → ThreadGroups executam UM DE CADA VEZ (sequencial).
        // Isso garante que a Fase 1 (aquecimento) termine antes da Fase 2 (pico).
        testPlan.setSerialized(true);

        // Modo funcional salva o conteúdo completo das respostas no .jtl.
        // false = modo performance: salva apenas metadados (tempo, status, etc.)
        testPlan.setFunctionalMode(false);

        ListedHashTree testPlanTree = (ListedHashTree) planoRaiz.add(testPlan);

        // ── 4. Fase 1: Aquecimento ────────────────────────────────────────────
        // O aquecimento serve para:
        //   a) Aquecer a JVM da aplicação (JIT compilation)
        //   b) Popular caches (Caffeine + Redis)
        //   c) Estabelecer conexões no pool do HikariCP
        // Sem aquecimento, os primeiros resultados seriam artificialmente lentos.
        ListedHashTree warmupTree = (ListedHashTree) testPlanTree.add(
            criarThreadGroup("Fase 1 – Aquecimento", WARMUP_USERS, WARMUP_RAMPUP_S, WARMUP_DURATION_S)
        );
        adicionarCenarioCompleto(warmupTree, "warmup");

        // ── 5. Fase 2: Carga de Pico ──────────────────────────────────────────
        // Com 50 usuários e ramp-up de 20 s, a carga cresce gradualmente
        // (2,5 novos usuários/segundo), evitando um spike artificial.
        ListedHashTree peakTree = (ListedHashTree) testPlanTree.add(
            criarThreadGroup("Fase 2 – Carga Máxima", PEAK_USERS, PEAK_RAMPUP_S, PEAK_DURATION_S)
        );
        adicionarCenarioCompleto(peakTree, "peak");

        // ── 6. Listener: ResultCollector ──────────────────────────────────────
        // O Summariser imprime um resumo no console a cada 30 s (padrão JMeter)
        // com média, throughput e taxa de erro.
        Summariser summariser = new Summariser("Resumo");

        // O ResultCollector persiste cada amostra no arquivo .jtl (formato CSV).
        ResultCollector resultCollector = new ResultCollector(summariser);
        resultCollector.setFilename(
            RESULTS_DIR.resolve(JTL_FILE).toAbsolutePath().toString()
        );

        // Filho direto do TestPlan → escuta TODOS os samplers de todas as fases.
        testPlanTree.add(resultCollector);

        // ── 7. Execução ───────────────────────────────────────────────────────
        engine.configure(planoRaiz);

        System.out.printf("%n%s%n", "═".repeat(60));
        System.out.println("  TESTE DE PERFORMANCE – CONDOMÍNIO WEB");
        System.out.printf("%s%n", "═".repeat(60));
        System.out.printf("  Alvo    : http://%s:%d/api%n", HOST, PORT);
        System.out.printf("  Fase 1  : %d usuários × %d s (ramp-up %d s)%n",
            WARMUP_USERS, WARMUP_DURATION_S, WARMUP_RAMPUP_S);
        System.out.printf("  Fase 2  : %d usuários × %d s (ramp-up %d s)%n",
            PEAK_USERS, PEAK_DURATION_S, PEAK_RAMPUP_S);
        System.out.printf("  Duração : ~%d s total%n", WARMUP_DURATION_S + PEAK_DURATION_S);
        System.out.printf("  Saída   : %s%n",
            RESULTS_DIR.resolve(JTL_FILE).toAbsolutePath());
        System.out.printf("%s%n%n", "═".repeat(60));

        // engine.run() bloqueia até o plano de teste terminar completamente.
        engine.run();

        System.out.printf("%n  Teste concluído. Resultados em: %s%n",
            RESULTS_DIR.resolve(JTL_FILE).toAbsolutePath());
        System.out.printf("  Para gerar relatório HTML:%n");
        System.out.printf("  jmeter -g %s -o %s%n%n",
            RESULTS_DIR.resolve(JTL_FILE).toAbsolutePath(),
            RESULTS_DIR.resolve("html-report").toAbsolutePath());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ORQUESTRAÇÃO DO CENÁRIO
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Adiciona ao ThreadGroup o ciclo completo de todas as APIs.
     *
     * <p>Sequência por iteração de cada thread:
     * <ol>
     *   <li>Login → JWT</li>
     *   <li>Estados (read-only)</li>
     *   <li>Users CRUD</li>
     *   <li>Condominios CRUD</li>
     *   <li>Blocos CRUD (requer condominioId no JWT)</li>
     *   <li>Apartamentos CRUD (requer condominioId + blocoId criado)</li>
     *   <li>Refresh de token + Logout</li>
     * </ol>
     *
     * @param groupTree sub-árvore do ThreadGroup
     * @param prefixo   prefixo para nomear os samplers nos relatórios (ex.: "warmup", "peak")
     */
    private void adicionarCenarioCompleto(ListedHashTree groupTree, String prefixo) {

        // ── Login ─────────────────────────────────────────────────────────────
        // Cada thread autentica individualmente ao início de cada iteração.
        // O JWT extraído é reutilizado por todos os samplers desta iteração.
        ListedHashTree loginTree = (ListedHashTree) groupTree.add(criarSamplerLogin(prefixo));
        loginTree.add(criarExtractorJwt());
        loginTree.add(criarExtractorRefreshToken());
        loginTree.add(criarAssercaoHttpStatus("200"));

        // ── HeaderManager com JWT ─────────────────────────────────────────────
        // Adicionado como filho direto do ThreadGroup, se aplica a TODOS os
        // samplers subsequentes neste grupo (exceto o login acima, que já foi
        // adicionado antes). O JMeter resolve ${jwt_token} em runtime.
        groupTree.add(criarHeaderManagerJwt());

        // ── Sub-cenários por domínio ──────────────────────────────────────────
        adicionarCenarioEstados(groupTree, prefixo);
        adicionarCenarioUsers(groupTree, prefixo);
        adicionarCenarioCondominios(groupTree, prefixo);
        adicionarCenarioBlocos(groupTree, prefixo);
        adicionarCenarioApartamentos(groupTree, prefixo);
        adicionarCenarioAuthRefresh(groupTree, prefixo);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SUB-CENÁRIOS POR DOMÍNIO
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Testa os endpoints de Estado (read-only).
     *
     * <p>Estados são dados de referência. Após a primeira carga, a resposta
     * deve ser servida pelo cache L1 (Caffeine) — latência esperada < 1 ms.
     * Este cenário verifica se o cache não introduz inconsistências.
     */
    private void adicionarCenarioEstados(ListedHashTree groupTree, String prefixo) {
        // Lista todos: testa o cache L1 com a chave "all"
        ListedHashTree allTree = (ListedHashTree) groupTree.add(
            criarSamplerGet(prefixo + "_estados_all", "/api/estados"));
        allTree.add(criarAssercaoHttpStatus("200"));

        // Por ID fixo 1: estados são semeados pelo Liquibase e sempre existem.
        // Testa o cache L1 com chave de ID.
        ListedHashTree byIdTree = (ListedHashTree) groupTree.add(
            criarSamplerGet(prefixo + "_estados_id", "/api/estados/1"));
        byIdTree.add(criarAssercaoHttpStatus("200"));
    }

    /**
     * Testa o CRUD completo de usuários.
     *
     * <p>Emails são gerados com {@code ${__threadNum}} e {@code ${__counter(TRUE,)}}
     * para garantir unicidade entre threads e entre iterações da mesma thread.
     * Como o delete é soft (deleted=true), o mesmo email não pode ser reutilizado
     * em iterações posteriores sem violar a constraint UNIQUE.
     */
    private void adicionarCenarioUsers(ListedHashTree groupTree, String prefixo) {

        // GET /users — lista todos os usuários (ADMIN pode ver todos)
        ListedHashTree allTree = (ListedHashTree) groupTree.add(
            criarSamplerGet(prefixo + "_users_all", "/api/users"));
        allTree.add(criarAssercaoHttpStatus("200"));

        // POST /users — cria um usuário temporário exclusivo desta iteração.
        // ${__threadNum}:    número da thread (1 a PEAK_USERS)
        // ${__counter(TRUE,)}: incrementa a cada iteração dentro da thread
        // Resultado: t1_1@perf.test, t1_2@perf.test, t2_1@perf.test, ...
        String bodyCreate = """
            {
              "email": "t${__threadNum}_${__counter(TRUE,)}@perf.test",
              "name": "Perf User T${__threadNum}",
              "enabled": true,
              "roles": ["ROLE_USER"]
            }
            """;
        ListedHashTree createTree = (ListedHashTree) groupTree.add(
            criarSamplerPost(prefixo + "_users_create", "/api/users", bodyCreate));
        // Extrai o id do JSON de resposta: {"data":{"id":123,...}}
        // O RegexExtractor busca "id": seguido de dígitos — primeiro match = id do recurso criado.
        createTree.add(criarExtractorId("created_user_id"));
        createTree.add(criarAssercaoHttpStatus("201"));

        // GET /users/{id} — lê o usuário recém-criado para verificar idempotência
        ListedHashTree byIdTree = (ListedHashTree) groupTree.add(
            criarSamplerGet(prefixo + "_users_id", "/api/users/${created_user_id}"));
        byIdTree.add(criarAssercaoHttpStatus("200"));

        // PUT /users/{id} — atualiza o nome; email deve permanecer único
        String bodyUpdate = """
            {
              "email": "t${__threadNum}_${__counter(TRUE,)}_upd@perf.test",
              "name": "Perf User T${__threadNum} Updated",
              "enabled": true,
              "roles": ["ROLE_USER"]
            }
            """;
        ListedHashTree updateTree = (ListedHashTree) groupTree.add(
            criarSamplerPut(prefixo + "_users_update", "/api/users/${created_user_id}", bodyUpdate));
        updateTree.add(criarAssercaoHttpStatus("200"));

        // DELETE /users/{id} — soft-delete: sets deleted=true no banco via @SQLDelete
        ListedHashTree deleteTree = (ListedHashTree) groupTree.add(
            criarSamplerDelete(prefixo + "_users_delete", "/api/users/${created_user_id}"));
        deleteTree.add(criarAssercaoHttpStatus("204"));
    }

    /**
     * Testa o CRUD completo de Condomínios.
     *
     * <p>Condomínios são globais (não tenant-scoped). O POST retorna 201 com o
     * objeto criado no campo {@code data.id}, que é extraído e reutilizado nas
     * chamadas GET/{id}, PUT/{id} e DELETE/{id} dentro desta iteração.
     */
    private void adicionarCenarioCondominios(ListedHashTree groupTree, String prefixo) {

        // GET /condominios — lista com JPA Specification (usa cache L2 Redis)
        ListedHashTree allTree = (ListedHashTree) groupTree.add(
            criarSamplerGet(prefixo + "_cond_all", "/api/condominios"));
        allTree.add(criarAssercaoHttpStatus("200"));

        // POST /condominios — cria condomínio temporário.
        // CNPJ: sem validação de formato; precisa ser único por thread+iteração.
        String bodyCreate = """
            {
              "nome": "Cond Perf T${__threadNum} I${__counter(TRUE,)}",
              "cnpj": "T${__threadNum}I${__counter(TRUE,)}",
              "email": "cond${__threadNum}@perf.test"
            }
            """;
        ListedHashTree createTree = (ListedHashTree) groupTree.add(
            criarSamplerPost(prefixo + "_cond_create", "/api/condominios", bodyCreate));
        createTree.add(criarExtractorId("created_condominio_id"));
        createTree.add(criarAssercaoHttpStatus("201"));

        // GET /condominios/{id}
        ListedHashTree byIdTree = (ListedHashTree) groupTree.add(
            criarSamplerGet(prefixo + "_cond_id", "/api/condominios/${created_condominio_id}"));
        byIdTree.add(criarAssercaoHttpStatus("200"));

        // PUT /condominios/{id}
        String bodyUpdate = """
            {
              "nome": "Cond Perf T${__threadNum} Updated",
              "cnpj": "T${__threadNum}I${__counter(TRUE,)}U",
              "email": "cond${__threadNum}upd@perf.test"
            }
            """;
        ListedHashTree updateTree = (ListedHashTree) groupTree.add(
            criarSamplerPut(prefixo + "_cond_update", "/api/condominios/${created_condominio_id}", bodyUpdate));
        updateTree.add(criarAssercaoHttpStatus("200"));

        // DELETE /condominios/{id} — soft-delete
        ListedHashTree deleteTree = (ListedHashTree) groupTree.add(
            criarSamplerDelete(prefixo + "_cond_delete", "/api/condominios/${created_condominio_id}"));
        deleteTree.add(criarAssercaoHttpStatus("204"));
    }

    /**
     * Testa o CRUD completo de Blocos.
     *
     * <p><b>Atenção:</b> a criação de Blocos requer que o JWT do admin contenha
     * o claim {@code condominio_id}. O {@code @PrePersist} lê o {@code TenantContext}
     * (populado pelo {@code CustomBearerTokenFilter} a partir deste claim) para
     * setar a FK {@code condominio}. Se o admin não tiver {@code condominioId}
     * no seu registro, o POST retornará 500 e os samplers by-id/update/delete
     * falharão em cascata (created_bloco_id = "0" → GET /blocos/0 → 404).
     */
    private void adicionarCenarioBlocos(ListedHashTree groupTree, String prefixo) {

        // GET /blocos — lista filtrada pelo Hibernate tenant filter (condominioFilter).
        // Retorna apenas blocos do condomínio do JWT. Se não houver condominioId
        // no JWT, o filtro não é aplicado e retorna todos.
        ListedHashTree allTree = (ListedHashTree) groupTree.add(
            criarSamplerGet(prefixo + "_bloco_all", "/api/blocos"));
        allTree.add(criarAssercaoHttpStatus("200"));

        // POST /blocos — o numero usa ${__counter(TRUE,)} para evitar conflito de
        // constraint de unicidade entre iterações da mesma thread.
        String bodyCreate = """
            {
              "numero": ${__counter(TRUE,)},
              "bloco": "P"
            }
            """;
        ListedHashTree createTree = (ListedHashTree) groupTree.add(
            criarSamplerPost(prefixo + "_bloco_create", "/api/blocos", bodyCreate));
        createTree.add(criarExtractorId("created_bloco_id"));
        createTree.add(criarAssercaoHttpStatus("201"));

        // GET /blocos/{id}
        ListedHashTree byIdTree = (ListedHashTree) groupTree.add(
            criarSamplerGet(prefixo + "_bloco_id", "/api/blocos/${created_bloco_id}"));
        byIdTree.add(criarAssercaoHttpStatus("200"));

        // PUT /blocos/{id}
        String bodyUpdate = """
            {
              "numero": ${__counter(TRUE,)},
              "bloco": "Q"
            }
            """;
        ListedHashTree updateTree = (ListedHashTree) groupTree.add(
            criarSamplerPut(prefixo + "_bloco_update", "/api/blocos/${created_bloco_id}", bodyUpdate));
        updateTree.add(criarAssercaoHttpStatus("200"));

        // DELETE /blocos/{id} — executado ANTES dos apartamentos serem criados,
        // mas os apartamentos já existentes não são afetados (soft-delete).
        // Nota: apartamentos criados nesta iteração referenciam este bloco e são
        // deletados no cenário de apartamentos, que vem a seguir.
        ListedHashTree deleteTree = (ListedHashTree) groupTree.add(
            criarSamplerDelete(prefixo + "_bloco_delete", "/api/blocos/${created_bloco_id}"));
        deleteTree.add(criarAssercaoHttpStatus("204"));
    }

    /**
     * Testa o CRUD completo de Apartamentos.
     *
     * <p>O apartamento referencia o bloco criado no cenário anterior via
     * {@code ${created_bloco_id}}. A variável JMeter é resolvida em runtime,
     * reusando o ID extraído pelo {@code criarExtractorId("created_bloco_id")}.
     *
     * <p>O campo {@code numero} é uma String no modelo — qualquer valor não nulo
     * é aceito. Usamos o contador para evitar repetição.
     */
    private void adicionarCenarioApartamentos(ListedHashTree groupTree, String prefixo) {

        // GET /apartamentos — usa JPA Specification para filtro + Hibernate tenant filter.
        // Testa a camada mais densa da stack: Specification + filter + cache L2.
        ListedHashTree allTree = (ListedHashTree) groupTree.add(
            criarSamplerGet(prefixo + "_apt_all", "/api/apartamentos"));
        allTree.add(criarAssercaoHttpStatus("200"));

        // POST /apartamentos — referencia o bloco criado nesta iteração.
        // Se created_bloco_id == "0" (bloco não foi criado), a FK será inválida e
        // o servidor retornará 4xx/5xx — registrado como falha no relatório.
        String bodyCreate = """
            {
              "blocoId": ${created_bloco_id},
              "numero": "${__counter(TRUE,)}"
            }
            """;
        ListedHashTree createTree = (ListedHashTree) groupTree.add(
            criarSamplerPost(prefixo + "_apt_create", "/api/apartamentos", bodyCreate));
        createTree.add(criarExtractorId("created_apt_id"));
        createTree.add(criarAssercaoHttpStatus("201"));

        // GET /apartamentos/{id}
        ListedHashTree byIdTree = (ListedHashTree) groupTree.add(
            criarSamplerGet(prefixo + "_apt_id", "/api/apartamentos/${created_apt_id}"));
        byIdTree.add(criarAssercaoHttpStatus("200"));

        // PUT /apartamentos/{id} — atualiza apenas o numero (blocoId é ignorado no update)
        String bodyUpdate = """
            {
              "numero": "${__counter(TRUE,)}_upd"
            }
            """;
        ListedHashTree updateTree = (ListedHashTree) groupTree.add(
            criarSamplerPut(prefixo + "_apt_update", "/api/apartamentos/${created_apt_id}", bodyUpdate));
        updateTree.add(criarAssercaoHttpStatus("200"));

        // DELETE /apartamentos/{id}
        ListedHashTree deleteTree = (ListedHashTree) groupTree.add(
            criarSamplerDelete(prefixo + "_apt_delete", "/api/apartamentos/${created_apt_id}"));
        deleteTree.add(criarAssercaoHttpStatus("204"));
    }

    /**
     * Testa o refresh de token e o logout ao final de cada iteração.
     *
     * <p>O refresh exercita o Redis: o refresh token é armazenado com TTL
     * e deve ser encontrado na chave gerada para o usuário. O logout adiciona
     * o JWT atual à blacklist também no Redis.
     */
    private void adicionarCenarioAuthRefresh(ListedHashTree groupTree, String prefixo) {

        // POST /auth/refresh — usa o refresh_token extraído no login.
        // Retorna um novo accessToken que substitui o anterior.
        // O novo token é extraído e sobrescreve ${jwt_token} para uso no logout.
        String bodyRefresh = """
            {
              "refreshToken": "${refresh_token}"
            }
            """;
        ListedHashTree refreshTree = (ListedHashTree) groupTree.add(
            criarSamplerPost(prefixo + "_auth_refresh", "/api/auth/refresh", bodyRefresh));
        // Sobrescreve jwt_token com o novo accessToken para o logout a seguir
        refreshTree.add(criarExtractorJwt());
        refreshTree.add(criarAssercaoHttpStatus("200"));

        // POST /auth/logout — invalida o token atual na blacklist do Redis.
        // Requer o JWT no header Authorization, que já está no HeaderManager.
        // Após este passo, ${jwt_token} não é mais válido — mas a iteração termina aqui.
        ListedHashTree logoutTree = (ListedHashTree) groupTree.add(
            criarSamplerPost(prefixo + "_auth_logout", "/api/auth/logout", ""));
        logoutTree.add(criarAssercaoHttpStatus("204"));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // FÁBRICAS DE ELEMENTOS JMETER — ESTRUTURA
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Cria um {@link ThreadGroup} — o elemento que representa um grupo de
     * usuários virtuais.
     *
     * <p>Conceitos importantes:
     * <ul>
     *   <li><b>Threads</b>: usuários virtuais simultâneos. Cada thread executa
     *       os samplers de forma independente, com seu próprio estado (variáveis,
     *       conexões HTTP).</li>
     *   <li><b>Ramp-up</b>: tempo para atingir o total de threads. Simula chegada
     *       gradual de usuários.</li>
     *   <li><b>Duration</b>: com {@code setScheduler(true)}, o ThreadGroup para
     *       quando o tempo esgota.</li>
     *   <li><b>LoopController com loops=-1</b>: cada thread repete indefinidamente
     *       o ciclo de samplers até o duration esgotar.</li>
     * </ul>
     */
    private ThreadGroup criarThreadGroup(String nome, int threads, int rampUpS, int duracaoS) {
        LoopController loopController = new LoopController();
        loopController.setName("Loop Controller");
        loopController.setLoops(-1);              // -1 = infinito
        loopController.setContinueForever(true);  // necessário junto com loops=-1
        loopController.initialize();

        ThreadGroup group = new ThreadGroup();
        group.setName(nome);
        group.setNumThreads(threads);
        group.setRampUp(rampUpS);
        group.setDuration(duracaoS);
        group.setScheduler(true);                 // usa duration em vez de loops
        group.setSamplerController(loopController);
        return group;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // FÁBRICAS DE ELEMENTOS JMETER — SAMPLERS HTTP
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Cria o {@link HTTPSamplerProxy} para autenticação (POST /api/auth/login).
     *
     * <p>{@code postBodyRaw=true} instrui o JMeter a enviar o body como texto puro,
     * sem encoding de formulário — equivale ao "Body Data" no JMeter GUI.
     */
    private HTTPSamplerProxy criarSamplerLogin(String prefixo) {
        HTTPSamplerProxy sampler = new HTTPSamplerProxy();
        sampler.setName(prefixo + "_auth_login");
        sampler.setDomain(HOST);
        sampler.setPort(PORT);
        sampler.setPath("/api/auth/login");
        sampler.setMethod("POST");
        sampler.setPostBodyRaw(true);
        // USER e PASS são resolvidos em tempo de compilação Java — não são variáveis JMeter
        sampler.addNonEncodedArgument("",
            String.format("{\"username\":\"%s\",\"password\":\"%s\"}", USER, PASS), "");
        sampler.setUseKeepAlive(true);
        sampler.setFollowRedirects(true);
        return sampler;
    }

    /**
     * Cria um {@link HTTPSamplerProxy} para requisições GET.
     *
     * @param nome nome exibido nos relatórios
     * @param path caminho completo — pode conter variáveis JMeter como {@code ${id}}
     */
    private HTTPSamplerProxy criarSamplerGet(String nome, String path) {
        HTTPSamplerProxy sampler = new HTTPSamplerProxy();
        sampler.setName(nome);
        sampler.setDomain(HOST);
        sampler.setPort(PORT);
        sampler.setPath(path);
        sampler.setMethod("GET");
        sampler.setUseKeepAlive(true);
        sampler.setFollowRedirects(true);
        return sampler;
    }

    /**
     * Cria um {@link HTTPSamplerProxy} para requisições POST com body JSON.
     *
     * <p>O parâmetro {@code body} pode conter variáveis JMeter como
     * {@code ${__threadNum}} e {@code ${__counter(TRUE,)}} — o JMeter as
     * resolve em runtime antes de enviar a requisição.
     *
     * @param nome  nome exibido nos relatórios
     * @param path  caminho completo da API
     * @param body  body JSON (pode conter variáveis JMeter {@code ${}})
     */
    private HTTPSamplerProxy criarSamplerPost(String nome, String path, String body) {
        HTTPSamplerProxy sampler = new HTTPSamplerProxy();
        sampler.setName(nome);
        sampler.setDomain(HOST);
        sampler.setPort(PORT);
        sampler.setPath(path);
        sampler.setMethod("POST");
        sampler.setPostBodyRaw(true);
        sampler.addNonEncodedArgument("", body, "");
        sampler.setUseKeepAlive(true);
        sampler.setFollowRedirects(true);
        return sampler;
    }

    /**
     * Cria um {@link HTTPSamplerProxy} para requisições PUT com body JSON.
     *
     * <p>O path pode conter variáveis JMeter como {@code ${created_bloco_id}}
     * extraídas do POST anterior na mesma iteração.
     *
     * @param nome  nome exibido nos relatórios
     * @param path  caminho completo — pode conter variáveis JMeter
     * @param body  body JSON — pode conter variáveis JMeter
     */
    private HTTPSamplerProxy criarSamplerPut(String nome, String path, String body) {
        HTTPSamplerProxy sampler = new HTTPSamplerProxy();
        sampler.setName(nome);
        sampler.setDomain(HOST);
        sampler.setPort(PORT);
        sampler.setPath(path);
        sampler.setMethod("PUT");
        sampler.setPostBodyRaw(true);
        sampler.addNonEncodedArgument("", body, "");
        sampler.setUseKeepAlive(true);
        sampler.setFollowRedirects(true);
        return sampler;
    }

    /**
     * Cria um {@link HTTPSamplerProxy} para requisições DELETE.
     *
     * <p>O path pode conter variáveis JMeter como {@code ${created_user_id}}.
     * DELETE não tem body — os IDs são passados na URL.
     *
     * @param nome nome exibido nos relatórios
     * @param path caminho completo — pode conter variáveis JMeter
     */
    private HTTPSamplerProxy criarSamplerDelete(String nome, String path) {
        HTTPSamplerProxy sampler = new HTTPSamplerProxy();
        sampler.setName(nome);
        sampler.setDomain(HOST);
        sampler.setPort(PORT);
        sampler.setPath(path);
        sampler.setMethod("DELETE");
        sampler.setUseKeepAlive(true);
        sampler.setFollowRedirects(true);
        return sampler;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // FÁBRICAS DE ELEMENTOS JMETER — PÓS-PROCESSADORES E ASSERÇÕES
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Cria o {@link RegexExtractor} que captura o accessToken da resposta de login.
     *
     * <p>Como funciona a extração:
     * <pre>
     *  Resposta JSON:  {"accessToken":"eyJhbGci...","refreshToken":"eyJhb..."}
     *  Regex:          "accessToken"\s*:\s*"([^"]+)"
     *  Template:       $1$  → conteúdo do grupo capturado
     *  Variável:       jwt_token → acessível como ${jwt_token}
     * </pre>
     *
     * <p>A variável é <b>local à thread</b>: cada usuário virtual tem o seu
     * próprio token, evitando colisões entre threads simultâneas.
     */
    private RegexExtractor criarExtractorJwt() {
        RegexExtractor extractor = new RegexExtractor();
        extractor.setName("Extrai accessToken → ${jwt_token}");
        extractor.setRegex("\"accessToken\"\\s*:\\s*\"([^\"]+)\"");
        extractor.setTemplate("$1$");
        // "0" = usa a primeira ocorrência (a API pública não expõe setMatchNo())
        extractor.setProperty("RegexExtractor.match_number", "0");
        extractor.setRefName("jwt_token");
        extractor.setDefaultValue("TOKEN_NAO_ENCONTRADO");
        // Inspeciona o body da resposta (não headers nem URL)
        extractor.setProperty("RegexExtractor.useHeaders", "false");
        return extractor;
    }

    /**
     * Cria o {@link RegexExtractor} que captura o refreshToken da resposta de login.
     *
     * <p>O refreshToken é armazenado em {@code ${refresh_token}} e reutilizado
     * no sampler {@code POST /auth/refresh} ao final de cada iteração.
     */
    private RegexExtractor criarExtractorRefreshToken() {
        RegexExtractor extractor = new RegexExtractor();
        extractor.setName("Extrai refreshToken → ${refresh_token}");
        extractor.setRegex("\"refreshToken\"\\s*:\\s*\"([^\"]+)\"");
        extractor.setTemplate("$1$");
        extractor.setProperty("RegexExtractor.match_number", "0");
        extractor.setRefName("refresh_token");
        extractor.setDefaultValue("REFRESH_TOKEN_NAO_ENCONTRADO");
        extractor.setProperty("RegexExtractor.useHeaders", "false");
        return extractor;
    }

    /**
     * Cria um {@link RegexExtractor} que captura o {@code id} do recurso criado
     * da resposta de um POST.
     *
     * <p>A aplicação retorna todos os recursos no envelope {@code ApiResponse<T>}:
     * <pre>
     *  {"requestId":"uuid","data":{"id":42,"nome":"...",...}}
     * </pre>
     *
     * A regex {@code "id"\s*:\s*(\d+)} captura a primeira ocorrência de {@code id}
     * no body — que sempre será o {@code data.id} do recurso criado.
     *
     * <p>O valor padrão {@code "0"} é usado se a criação falhar (ex.: login
     * inválido → 401 → body sem id). As chamadas subsequentes ({@code GET /x/0},
     * {@code DELETE /x/0}) retornarão 404 e serão registradas como falha no JTL —
     * o que é o comportamento correto para diagnóstico.
     *
     * @param variableName nome da variável JMeter onde armazenar o id (ex.: "created_user_id")
     */
    private RegexExtractor criarExtractorId(String variableName) {
        RegexExtractor extractor = new RegexExtractor();
        extractor.setName("Extrai id → ${" + variableName + "}");
        extractor.setRegex("\"id\"\\s*:\\s*(\\d+)");
        extractor.setTemplate("$1$");
        extractor.setProperty("RegexExtractor.match_number", "0");
        extractor.setRefName(variableName);
        // "0" como sentinela: indica que a criação falhou (GET/0 → 404)
        extractor.setDefaultValue("0");
        extractor.setProperty("RegexExtractor.useHeaders", "false");
        return extractor;
    }

    /**
     * Cria o {@link HeaderManager} com o token JWT e os headers padrão.
     *
     * <p>O HeaderManager adicionado como filho do ThreadGroup herda para todos
     * os samplers do grupo. O JMeter resolve {@code ${jwt_token}} em runtime
     * com o valor extraído pelo RegexExtractor na mesma iteração desta thread.
     */
    private HeaderManager criarHeaderManagerJwt() {
        HeaderManager manager = new HeaderManager();
        manager.setName("Headers de Autenticação");
        // ${jwt_token} é local à thread — cada usuário virtual usa seu próprio token
        manager.add(new Header("Authorization", "Bearer ${jwt_token}"));
        manager.add(new Header("Content-Type", "application/json"));
        manager.add(new Header("Accept", "application/json"));
        // X-Correlation-ID propagado pelo MdcContextFilter para rastreio nos logs Loki
        manager.add(new Header("X-Correlation-ID", "perf-test-${__threadNum}"));
        return manager;
    }

    /**
     * Cria uma {@link ResponseAssertion} que valida o código HTTP da resposta.
     *
     * <p>Se a asserção falhar, o sampler é marcado como "FAIL" no arquivo .jtl,
     * incrementando a taxa de erros no relatório — sem interromper o teste.
     *
     * @param codigoEsperado código HTTP esperado (ex.: "200", "201", "204")
     */
    private ResponseAssertion criarAssercaoHttpStatus(String codigoEsperado) {
        ResponseAssertion assertion = new ResponseAssertion();
        assertion.setName("Verifica HTTP " + codigoEsperado);
        // Define O QUE será testado: o código de resposta HTTP
        assertion.setTestFieldResponseCode();
        // Tipo de comparação: EQUALS (o código deve ser exatamente igual)
        assertion.setToEqualsType();
        assertion.addTestString(codigoEsperado);
        return assertion;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CONFIGURAÇÃO INTERNA DO JMETER
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Gera um arquivo {@code jmeter.properties} mínimo para inicialização headless.
     *
     * <p>O JMeter usa este arquivo para configurar como os resultados são salvos
     * (formato CSV vs XML, quais campos incluir) e o nível de log interno.
     *
     * @param arquivo arquivo de destino (em JMETER_HOME/bin/jmeter.properties)
     */
    private static void escreverPropriedadesMinimas(File arquivo) throws IOException {
        Properties props = new Properties();

        // ── Formato de saída ──────────────────────────────────────────────────
        // CSV é mais compacto e rápido de gravar que XML.
        // Para grandes volumes (ex.: 50 threads × 60 s × ~25 samplers por iteração)
        // o XML pode crescer para centenas de MB.
        props.setProperty("jmeter.save.saveservice.output_format", "csv");
        props.setProperty("jmeter.save.saveservice.print_field_names", "true");

        // ── Campos salvos no CSV ──────────────────────────────────────────────
        props.setProperty("jmeter.save.saveservice.time", "true");           // tempo total (ms)
        props.setProperty("jmeter.save.saveservice.latency", "true");        // tempo até 1º byte
        props.setProperty("jmeter.save.saveservice.connect_time", "true");   // TCP connect
        props.setProperty("jmeter.save.saveservice.label", "true");          // nome do sampler
        props.setProperty("jmeter.save.saveservice.thread_name", "true");    // nome da thread
        props.setProperty("jmeter.save.saveservice.response_code", "true");  // HTTP status
        props.setProperty("jmeter.save.saveservice.response_message", "true");
        props.setProperty("jmeter.save.saveservice.successful", "true");     // passou asserções
        props.setProperty("jmeter.save.saveservice.bytes", "true");          // tamanho resposta
        props.setProperty("jmeter.save.saveservice.url", "true");            // URL
        props.setProperty("jmeter.save.saveservice.assertions", "true");     // detalhes asserções

        // Timestamp em epoch milliseconds (facilita integração com Grafana/Loki)
        props.setProperty("jmeter.save.saveservice.timestamp_format", "ms");

        // ── Log interno do JMeter ─────────────────────────────────────────────
        // WARN evita poluir o console do Maven com logs internos do JMeter.
        props.setProperty("log_level.jmeter", "WARN");
        props.setProperty("log_level.jorphan", "WARN");

        // Número de conexões no pool HTTP por host
        props.setProperty("httpclient4.max_conn_per_host", "10");

        try (OutputStream out = new FileOutputStream(arquivo)) {
            props.store(out, "jmeter.properties mínimo para execução headless");
        }
    }
}
