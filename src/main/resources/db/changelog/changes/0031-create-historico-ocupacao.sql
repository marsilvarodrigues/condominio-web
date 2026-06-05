-- liquibase formatted sql
-- changeset pmrodrigues:0031-drop-historico-ocupacao dbms:postgresql
-- HistoricoOcupacao replaced by direct Pessoa.apartamento_id FK (1-to-many).
-- Also drops the old partitioned proprietarios table, replaced by proprietario_apartamentos join table.
DROP TABLE IF EXISTS historico_ocupacao_p0, historico_ocupacao_p1, historico_ocupacao_p2, historico_ocupacao_p3, historico_ocupacao CASCADE;
DROP SEQUENCE IF EXISTS historico_ocupacao_id_seq;
DROP TABLE IF EXISTS proprietarios_p0, proprietarios_p1, proprietarios_p2, proprietarios_p3, proprietarios CASCADE;
DROP SEQUENCE IF EXISTS proprietarios_id_seq;
-- Also drop old pessoas partitions and sequence if they exist (replaced by JOINED inheritance from users)
DROP TABLE IF EXISTS pessoas_p0, pessoas_p1, pessoas_p2, pessoas_p3 CASCADE;
DROP SEQUENCE IF EXISTS pessoas_id_seq;
-- rollback -- irreversible
