package com.pmrodrigues.bdd.hooks;

import com.pmrodrigues.bdd.ScenarioContext;
import com.pmrodrigues.commons.embeddable.Endereco;
import com.pmrodrigues.commons.model.Estado;
import com.pmrodrigues.commons.repository.EstadoRepository;
import com.pmrodrigues.condominio.model.Bloco;
import com.pmrodrigues.condominio.model.Condominio;
import com.pmrodrigues.condominio.repository.BlocoRepository;
import com.pmrodrigues.condominio.repository.CondominioRepository;
import io.cucumber.java.Before;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Hooks Cucumber executados antes de cada cenário.
 *
 * <p>O {@code @Before} (Cucumber) limpa todas as tabelas e recria o estado mínimo
 * necessário para os testes BDD:
 *
 * <ul>
 *   <li><b>Usuário master</b>: ROLE_ADMIN <em>sem</em> {@code condominioId}.
 *       Criado diretamente via JDBC (não via API REST), contornando a validação
 *       {@code @NotNull} do UserDTO — que é exatamente o comportamento desejado:
 *       o código proíbe criar master via API, mas o SQL permite.</li>
 *   <li><b>Admin de condomínio</b>: ROLE_ADMIN <em>com</em> {@code condominioId},
 *       necessário para criar Blocos e Apartamentos (o JWT precisa do claim
 *       {@code condominio_id} para que o {@code @PrePersist} popule a FK).</li>
 *   <li><b>Usuário regular</b>: ROLE_USER, para testar restrições de acesso (403).</li>
 *   <li><b>Condomínio de teste</b>: armazenado em {@link ScenarioContext#getTestCondominioId()}
 *       e usado pelos steps que precisam de um ID de condomínio válido.</li>
 *   <li><b>Bloco de teste</b>: armazenado em {@link ScenarioContext#getTestBlocoId()}
 *       para cenários de Apartamento.</li>
 * </ul>
 *
 * <p><b>Nota sobre auto-commit:</b> {@code application.yaml} define
 * {@code hikari.auto-commit=false} para evitar round-trips desnecessários no
 * Hibernate ({@code connection.provider_disables_autocommit=true}). Por isso,
 * cada {@code jdbc.update()} sem transação explícita usa sua própria connection
 * que é revertida ao ser devolvida ao pool. Este hook envolve toda a lógica de
 * setup em um {@code TransactionTemplate} para garantir o commit.
 *
 * <p>Para criar o usuário master em produção use SQL diretamente:
 * <pre>
 *   -- Gere o hash: new BCryptPasswordEncoder().encode("senha-forte")
 *   INSERT INTO users (email, password, name, enabled, deleted)
 *   VALUES ('master@condominio.com', '{hash}', 'Master Admin', true, false);
 *   INSERT INTO user_roles (user_id, role)
 *   SELECT id, 'ROLE_ADMIN' FROM users WHERE email = 'master@condominio.com';
 * </pre>
 */
@RequiredArgsConstructor
public class DatabaseSetupHooks {

    // credenciais dos usuários de teste — constantes acessíveis pelos step definitions
    public static final String MASTER_EMAIL    = "master@bdd.com";
    public static final String MASTER_PASSWORD = "Master@123";
    public static final String ADMIN_EMAIL     = "admin@bdd.com";
    public static final String ADMIN_PASSWORD  = "Admin@123";
    public static final String USER_EMAIL      = "user@bdd.com";
    public static final String USER_PASSWORD   = "User@123";

    private final JdbcTemplate jdbc;
    private final PasswordEncoder passwordEncoder;
    private final EstadoRepository estadoRepository;
    private final CondominioRepository condominioRepository;
    private final BlocoRepository blocoRepository;
    private final ScenarioContext ctx;
    private final PlatformTransactionManager txManager;

    /**
     * Executado antes de CADA cenário. A ordem de deleção respeita as FKs:
     * apartamentos → blocos → user_roles → users → condominios.
     * Estados não são deletados pois são dados de referência semeados pelo Liquibase.
     */
    @Before
    public void prepararBancoDeDados() {
        new TransactionTemplate(txManager).execute(status -> {
            limparTabelas();
            var condominio = criarCondominioTeste();
            ctx.setTestCondominioId(condominio.getId());
            ctx.setTestEstadoId(condominio.getEndereco().getEstado().getId());

            criarUsuarioMasterViaSql();
            criarAdminCondominio(condominio.getId());
            criarUsuarioRegular(condominio.getId());

            var bloco = blocoRepository.save(
                    Bloco.builder().condominio(condominio).numero(1).bloco("A").build());
            ctx.setTestBlocoId(bloco.getId());
            return null;
        });
    }

    private void limparTabelas() {
        // itens_extrato FK → item_orcamento, extrato_importacoes, lancamentos_bancarios
        jdbc.update("DELETE FROM itens_extrato");
        jdbc.update("DELETE FROM extrato_importacoes");
        jdbc.update("DELETE FROM item_orcamento");
        jdbc.update("DELETE FROM orcamento_anual");
        jdbc.update("DELETE FROM fundo_reserva_movimentacao");
        jdbc.update("DELETE FROM fundo_reserva");
        jdbc.update("DELETE FROM plano_contas");
        jdbc.update("DELETE FROM lancamentos_bancarios");
        jdbc.update("DELETE FROM contas_bancarias");
        jdbc.update("DELETE FROM bancos");
        jdbc.update("DELETE FROM apartamentos");
        jdbc.update("DELETE FROM blocos");
        jdbc.update("DELETE FROM user_roles");
        jdbc.update("DELETE FROM users");
        jdbc.update("DELETE FROM condominios");
        // estados mantidos — semeados pelo Liquibase (0006-insert-estados.sql)
    }

    private Condominio criarCondominioTeste() {
        var estados = estadoRepository.findAll();
        Estado estado;
        if (estados.isEmpty()) {
            estado = estadoRepository.save(Estado.builder().nome("São Paulo").uf("SP").build());
        } else {
            estado = estados.get(0);
        }
        var endereco = Endereco.builder()
                .logradouro("Av. BDD, 100").cep("01001000").cidade("São Paulo").estado(estado).build();
        return condominioRepository.save(Condominio.builder()
                .nome("Cond Test BDD").cnpj("00.000.000/0001-00")
                .email("bdd@test.com").endereco(endereco).build());
    }

    /**
     * Cria o usuário master SEM condominioId via JDBC puro.
     * Este é o bypass intencional da validação @NotNull do UserDTO.
     * Sem condominioId no JWT, o Hibernate tenant filter não é aplicado
     * → o master enxerga dados de todos os condomínios.
     */
    private void criarUsuarioMasterViaSql() {
        String hash = passwordEncoder.encode(MASTER_PASSWORD);
        jdbc.update(
            "INSERT INTO users (email, password, name, enabled, deleted) VALUES (?,?,?,?,?)",
            MASTER_EMAIL, hash, "Master Admin BDD", true, false);
        jdbc.update(
            "INSERT INTO user_roles (user_id, role) SELECT id, 'ROLE_ADMIN' FROM users WHERE email = ?",
            MASTER_EMAIL);
        Long masterId = jdbc.queryForObject(
            "SELECT id FROM users WHERE email = ?", Long.class, MASTER_EMAIL);
        ctx.setMasterUserId(masterId);
    }

    private void criarAdminCondominio(Long condominioId) {
        String hash = passwordEncoder.encode(ADMIN_PASSWORD);
        jdbc.update(
            "INSERT INTO users (email, password, name, enabled, deleted, condominio_id) VALUES (?,?,?,?,?,?)",
            ADMIN_EMAIL, hash, "Admin BDD", true, false, condominioId);
        jdbc.update(
            "INSERT INTO user_roles (user_id, role) SELECT id, 'ROLE_ADMIN' FROM users WHERE email = ?",
            ADMIN_EMAIL);
    }

    private void criarUsuarioRegular(Long condominioId) {
        String hash = passwordEncoder.encode(USER_PASSWORD);
        jdbc.update(
            "INSERT INTO users (email, password, name, enabled, deleted, condominio_id) VALUES (?,?,?,?,?,?)",
            USER_EMAIL, hash, "User BDD", true, false, condominioId);
        jdbc.update(
            "INSERT INTO user_roles (user_id, role) SELECT id, 'ROLE_USER' FROM users WHERE email = ?",
            USER_EMAIL);
        Long regularId = jdbc.queryForObject(
            "SELECT id FROM users WHERE email = ?", Long.class, USER_EMAIL);
        ctx.setRegularUserId(regularId);
    }
}
