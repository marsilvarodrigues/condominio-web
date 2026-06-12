-- =============================================================================
-- SEED DATA — Condomínio Web
-- =============================================================================
-- Limpa toda a base de negócio e popula massa de testes cobrindo:
--
--   Condomínios
--     • Edifício Solar das Palmeiras (SP) — 2 blocos, 10 apartamentos
--     • Residencial Jardim Europa (RJ)   — 1 bloco,  4 apartamentos
--
--   Usuários e papéis
--     • Síndico João Batista — acesso aos 2 condomínios (ROLE_SINDICO)
--     • José Pereira         — proprietário de 2 apts NO MESMO condomínio
--     • Ana Costa            — proprietária de apts EM CONDOMÍNIOS DIFERENTES
--     • Pedro Santos         — proprietário que MORA no próprio imóvel
--     • Carlos, Fernanda     — moradores simples (cond1)
--     • Roberta, Marcos      — moradores simples (cond2)
--     • Luciana Souza        — proprietária cond2
--
--   Estrutura financeira (por condomínio)
--     • Plano de contas hierárquico (Receitas / Despesas)
--     • Orçamento anual 2026
--     • Grupos de despesa (IGUALITÁRIO e FRAÇÃO IDEAL)
--     • Coeficientes de rateio por apartamento
--     • 2 despesas rateadas, cobranças geradas (PAGA / PENDENTE / VENCIDA)
--     • Conta corrente + fundo de reserva com movimentações
--     • Lançamentos bancários (conciliados e pendentes)
--
--   Conciliação bancária
--     • Um extrato importado com itens CONCILIADO / PENDENTE / IGNORADO
--     • Arquivo OFX correspondente em: scripts/mock-extrato-cond1-maio2026.ofx
--
-- SENHAS:
--   admin@condominio.com  → Admin@123  (preservado da migration)
--   Todos os demais       → Senha@123
--
-- EXECUÇÃO:
--   psql -h localhost -U <usuario> -d <banco> -f scripts/seed-data.sql
--
-- REQUISITOS:
--   • PostgreSQL 14+
--   • Extensão pgcrypto disponível no banco
--   • Usuário com privilégio SUPERUSER (necessário para session_replication_role)
--     OU executar a limpeza manualmente em ordem e remover as linhas SET abaixo.
-- =============================================================================

BEGIN;

CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- =============================================================================
-- LIMPEZA: apaga todos os dados de negócio, preserva:
--   • estados  (dados de referência da migration 0006)
--   • bancos   (dados de referência da migration 0017)
--   • admin@condominio.com (usuário global criado na migration 0010)
-- =============================================================================

-- Desabilita verificação de FK para truncar em qualquer ordem
SET session_replication_role = replica;

TRUNCATE TABLE
    historico_ocupacao,
    itens_extrato,
    extrato_importacoes,
    cobrancas,
    cotas_rateio,
    rateio_execucoes,
    coeficientes_rateio,
    despesas,
    grupos_despesa,
    item_orcamento,
    orcamento_anual,
    plano_contas,
    lancamentos_bancarios,
    fundo_reserva_movimentacao,
    fundo_reserva,
    contas_bancarias,
    cobranca_configuracoes,
    asaas_customers,
    proprietario_apartamentos,
    proprietario_pj,
    proprietario_pf,
    proprietarios,
    moradores,
    pessoas,
    apartamentos,
    blocos,
    condominios,
    user_condominios,
    password_history
RESTART IDENTITY;

DELETE FROM user_roles
WHERE user_id NOT IN (SELECT id FROM users WHERE email = 'admin@condominio.com');

DELETE FROM users WHERE email != 'admin@condominio.com';

-- Restartar sequências explícitas (tabelas particionadas que usam nextval)
ALTER SEQUENCE grupos_despesa_id_seq       RESTART WITH 1;
ALTER SEQUENCE coeficientes_rateio_id_seq  RESTART WITH 1;
ALTER SEQUENCE despesas_id_seq             RESTART WITH 1;
ALTER SEQUENCE rateio_execucoes_id_seq     RESTART WITH 1;
ALTER SEQUENCE cotas_rateio_id_seq         RESTART WITH 1;
ALTER SEQUENCE contas_bancarias_id_seq     RESTART WITH 1;
ALTER SEQUENCE lancamentos_bancarios_id_seq RESTART WITH 1;
ALTER SEQUENCE extrato_importacoes_id_seq  RESTART WITH 1;
ALTER SEQUENCE itens_extrato_id_seq        RESTART WITH 1;
ALTER SEQUENCE historico_ocupacao_id_seq   RESTART WITH 1;

SET session_replication_role = DEFAULT;

-- =============================================================================
-- CARGA DE DADOS
-- =============================================================================
DO $$
DECLARE
    -- hash único para todos os usuários de teste (Senha@123)
    v_hash              TEXT := crypt('Senha@123', gen_salt('bf', 10));

    -- estados
    v_sp_id             BIGINT;
    v_rj_id             BIGINT;

    -- bancos
    v_itau_id           BIGINT;
    v_cef_id            BIGINT;
    v_bradesco_id       BIGINT;

    -- condominios
    v_cond1_id          BIGINT;
    v_cond2_id          BIGINT;

    -- blocos
    v_c1_bla_id         BIGINT;   -- cond1 Bloco A (Torre Leste)
    v_c1_blb_id         BIGINT;   -- cond1 Bloco B (Torre Oeste)
    v_c2_bl_id          BIGINT;   -- cond2 Bloco Único

    -- apartamentos cond1 bloco A
    v_c1_a101_id        BIGINT;
    v_c1_a102_id        BIGINT;
    v_c1_a201_id        BIGINT;
    v_c1_a202_id        BIGINT;
    v_c1_a301_id        BIGINT;
    v_c1_a302_id        BIGINT;
    -- apartamentos cond1 bloco B
    v_c1_b101_id        BIGINT;
    v_c1_b102_id        BIGINT;
    v_c1_b201_id        BIGINT;
    v_c1_b202_id        BIGINT;
    -- apartamentos cond2
    v_c2_101_id         BIGINT;
    v_c2_102_id         BIGINT;
    v_c2_201_id         BIGINT;
    v_c2_202_id         BIGINT;

    -- usuarios
    v_sindico_id        BIGINT;
    v_jose_id           BIGINT;   -- proprietário 2 apts mesmo cond
    v_ana_id            BIGINT;   -- proprietária apts em condos diferentes
    v_pedro_id          BIGINT;   -- proprietário que mora no imóvel
    v_carlos_id         BIGINT;
    v_fernanda_id       BIGINT;
    v_roberta_id        BIGINT;
    v_marcos_id         BIGINT;
    v_luciana_id        BIGINT;   -- proprietária cond2

    -- plano de contas cond1
    v_c1_rec_id         BIGINT;
    v_c1_rec_ord_id     BIGINT;
    v_c1_taxa_id        BIGINT;
    v_c1_txextra_id     BIGINT;
    v_c1_rec_ext_id     BIGINT;
    v_c1_multa_id       BIGINT;
    v_c1_rend_id        BIGINT;
    v_c1_desp_id        BIGINT;
    v_c1_desp_ord_id    BIGINT;
    v_c1_limp_id        BIGINT;
    v_c1_seg_id         BIGINT;
    v_c1_manut_id       BIGINT;
    v_c1_energ_id       BIGINT;
    v_c1_agua_id        BIGINT;
    v_c1_adm_id         BIGINT;
    v_c1_desp_ext_id    BIGINT;
    v_c1_obras_id       BIGINT;
    v_c1_reserva_id     BIGINT;

    -- plano de contas cond2
    v_c2_rec_id         BIGINT;
    v_c2_rec_ord_id     BIGINT;
    v_c2_taxa_id        BIGINT;
    v_c2_txextra_id     BIGINT;
    v_c2_rec_ext_id     BIGINT;
    v_c2_multa_id       BIGINT;
    v_c2_rend_id        BIGINT;
    v_c2_desp_id        BIGINT;
    v_c2_desp_ord_id    BIGINT;
    v_c2_limp_id        BIGINT;
    v_c2_seg_id         BIGINT;
    v_c2_manut_id       BIGINT;
    v_c2_energ_id       BIGINT;
    v_c2_agua_id        BIGINT;
    v_c2_adm_id         BIGINT;
    v_c2_desp_ext_id    BIGINT;
    v_c2_obras_id       BIGINT;
    v_c2_reserva_id     BIGINT;

    -- orçamento anual
    v_c1_orc_id         BIGINT;
    v_c2_orc_id         BIGINT;

    -- contas bancárias
    v_c1_cc_id          BIGINT;   -- corrente cond1 (Itaú)
    v_c1_fr_id          BIGINT;   -- fundo reserva cond1 (CEF)
    v_c2_cc_id          BIGINT;   -- corrente cond2 (Bradesco)

    -- grupos despesa
    v_c1_gd_manut_id    BIGINT;
    v_c1_gd_seg_id      BIGINT;
    v_c1_gd_energ_id    BIGINT;
    v_c2_gd_manut_id    BIGINT;
    v_c2_gd_seg_id      BIGINT;

    -- despesas
    v_c1_d_limp_id      BIGINT;
    v_c1_d_seg_id       BIGINT;
    v_c2_d_limp_id      BIGINT;

    -- rateio execucoes
    v_c1_re_limp_id     BIGINT;
    v_c1_re_seg_id      BIGINT;
    v_c2_re_limp_id     BIGINT;

    -- cotas rateio (cond1 limpeza — valor 200 por apt)
    v_cr_l_a101         BIGINT;
    v_cr_l_a102         BIGINT;
    v_cr_l_a201         BIGINT;
    v_cr_l_a202         BIGINT;
    v_cr_l_a301         BIGINT;
    v_cr_l_a302         BIGINT;
    v_cr_l_b101         BIGINT;
    v_cr_l_b102         BIGINT;
    v_cr_l_b201         BIGINT;
    v_cr_l_b202         BIGINT;
    -- cotas rateio (cond1 segurança — valor 300 por apt)
    v_cr_s_a101         BIGINT;
    v_cr_s_a102         BIGINT;
    v_cr_s_a201         BIGINT;
    v_cr_s_a202         BIGINT;
    v_cr_s_a301         BIGINT;
    v_cr_s_a302         BIGINT;
    v_cr_s_b101         BIGINT;
    v_cr_s_b102         BIGINT;
    v_cr_s_b201         BIGINT;
    v_cr_s_b202         BIGINT;
    -- cotas rateio (cond2 limpeza — valor 200 por apt)
    v_cr_c2_101         BIGINT;
    v_cr_c2_102         BIGINT;
    v_cr_c2_201         BIGINT;
    v_cr_c2_202         BIGINT;

    -- lancamentos bancários
    v_lb_l_a101         BIGINT;
    v_lb_l_a102         BIGINT;
    v_lb_l_a201         BIGINT;
    v_lb_l_a202         BIGINT;
    v_lb_pag_limp       BIGINT;
    v_lb_s_a101         BIGINT;

    -- extrato importado
    v_extrato_id        BIGINT;

BEGIN

-- ===========================================================================
-- 1. REFERÊNCIAS GLOBAIS
-- ===========================================================================
SELECT id INTO v_sp_id       FROM estados WHERE uf = 'SP';
SELECT id INTO v_rj_id       FROM estados WHERE uf = 'RJ';
SELECT id INTO v_itau_id     FROM bancos   WHERE codigo = '341';
SELECT id INTO v_cef_id      FROM bancos   WHERE codigo = '104';
SELECT id INTO v_bradesco_id FROM bancos   WHERE codigo = '237';

-- ===========================================================================
-- 2. CONDOMÍNIOS
-- ===========================================================================

INSERT INTO condominios (nome, cnpj, email, logradouro, cep, cidade, estado_id,
                         deleted, created_at, updated_at, created_by, updated_by)
VALUES ('Edifício Solar das Palmeiras',
        '12.345.678/0001-90', 'contato@solardaspalmeiras.com.br',
        'Rua das Palmeiras, 100', '01310100', 'São Paulo', v_sp_id,
        false, NOW(), NOW(), 'seed', 'seed')
RETURNING id INTO v_cond1_id;

INSERT INTO condominios (nome, cnpj, email, logradouro, cep, cidade, estado_id,
                         deleted, created_at, updated_at, created_by, updated_by)
VALUES ('Residencial Jardim Europa',
        '98.765.432/0001-10', 'contato@jardimeuropa.com.br',
        'Av. Europa, 500', '22610000', 'Rio de Janeiro', v_rj_id,
        false, NOW(), NOW(), 'seed', 'seed')
RETURNING id INTO v_cond2_id;

-- ===========================================================================
-- 3. BLOCOS
-- ===========================================================================

-- Cond1: duas torres
INSERT INTO blocos (condominio_id, numero, bloco, deleted, created_at, updated_at, created_by, updated_by)
VALUES (v_cond1_id, 1, 'A', false, NOW(), NOW(), 'seed', 'seed')
RETURNING id INTO v_c1_bla_id;

INSERT INTO blocos (condominio_id, numero, bloco, deleted, created_at, updated_at, created_by, updated_by)
VALUES (v_cond1_id, 2, 'B', false, NOW(), NOW(), 'seed', 'seed')
RETURNING id INTO v_c1_blb_id;

-- Cond2: bloco único
INSERT INTO blocos (condominio_id, numero, bloco, deleted, created_at, updated_at, created_by, updated_by)
VALUES (v_cond2_id, 1, 'Único', false, NOW(), NOW(), 'seed', 'seed')
RETURNING id INTO v_c2_bl_id;

-- ===========================================================================
-- 4. APARTAMENTOS
-- fracao_ideal = area_unidade / area_total_condominio (proporcional)
--   Cond1: área total = 6×80 + 4×100 = 880 m²
--     Bloco A (80 m²): 80/880 = 0.090909
--     Bloco B (100 m²): 100/880 = 0.113636
--   Cond2: área total = 4×70 = 280 m²  →  70/280 = 0.250000
-- ===========================================================================

-- Cond1 / Bloco A (80 m² cada, fração 0.090909)
INSERT INTO apartamentos (condominio_id, bloco_id, numero, area_construida, fracao_ideal, andar, deleted, created_at, updated_at, created_by, updated_by)
VALUES (v_cond1_id, v_c1_bla_id, '101', 80.00, 0.090909, 1, false, NOW(), NOW(), 'seed', 'seed') RETURNING id INTO v_c1_a101_id;
INSERT INTO apartamentos (condominio_id, bloco_id, numero, area_construida, fracao_ideal, andar, deleted, created_at, updated_at, created_by, updated_by)
VALUES (v_cond1_id, v_c1_bla_id, '102', 80.00, 0.090909, 1, false, NOW(), NOW(), 'seed', 'seed') RETURNING id INTO v_c1_a102_id;
INSERT INTO apartamentos (condominio_id, bloco_id, numero, area_construida, fracao_ideal, andar, deleted, created_at, updated_at, created_by, updated_by)
VALUES (v_cond1_id, v_c1_bla_id, '201', 80.00, 0.090909, 2, false, NOW(), NOW(), 'seed', 'seed') RETURNING id INTO v_c1_a201_id;
INSERT INTO apartamentos (condominio_id, bloco_id, numero, area_construida, fracao_ideal, andar, deleted, created_at, updated_at, created_by, updated_by)
VALUES (v_cond1_id, v_c1_bla_id, '202', 80.00, 0.090909, 2, false, NOW(), NOW(), 'seed', 'seed') RETURNING id INTO v_c1_a202_id;
INSERT INTO apartamentos (condominio_id, bloco_id, numero, area_construida, fracao_ideal, andar, deleted, created_at, updated_at, created_by, updated_by)
VALUES (v_cond1_id, v_c1_bla_id, '301', 80.00, 0.090909, 3, false, NOW(), NOW(), 'seed', 'seed') RETURNING id INTO v_c1_a301_id;
INSERT INTO apartamentos (condominio_id, bloco_id, numero, area_construida, fracao_ideal, andar, deleted, created_at, updated_at, created_by, updated_by)
VALUES (v_cond1_id, v_c1_bla_id, '302', 80.00, 0.090909, 3, false, NOW(), NOW(), 'seed', 'seed') RETURNING id INTO v_c1_a302_id;

-- Cond1 / Bloco B (100 m² cada — apartamentos maiores, fração 0.113636)
INSERT INTO apartamentos (condominio_id, bloco_id, numero, area_construida, fracao_ideal, andar, deleted, created_at, updated_at, created_by, updated_by)
VALUES (v_cond1_id, v_c1_blb_id, '101', 100.00, 0.113636, 1, false, NOW(), NOW(), 'seed', 'seed') RETURNING id INTO v_c1_b101_id;
INSERT INTO apartamentos (condominio_id, bloco_id, numero, area_construida, fracao_ideal, andar, deleted, created_at, updated_at, created_by, updated_by)
VALUES (v_cond1_id, v_c1_blb_id, '102', 100.00, 0.113636, 1, false, NOW(), NOW(), 'seed', 'seed') RETURNING id INTO v_c1_b102_id;
INSERT INTO apartamentos (condominio_id, bloco_id, numero, area_construida, fracao_ideal, andar, deleted, created_at, updated_at, created_by, updated_by)
VALUES (v_cond1_id, v_c1_blb_id, '201', 100.00, 0.113636, 2, false, NOW(), NOW(), 'seed', 'seed') RETURNING id INTO v_c1_b201_id;
INSERT INTO apartamentos (condominio_id, bloco_id, numero, area_construida, fracao_ideal, andar, deleted, created_at, updated_at, created_by, updated_by)
VALUES (v_cond1_id, v_c1_blb_id, '202', 100.00, 0.113636, 2, false, NOW(), NOW(), 'seed', 'seed') RETURNING id INTO v_c1_b202_id;

-- Cond2 / Bloco Único (70 m² cada, fração 0.250000)
INSERT INTO apartamentos (condominio_id, bloco_id, numero, area_construida, fracao_ideal, andar, deleted, created_at, updated_at, created_by, updated_by)
VALUES (v_cond2_id, v_c2_bl_id, '101', 70.00, 0.250000, 1, false, NOW(), NOW(), 'seed', 'seed') RETURNING id INTO v_c2_101_id;
INSERT INTO apartamentos (condominio_id, bloco_id, numero, area_construida, fracao_ideal, andar, deleted, created_at, updated_at, created_by, updated_by)
VALUES (v_cond2_id, v_c2_bl_id, '102', 70.00, 0.250000, 1, false, NOW(), NOW(), 'seed', 'seed') RETURNING id INTO v_c2_102_id;
INSERT INTO apartamentos (condominio_id, bloco_id, numero, area_construida, fracao_ideal, andar, deleted, created_at, updated_at, created_by, updated_by)
VALUES (v_cond2_id, v_c2_bl_id, '201', 70.00, 0.250000, 2, false, NOW(), NOW(), 'seed', 'seed') RETURNING id INTO v_c2_201_id;
INSERT INTO apartamentos (condominio_id, bloco_id, numero, area_construida, fracao_ideal, andar, deleted, created_at, updated_at, created_by, updated_by)
VALUES (v_cond2_id, v_c2_bl_id, '202', 70.00, 0.250000, 2, false, NOW(), NOW(), 'seed', 'seed') RETURNING id INTO v_c2_202_id;

-- ===========================================================================
-- 5. USUÁRIOS E PAPÉIS
-- ===========================================================================

-- ─── Síndico João Batista — acesso aos 2 condomínios ───────────────────────
INSERT INTO users (email, password, name, enabled, deleted, created_at, updated_at)
VALUES ('sindico.joao@solardaspalmeiras.com.br', v_hash, 'João Batista',
        true, false, NOW(), NOW())
RETURNING id INTO v_sindico_id;

INSERT INTO user_roles (user_id, role) VALUES (v_sindico_id, 'ROLE_USER');
INSERT INTO user_roles (user_id, role) VALUES (v_sindico_id, 'ROLE_SINDICO');
-- Síndico tem acesso a ambos os condomínios
INSERT INTO user_condominios (user_id, condominio_id) VALUES (v_sindico_id, v_cond1_id);
INSERT INTO user_condominios (user_id, condominio_id) VALUES (v_sindico_id, v_cond2_id);

-- ─── José Pereira — proprietário de 2 apts no mesmo condomínio ─────────────
-- Mora no Apt A101 e é dono também do Apt A102.
-- CPF 529.982.247-25 (válido)
INSERT INTO users (email, password, name, enabled, deleted, created_at, updated_at)
VALUES ('jose.pereira@email.com', v_hash, 'José Pereira',
        true, false, NOW(), NOW())
RETURNING id INTO v_jose_id;

INSERT INTO user_roles (user_id, role) VALUES (v_jose_id, 'ROLE_USER');
INSERT INTO user_roles (user_id, role) VALUES (v_jose_id, 'ROLE_MORADOR');
INSERT INTO user_roles (user_id, role) VALUES (v_jose_id, 'ROLE_PROPRIETARIO');
INSERT INTO user_condominios (user_id, condominio_id) VALUES (v_jose_id, v_cond1_id);

-- Pessoa (JOINED do users) + Proprietário + PF
-- apartamento_id = A101 (onde mora)
INSERT INTO pessoas (id, condominio_id, apartamento_id, pessoa_tipo, telefone, created_by, updated_by)
VALUES (v_jose_id, v_cond1_id, v_c1_a101_id, 'PROP_PF', '(11) 91111-0001', 'seed', 'seed');
INSERT INTO proprietarios  (id)       VALUES (v_jose_id);
INSERT INTO proprietario_pf(id, cpf)  VALUES (v_jose_id, '529.982.247-25');

-- Propriedades: A101 (onde mora) e A102 (imóvel extra no mesmo cond)
INSERT INTO proprietario_apartamentos (proprietario_id, apartamento_id)
VALUES (v_jose_id, v_c1_a101_id);
INSERT INTO proprietario_apartamentos (proprietario_id, apartamento_id)
VALUES (v_jose_id, v_c1_a102_id);

-- ─── Ana Costa — proprietária com apts em condomínios diferentes ────────────
-- Não mora em nenhum dos dois (apartamento_id = NULL).
-- CPF 111.444.777-35 (válido)
INSERT INTO users (email, password, name, enabled, deleted, created_at, updated_at)
VALUES ('ana.costa@email.com', v_hash, 'Ana Costa',
        true, false, NOW(), NOW())
RETURNING id INTO v_ana_id;

INSERT INTO user_roles (user_id, role) VALUES (v_ana_id, 'ROLE_USER');
INSERT INTO user_roles (user_id, role) VALUES (v_ana_id, 'ROLE_MORADOR');
INSERT INTO user_roles (user_id, role) VALUES (v_ana_id, 'ROLE_PROPRIETARIO');
-- Acesso a ambos os condomínios (tem propriedades nos dois)
INSERT INTO user_condominios (user_id, condominio_id) VALUES (v_ana_id, v_cond1_id);
INSERT INTO user_condominios (user_id, condominio_id) VALUES (v_ana_id, v_cond2_id);

-- Pessoa no cond1 (condomínio principal)
INSERT INTO pessoas (id, condominio_id, apartamento_id, pessoa_tipo, telefone, created_by, updated_by)
VALUES (v_ana_id, v_cond1_id, NULL, 'PROP_PF', '(21) 92222-0002', 'seed', 'seed');
INSERT INTO proprietarios  (id)       VALUES (v_ana_id);
INSERT INTO proprietario_pf(id, cpf)  VALUES (v_ana_id, '111.444.777-35');

-- Propriedades: B101 em cond1 e 202 em cond2
INSERT INTO proprietario_apartamentos (proprietario_id, apartamento_id)
VALUES (v_ana_id, v_c1_b101_id);
INSERT INTO proprietario_apartamentos (proprietario_id, apartamento_id)
VALUES (v_ana_id, v_c2_202_id);

-- ─── Pedro Santos — proprietário que mora no próprio imóvel ────────────────
-- Dono e morador do Apt B201 (cond1). Caso clássico de proprietário-ocupante.
-- CPF 046.055.750-57 (válido)
INSERT INTO users (email, password, name, enabled, deleted, created_at, updated_at)
VALUES ('pedro.santos@email.com', v_hash, 'Pedro Santos',
        true, false, NOW(), NOW())
RETURNING id INTO v_pedro_id;

INSERT INTO user_roles (user_id, role) VALUES (v_pedro_id, 'ROLE_USER');
INSERT INTO user_roles (user_id, role) VALUES (v_pedro_id, 'ROLE_MORADOR');
INSERT INTO user_roles (user_id, role) VALUES (v_pedro_id, 'ROLE_PROPRIETARIO');
INSERT INTO user_condominios (user_id, condominio_id) VALUES (v_pedro_id, v_cond1_id);

-- apartamento_id = B201 (mesmo apartamento que é dono)
INSERT INTO pessoas (id, condominio_id, apartamento_id, pessoa_tipo, telefone, created_by, updated_by)
VALUES (v_pedro_id, v_cond1_id, v_c1_b201_id, 'PROP_PF', '(11) 93333-0003', 'seed', 'seed');
INSERT INTO proprietarios  (id)       VALUES (v_pedro_id);
INSERT INTO proprietario_pf(id, cpf)  VALUES (v_pedro_id, '046.055.750-57');

-- Propri: B201 (o mesmo onde mora — owner-occupier)
INSERT INTO proprietario_apartamentos (proprietario_id, apartamento_id)
VALUES (v_pedro_id, v_c1_b201_id);

-- ─── Carlos Mendes — morador simples cond1 / A201 ──────────────────────────
INSERT INTO users (email, password, name, enabled, deleted, created_at, updated_at)
VALUES ('carlos.mendes@email.com', v_hash, 'Carlos Mendes',
        true, false, NOW(), NOW())
RETURNING id INTO v_carlos_id;

INSERT INTO user_roles (user_id, role) VALUES (v_carlos_id, 'ROLE_USER');
INSERT INTO user_roles (user_id, role) VALUES (v_carlos_id, 'ROLE_MORADOR');
INSERT INTO user_condominios (user_id, condominio_id) VALUES (v_carlos_id, v_cond1_id);

INSERT INTO pessoas (id, condominio_id, apartamento_id, pessoa_tipo, telefone, created_by, updated_by)
VALUES (v_carlos_id, v_cond1_id, v_c1_a201_id, 'MORADOR', '(11) 94444-0004', 'seed', 'seed');
INSERT INTO moradores (id, cpf) VALUES (v_carlos_id, NULL);

-- ─── Fernanda Lima — moradora simples cond1 / A301 ─────────────────────────
INSERT INTO users (email, password, name, enabled, deleted, created_at, updated_at)
VALUES ('fernanda.lima@email.com', v_hash, 'Fernanda Lima',
        true, false, NOW(), NOW())
RETURNING id INTO v_fernanda_id;

INSERT INTO user_roles (user_id, role) VALUES (v_fernanda_id, 'ROLE_USER');
INSERT INTO user_roles (user_id, role) VALUES (v_fernanda_id, 'ROLE_MORADOR');
INSERT INTO user_condominios (user_id, condominio_id) VALUES (v_fernanda_id, v_cond1_id);

INSERT INTO pessoas (id, condominio_id, apartamento_id, pessoa_tipo, telefone, created_by, updated_by)
VALUES (v_fernanda_id, v_cond1_id, v_c1_a301_id, 'MORADOR', '(11) 95555-0005', 'seed', 'seed');
INSERT INTO moradores (id, cpf) VALUES (v_fernanda_id, NULL);

-- ─── Roberta Alves — moradora simples cond2 / 101 ──────────────────────────
INSERT INTO users (email, password, name, enabled, deleted, created_at, updated_at)
VALUES ('roberta.alves@email.com', v_hash, 'Roberta Alves',
        true, false, NOW(), NOW())
RETURNING id INTO v_roberta_id;

INSERT INTO user_roles (user_id, role) VALUES (v_roberta_id, 'ROLE_USER');
INSERT INTO user_roles (user_id, role) VALUES (v_roberta_id, 'ROLE_MORADOR');
INSERT INTO user_condominios (user_id, condominio_id) VALUES (v_roberta_id, v_cond2_id);

INSERT INTO pessoas (id, condominio_id, apartamento_id, pessoa_tipo, telefone, created_by, updated_by)
VALUES (v_roberta_id, v_cond2_id, v_c2_101_id, 'MORADOR', '(21) 96666-0006', 'seed', 'seed');
INSERT INTO moradores (id, cpf) VALUES (v_roberta_id, NULL);

-- ─── Marcos Rodrigues — morador simples cond2 / 201 ────────────────────────
INSERT INTO users (email, password, name, enabled, deleted, created_at, updated_at)
VALUES ('marcos.rodrigues@email.com', v_hash, 'Marcos Rodrigues',
        true, false, NOW(), NOW())
RETURNING id INTO v_marcos_id;

INSERT INTO user_roles (user_id, role) VALUES (v_marcos_id, 'ROLE_USER');
INSERT INTO user_roles (user_id, role) VALUES (v_marcos_id, 'ROLE_MORADOR');
INSERT INTO user_condominios (user_id, condominio_id) VALUES (v_marcos_id, v_cond2_id);

INSERT INTO pessoas (id, condominio_id, apartamento_id, pessoa_tipo, telefone, created_by, updated_by)
VALUES (v_marcos_id, v_cond2_id, v_c2_201_id, 'MORADOR', '(21) 97777-0007', 'seed', 'seed');
INSERT INTO moradores (id, cpf) VALUES (v_marcos_id, NULL);

-- ─── Luciana Souza — proprietária cond2 / apt 102 ──────────────────────────
-- CPF 282.894.530-86 (válido)
INSERT INTO users (email, password, name, enabled, deleted, created_at, updated_at)
VALUES ('luciana.souza@email.com', v_hash, 'Luciana Souza',
        true, false, NOW(), NOW())
RETURNING id INTO v_luciana_id;

INSERT INTO user_roles (user_id, role) VALUES (v_luciana_id, 'ROLE_USER');
INSERT INTO user_roles (user_id, role) VALUES (v_luciana_id, 'ROLE_MORADOR');
INSERT INTO user_roles (user_id, role) VALUES (v_luciana_id, 'ROLE_PROPRIETARIO');
INSERT INTO user_condominios (user_id, condominio_id) VALUES (v_luciana_id, v_cond2_id);

-- Mora no próprio imóvel em cond2
INSERT INTO pessoas (id, condominio_id, apartamento_id, pessoa_tipo, telefone, created_by, updated_by)
VALUES (v_luciana_id, v_cond2_id, v_c2_102_id, 'PROP_PF', '(21) 98888-0008', 'seed', 'seed');
INSERT INTO proprietarios  (id)       VALUES (v_luciana_id);
INSERT INTO proprietario_pf(id, cpf)  VALUES (v_luciana_id, '282.894.530-86');

INSERT INTO proprietario_apartamentos (proprietario_id, apartamento_id)
VALUES (v_luciana_id, v_c2_102_id);

-- ===========================================================================
-- 6. HISTÓRICO DE OCUPAÇÃO (ex-morador do Apt A102 antes de José Pereira)
-- ===========================================================================
INSERT INTO historico_ocupacao
    (condominio_id, apartamento_id, pessoa_id, nome_morador, email_morador, cpf_morador,
     data_entrada, data_saida, criado_em)
VALUES (v_cond1_id, v_c1_a102_id, v_carlos_id,
        'Ricardo Almeida', 'ricardo.almeida@email.com', NULL,
        '2023-01-01', '2025-12-31', NOW());

-- ===========================================================================
-- 7. PLANO DE CONTAS — Cond1
-- ===========================================================================

INSERT INTO plano_contas (condominio_id, pai_id, codigo, descricao, tipo, deleted, created_at, updated_at, created_by, updated_by)
VALUES (v_cond1_id, NULL, '1', 'RECEITAS', 'RECEITA', false, NOW(), NOW(), 'seed', 'seed')
RETURNING id INTO v_c1_rec_id;

  INSERT INTO plano_contas (condominio_id, pai_id, codigo, descricao, tipo, deleted, created_at, updated_at, created_by, updated_by)
  VALUES (v_cond1_id, v_c1_rec_id, '1.1', 'Receitas Ordinárias', 'RECEITA', false, NOW(), NOW(), 'seed', 'seed')
  RETURNING id INTO v_c1_rec_ord_id;

    INSERT INTO plano_contas (condominio_id, pai_id, codigo, descricao, tipo, deleted, created_at, updated_at, created_by, updated_by)
    VALUES (v_cond1_id, v_c1_rec_ord_id, '1.1.1', 'Taxa Condominial', 'RECEITA', false, NOW(), NOW(), 'seed', 'seed')
    RETURNING id INTO v_c1_taxa_id;

    INSERT INTO plano_contas (condominio_id, pai_id, codigo, descricao, tipo, deleted, created_at, updated_at, created_by, updated_by)
    VALUES (v_cond1_id, v_c1_rec_ord_id, '1.1.2', 'Taxa Extra', 'RECEITA', false, NOW(), NOW(), 'seed', 'seed')
    RETURNING id INTO v_c1_txextra_id;

  INSERT INTO plano_contas (condominio_id, pai_id, codigo, descricao, tipo, deleted, created_at, updated_at, created_by, updated_by)
  VALUES (v_cond1_id, v_c1_rec_id, '1.2', 'Receitas Extraordinárias', 'RECEITA', false, NOW(), NOW(), 'seed', 'seed')
  RETURNING id INTO v_c1_rec_ext_id;

    INSERT INTO plano_contas (condominio_id, pai_id, codigo, descricao, tipo, deleted, created_at, updated_at, created_by, updated_by)
    VALUES (v_cond1_id, v_c1_rec_ext_id, '1.2.1', 'Multas e Juros de Mora', 'RECEITA', false, NOW(), NOW(), 'seed', 'seed')
    RETURNING id INTO v_c1_multa_id;

    INSERT INTO plano_contas (condominio_id, pai_id, codigo, descricao, tipo, deleted, created_at, updated_at, created_by, updated_by)
    VALUES (v_cond1_id, v_c1_rec_ext_id, '1.2.2', 'Rendimentos Financeiros', 'RECEITA', false, NOW(), NOW(), 'seed', 'seed')
    RETURNING id INTO v_c1_rend_id;

INSERT INTO plano_contas (condominio_id, pai_id, codigo, descricao, tipo, deleted, created_at, updated_at, created_by, updated_by)
VALUES (v_cond1_id, NULL, '2', 'DESPESAS', 'DESPESA', false, NOW(), NOW(), 'seed', 'seed')
RETURNING id INTO v_c1_desp_id;

  INSERT INTO plano_contas (condominio_id, pai_id, codigo, descricao, tipo, deleted, created_at, updated_at, created_by, updated_by)
  VALUES (v_cond1_id, v_c1_desp_id, '2.1', 'Despesas Ordinárias', 'DESPESA', false, NOW(), NOW(), 'seed', 'seed')
  RETURNING id INTO v_c1_desp_ord_id;

    INSERT INTO plano_contas (condominio_id, pai_id, codigo, descricao, tipo, deleted, created_at, updated_at, created_by, updated_by)
    VALUES (v_cond1_id, v_c1_desp_ord_id, '2.1.1', 'Limpeza e Conservação', 'DESPESA', false, NOW(), NOW(), 'seed', 'seed')
    RETURNING id INTO v_c1_limp_id;

    INSERT INTO plano_contas (condominio_id, pai_id, codigo, descricao, tipo, deleted, created_at, updated_at, created_by, updated_by)
    VALUES (v_cond1_id, v_c1_desp_ord_id, '2.1.2', 'Segurança e Vigilância', 'DESPESA', false, NOW(), NOW(), 'seed', 'seed')
    RETURNING id INTO v_c1_seg_id;

    INSERT INTO plano_contas (condominio_id, pai_id, codigo, descricao, tipo, deleted, created_at, updated_at, created_by, updated_by)
    VALUES (v_cond1_id, v_c1_desp_ord_id, '2.1.3', 'Manutenção e Reparos', 'DESPESA', false, NOW(), NOW(), 'seed', 'seed')
    RETURNING id INTO v_c1_manut_id;

    INSERT INTO plano_contas (condominio_id, pai_id, codigo, descricao, tipo, deleted, created_at, updated_at, created_by, updated_by)
    VALUES (v_cond1_id, v_c1_desp_ord_id, '2.1.4', 'Energia Elétrica – Áreas Comuns', 'DESPESA', false, NOW(), NOW(), 'seed', 'seed')
    RETURNING id INTO v_c1_energ_id;

    INSERT INTO plano_contas (condominio_id, pai_id, codigo, descricao, tipo, deleted, created_at, updated_at, created_by, updated_by)
    VALUES (v_cond1_id, v_c1_desp_ord_id, '2.1.5', 'Água e Esgoto', 'DESPESA', false, NOW(), NOW(), 'seed', 'seed')
    RETURNING id INTO v_c1_agua_id;

    INSERT INTO plano_contas (condominio_id, pai_id, codigo, descricao, tipo, deleted, created_at, updated_at, created_by, updated_by)
    VALUES (v_cond1_id, v_c1_desp_ord_id, '2.1.6', 'Administração e Contabilidade', 'DESPESA', false, NOW(), NOW(), 'seed', 'seed')
    RETURNING id INTO v_c1_adm_id;

  INSERT INTO plano_contas (condominio_id, pai_id, codigo, descricao, tipo, deleted, created_at, updated_at, created_by, updated_by)
  VALUES (v_cond1_id, v_c1_desp_id, '2.2', 'Despesas Extraordinárias', 'DESPESA', false, NOW(), NOW(), 'seed', 'seed')
  RETURNING id INTO v_c1_desp_ext_id;

    INSERT INTO plano_contas (condominio_id, pai_id, codigo, descricao, tipo, deleted, created_at, updated_at, created_by, updated_by)
    VALUES (v_cond1_id, v_c1_desp_ext_id, '2.2.1', 'Obras e Reformas', 'DESPESA', false, NOW(), NOW(), 'seed', 'seed')
    RETURNING id INTO v_c1_obras_id;

    INSERT INTO plano_contas (condominio_id, pai_id, codigo, descricao, tipo, deleted, created_at, updated_at, created_by, updated_by)
    VALUES (v_cond1_id, v_c1_desp_ext_id, '2.2.2', 'Fundo de Reserva', 'DESPESA', false, NOW(), NOW(), 'seed', 'seed')
    RETURNING id INTO v_c1_reserva_id;

-- ===========================================================================
-- 8. PLANO DE CONTAS — Cond2 (estrutura idêntica, condomínio diferente)
-- ===========================================================================

INSERT INTO plano_contas (condominio_id, pai_id, codigo, descricao, tipo, deleted, created_at, updated_at, created_by, updated_by)
VALUES (v_cond2_id, NULL, '1', 'RECEITAS', 'RECEITA', false, NOW(), NOW(), 'seed', 'seed')
RETURNING id INTO v_c2_rec_id;

  INSERT INTO plano_contas (condominio_id, pai_id, codigo, descricao, tipo, deleted, created_at, updated_at, created_by, updated_by)
  VALUES (v_cond2_id, v_c2_rec_id, '1.1', 'Receitas Ordinárias', 'RECEITA', false, NOW(), NOW(), 'seed', 'seed')
  RETURNING id INTO v_c2_rec_ord_id;
    INSERT INTO plano_contas (condominio_id, pai_id, codigo, descricao, tipo, deleted, created_at, updated_at, created_by, updated_by)
    VALUES (v_cond2_id, v_c2_rec_ord_id, '1.1.1', 'Taxa Condominial', 'RECEITA', false, NOW(), NOW(), 'seed', 'seed')
    RETURNING id INTO v_c2_taxa_id;
    INSERT INTO plano_contas (condominio_id, pai_id, codigo, descricao, tipo, deleted, created_at, updated_at, created_by, updated_by)
    VALUES (v_cond2_id, v_c2_rec_ord_id, '1.1.2', 'Taxa Extra', 'RECEITA', false, NOW(), NOW(), 'seed', 'seed')
    RETURNING id INTO v_c2_txextra_id;

  INSERT INTO plano_contas (condominio_id, pai_id, codigo, descricao, tipo, deleted, created_at, updated_at, created_by, updated_by)
  VALUES (v_cond2_id, v_c2_rec_id, '1.2', 'Receitas Extraordinárias', 'RECEITA', false, NOW(), NOW(), 'seed', 'seed')
  RETURNING id INTO v_c2_rec_ext_id;
    INSERT INTO plano_contas (condominio_id, pai_id, codigo, descricao, tipo, deleted, created_at, updated_at, created_by, updated_by)
    VALUES (v_cond2_id, v_c2_rec_ext_id, '1.2.1', 'Multas e Juros de Mora', 'RECEITA', false, NOW(), NOW(), 'seed', 'seed')
    RETURNING id INTO v_c2_multa_id;
    INSERT INTO plano_contas (condominio_id, pai_id, codigo, descricao, tipo, deleted, created_at, updated_at, created_by, updated_by)
    VALUES (v_cond2_id, v_c2_rec_ext_id, '1.2.2', 'Rendimentos Financeiros', 'RECEITA', false, NOW(), NOW(), 'seed', 'seed')
    RETURNING id INTO v_c2_rend_id;

INSERT INTO plano_contas (condominio_id, pai_id, codigo, descricao, tipo, deleted, created_at, updated_at, created_by, updated_by)
VALUES (v_cond2_id, NULL, '2', 'DESPESAS', 'DESPESA', false, NOW(), NOW(), 'seed', 'seed')
RETURNING id INTO v_c2_desp_id;

  INSERT INTO plano_contas (condominio_id, pai_id, codigo, descricao, tipo, deleted, created_at, updated_at, created_by, updated_by)
  VALUES (v_cond2_id, v_c2_desp_id, '2.1', 'Despesas Ordinárias', 'DESPESA', false, NOW(), NOW(), 'seed', 'seed')
  RETURNING id INTO v_c2_desp_ord_id;
    INSERT INTO plano_contas (condominio_id, pai_id, codigo, descricao, tipo, deleted, created_at, updated_at, created_by, updated_by)
    VALUES (v_cond2_id, v_c2_desp_ord_id, '2.1.1', 'Limpeza e Conservação', 'DESPESA', false, NOW(), NOW(), 'seed', 'seed')
    RETURNING id INTO v_c2_limp_id;
    INSERT INTO plano_contas (condominio_id, pai_id, codigo, descricao, tipo, deleted, created_at, updated_at, created_by, updated_by)
    VALUES (v_cond2_id, v_c2_desp_ord_id, '2.1.2', 'Segurança e Vigilância', 'DESPESA', false, NOW(), NOW(), 'seed', 'seed')
    RETURNING id INTO v_c2_seg_id;
    INSERT INTO plano_contas (condominio_id, pai_id, codigo, descricao, tipo, deleted, created_at, updated_at, created_by, updated_by)
    VALUES (v_cond2_id, v_c2_desp_ord_id, '2.1.3', 'Manutenção e Reparos', 'DESPESA', false, NOW(), NOW(), 'seed', 'seed')
    RETURNING id INTO v_c2_manut_id;
    INSERT INTO plano_contas (condominio_id, pai_id, codigo, descricao, tipo, deleted, created_at, updated_at, created_by, updated_by)
    VALUES (v_cond2_id, v_c2_desp_ord_id, '2.1.4', 'Energia Elétrica – Áreas Comuns', 'DESPESA', false, NOW(), NOW(), 'seed', 'seed')
    RETURNING id INTO v_c2_energ_id;
    INSERT INTO plano_contas (condominio_id, pai_id, codigo, descricao, tipo, deleted, created_at, updated_at, created_by, updated_by)
    VALUES (v_cond2_id, v_c2_desp_ord_id, '2.1.5', 'Água e Esgoto', 'DESPESA', false, NOW(), NOW(), 'seed', 'seed')
    RETURNING id INTO v_c2_agua_id;
    INSERT INTO plano_contas (condominio_id, pai_id, codigo, descricao, tipo, deleted, created_at, updated_at, created_by, updated_by)
    VALUES (v_cond2_id, v_c2_desp_ord_id, '2.1.6', 'Administração e Contabilidade', 'DESPESA', false, NOW(), NOW(), 'seed', 'seed')
    RETURNING id INTO v_c2_adm_id;

  INSERT INTO plano_contas (condominio_id, pai_id, codigo, descricao, tipo, deleted, created_at, updated_at, created_by, updated_by)
  VALUES (v_cond2_id, v_c2_desp_id, '2.2', 'Despesas Extraordinárias', 'DESPESA', false, NOW(), NOW(), 'seed', 'seed')
  RETURNING id INTO v_c2_desp_ext_id;
    INSERT INTO plano_contas (condominio_id, pai_id, codigo, descricao, tipo, deleted, created_at, updated_at, created_by, updated_by)
    VALUES (v_cond2_id, v_c2_desp_ext_id, '2.2.1', 'Obras e Reformas', 'DESPESA', false, NOW(), NOW(), 'seed', 'seed')
    RETURNING id INTO v_c2_obras_id;
    INSERT INTO plano_contas (condominio_id, pai_id, codigo, descricao, tipo, deleted, created_at, updated_at, created_by, updated_by)
    VALUES (v_cond2_id, v_c2_desp_ext_id, '2.2.2', 'Fundo de Reserva', 'DESPESA', false, NOW(), NOW(), 'seed', 'seed')
    RETURNING id INTO v_c2_reserva_id;

-- ===========================================================================
-- 9. ORÇAMENTO ANUAL 2026
-- ===========================================================================

INSERT INTO orcamento_anual (condominio_id, exercicio, status, taxa_estimada_unidade, deleted, created_at, updated_at, created_by, updated_by)
VALUES (v_cond1_id, 2026, 'APROVADO', 500.00, false, NOW(), NOW(), 'seed', 'seed')
RETURNING id INTO v_c1_orc_id;

INSERT INTO item_orcamento (orcamento_anual_id, condominio_id, plano_contas_id, valor_previsto, valor_realizado, deleted, created_at, updated_at)
VALUES (v_c1_orc_id, v_cond1_id, v_c1_taxa_id,   60000.00, 48000.00, false, NOW(), NOW()),
       (v_c1_orc_id, v_cond1_id, v_c1_limp_id,   24000.00, 20000.00, false, NOW(), NOW()),
       (v_c1_orc_id, v_cond1_id, v_c1_seg_id,    36000.00, 30000.00, false, NOW(), NOW()),
       (v_c1_orc_id, v_cond1_id, v_c1_energ_id,  12000.00,  8500.00, false, NOW(), NOW()),
       (v_c1_orc_id, v_cond1_id, v_c1_agua_id,    7200.00,  5800.00, false, NOW(), NOW()),
       (v_c1_orc_id, v_cond1_id, v_c1_adm_id,     9600.00,  7200.00, false, NOW(), NOW());

INSERT INTO orcamento_anual (condominio_id, exercicio, status, taxa_estimada_unidade, deleted, created_at, updated_at, created_by, updated_by)
VALUES (v_cond2_id, 2026, 'RASCUNHO', 350.00, false, NOW(), NOW(), 'seed', 'seed')
RETURNING id INTO v_c2_orc_id;

INSERT INTO item_orcamento (orcamento_anual_id, condominio_id, plano_contas_id, valor_previsto, valor_realizado, deleted, created_at, updated_at)
VALUES (v_c2_orc_id, v_cond2_id, v_c2_taxa_id,  16800.00,  8400.00, false, NOW(), NOW()),
       (v_c2_orc_id, v_cond2_id, v_c2_limp_id,   9600.00,  4800.00, false, NOW(), NOW()),
       (v_c2_orc_id, v_cond2_id, v_c2_seg_id,    7200.00,  3600.00, false, NOW(), NOW());

-- ===========================================================================
-- 10. CONTAS BANCÁRIAS
-- ===========================================================================

-- Cond1 — Conta Corrente Itaú (usada no OFX de conciliação)
INSERT INTO contas_bancarias (condominio_id, banco_id, tipo, agencia, conta, digito,
    descricao, chave_pix, saldo_contabil, ativa, deleted, created_at, updated_at, created_by, updated_by)
VALUES (v_cond1_id, v_itau_id, 'CORRENTE', '1234', '56789', '0',
    'Conta corrente operacional', 'solar@palmeiras.com.br',
    15750.00, true, false, NOW(), NOW(), 'seed', 'seed')
RETURNING id INTO v_c1_cc_id;

-- Cond1 — Fundo de Reserva CEF
INSERT INTO contas_bancarias (condominio_id, banco_id, tipo, agencia, conta, digito,
    descricao, saldo_contabil, ativa, deleted, created_at, updated_at, created_by, updated_by)
VALUES (v_cond1_id, v_cef_id, 'FUNDO_RESERVA', '0001', '11111', '1',
    'Fundo de reserva', 8250.00, true, false, NOW(), NOW(), 'seed', 'seed')
RETURNING id INTO v_c1_fr_id;

-- Cond2 — Conta Corrente Bradesco
INSERT INTO contas_bancarias (condominio_id, banco_id, tipo, agencia, conta, digito,
    descricao, chave_pix, saldo_contabil, ativa, deleted, created_at, updated_at, created_by, updated_by)
VALUES (v_cond2_id, v_bradesco_id, 'CORRENTE', '5678', '98765', '4',
    'Conta corrente operacional', 'jardimeuropa@jardimeuropa.com.br',
    4200.00, true, false, NOW(), NOW(), 'seed', 'seed')
RETURNING id INTO v_c2_cc_id;

-- ===========================================================================
-- 11. FUNDO DE RESERVA
-- ===========================================================================

INSERT INTO fundo_reserva (condominio_id, percentual_arrecadacao, saldo_atual,
    conta_bancaria_id, deleted, created_at, updated_at, created_by, updated_by)
VALUES (v_cond1_id, 10.00, 8250.00, v_c1_fr_id, false, NOW(), NOW(), 'seed', 'seed');

INSERT INTO fundo_reserva (condominio_id, percentual_arrecadacao, saldo_atual,
    deleted, created_at, updated_at, created_by, updated_by)
VALUES (v_cond2_id, 10.00, 1200.00, false, NOW(), NOW(), 'seed', 'seed');

-- Movimentações do fundo cond1
INSERT INTO fundo_reserva_movimentacao (fundo_reserva_id, condominio_id, tipo, valor,
    justificativa, data_movimentacao, deleted, created_at, updated_at)
SELECT fr.id, v_cond1_id, 'CREDITO', 500.00, NULL, '2026-01-31', false, NOW(), NOW()
FROM fundo_reserva fr WHERE fr.condominio_id = v_cond1_id;

INSERT INTO fundo_reserva_movimentacao (fundo_reserva_id, condominio_id, tipo, valor,
    justificativa, data_movimentacao, deleted, created_at, updated_at)
SELECT fr.id, v_cond1_id, 'CREDITO', 500.00, NULL, '2026-02-28', false, NOW(), NOW()
FROM fundo_reserva fr WHERE fr.condominio_id = v_cond1_id;

INSERT INTO fundo_reserva_movimentacao (fundo_reserva_id, condominio_id, tipo, valor,
    justificativa, data_movimentacao, deleted, created_at, updated_at)
SELECT fr.id, v_cond1_id, 'DEBITO', 1200.00, 'Reparo emergencial – infiltração cobertura', '2026-03-15', false, NOW(), NOW()
FROM fundo_reserva fr WHERE fr.condominio_id = v_cond1_id;

-- ===========================================================================
-- 12. CONFIGURAÇÃO DE COBRANÇA
-- ===========================================================================

INSERT INTO cobranca_configuracoes
    (condominio_id, vencimento_dias, juros_mora_percent, multa_percent,
     descricao_padrao, created_at, updated_at, created_by, updated_by)
VALUES (v_cond1_id, 10, 1.00, 2.00, 'Taxa condominial – Ed. Solar das Palmeiras', NOW(), NOW(), 'seed', 'seed');

INSERT INTO cobranca_configuracoes
    (condominio_id, vencimento_dias, juros_mora_percent, multa_percent,
     descricao_padrao, created_at, updated_at, created_by, updated_by)
VALUES (v_cond2_id, 5, 1.50, 3.00, 'Taxa condominial – Res. Jardim Europa', NOW(), NOW(), 'seed', 'seed');

-- ===========================================================================
-- 13. GRUPOS DE DESPESA
-- ===========================================================================

-- Cond1: manutenção geral (igualitário — todas as unidades pagam igual)
INSERT INTO grupos_despesa (condominio_id, nome, tipo_rateio, escopo, plano_contas_id,
    deleted, created_at, updated_at, created_by, updated_by)
VALUES (v_cond1_id, 'Limpeza e Conservação', 'IGUALITARIO', 'TODOS', v_c1_limp_id,
    false, NOW(), NOW(), 'seed', 'seed')
RETURNING id INTO v_c1_gd_manut_id;

-- Cond1: segurança (igualitário)
INSERT INTO grupos_despesa (condominio_id, nome, tipo_rateio, escopo, plano_contas_id,
    deleted, created_at, updated_at, created_by, updated_by)
VALUES (v_cond1_id, 'Segurança e Vigilância', 'IGUALITARIO', 'TODOS', v_c1_seg_id,
    false, NOW(), NOW(), 'seed', 'seed')
RETURNING id INTO v_c1_gd_seg_id;

-- Cond1: energia (fração ideal — baseado em m²)
INSERT INTO grupos_despesa (condominio_id, nome, tipo_rateio, escopo, plano_contas_id,
    deleted, created_at, updated_at, created_by, updated_by)
VALUES (v_cond1_id, 'Energia Elétrica – Áreas Comuns', 'FRACAO_IDEAL', 'TODOS', v_c1_energ_id,
    false, NOW(), NOW(), 'seed', 'seed')
RETURNING id INTO v_c1_gd_energ_id;

-- Cond2
INSERT INTO grupos_despesa (condominio_id, nome, tipo_rateio, escopo, plano_contas_id,
    deleted, created_at, updated_at, created_by, updated_by)
VALUES (v_cond2_id, 'Limpeza e Conservação', 'IGUALITARIO', 'TODOS', v_c2_limp_id,
    false, NOW(), NOW(), 'seed', 'seed')
RETURNING id INTO v_c2_gd_manut_id;

INSERT INTO grupos_despesa (condominio_id, nome, tipo_rateio, escopo, plano_contas_id,
    deleted, created_at, updated_at, created_by, updated_by)
VALUES (v_cond2_id, 'Segurança e Vigilância', 'IGUALITARIO', 'TODOS', v_c2_seg_id,
    false, NOW(), NOW(), 'seed', 'seed')
RETURNING id INTO v_c2_gd_seg_id;

-- ===========================================================================
-- 14. COEFICIENTES DE RATEIO (vigência 2026-01-01)
-- Igualitário: coeficiente = 1/total_unidades
-- Fração ideal: proporcional à área
-- ===========================================================================

-- Cond1 — grupo limpeza (IGUALITARIO, 10 apts, coef = 0.100000)
INSERT INTO coeficientes_rateio (condominio_id, grupo_despesa_id, apartamento_id, coeficiente, area_m2, vigencia, deleted, created_at, created_by)
VALUES
    (v_cond1_id, v_c1_gd_manut_id, v_c1_a101_id, 0.100000, 80.00, '2026-01-01', false, NOW(), 'seed'),
    (v_cond1_id, v_c1_gd_manut_id, v_c1_a102_id, 0.100000, 80.00, '2026-01-01', false, NOW(), 'seed'),
    (v_cond1_id, v_c1_gd_manut_id, v_c1_a201_id, 0.100000, 80.00, '2026-01-01', false, NOW(), 'seed'),
    (v_cond1_id, v_c1_gd_manut_id, v_c1_a202_id, 0.100000, 80.00, '2026-01-01', false, NOW(), 'seed'),
    (v_cond1_id, v_c1_gd_manut_id, v_c1_a301_id, 0.100000, 80.00, '2026-01-01', false, NOW(), 'seed'),
    (v_cond1_id, v_c1_gd_manut_id, v_c1_a302_id, 0.100000, 80.00, '2026-01-01', false, NOW(), 'seed'),
    (v_cond1_id, v_c1_gd_manut_id, v_c1_b101_id, 0.100000, 100.00, '2026-01-01', false, NOW(), 'seed'),
    (v_cond1_id, v_c1_gd_manut_id, v_c1_b102_id, 0.100000, 100.00, '2026-01-01', false, NOW(), 'seed'),
    (v_cond1_id, v_c1_gd_manut_id, v_c1_b201_id, 0.100000, 100.00, '2026-01-01', false, NOW(), 'seed'),
    (v_cond1_id, v_c1_gd_manut_id, v_c1_b202_id, 0.100000, 100.00, '2026-01-01', false, NOW(), 'seed');

-- Cond1 — grupo segurança (IGUALITARIO, mesmos coeficientes)
INSERT INTO coeficientes_rateio (condominio_id, grupo_despesa_id, apartamento_id, coeficiente, area_m2, vigencia, deleted, created_at, created_by)
VALUES
    (v_cond1_id, v_c1_gd_seg_id, v_c1_a101_id, 0.100000, 80.00, '2026-01-01', false, NOW(), 'seed'),
    (v_cond1_id, v_c1_gd_seg_id, v_c1_a102_id, 0.100000, 80.00, '2026-01-01', false, NOW(), 'seed'),
    (v_cond1_id, v_c1_gd_seg_id, v_c1_a201_id, 0.100000, 80.00, '2026-01-01', false, NOW(), 'seed'),
    (v_cond1_id, v_c1_gd_seg_id, v_c1_a202_id, 0.100000, 80.00, '2026-01-01', false, NOW(), 'seed'),
    (v_cond1_id, v_c1_gd_seg_id, v_c1_a301_id, 0.100000, 80.00, '2026-01-01', false, NOW(), 'seed'),
    (v_cond1_id, v_c1_gd_seg_id, v_c1_a302_id, 0.100000, 80.00, '2026-01-01', false, NOW(), 'seed'),
    (v_cond1_id, v_c1_gd_seg_id, v_c1_b101_id, 0.100000, 100.00, '2026-01-01', false, NOW(), 'seed'),
    (v_cond1_id, v_c1_gd_seg_id, v_c1_b102_id, 0.100000, 100.00, '2026-01-01', false, NOW(), 'seed'),
    (v_cond1_id, v_c1_gd_seg_id, v_c1_b201_id, 0.100000, 100.00, '2026-01-01', false, NOW(), 'seed'),
    (v_cond1_id, v_c1_gd_seg_id, v_c1_b202_id, 0.100000, 100.00, '2026-01-01', false, NOW(), 'seed');

-- Cond1 — grupo energia (FRACAO_IDEAL, total área = 880 m²)
-- Blocos A (80m²): 80/880 = 0.090909 | Blocos B (100m²): 100/880 = 0.113636
INSERT INTO coeficientes_rateio (condominio_id, grupo_despesa_id, apartamento_id, coeficiente, area_m2, vigencia, deleted, created_at, created_by)
VALUES
    (v_cond1_id, v_c1_gd_energ_id, v_c1_a101_id, 0.090909, 80.00, '2026-01-01', false, NOW(), 'seed'),
    (v_cond1_id, v_c1_gd_energ_id, v_c1_a102_id, 0.090909, 80.00, '2026-01-01', false, NOW(), 'seed'),
    (v_cond1_id, v_c1_gd_energ_id, v_c1_a201_id, 0.090909, 80.00, '2026-01-01', false, NOW(), 'seed'),
    (v_cond1_id, v_c1_gd_energ_id, v_c1_a202_id, 0.090909, 80.00, '2026-01-01', false, NOW(), 'seed'),
    (v_cond1_id, v_c1_gd_energ_id, v_c1_a301_id, 0.090909, 80.00, '2026-01-01', false, NOW(), 'seed'),
    (v_cond1_id, v_c1_gd_energ_id, v_c1_a302_id, 0.090909, 80.00, '2026-01-01', false, NOW(), 'seed'),
    (v_cond1_id, v_c1_gd_energ_id, v_c1_b101_id, 0.113636, 100.00, '2026-01-01', false, NOW(), 'seed'),
    (v_cond1_id, v_c1_gd_energ_id, v_c1_b102_id, 0.113636, 100.00, '2026-01-01', false, NOW(), 'seed'),
    (v_cond1_id, v_c1_gd_energ_id, v_c1_b201_id, 0.113636, 100.00, '2026-01-01', false, NOW(), 'seed'),
    (v_cond1_id, v_c1_gd_energ_id, v_c1_b202_id, 0.113636, 100.00, '2026-01-01', false, NOW(), 'seed');

-- Cond2 — grupos (IGUALITARIO, 4 apts, coef = 0.250000)
INSERT INTO coeficientes_rateio (condominio_id, grupo_despesa_id, apartamento_id, coeficiente, area_m2, vigencia, deleted, created_at, created_by)
VALUES
    (v_cond2_id, v_c2_gd_manut_id, v_c2_101_id, 0.250000, 70.00, '2026-01-01', false, NOW(), 'seed'),
    (v_cond2_id, v_c2_gd_manut_id, v_c2_102_id, 0.250000, 70.00, '2026-01-01', false, NOW(), 'seed'),
    (v_cond2_id, v_c2_gd_manut_id, v_c2_201_id, 0.250000, 70.00, '2026-01-01', false, NOW(), 'seed'),
    (v_cond2_id, v_c2_gd_manut_id, v_c2_202_id, 0.250000, 70.00, '2026-01-01', false, NOW(), 'seed'),
    (v_cond2_id, v_c2_gd_seg_id, v_c2_101_id, 0.250000, 70.00, '2026-01-01', false, NOW(), 'seed'),
    (v_cond2_id, v_c2_gd_seg_id, v_c2_102_id, 0.250000, 70.00, '2026-01-01', false, NOW(), 'seed'),
    (v_cond2_id, v_c2_gd_seg_id, v_c2_201_id, 0.250000, 70.00, '2026-01-01', false, NOW(), 'seed'),
    (v_cond2_id, v_c2_gd_seg_id, v_c2_202_id, 0.250000, 70.00, '2026-01-01', false, NOW(), 'seed');

-- ===========================================================================
-- 15. DESPESAS E RATEIO — COND1 MAIO/2026
-- ===========================================================================

-- Despesa 1: Limpeza Maio/2026 (R$ 2.000 / 10 apts = R$ 200 por apt)
INSERT INTO despesas (condominio_id, grupo_despesa_id, descricao, valor_total, competencia,
    rateio_status, deleted, created_at, updated_at, created_by, updated_by)
VALUES (v_cond1_id, v_c1_gd_manut_id, 'Limpeza e Conservação – Maio/2026',
    2000.00, '2026-05-01', 'RATEADA', false, NOW(), NOW(), 'seed', 'seed')
RETURNING id INTO v_c1_d_limp_id;

-- Execução rateio: limpeza cond1
INSERT INTO rateio_execucoes (condominio_id, despesa_id, grupo_despesa_id, tipo_execucao,
    data_execucao, despesa_total, total_unidades, total_cotas, status)
VALUES (v_cond1_id, v_c1_d_limp_id, v_c1_gd_manut_id, 'MANUAL',
    NOW(), 2000.00, 10, 2000.00, 'SUCESSO')
RETURNING id INTO v_c1_re_limp_id;

UPDATE despesas SET data_ultimo_rateio = NOW()
WHERE id = v_c1_d_limp_id AND condominio_id = v_cond1_id;

-- Cotas limpeza cond1 (R$ 200 por apt)
INSERT INTO cotas_rateio (condominio_id, despesa_id, apartamento_id, valor, rateio_execucao_id)
VALUES (v_cond1_id, v_c1_d_limp_id, v_c1_a101_id, 200.00, v_c1_re_limp_id) RETURNING id INTO v_cr_l_a101;
INSERT INTO cotas_rateio (condominio_id, despesa_id, apartamento_id, valor, rateio_execucao_id)
VALUES (v_cond1_id, v_c1_d_limp_id, v_c1_a102_id, 200.00, v_c1_re_limp_id) RETURNING id INTO v_cr_l_a102;
INSERT INTO cotas_rateio (condominio_id, despesa_id, apartamento_id, valor, rateio_execucao_id)
VALUES (v_cond1_id, v_c1_d_limp_id, v_c1_a201_id, 200.00, v_c1_re_limp_id) RETURNING id INTO v_cr_l_a201;
INSERT INTO cotas_rateio (condominio_id, despesa_id, apartamento_id, valor, rateio_execucao_id)
VALUES (v_cond1_id, v_c1_d_limp_id, v_c1_a202_id, 200.00, v_c1_re_limp_id) RETURNING id INTO v_cr_l_a202;
INSERT INTO cotas_rateio (condominio_id, despesa_id, apartamento_id, valor, rateio_execucao_id)
VALUES (v_cond1_id, v_c1_d_limp_id, v_c1_a301_id, 200.00, v_c1_re_limp_id) RETURNING id INTO v_cr_l_a301;
INSERT INTO cotas_rateio (condominio_id, despesa_id, apartamento_id, valor, rateio_execucao_id)
VALUES (v_cond1_id, v_c1_d_limp_id, v_c1_a302_id, 200.00, v_c1_re_limp_id) RETURNING id INTO v_cr_l_a302;
INSERT INTO cotas_rateio (condominio_id, despesa_id, apartamento_id, valor, rateio_execucao_id)
VALUES (v_cond1_id, v_c1_d_limp_id, v_c1_b101_id, 200.00, v_c1_re_limp_id) RETURNING id INTO v_cr_l_b101;
INSERT INTO cotas_rateio (condominio_id, despesa_id, apartamento_id, valor, rateio_execucao_id)
VALUES (v_cond1_id, v_c1_d_limp_id, v_c1_b102_id, 200.00, v_c1_re_limp_id) RETURNING id INTO v_cr_l_b102;
INSERT INTO cotas_rateio (condominio_id, despesa_id, apartamento_id, valor, rateio_execucao_id)
VALUES (v_cond1_id, v_c1_d_limp_id, v_c1_b201_id, 200.00, v_c1_re_limp_id) RETURNING id INTO v_cr_l_b201;
INSERT INTO cotas_rateio (condominio_id, despesa_id, apartamento_id, valor, rateio_execucao_id)
VALUES (v_cond1_id, v_c1_d_limp_id, v_c1_b202_id, 200.00, v_c1_re_limp_id) RETURNING id INTO v_cr_l_b202;

-- Despesa 2: Segurança Maio/2026 (R$ 3.000 / 10 apts = R$ 300 por apt)
INSERT INTO despesas (condominio_id, grupo_despesa_id, descricao, valor_total, competencia,
    rateio_status, deleted, created_at, updated_at, created_by, updated_by)
VALUES (v_cond1_id, v_c1_gd_seg_id, 'Segurança e Vigilância – Maio/2026',
    3000.00, '2026-05-01', 'RATEADA', false, NOW(), NOW(), 'seed', 'seed')
RETURNING id INTO v_c1_d_seg_id;

INSERT INTO rateio_execucoes (condominio_id, despesa_id, grupo_despesa_id, tipo_execucao,
    data_execucao, despesa_total, total_unidades, total_cotas, status)
VALUES (v_cond1_id, v_c1_d_seg_id, v_c1_gd_seg_id, 'MANUAL',
    NOW(), 3000.00, 10, 3000.00, 'SUCESSO')
RETURNING id INTO v_c1_re_seg_id;

UPDATE despesas SET data_ultimo_rateio = NOW()
WHERE id = v_c1_d_seg_id AND condominio_id = v_cond1_id;

-- Cotas segurança cond1 (R$ 300 por apt)
INSERT INTO cotas_rateio (condominio_id, despesa_id, apartamento_id, valor, rateio_execucao_id)
VALUES (v_cond1_id, v_c1_d_seg_id, v_c1_a101_id, 300.00, v_c1_re_seg_id) RETURNING id INTO v_cr_s_a101;
INSERT INTO cotas_rateio (condominio_id, despesa_id, apartamento_id, valor, rateio_execucao_id)
VALUES (v_cond1_id, v_c1_d_seg_id, v_c1_a102_id, 300.00, v_c1_re_seg_id) RETURNING id INTO v_cr_s_a102;
INSERT INTO cotas_rateio (condominio_id, despesa_id, apartamento_id, valor, rateio_execucao_id)
VALUES (v_cond1_id, v_c1_d_seg_id, v_c1_a201_id, 300.00, v_c1_re_seg_id) RETURNING id INTO v_cr_s_a201;
INSERT INTO cotas_rateio (condominio_id, despesa_id, apartamento_id, valor, rateio_execucao_id)
VALUES (v_cond1_id, v_c1_d_seg_id, v_c1_a202_id, 300.00, v_c1_re_seg_id) RETURNING id INTO v_cr_s_a202;
INSERT INTO cotas_rateio (condominio_id, despesa_id, apartamento_id, valor, rateio_execucao_id)
VALUES (v_cond1_id, v_c1_d_seg_id, v_c1_a301_id, 300.00, v_c1_re_seg_id) RETURNING id INTO v_cr_s_a301;
INSERT INTO cotas_rateio (condominio_id, despesa_id, apartamento_id, valor, rateio_execucao_id)
VALUES (v_cond1_id, v_c1_d_seg_id, v_c1_a302_id, 300.00, v_c1_re_seg_id) RETURNING id INTO v_cr_s_a302;
INSERT INTO cotas_rateio (condominio_id, despesa_id, apartamento_id, valor, rateio_execucao_id)
VALUES (v_cond1_id, v_c1_d_seg_id, v_c1_b101_id, 300.00, v_c1_re_seg_id) RETURNING id INTO v_cr_s_b101;
INSERT INTO cotas_rateio (condominio_id, despesa_id, apartamento_id, valor, rateio_execucao_id)
VALUES (v_cond1_id, v_c1_d_seg_id, v_c1_b102_id, 300.00, v_c1_re_seg_id) RETURNING id INTO v_cr_s_b102;
INSERT INTO cotas_rateio (condominio_id, despesa_id, apartamento_id, valor, rateio_execucao_id)
VALUES (v_cond1_id, v_c1_d_seg_id, v_c1_b201_id, 300.00, v_c1_re_seg_id) RETURNING id INTO v_cr_s_b201;
INSERT INTO cotas_rateio (condominio_id, despesa_id, apartamento_id, valor, rateio_execucao_id)
VALUES (v_cond1_id, v_c1_d_seg_id, v_c1_b202_id, 300.00, v_c1_re_seg_id) RETURNING id INTO v_cr_s_b202;

-- Despesa 3: Energia (PENDENTE — ainda não rateada — serve para testar o fluxo de rateio)
INSERT INTO despesas (condominio_id, grupo_despesa_id, descricao, valor_total, competencia,
    rateio_status, deleted, created_at, updated_at, created_by, updated_by)
VALUES (v_cond1_id, v_c1_gd_energ_id, 'Energia Elétrica Áreas Comuns – Maio/2026',
    1800.00, '2026-05-01', 'PENDENTE', false, NOW(), NOW(), 'seed', 'seed');

-- ===========================================================================
-- 16. DESPESAS E RATEIO — COND2 MAIO/2026
-- ===========================================================================

INSERT INTO despesas (condominio_id, grupo_despesa_id, descricao, valor_total, competencia,
    rateio_status, deleted, created_at, updated_at, created_by, updated_by)
VALUES (v_cond2_id, v_c2_gd_manut_id, 'Limpeza e Conservação – Maio/2026',
    800.00, '2026-05-01', 'RATEADA', false, NOW(), NOW(), 'seed', 'seed')
RETURNING id INTO v_c2_d_limp_id;

INSERT INTO rateio_execucoes (condominio_id, despesa_id, grupo_despesa_id, tipo_execucao,
    data_execucao, despesa_total, total_unidades, total_cotas, status)
VALUES (v_cond2_id, v_c2_d_limp_id, v_c2_gd_manut_id, 'MANUAL',
    NOW(), 800.00, 4, 800.00, 'SUCESSO')
RETURNING id INTO v_c2_re_limp_id;

UPDATE despesas SET data_ultimo_rateio = NOW()
WHERE id = v_c2_d_limp_id AND condominio_id = v_cond2_id;

INSERT INTO cotas_rateio (condominio_id, despesa_id, apartamento_id, valor, rateio_execucao_id)
VALUES (v_cond2_id, v_c2_d_limp_id, v_c2_101_id, 200.00, v_c2_re_limp_id) RETURNING id INTO v_cr_c2_101;
INSERT INTO cotas_rateio (condominio_id, despesa_id, apartamento_id, valor, rateio_execucao_id)
VALUES (v_cond2_id, v_c2_d_limp_id, v_c2_102_id, 200.00, v_c2_re_limp_id) RETURNING id INTO v_cr_c2_102;
INSERT INTO cotas_rateio (condominio_id, despesa_id, apartamento_id, valor, rateio_execucao_id)
VALUES (v_cond2_id, v_c2_d_limp_id, v_c2_201_id, 200.00, v_c2_re_limp_id) RETURNING id INTO v_cr_c2_201;
INSERT INTO cotas_rateio (condominio_id, despesa_id, apartamento_id, valor, rateio_execucao_id)
VALUES (v_cond2_id, v_c2_d_limp_id, v_c2_202_id, 200.00, v_c2_re_limp_id) RETURNING id INTO v_cr_c2_202;

-- ===========================================================================
-- 17. COBRANÇAS — COND1
-- Limpeza: 4×PAGA, 2×ENVIADA, 2×PENDENTE, 1×VENCIDA, 1×CANCELADA
-- Segurança: todas PENDENTE (cobranças futuras, não vencidas)
-- ===========================================================================

-- Limpeza: A101 PAGA (José Pereira pagou)
INSERT INTO cobrancas (condominio_id, apartamento_id, cota_rateio_id, morador_id, valor,
    vencimento, status, pago_em, email_enviado, email_enviado_em, deleted, created_at, updated_at, created_by, updated_by)
VALUES (v_cond1_id, v_c1_a101_id, v_cr_l_a101, v_jose_id, 200.00,
    '2026-05-10', 'PAGA', '2026-05-07 10:23:00', true, '2026-04-30 09:00:00', false, NOW(), NOW(), 'seed', 'seed');

-- Limpeza: A102 PAGA
INSERT INTO cobrancas (condominio_id, apartamento_id, cota_rateio_id, valor,
    vencimento, status, pago_em, email_enviado, email_enviado_em, deleted, created_at, updated_at, created_by, updated_by)
VALUES (v_cond1_id, v_c1_a102_id, v_cr_l_a102, 200.00,
    '2026-05-10', 'PAGA', '2026-05-08 14:05:00', true, '2026-04-30 09:00:00', false, NOW(), NOW(), 'seed', 'seed');

-- Limpeza: A201 PAGA (Carlos Mendes)
INSERT INTO cobrancas (condominio_id, apartamento_id, cota_rateio_id, morador_id, valor,
    vencimento, status, pago_em, email_enviado, email_enviado_em, deleted, created_at, updated_at, created_by, updated_by)
VALUES (v_cond1_id, v_c1_a201_id, v_cr_l_a201, v_carlos_id, 200.00,
    '2026-05-10', 'PAGA', '2026-05-05 08:30:00', true, '2026-04-30 09:00:00', false, NOW(), NOW(), 'seed', 'seed');

-- Limpeza: A202 PAGA
INSERT INTO cobrancas (condominio_id, apartamento_id, cota_rateio_id, valor,
    vencimento, status, pago_em, email_enviado, email_enviado_em, deleted, created_at, updated_at, created_by, updated_by)
VALUES (v_cond1_id, v_c1_a202_id, v_cr_l_a202, 200.00,
    '2026-05-10', 'PAGA', '2026-05-03 16:45:00', true, '2026-04-30 09:00:00', false, NOW(), NOW(), 'seed', 'seed');

-- Limpeza: A301 ENVIADA (Fernanda Lima — email enviado, ainda não pagou)
INSERT INTO cobrancas (condominio_id, apartamento_id, cota_rateio_id, morador_id, valor,
    vencimento, status, email_enviado, email_enviado_em, deleted, created_at, updated_at, created_by, updated_by)
VALUES (v_cond1_id, v_c1_a301_id, v_cr_l_a301, v_fernanda_id, 200.00,
    '2026-05-10', 'ENVIADA', true, '2026-04-30 09:00:00', false, NOW(), NOW(), 'seed', 'seed');

-- Limpeza: A302 ENVIADA
INSERT INTO cobrancas (condominio_id, apartamento_id, cota_rateio_id, valor,
    vencimento, status, email_enviado, email_enviado_em, deleted, created_at, updated_at, created_by, updated_by)
VALUES (v_cond1_id, v_c1_a302_id, v_cr_l_a302, 200.00,
    '2026-05-10', 'ENVIADA', true, '2026-04-30 09:00:00', false, NOW(), NOW(), 'seed', 'seed');

-- Limpeza: B101 PENDENTE (Ana Costa — proprietária não residente, e-mail não enviado)
INSERT INTO cobrancas (condominio_id, apartamento_id, cota_rateio_id, valor,
    vencimento, status, email_enviado, deleted, created_at, updated_at, created_by, updated_by)
VALUES (v_cond1_id, v_c1_b101_id, v_cr_l_b101, 200.00,
    '2026-05-10', 'PENDENTE', false, false, NOW(), NOW(), 'seed', 'seed');

-- Limpeza: B102 PENDENTE
INSERT INTO cobrancas (condominio_id, apartamento_id, cota_rateio_id, valor,
    vencimento, status, email_enviado, deleted, created_at, updated_at, created_by, updated_by)
VALUES (v_cond1_id, v_c1_b102_id, v_cr_l_b102, 200.00,
    '2026-05-10', 'PENDENTE', false, false, NOW(), NOW(), 'seed', 'seed');

-- Limpeza: B201 VENCIDA (Pedro Santos — proprietário-morador em atraso)
INSERT INTO cobrancas (condominio_id, apartamento_id, cota_rateio_id, morador_id, valor,
    vencimento, status, email_enviado, email_enviado_em, deleted, created_at, updated_at, created_by, updated_by)
VALUES (v_cond1_id, v_c1_b201_id, v_cr_l_b201, v_pedro_id, 200.00,
    '2026-05-10', 'VENCIDA', true, '2026-04-30 09:00:00', false, NOW(), NOW(), 'seed', 'seed');

-- Limpeza: B202 CANCELADA (ex-morador)
INSERT INTO cobrancas (condominio_id, apartamento_id, cota_rateio_id, valor,
    vencimento, status, email_enviado, deleted, created_at, updated_at, created_by, updated_by)
VALUES (v_cond1_id, v_c1_b202_id, v_cr_l_b202, 200.00,
    '2026-05-10', 'CANCELADA', false, false, NOW(), NOW(), 'seed', 'seed');

-- Segurança: todas PENDENTE (10 apts × R$ 300, vence em 10/06/2026)
INSERT INTO cobrancas (condominio_id, apartamento_id, cota_rateio_id, valor,
    vencimento, status, email_enviado, deleted, created_at, updated_at, created_by, updated_by)
VALUES
    (v_cond1_id, v_c1_a101_id, v_cr_s_a101, 300.00, '2026-06-10', 'PENDENTE', false, false, NOW(), NOW(), 'seed', 'seed'),
    (v_cond1_id, v_c1_a102_id, v_cr_s_a102, 300.00, '2026-06-10', 'PENDENTE', false, false, NOW(), NOW(), 'seed', 'seed'),
    (v_cond1_id, v_c1_a201_id, v_cr_s_a201, 300.00, '2026-06-10', 'PENDENTE', false, false, NOW(), NOW(), 'seed', 'seed'),
    (v_cond1_id, v_c1_a202_id, v_cr_s_a202, 300.00, '2026-06-10', 'PENDENTE', false, false, NOW(), NOW(), 'seed', 'seed'),
    (v_cond1_id, v_c1_a301_id, v_cr_s_a301, 300.00, '2026-06-10', 'PENDENTE', false, false, NOW(), NOW(), 'seed', 'seed'),
    (v_cond1_id, v_c1_a302_id, v_cr_s_a302, 300.00, '2026-06-10', 'PENDENTE', false, false, NOW(), NOW(), 'seed', 'seed'),
    (v_cond1_id, v_c1_b101_id, v_cr_s_b101, 300.00, '2026-06-10', 'PENDENTE', false, false, NOW(), NOW(), 'seed', 'seed'),
    (v_cond1_id, v_c1_b102_id, v_cr_s_b102, 300.00, '2026-06-10', 'PENDENTE', false, false, NOW(), NOW(), 'seed', 'seed'),
    (v_cond1_id, v_c1_b201_id, v_cr_s_b201, 300.00, '2026-06-10', 'PENDENTE', false, false, NOW(), NOW(), 'seed', 'seed'),
    (v_cond1_id, v_c1_b202_id, v_cr_s_b202, 300.00, '2026-06-10', 'PENDENTE', false, false, NOW(), NOW(), 'seed', 'seed');

-- ===========================================================================
-- 18. COBRANÇAS — COND2
-- ===========================================================================

INSERT INTO cobrancas (condominio_id, apartamento_id, cota_rateio_id, morador_id, valor,
    vencimento, status, pago_em, email_enviado, email_enviado_em, deleted, created_at, updated_at, created_by, updated_by)
VALUES (v_cond2_id, v_c2_101_id, v_cr_c2_101, v_roberta_id, 200.00,
    '2026-05-10', 'PAGA', '2026-05-06 11:00:00', true, '2026-04-30 09:00:00', false, NOW(), NOW(), 'seed', 'seed');

INSERT INTO cobrancas (condominio_id, apartamento_id, cota_rateio_id, morador_id, valor,
    vencimento, status, pago_em, email_enviado, email_enviado_em, deleted, created_at, updated_at, created_by, updated_by)
VALUES (v_cond2_id, v_c2_102_id, v_cr_c2_102, v_luciana_id, 200.00,
    '2026-05-10', 'PAGA', '2026-05-04 09:15:00', true, '2026-04-30 09:00:00', false, NOW(), NOW(), 'seed', 'seed');

INSERT INTO cobrancas (condominio_id, apartamento_id, cota_rateio_id, morador_id, valor,
    vencimento, status, email_enviado, deleted, created_at, updated_at, created_by, updated_by)
VALUES (v_cond2_id, v_c2_201_id, v_cr_c2_201, v_marcos_id, 200.00,
    '2026-05-10', 'PENDENTE', false, false, NOW(), NOW(), 'seed', 'seed');

INSERT INTO cobrancas (condominio_id, apartamento_id, cota_rateio_id, valor,
    vencimento, status, email_enviado, deleted, created_at, updated_at, created_by, updated_by)
VALUES (v_cond2_id, v_c2_202_id, v_cr_c2_202, 200.00,
    '2026-05-10', 'VENCIDA', false, false, NOW(), NOW(), 'seed', 'seed');

-- ===========================================================================
-- 19. LANÇAMENTOS BANCÁRIOS — COND1
-- Refletem pagamentos recebidos e despesas pagas em Maio/2026.
-- 6 CONCILIADOS (vinculados ao extrato), 2 PENDENTES.
-- ===========================================================================

INSERT INTO lancamentos_bancarios (condominio_id, conta_bancaria_id, data_lancamento, valor,
    tipo, descricao, origem, status, deleted, created_at, updated_at, created_by, updated_by)
VALUES (v_cond1_id, v_c1_cc_id, '2026-05-02', 200.00, 'CREDITO',
    'Cota cond. limpeza – Apto A101', 'COTA_CONDOMINIO', 'CONCILIADO',
    false, NOW(), NOW(), 'seed', 'seed')
RETURNING id INTO v_lb_l_a101;

INSERT INTO lancamentos_bancarios (condominio_id, conta_bancaria_id, data_lancamento, valor,
    tipo, descricao, origem, status, deleted, created_at, updated_at, created_by, updated_by)
VALUES (v_cond1_id, v_c1_cc_id, '2026-05-03', 200.00, 'CREDITO',
    'Cota cond. limpeza – Apto A102', 'COTA_CONDOMINIO', 'CONCILIADO',
    false, NOW(), NOW(), 'seed', 'seed')
RETURNING id INTO v_lb_l_a102;

INSERT INTO lancamentos_bancarios (condominio_id, conta_bancaria_id, data_lancamento, valor,
    tipo, descricao, origem, status, deleted, created_at, updated_at, created_by, updated_by)
VALUES (v_cond1_id, v_c1_cc_id, '2026-05-05', 200.00, 'CREDITO',
    'Cota cond. limpeza – Apto A201', 'COTA_CONDOMINIO', 'CONCILIADO',
    false, NOW(), NOW(), 'seed', 'seed')
RETURNING id INTO v_lb_l_a201;

INSERT INTO lancamentos_bancarios (condominio_id, conta_bancaria_id, data_lancamento, valor,
    tipo, descricao, origem, status, deleted, created_at, updated_at, created_by, updated_by)
VALUES (v_cond1_id, v_c1_cc_id, '2026-05-07', 200.00, 'CREDITO',
    'Cota cond. limpeza – Apto A202', 'COTA_CONDOMINIO', 'CONCILIADO',
    false, NOW(), NOW(), 'seed', 'seed')
RETURNING id INTO v_lb_l_a202;

INSERT INTO lancamentos_bancarios (condominio_id, conta_bancaria_id, data_lancamento, valor,
    tipo, descricao, origem, status, deleted, created_at, updated_at, created_by, updated_by)
VALUES (v_cond1_id, v_c1_cc_id, '2026-05-10', 2000.00, 'DEBITO',
    'Pgto empresa limpeza SERVLIMPO LTDA', 'DESPESA_ORDINARIA', 'CONCILIADO',
    false, NOW(), NOW(), 'seed', 'seed')
RETURNING id INTO v_lb_pag_limp;

INSERT INTO lancamentos_bancarios (condominio_id, conta_bancaria_id, data_lancamento, valor,
    tipo, descricao, origem, status, deleted, created_at, updated_at, created_by, updated_by)
VALUES (v_cond1_id, v_c1_cc_id, '2026-05-02', 300.00, 'CREDITO',
    'Cota cond. segurança – Apto A101', 'COTA_CONDOMINIO', 'CONCILIADO',
    false, NOW(), NOW(), 'seed', 'seed')
RETURNING id INTO v_lb_s_a101;

-- Lançamentos PENDENTES (não estão no extrato importado)
INSERT INTO lancamentos_bancarios (condominio_id, conta_bancaria_id, data_lancamento, valor,
    tipo, descricao, origem, status, deleted, created_at, updated_at, created_by, updated_by)
VALUES (v_cond1_id, v_c1_cc_id, '2026-05-28', 600.00, 'DEBITO',
    'Conta de energia elétrica áreas comuns', 'DESPESA_ORDINARIA', 'PENDENTE',
    false, NOW(), NOW(), 'seed', 'seed');

INSERT INTO lancamentos_bancarios (condominio_id, conta_bancaria_id, data_lancamento, valor,
    tipo, descricao, origem, status, deleted, created_at, updated_at, created_by, updated_by)
VALUES (v_cond1_id, v_c1_cc_id, '2026-05-20', 350.00, 'DEBITO',
    'Manutenção portão eletrônico', 'DESPESA_ORDINARIA', 'PENDENTE',
    false, NOW(), NOW(), 'seed', 'seed');

-- ===========================================================================
-- 20. LANÇAMENTOS BANCÁRIOS — COND2
-- ===========================================================================

INSERT INTO lancamentos_bancarios (condominio_id, conta_bancaria_id, data_lancamento, valor,
    tipo, descricao, origem, status, deleted, created_at, updated_at, created_by, updated_by)
VALUES (v_cond2_id, v_c2_cc_id, '2026-05-06', 200.00, 'CREDITO',
    'Cota cond. limpeza – Apto 101', 'COTA_CONDOMINIO', 'PENDENTE',
    false, NOW(), NOW(), 'seed', 'seed');

INSERT INTO lancamentos_bancarios (condominio_id, conta_bancaria_id, data_lancamento, valor,
    tipo, descricao, origem, status, deleted, created_at, updated_at, created_by, updated_by)
VALUES (v_cond2_id, v_c2_cc_id, '2026-05-04', 200.00, 'CREDITO',
    'Cota cond. limpeza – Apto 102', 'COTA_CONDOMINIO', 'PENDENTE',
    false, NOW(), NOW(), 'seed', 'seed');

INSERT INTO lancamentos_bancarios (condominio_id, conta_bancaria_id, data_lancamento, valor,
    tipo, descricao, origem, status, deleted, created_at, updated_at, created_by, updated_by)
VALUES (v_cond2_id, v_c2_cc_id, '2026-05-15', 800.00, 'DEBITO',
    'Empresa de limpeza LIMPO TOTAL LTDA', 'DESPESA_ORDINARIA', 'PENDENTE',
    false, NOW(), NOW(), 'seed', 'seed');

-- ===========================================================================
-- 21. EXTRATO IMPORTADO (conciliação bancária) — COND1 MAIO/2026
-- Arquivo OFX correspondente em: scripts/mock-extrato-cond1-maio2026.ofx
--
-- Itens:
--   CONCILIADO (6): OFX001-OFX006 — casam com lancamentos já no sistema
--   PENDENTE   (2): OFX007 (TED desconhecido), OFX008 (portão — sem lancamento)
--   IGNORADO   (1): OFX009 (tarifa bancária — dispensado manualmente)
-- ===========================================================================

INSERT INTO extrato_importacoes
    (condominio_id, conta_bancaria_id, formato, data_importacao, data_inicio, data_fim,
     total_itens, itens_conciliados, itens_pendentes, status,
     nome_arquivo, deleted, created_at, updated_at, created_by, updated_by)
VALUES
    (v_cond1_id, v_c1_cc_id, 'OFX', NOW(), '2026-05-01', '2026-05-31',
     9, 6, 3, 'CONCLUIDO',
     'mock-extrato-cond1-maio2026.ofx', false, NOW(), NOW(), 'seed', 'seed')
RETURNING id INTO v_extrato_id;

-- Itens conciliados (lancamento_id aponta para o lançamento correspondente)
INSERT INTO itens_extrato (condominio_id, extrato_importacao_id, lancamento_id,
    data_lancamento, valor, tipo, descricao, numero_documento, status, deleted, created_at, updated_at)
VALUES
    (v_cond1_id, v_extrato_id, v_lb_l_a101, '2026-05-02',  200.00, 'CREDITO', 'COB COND APT A101 LIMPEZA',         'OFX001', 'CONCILIADO', false, NOW(), NOW()),
    (v_cond1_id, v_extrato_id, v_lb_l_a102, '2026-05-03',  200.00, 'CREDITO', 'COB COND APT A102 LIMPEZA',         'OFX002', 'CONCILIADO', false, NOW(), NOW()),
    (v_cond1_id, v_extrato_id, v_lb_l_a201, '2026-05-05',  200.00, 'CREDITO', 'COB COND APT A201 LIMPEZA',         'OFX003', 'CONCILIADO', false, NOW(), NOW()),
    (v_cond1_id, v_extrato_id, v_lb_l_a202, '2026-05-07',  200.00, 'CREDITO', 'COB COND APT A202 LIMPEZA',         'OFX004', 'CONCILIADO', false, NOW(), NOW()),
    (v_cond1_id, v_extrato_id, v_lb_pag_limp,'2026-05-10', 2000.00, 'DEBITO',  'PAG SERVLIMPO LTDA',               'OFX005', 'CONCILIADO', false, NOW(), NOW()),
    (v_cond1_id, v_extrato_id, v_lb_s_a101, '2026-05-02',  300.00, 'CREDITO', 'COB COND APT A101 SEGURANCA',       'OFX006', 'CONCILIADO', false, NOW(), NOW());

-- Itens sem correspondência (lancamento_id = NULL)
INSERT INTO itens_extrato (condominio_id, extrato_importacao_id, lancamento_id,
    data_lancamento, valor, tipo, descricao, numero_documento, status, deleted, created_at, updated_at)
VALUES
    (v_cond1_id, v_extrato_id, NULL, '2026-05-15', 1500.00, 'CREDITO', 'TED RECEBIDA DOC 7823901',          'OFX007', 'PENDENTE', false, NOW(), NOW()),
    (v_cond1_id, v_extrato_id, NULL, '2026-05-18',  350.00, 'DEBITO',  'MANUT PORTAO ELETRONICO',           'OFX008', 'PENDENTE', false, NOW(), NOW()),
    (v_cond1_id, v_extrato_id, NULL, '2026-05-31',   80.00, 'DEBITO',  'TARIFA BANCARIA MENSAL MAIO/2026',  'OFX009', 'IGNORADO', false, NOW(), NOW());

END $$;

COMMIT;

-- =============================================================================
-- VERIFICAÇÃO RÁPIDA (opcional — executar separadamente)
-- =============================================================================
/*
SELECT 'condominios'         AS tabela, COUNT(*) FROM condominios         UNION ALL
SELECT 'blocos',                        COUNT(*) FROM blocos               UNION ALL
SELECT 'apartamentos',                  COUNT(*) FROM apartamentos         UNION ALL
SELECT 'users (excl admin)',            COUNT(*) FROM users WHERE email != 'admin@condominio.com' UNION ALL
SELECT 'pessoas',                       COUNT(*) FROM pessoas              UNION ALL
SELECT 'moradores',                     COUNT(*) FROM moradores            UNION ALL
SELECT 'proprietarios',                 COUNT(*) FROM proprietarios        UNION ALL
SELECT 'proprietario_apartamentos',     COUNT(*) FROM proprietario_apartamentos UNION ALL
SELECT 'plano_contas',                  COUNT(*) FROM plano_contas         UNION ALL
SELECT 'grupos_despesa',                COUNT(*) FROM grupos_despesa       UNION ALL
SELECT 'coeficientes_rateio',           COUNT(*) FROM coeficientes_rateio  UNION ALL
SELECT 'despesas',                      COUNT(*) FROM despesas             UNION ALL
SELECT 'cotas_rateio',                  COUNT(*) FROM cotas_rateio         UNION ALL
SELECT 'cobrancas',                     COUNT(*) FROM cobrancas            UNION ALL
SELECT 'contas_bancarias',              COUNT(*) FROM contas_bancarias     UNION ALL
SELECT 'lancamentos_bancarios',         COUNT(*) FROM lancamentos_bancarios UNION ALL
SELECT 'extrato_importacoes',           COUNT(*) FROM extrato_importacoes  UNION ALL
SELECT 'itens_extrato',                 COUNT(*) FROM itens_extrato
ORDER BY 1;
*/
