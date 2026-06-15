# Refactoring Plan — Ciclo 9

**Source:** inline — avaliação arquitetural 2026-06-15 (dashboard/resumo endpoints, historico repository)
**Created:** 2026-06-15
**Target:** Cobertura completa dos novos endpoints (resumo de cobranças), limpeza de FQCNs e repositoryTest do HistoricoOcupacao.

## Pre-flight Checks
- [x] All tests pass before starting (1137/1137 unit + BDD)
- [x] Working branch active (master_data)

## Deliberate non-changes
- **Frontend changes** (dashboard.api.ts, useAuth.ts, DashboardSindico.tsx, ApartamentoDetailPage.tsx, HierarquiaPage.tsx, types/index.ts) — fora do escopo deste skill; sem framework de testes Java para UI.
- **migration 0043** — DDL coberto pelos BDD integration tests em PostgreSQL real.

---

## Chunk 1: Adicionar import para ResumoCobrancasDTO em CobrancaService e CobrancaController
**Why:** `CobrancaService.resumo()` e `CobrancaController.resumo()` referenciam `ResumoCobrancasDTO` pelo nome fully-qualified (`com.pmrodrigues.cobranca.dto.ResumoCobrancasDTO`) em vez de usar import. Toda outra DTO na codebase usa imports normais. É um descuido de código que dificulta a leitura.
**Entry criteria:** 1137 testes passando
**Steps:**
1. Editar `src/main/java/com/pmrodrigues/cobranca/service/CobrancaService.java`:
   - Adicionar `import com.pmrodrigues.cobranca.dto.ResumoCobrancasDTO;` no bloco de imports (após `CobrancaResumoDTO`)
   - Substituir a assinatura do método `public com.pmrodrigues.cobranca.dto.ResumoCobrancasDTO resumo()` → `public ResumoCobrancasDTO resumo()`
   - Substituir `var result = new com.pmrodrigues.cobranca.dto.ResumoCobrancasDTO(` → `var result = new ResumoCobrancasDTO(`
2. Editar `src/main/java/com/pmrodrigues/cobranca/controller/CobrancaController.java`:
   - Adicionar `import com.pmrodrigues.cobranca.dto.ResumoCobrancasDTO;` no bloco de imports (após `CobrancaResumoDTO`)
   - Substituir `ResponseEntity<ApiResponse<com.pmrodrigues.cobranca.dto.ResumoCobrancasDTO>>` → `ResponseEntity<ApiResponse<ResumoCobrancasDTO>>`
**Exit criteria:** `mvn compile -q` → BUILD SUCCESS (sem mudança de testes, apenas limpeza)
**Commit message:** `refactor(cobranca): replace FQCN with import for ResumoCobrancasDTO`

---

## Chunk 2: CobrancaServiceTest — cobertura de resumo()
**Why:** `CobrancaService.resumo()` chama `cobrancaRepository.countByStatusIn()` e `cobrancaRepository.sumValorByStatusIn()` para dois grupos de status (pendentes + enviadas; vencidas). Não há nenhum teste para esse método em `CobrancaServiceTest`. Todos os outros métodos públicos do service têm cobertura.
**Entry criteria:** Chunk 1 completo (clean compile)
**Steps:**
1. Editar `src/test/java/com/pmrodrigues/cobranca/service/CobrancaServiceTest.java`:
   - No `@BeforeEach` `setUp()`, adicionar stubs lenient para os dois métodos de agregação:
     ```java
     lenient().when(cobrancaRepository.countByStatusIn(any())).thenReturn(3L);
     lenient().when(cobrancaRepository.sumValorByStatusIn(any())).thenReturn(new BigDecimal("1500.00"));
     ```
   - Adicionar seção `// ── resumo ────────────────────────────────────────────────────────────` depois de `porApartamento`
   - Adicionar teste `resumo_deveRetornarContadoresEValores`:
     - stub específico: `countByStatusIn(List.of(PENDENTE, ENVIADA))` → 3L, `countByStatusIn(List.of(VENCIDA))` → 1L
     - stub específico: `sumValorByStatusIn(List.of(PENDENTE, ENVIADA))` → `"1500.00"`, `sumValorByStatusIn(List.of(VENCIDA))` → `"300.00"`
     - verificar que o resultado tem `quantidadePendente=3`, `totalPendente=1500.00`, `quantidadeVencida=1`, `totalVencido=300.00`
**Exit criteria:** `mvn test -q` → BUILD SUCCESS, CobrancaServiceTest com 1 teste adicional
**Commit message:** `test(cobranca): add CobrancaServiceTest coverage for resumo()`

---

## Chunk 3: CobrancaControllerTest — GET /cobrancas/resumo
**Why:** O endpoint `GET /cobrancas/resumo` (novo no ciclo dashboard) não tem nenhum teste em `CobrancaControllerTest`. CLAUDE.md exige pelo menos 2 cenários para GET sem parâmetro de path: 200 (autenticado) e 401 (não autenticado).
**Entry criteria:** Chunk 1 completo (usa `ResumoCobrancasDTO` com import limpo)
**Steps:**
1. Editar `src/test/java/com/pmrodrigues/cobranca/controller/CobrancaControllerTest.java`:
   - Adicionar import `import com.pmrodrigues.cobranca.dto.ResumoCobrancasDTO;` no bloco de imports (após `CobrancaResumoDTO`)
   - Adicionar helper:
     ```java
     private ResumoCobrancasDTO resumoCobrancasDTO() {
         return new ResumoCobrancasDTO(3L, new BigDecimal("1500.00"), 1L, new BigDecimal("300.00"));
     }
     ```
   - Adicionar seção `// ── resumo ───────────────────────────────────────────────────────────────`
   - Adicionar teste `resumo_returns200` com `@WithMockUser`: stub `cobrancaService.resumo()` → `resumoCobrancasDTO()`; verificar status 200 e `$.data.quantidadePendente` == 3
   - Adicionar teste `resumo_unauthenticated_returns401`: sem `@WithMockUser`, GET para `/cobrancas/resumo`, esperar 401
**Exit criteria:** `mvn test -q` → BUILD SUCCESS, CobrancaControllerTest com 2 testes adicionais
**Commit message:** `test(cobranca): add CobrancaControllerTest coverage for GET /cobrancas/resumo`

---

## Chunk 4: CobrancaRepositoryTest — countByStatusIn e sumValorByStatusIn
**Why:** Os dois novos métodos JPQL `countByStatusIn` e `sumValorByStatusIn` em `CobrancaRepository` (adicionados para o dashboard) não estão cobertos por testes de repositório. Todos os outros custom queries do repositório têm testes.
**Entry criteria:** Baseline de testes passando (independente dos chunks anteriores)
**Steps:**
1. Editar `src/test/java/com/pmrodrigues/cobranca/repository/CobrancaRepositoryTest.java`:
   - Adicionar seção `// ── countByStatusIn / sumValorByStatusIn ─────────────────────────────────────`
   - Adicionar teste `countByStatusIn_retornaContagemCorreta`:
     - persistir: 2 PENDENTE (apt1 e apt2) + 1 VENCIDA (apt1) via `cobranca(...)` helper
     - assertar `countByStatusIn(List.of(StatusCobranca.PENDENTE))` == 2
     - assertar `countByStatusIn(List.of(StatusCobranca.VENCIDA))` == 1
     - assertar `countByStatusIn(List.of(StatusCobranca.PENDENTE, StatusCobranca.VENCIDA))` == 3
   - Adicionar teste `sumValorByStatusIn_retornaSomaCorreta`:
     - persistir: 1 PENDENTE com valor 500.00 + 1 VENCIDA com valor 300.00 (usar helper cobranca mas com BigDecimal diferente — ou ajustar o helper)
     - Nota: o helper atual usa valor fixo de 500.00; usar diretamente `cobrancaRepository.save(Cobranca.builder()...)` para valores diferentes
     - assertar `sumValorByStatusIn(List.of(StatusCobranca.PENDENTE))` == 500.00
     - assertar `sumValorByStatusIn(List.of(StatusCobranca.VENCIDA))` == 300.00
     - assertar `sumValorByStatusIn(List.of(StatusCobranca.PAGA))` == 0 (nenhuma PAGA → COALESCE retorna 0)
**Exit criteria:** `mvn test -q` → BUILD SUCCESS, CobrancaRepositoryTest com 2 testes adicionais
**Commit message:** `test(cobranca): add CobrancaRepositoryTest coverage for countByStatusIn and sumValorByStatusIn`

---

## Chunk 5: HistoricoOcupacaoRepositoryTest
**Why:** `HistoricoOcupacaoRepository` tem um método derivado `findByApartamento_IdAndCondominio_IdOrderByDataSaidaDesc` (com lógica de filtragem por condomínio e ordenação por data_saida DESC) sem nenhum `@DataJpaTest` verificando o comportamento. O serviço e o mapper têm testes; o repositório não.
**Entry criteria:** Baseline de testes passando (independente dos chunks anteriores)
**Steps:**
1. Criar `src/test/java/com/pmrodrigues/morador/repository/HistoricoOcupacaoRepositoryTest.java`:
   - Anotações: `@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop") @Import(JpaAuditingConfig.class)`
   - Autowire: `HistoricoOcupacaoRepository historicoRepository`, `CondominioRepository condominioRepository`, `BlocoRepository blocoRepository`, `ApartamentoRepository apartamentoRepository`, `EstadoRepository estadoRepository`, `UserRepository userRepository`, `TestEntityManager em`
   - Campos de fixture: `Condominio cond1`, `Condominio cond2`, `Apartamento apt1`, `Apartamento apt2`
   - `@BeforeEach setUp()`:
     - persistir Estado
     - persistir cond1 e cond2 (dois condomínios para testar isolamento)
     - `TenantContext.setCondominioId(cond1.getId())`
     - persistir Bloco → apt1 (cond1), apt2 (cond1)
     - Para HistoricoOcupacao, usar `em.persist(entity)` diretamente (sem TenantContext ativo no prePersist — precisará setar manualmente o campo condominio via ReflectionTestUtils OU usar `TenantContext` ativo)
     - Estratégia: como `@PrePersist` lê `TenantContext`, manter `TenantContext.setCondominioId(cond1.getId())` e usar o repository.save que chama prePersist
     - Nota: HistoricoOcupacao.prePersist usa `new Condominio()` + `setId(condominioId)` — H2 tem FK constraint? Verificar se isso causa ConstraintViolation. Se sim, usar `em.getEntityManager().getReference(Condominio.class, cond1.getId())`.
     - Para `Pessoa`: HistoricoOcupacao tem FK para `pessoas.id` (via `pessoa_id`). Precisa inserir um User/Pessoa mínimo. Criar `User` mínimo via `UserRepository` e usá-lo como `Pessoa` (já que `Pessoa` é subtype de `User` — SINGLE_TABLE inheritance). Ou usar `TestEntityManager.persist()`.
     - Alternativa mais simples: usar `TestEntityManager.persist()` diretamente no `HistoricoOcupacao` setando manualmente o campo `condominio` via reflection (`ReflectionTestUtils.setField(entity, "condominio", cond1)`).
   - `@AfterEach tearDown()`: `TenantContext.clear()`
   - Testes:
     - `findByApartamento_IdAndCondominio_IdOrderByDataSaidaDesc_retornaOrdenadoPorDataSaidaDesc`:
       - Persistir 3 registros para apt1/cond1 com dataSaida diferentes (2024-01-01, 2025-06-30, 2023-01-01)
       - Chamar o repository method com apt1.getId() e cond1.getId()
       - Assertar que `content.get(0).getDataSaida()` == 2025-06-30 (mais recente primeiro)
       - Assertar `totalElements == 3`
     - `findByApartamento_IdAndCondominio_IdOrderByDataSaidaDesc_filtraPorApartamento`:
       - Persistir 2 registros para apt1 e 1 para apt2 (mesmo cond1)
       - Assertar que busca por apt1 retorna 2, busca por apt2 retorna 1
**Exit criteria:** `mvn test -q` → BUILD SUCCESS, HistoricoOcupacaoRepositoryTest com 2 testes criados
**Commit message:** `test(morador): add HistoricoOcupacaoRepositoryTest`

---

## Post-flight Checks
- [ ] Full test suite passes (`mvn test -q`)
- [ ] No TODO/FIXME markers left from this cycle
- [ ] `ResumoCobrancasDTO` importado corretamente em CobrancaService e CobrancaController
- [ ] `CobrancaService.resumo()` coberto
- [ ] `GET /cobrancas/resumo` coberto (200 + 401)
- [ ] `countByStatusIn` e `sumValorByStatusIn` cobertos em CobrancaRepositoryTest
- [ ] `HistoricoOcupacaoRepositoryTest` criado com 2 testes