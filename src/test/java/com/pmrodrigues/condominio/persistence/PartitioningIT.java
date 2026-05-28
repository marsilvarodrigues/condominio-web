package com.pmrodrigues.condominio.persistence;

import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies PostgreSQL HASH partitioning on blocos and apartamentos.
 * Runs against a real PostgreSQL instance (Testcontainers) with full Liquibase migrations.
 * H2-based @DataJpaTest tests cannot verify partitioning — this is the authoritative check.
 */
@Testcontainers
class PartitioningIT {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    private static Connection conn;

    @BeforeAll
    static void runMigrations() throws Exception {
        conn = DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        var database = DatabaseFactory.getInstance()
                .findCorrectDatabaseImplementation(new JdbcConnection(conn));
        new Liquibase("db/changelog/db.changelog-master.yaml",
                new ClassLoaderResourceAccessor(), database)
                .update(new Contexts(), new LabelExpression());
    }

    @AfterAll
    static void cleanup() throws Exception {
        if (conn != null && !conn.isClosed()) conn.close();
    }

    // ── helpers ───────────────────────────────────────────────────────────

    /** Returns "HASH (col)" or null if the table is not partitioned. */
    private String partitionDef(String table) throws Exception {
        var sql = "SELECT pg_get_partkeydef(c.oid) FROM pg_class c WHERE c.relname = ? AND c.relkind = 'p'";
        try (var ps = conn.prepareStatement(sql)) {
            ps.setString(1, table);
            var rs = ps.executeQuery();
            return rs.next() ? rs.getString(1) : null;
        }
    }

    /** Counts the child partition tables of a partitioned parent. */
    private int partitionCount(String parentTable) throws Exception {
        var sql = """
                SELECT COUNT(*) FROM pg_inherits i
                JOIN pg_class p ON p.oid = i.inhparent
                WHERE p.relname = ?
                """;
        try (var ps = conn.prepareStatement(sql)) {
            ps.setString(1, parentTable);
            var rs = ps.executeQuery();
            rs.next();
            return rs.getInt(1);
        }
    }

    /** Returns names of all child partition tables for a parent. */
    private Set<String> partitionNames(String parentTable) throws Exception {
        var sql = """
                SELECT c.relname FROM pg_inherits i
                JOIN pg_class p ON p.oid = i.inhparent
                JOIN pg_class c ON c.oid = i.inhrelid
                WHERE p.relname = ?
                """;
        try (var ps = conn.prepareStatement(sql)) {
            ps.setString(1, parentTable);
            var rs = ps.executeQuery();
            var names = new java.util.HashSet<String>();
            while (rs.next()) names.add(rs.getString(1));
            return names;
        }
    }

    /**
     * Inserts minimal fixture data (estado → condominio) and returns condominioId.
     * Uses a savepoint so each routing test can rollback independently.
     */
    private long insertTestCondominio() throws Exception {
        conn.setAutoCommit(false);
        long estadoId;
        try (var ps = conn.prepareStatement(
                "INSERT INTO estados (nome, uf) VALUES ('Teste', 'TT') RETURNING id")) {
            var rs = ps.executeQuery();
            rs.next();
            estadoId = rs.getLong(1);
        }
        try (var ps = conn.prepareStatement("""
                INSERT INTO condominios (nome, cnpj, email, logradouro, cep, cidade, estado_id)
                VALUES ('Cond Teste', '00.000.000/0001-00', 'teste@test.com',
                        'Rua Teste', '00000000', 'Cidade', ?)
                RETURNING id
                """)) {
            ps.setLong(1, estadoId);
            var rs = ps.executeQuery();
            rs.next();
            return rs.getLong(1);
        }
    }

    /** Returns the actual partition table that contains the row, e.g. "blocos_p2". */
    private String rowPartition(String baseTable, long rowId) throws Exception {
        var sql = "SELECT tableoid::regclass::text FROM " + baseTable + " WHERE id = ?";
        try (var ps = conn.prepareStatement(sql)) {
            ps.setLong(1, rowId);
            var rs = ps.executeQuery();
            return rs.next() ? rs.getString(1) : null;
        }
    }

    // ── blocos metadata ───────────────────────────────────────────────────

    @Test
    void blocos_isHashPartitionedByCondominioId() throws Exception {
        assertThat(partitionDef("blocos")).isEqualTo("HASH (condominio_id)");
    }

    @Test
    void blocos_hasFourPartitions() throws Exception {
        assertThat(partitionCount("blocos")).isEqualTo(4);
    }

    @Test
    void blocos_partitionNamesAreCorrect() throws Exception {
        assertThat(partitionNames("blocos"))
                .containsExactlyInAnyOrder("blocos_p0", "blocos_p1", "blocos_p2", "blocos_p3");
    }

    // ── apartamentos metadata ─────────────────────────────────────────────

    @Test
    void apartamentos_isHashPartitionedByCondominioId() throws Exception {
        assertThat(partitionDef("apartamentos")).isEqualTo("HASH (condominio_id)");
    }

    @Test
    void apartamentos_hasFourPartitions() throws Exception {
        assertThat(partitionCount("apartamentos")).isEqualTo(4);
    }

    @Test
    void apartamentos_partitionNamesAreCorrect() throws Exception {
        assertThat(partitionNames("apartamentos"))
                .containsExactlyInAnyOrder(
                        "apartamentos_p0", "apartamentos_p1",
                        "apartamentos_p2", "apartamentos_p3");
    }

    // ── partition routing ─────────────────────────────────────────────────

    @Test
    void blocos_rowIsRoutedToOneOfTheFourPartitions() throws Exception {
        long condominioId = insertTestCondominio();
        long blocoId;
        try (var ps = conn.prepareStatement(
                "INSERT INTO blocos (condominio_id, numero, bloco) VALUES (?, 1, 'A') RETURNING id")) {
            ps.setLong(1, condominioId);
            var rs = ps.executeQuery();
            rs.next();
            blocoId = rs.getLong(1);
        }

        String partition = rowPartition("blocos", blocoId);

        assertThat(partition).isIn("blocos_p0", "blocos_p1", "blocos_p2", "blocos_p3");
        conn.rollback();
        conn.setAutoCommit(true);
    }

    @Test
    void apartamentos_rowIsRoutedToOneOfTheFourPartitions() throws Exception {
        long condominioId = insertTestCondominio();
        long blocoId;
        try (var ps = conn.prepareStatement(
                "INSERT INTO blocos (condominio_id, numero, bloco) VALUES (?, 1, 'A') RETURNING id")) {
            ps.setLong(1, condominioId);
            var rs = ps.executeQuery();
            rs.next();
            blocoId = rs.getLong(1);
        }
        long apartamentoId;
        try (var ps = conn.prepareStatement(
                "INSERT INTO apartamentos (condominio_id, bloco_id, numero) VALUES (?, ?, '101') RETURNING id")) {
            ps.setLong(1, condominioId);
            ps.setLong(2, blocoId);
            var rs = ps.executeQuery();
            rs.next();
            apartamentoId = rs.getLong(1);
        }

        String partition = rowPartition("apartamentos", apartamentoId);

        assertThat(partition).isIn(
                "apartamentos_p0", "apartamentos_p1",
                "apartamentos_p2", "apartamentos_p3");
        conn.rollback();
        conn.setAutoCommit(true);
    }

    @Test
    void blocos_sameCondominioIdAlwaysRoutesToSamePartition() throws Exception {
        long condominioId = insertTestCondominio();
        long blocoId1, blocoId2;
        try (var ps = conn.prepareStatement(
                "INSERT INTO blocos (condominio_id, numero, bloco) VALUES (?, 1, 'A') RETURNING id")) {
            ps.setLong(1, condominioId);
            var rs = ps.executeQuery();
            rs.next();
            blocoId1 = rs.getLong(1);
        }
        try (var ps = conn.prepareStatement(
                "INSERT INTO blocos (condominio_id, numero, bloco) VALUES (?, 2, 'B') RETURNING id")) {
            ps.setLong(1, condominioId);
            var rs = ps.executeQuery();
            rs.next();
            blocoId2 = rs.getLong(1);
        }

        assertThat(rowPartition("blocos", blocoId1))
                .isEqualTo(rowPartition("blocos", blocoId2));

        conn.rollback();
        conn.setAutoCommit(true);
    }
}
