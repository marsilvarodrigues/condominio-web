# Refactoring Plan — Ciclo 8

**Source:** inline — avaliação arquitetural 2026-06-11 (módulos cobranca, morador, ArchUnit)
**Created:** 2026-06-11
**Target:** Cobertura de testes completa para CobrancaMapper, PessoaSpecification, ProprietarioSpecification, CobrancaRepository, PessoaRepository e ProprietarioRepository; ArchUnit cross-module rule mais robusta.

## Pre-flight Checks
- [ ] All tests pass before starting
- [ ] No uncommitted changes
- [ ] Working branch active

## Deliberate non-changes (assessment findings rejected)
- **HistoricoOcupacao cascade soft-delete** — A entidade é um audit log imutável por design (Javadoc explícito: "never updated or soft-deleted"). Os registros denormalizam nome/email/CPF justamente para permanecer legíveis após a deleção da Pessoa. Cascatear o soft-delete destruiria a trilha de auditoria — comportamento indesejado.
- **ProprietarioService.softDeleteByCondominioId()** — Coberto por `PessoaService.softDeleteByCondominioId()` via herança SINGLE_TABLE: uma query `UPDATE Pessoa WHERE condominio.id = :id` já elimina todos os discriminadores (MORADOR, PROP_PF, PROP_PJ). Adicionar um método redundante violaría DRY.
- **@Timed description em PessoaController.historicoOcupacao** — Low/YAGNI.

---

## Chunk 1: PessoaSpecificationTest + ProprietarioSpecificationTest
**Why:** PessoaSpecification (hasNome, hasTipo, hasCpf, hasEmail) e ProprietarioSpecification (hasApartamentoId) existem sem nenhuma cobertura de teste. Todos os outros Specification classes na base têm *SpecificationTest.
**Entry criteria:** Baseline de testes passando (`mvn test -q`)
**Steps:**
1. Criar `src/test/java/com/pmrodrigues/morador/specification/PessoaSpecificationTest.java`
   - `@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop") @Import(JpaAuditingConfig.class)`
   - `@Autowired PessoaRepository pessoaRepository` + repositórios de fixture (CondominioRepository, ApartamentoRepository, BlocoRepository, EstadoRepository + UserRepository para User insert)
   - `@BeforeEach`: persistir Condominio → Bloco → Apartamento; criar 2 Morador (nomes distintos, cpfs distintos, emails distintos, tipos distintos se possível) com `em.persist()`
   - `@AfterEach`: `TenantContext.clear()`
   - Testes: `hasNome_filtrarPorSubstring`, `hasNome_comNullRetornaTodos`, `hasNome_comBlankRetornaTodos`, `hasCpf_filtrarPorCpf`, `hasCpf_comNullRetornaTodos`, `hasEmail_filtrarPorEmail`, `hasEmail_comNullRetornaTodos`
2. Criar `src/test/java/com/pmrodrigues/morador/specification/ProprietarioSpecificationTest.java`
   - Mesma estrutura `@DataJpaTest`
   - `@Autowired ProprietarioRepository proprietarioRepository`
   - `@BeforeEach`: persistir Condominio → Bloco → 2 Apartamentos; criar 2 ProprietarioPessoaFisica, associar apt1 a prop1 e apt2 a prop2
   - Testes: `hasApartamentoId_filtrarPorApartamento`, `hasApartamentoId_comNullRetornaTodos`
**Exit criteria:** `mvn test -q` → BUILD SUCCESS, ambos os novos test files executados
**Commit message:** `test: add PessoaSpecificationTest and ProprietarioSpecificationTest`

---

## Chunk 2: CobrancaMapperTest
**Why:** CobrancaMapper tem lógica manual de mapeamento (@Mapping com source paths aninhados: apartamento.id, apartamento.numero, apartamento.bloco.bloco) e campos ignorados (moradorNome, moradorEmail). Todos os outros mappers com lógica manual têm MapperTest.
**Entry criteria:** Baseline passando (independente de Chunk 1)
**Steps:**
1. Criar `src/test/java/com/pmrodrigues/cobranca/mapper/CobrancaMapperTest.java`
   - `@ExtendWith(SpringExtension.class) @ContextConfiguration(classes = {CobrancaMapperImpl.class})`
   - `@Autowired CobrancaMapper mapper`
   - Helper `cobranca()` que monta Cobranca com Apartamento (id=10, numero="101") + Bloco (bloco="A") inline (não @BeforeEach — apenas dentro do helper)
   - Testes:
     - `toDTO_mapsApartamentoFields`: verifica `apartamentoId=10`, `apartamentoNumero="101"`, `blocoNome="A"`
     - `toDTO_moradorFieldsAreNull`: verifica que `moradorNome` e `moradorEmail` são null (campos ignorados)
     - `toDTO_mapsValorAndStatus`: verifica `valor`, `status`, `vencimento`
     - `toResumoDTO_mapsCriadaEmFromCreatedAt`: verifica `criadaEm` não é null (mapeado de `createdAt`)
**Exit criteria:** `mvn test -q` → BUILD SUCCESS
**Commit message:** `test: add CobrancaMapperTest`

---

## Chunk 3: CobrancaRepositoryTest + PessoaRepositoryTest + ProprietarioRepositoryTest
**Why:** Os três repositórios têm custom query methods sem cobertura de testes de repositório (CobrancaSpecificationTest cobre apenas as Specifications, não os finders customizados nem o softDelete).
**Entry criteria:** Baseline passando (independente de Chunks 1 e 2)
**Steps:**
1. Criar `src/test/java/com/pmrodrigues/cobranca/repository/CobrancaRepositoryTest.java`
   - `@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop") @Import(JpaAuditingConfig.class)`
   - `@Autowired CobrancaRepository cobrancaRepository` + repositórios de fixture (CondominioRepository, BlocoRepository, ApartamentoRepository, EstadoRepository)
   - `@BeforeEach`: persistir Condominio → Bloco → 2 Apartamentos; inserir 3 Cobrancas (com cotaRateioId, status, emailEnviado, asaasId distintos); `TenantContext.setCondominioId(...)`
   - `@AfterEach`: `TenantContext.clear()`
   - Testes:
     - `findByCotaRateioId_whenExists_returnsCobranca`
     - `findByCotaRateioId_whenNotExists_returnsEmpty`
     - `findByApartamentoIdAndStatus_filtrarPorStatusCorreto`
     - `findByStatusAndEmailEnviadoFalse_retornaApenasNaoEnviados`
     - `findByApartamentoIdOrderByCreatedAtDesc_retornaEmOrdemDescendente`
     - `findByAsaasIdNative_whenExists_returnsCobranca`
     - `findByAsaasIdNative_whenDeleted_returnsEmpty` (verificar que o filter `deleted=false` funciona)
     - `softDeleteByCondominioId_marcaTodasDeletadas` (verificar com native SQL `SELECT deleted FROM cobrancas WHERE condominio_id = ?`)
2. Criar `src/test/java/com/pmrodrigues/morador/repository/PessoaRepositoryTest.java`
   - `@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop") @Import(JpaAuditingConfig.class)`
   - `@Autowired PessoaRepository pessoaRepository` + repositórios de fixture + `@Autowired EntityManager em`
   - `@BeforeEach`: persistir Condominio → Bloco → Apartamento; criar 2 Morador (um com apartamento_id=apt.id, outro sem); `TenantContext.setCondominioId(...)`
   - `@AfterEach`: `TenantContext.clear()`
   - Testes:
     - `findByApartamentoId_retornaMoradoresDoApartamento`
     - `findByApartamentoId_comApartamentoSemMoradores_retornaVazio`
     - `softDeleteByCondominioId_marcaTodasPessoasDeletadas` (verificar com `em.createNativeQuery("SELECT deleted FROM users WHERE id = ?")`)
3. Criar `src/test/java/com/pmrodrigues/morador/repository/ProprietarioRepositoryTest.java`
   - Mesma estrutura `@DataJpaTest`
   - `@Autowired ProprietarioRepository proprietarioRepository`
   - `@BeforeEach`: persistir Condominio → Bloco → 2 Apartamentos; criar 2 ProprietarioPessoaFisica com emails distintos; associar apt1 a prop1, apt2 a prop2
   - Testes:
     - `findByApartamentosId_retornaProprietariosDoApartamento`
     - `findByApartamentosId_comApartamentoSemProprietario_retornaVazio`
     - `findByUserEmail_whenExists_returnsProprietario`
     - `findByUserEmail_whenNotExists_returnsEmpty`
**Exit criteria:** `mvn test -q` → BUILD SUCCESS, todos os 3 novos test files executados
**Commit message:** `test: add CobrancaRepositoryTest, PessoaRepositoryTest and ProprietarioRepositoryTest`

---

## Chunk 4: ArchUnit — substituir check brittle de UserService por constante nomeada
**Why:** A condição `notAccessRepositoriesFromOtherModules()` em `ArchitectureTest.java` lines 110–111 usa construção dinâmica de string `"com.pmrodrigues." + depModule + ".service.UserService"` para permitir que PessoaService/ProprietarioService (módulo morador) acessem UserRepository (módulo security) via herança. A construção dinâmica torna o intent opaco — parece genérica mas só funciona porque ambos os casos concretos (PessoaService, ProprietarioService) estendem exatamente `com.pmrodrigues.security.service.UserService`.
**Entry criteria:** Baseline passando (independente de Chunks 1–3)
**Steps:**
1. Editar `src/test/java/com/pmrodrigues/arch/ArchitectureTest.java`:
   - Adicionar constante `private static final String USER_SERVICE_FQCN = "com.pmrodrigues.security.service.UserService";` logo abaixo de `BE_A_RECORD`
   - No método `notAccessRepositoriesFromOtherModules()` (linha 110), substituir:
     ```java
     boolean inheritedViaParent = clazz.isAssignableTo(
         "com.pmrodrigues." + depModule + ".service.UserService");
     ```
     por:
     ```java
     // PessoaService and ProprietarioService (morador module) extend UserService (security module),
     // inheriting its UserRepository dependency — this cross-module access is intentional.
     boolean inheritedViaParent = clazz.isAssignableTo(USER_SERVICE_FQCN);
     ```
**Exit criteria:** `mvn test -q` → BUILD SUCCESS, ArchUnit rule ainda passa (PessoaService/ProprietarioService não disparam violação)
**Commit message:** `refactor(arch): replace dynamic UserService string with named constant in cross-module rule`

---

## Post-flight Checks
- [ ] Full test suite passes (`mvn test -q`)
- [ ] No TODO/FIXME markers left from this cycle
- [ ] CobrancaMapper, PessoaSpecification, ProprietarioSpecification, CobrancaRepository, PessoaRepository e ProprietarioRepository todos com cobertura de teste
- [ ] ArchUnit rule legível e documentada
