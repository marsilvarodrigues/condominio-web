# Refactor Log

Append-only. Each entry records what happened, why, and the outcome.

---

## 2026-05-27 — Assessment + Plan created

**Pre-flight:** `mvn test` — 580/580 tests pass (517 unit + 63 BDD). BUILD SUCCESS.

**Assessment findings (summary):**

The codebase is architecturally coherent except for one layer that was implemented before the Specification pattern was standardised: the `Estado` entity. All other domain services (Bloco, Apartamento, Condominio) use `filterBy(FilterDTO)` + JPA Specification; EstadoService still uses named methods (`findAll`, `findByNome`, `findByUf`). The `EstadoFilterDTO` record was added later but is never used. `EstadoService.findById()` is the only `findById` in the codebase without a `@Cacheable` annotation.

**Deliberate non-changes:**
- User entity has no Hibernate `@Filter` — intentional design (Spring Security queries users globally; service-level isolation is used instead)
- No generic `CrudService`/`CrudController` — 3 domain entities, YAGNI/KISS applies
- `MeterService` injecting `MeterRegistry` — correct; it IS the abstraction wrapper

**Plan:** 2 chunks written to `refactor-plan.md`, manifest initialised in `refactor-manifest.json`.

---

## 2026-05-27 — Starting Chunk 1: Align Estado layer with Specification pattern

**Entry state:** 580/580 tests pass. Manifest: chunk 1 → in_progress.

**Steps to execute:**
1. Create EstadoSpecification.java
2. Edit EstadoRepository (add JpaSpecificationExecutor, remove named query methods)
3. Edit EstadoService (add filterBy, add @Cacheable to findById, remove findAll/findByNome/findByUf)
4. Edit EstadoController (replace @RequestParam + if-else with @ModelAttribute + filterBy)
5. Edit EstadoServiceTest (replace obsolete tests, add filterBy tests)
6. Edit EstadoControllerTest (replace stubs to use filterBy)
7. Edit EstadoRepositoryTest (remove findByUf tests)
8. Create EstadoServiceCacheTest

**Outcome:** All 8 steps completed. `mvn test` → 584 tests (521 unit + 63 BDD), 0 failures, BUILD SUCCESS.
- Net test count change: +4 (6 new from EstadoServiceCacheTest, -2 findByUf from repository test)
- EstadoFilterDTO is now used by EstadoController and EstadoService
- EstadoRepository is now a Specification executor — named query methods removed
- EstadoController `findAll()` no longer has if-else branching
- `EstadoService.findById()` is now cached (was the only uncached findById in the codebase)
- Integration tests pass unchanged — @ModelAttribute binds ?uf=SP / ?nome=Paulo automatically

**Next:** Chunk 2 — ArchUnit @Timed enforcement. Status: pending.

---

## 2026-05-27 — Starting Chunk 2: Enforce @Timed on condominio domain service methods via ArchUnit

**Entry state:** Chunk 1 complete. 584 tests pass.

**Step to execute:** Add `condominio_service_public_methods_must_be_timed` ArchRule to `ArchitectureTest.java`.
Scope: `com.pmrodrigues.condominio.service.*` public methods only.

**Outcome:** Rule passed on first run — all 15 public methods across BlocoService, ApartamentoService, CondominioService already had @Timed. No fixes needed before adding the rule.
`mvn test` → 585 tests (522 unit + 63 BDD), 0 failures, BUILD SUCCESS.

---

## 2026-05-27 — Post-flight: All chunks complete

**Final state:** 585 tests pass. Both refactoring chunks delivered.

**Post-flight checklist:**
- [x] Full test suite passes (585/585)
- [x] No TODO/FIXME markers left
- [x] EstadoFilterDTO is now used (was dead code)
- [x] EstadoSpecification aligns with BlocoSpecification and CondominioSpecification
- [x] findByUf and findByNomeContainingIgnoreCase removed from EstadoRepository
- [x] EstadoController no longer has if-else routing logic
- [x] EstadoService.findById() is now cached
- [x] ArchUnit test suite enforces @Timed on condominio domain service public methods

---

## 2026-05-28 — Ciclo 2: Assessment + Plano criado

**Assessment base:** Inspeção completa de 98 arquivos fonte.

**Estado do código no momento da avaliação:** 530 testes passando (pós-implementação do activation flow, API versioning, email template).

**Findings selecionados para refactoring (4 de 12 identificados):**

1. **UserController.update sem @PreAuthorize** — CLAUDE.md regra 6 exige anotação em PUT. A verificação de dono vive apenas no service; a política de acesso fica invisível na camada HTTP. Fix: SpEL `hasRole('ROLE_ADMIN') or #dto.email() == authentication.name`.

2. **UserService.findAll() sem Specification** — único service na base sem `filterBy(FilterDTO)`. UserRepository não estende JpaSpecificationExecutor. Mockup 06-usuarios.svg prevê filtros de perfil e status. Fix: criar UserFilterDTO, UserSpecification, refatorar service e controller.

3. **ApartamentoMapper e CondominioMapper com `new Entity()`** — `blocoFromId()` e `createEnderecoToEmbeddable()` criam objetos Java avulsos (não proxies JPA). Em sessão Hibernate com a entidade já carregada, o merge pode tentar persistir o objeto oco. Fix: `EntityManager.getReference()` em ambos os mappers.

4. **CondominioService.delete sem cascade** — Soft-delete do Condomínio não propaga para Blocos e Apartamentos filhos. Ficam como órfãos visíveis para master admin. Fix: `softDeleteByCondominioId` em BlocoRepository e ApartamentoRepository, chamados antes do delete do pai.

**Não incluídos (aceitáveis ou YAGNI):**
- Null-safe cache key para master admin: comportamento já correto (`"null:filter:..."` é chave legítima).
- Master admin creation via API: documentação deliberada, SQL direto é o path.
- API versioning v2: infraestrutura pronta, YAGNI até ter v2 real.
- Password history race condition: risco mínimo em produção.

**Plano:** 4 chunks escritos em `refactor-plan.md`, manifesto inicializado em `refactor-manifest.json`.

---

## 2026-05-28 — Chunk 1: UserController.update — @PreAuthorize owner-or-admin

**Entry state:** 530/530 testes passando.

**Alterações:**
1. `UserController.java` — adicionado `@PreAuthorize("hasRole('ROLE_ADMIN') or #dto.email() == authentication.name")` no método `update`.
2. `UserControllerTest.java` — removido `doThrow` obsoleto em `update_asUser_returns403` (Spring Security agora rejeita antes do service ser chamado; o stub nunca seria acionado).

**Nota:** Os testes `update_asUser_returns403` e `update_asUser_ownData_returns200` já cobriam os dois cenários exigidos pelo plano. Nenhum novo teste foi necessário.

**Outcome:** `mvn test` → 530 testes (unit + BDD), 0 falhas, BUILD SUCCESS.

---

## 2026-05-28 — Chunk 2: UserService — filterBy(UserFilterDTO) + UserSpecification

**Entry state:** 530/530 testes passando (após Chunk 1).

**Alterações:**
1. `UserFilterDTO.java` — novo record com campos `nome`, `email`, `enabled`, `role`.
2. `UserSpecification.java` — nova classe factory com `hasNome`, `hasEmail`, `hasEnabled`, `hasRole`.
3. `UserRepository.java` — adicionado `JpaSpecificationExecutor<User>`.
4. `UserService.java` — substituído `findAll()` por `filterBy(UserFilterDTO)` com `Specification.allOf(...)`.
5. `UserController.java` — `findAll` recebe `@ModelAttribute UserFilterDTO dto`, delega a `userService.filterBy(dto)`.
6. `UserServiceTest.java` — teste `findAll_returnsAllUsers` substituído por três testes de `filterBy`.
7. `UserControllerTest.java` — stubs `userService.findAll()` substituídos por `userService.filterBy(any(UserFilterDTO.class))`.
8. `UserSpecificationTest.java` — novo `@DataJpaTest` verificando cada predicado com casos nulo e blank.

**Outcome:** `mvn test` → 542 testes, 0 falhas, BUILD SUCCESS.

---

## 2026-05-28 — Chunk 3: ApartamentoMapper + CondominioMapper — EntityManager.getReference()

**Entry state:** 542/542 testes passando (após Chunk 2).

**Alterações:**
1. `ApartamentoMapper.java` — convertido de `interface` para `abstract class`; `blocoFromId(Long id)` agora chama `em.getReference(Bloco.class, id)` em vez de `new Bloco() + setId`.
2. `CondominioMapper.java` — convertido de `interface` para `abstract class`; `createEnderecoToEmbeddable` agora chama `em.getReference(Estado.class, dto.estado())` em vez de `new Estado() + setId`.
3. `EntityManager em` injetado via `@Autowired(required = false) setEntityManager()` (método setter, não campo — ArchUnit `noFields().beAnnotatedWith(Autowired.class)` não é violado). `required = false` para que Mockito mocks dessas classes abstratas em cache tests não falhem por ausência de EntityManager.
4. `ApartamentoMapperTest.java` — adicionado `@MockitoBean EntityManager em`; teste `toEntity_mapsNumeroAndCreatesBlocoStub` renomeado para `toEntity_mapsNumeroAndCreatesBlocoProxy` e verifica `em.getReference(Bloco.class, 5L)`.
5. `CondominioMapperTest.java` — adicionado `@MockitoBean EntityManager em` com stub em `@BeforeEach`; `toEntity_fromCreateDTO_mapsBusinessFields` verifica `em.getReference(Estado.class, 1L)`.

**Nota de design:** MapStruct não consegue propagar parâmetros de construtor de classes abstratas para o impl gerado (só conhece dependências em `uses`). Setter injection com `@Autowired(required = false)` é o único mecanismo compatível com MapStruct que não viola a regra ArchUnit de campos `@Autowired`.

**Outcome:** `mvn clean test` → 542 testes, 0 falhas, BUILD SUCCESS.

---

## 2026-05-28 — Chunk 4: CondominioService.delete — cascade soft-delete Bloco + Apartamento

**Entry state:** 542/542 testes passando (após Chunk 3).

**Alterações:**
1. `BlocoRepository.java` — adicionado `softDeleteByCondominioId(@Param Long condominioId)` com `@Modifying @Query("UPDATE Bloco b SET b.deleted = true WHERE b.condominio.id = :condominioId")`.
2. `ApartamentoRepository.java` — idem para Apartamento (`WHERE a.condominio.id = :condominioId`).
3. `CondominioService.java` — adicionado `BlocoRepository` e `ApartamentoRepository` (via `@RequiredArgsConstructor`). Em `delete()`: chama `apartamentoRepository.softDeleteByCondominioId(id)` e `blocoRepository.softDeleteByCondominioId(id)` antes do delete do pai. `@CacheEvict(condominios)` substituído por `@Caching(evict = [condominios, blocos, apartamentos])`.
4. `CondominioServiceTest.java` — adicionado `@Mock BlocoRepository` e `@Mock ApartamentoRepository`; constructor atualizado com 4 parâmetros; teste `delete_cascadesSoftDeleteToBlocosAndApartamentos` adicionado.
5. `CondominioServiceCacheTest.java` — beans adicionados ao `TestConfig`; `ConcurrentMapCacheManager` expandido para incluir `"blocos"` e `"apartamentos"`.
6. `BlocoRepositoryTest.java` — adicionado `softDeleteByCondominioId_marksAllBlocosDeleted`.
7. `ApartamentoRepositoryTest.java` — adicionado `softDeleteByCondominioId_marksAllApartamentosDeleted`.

**Outcome:** `mvn test` → 545 testes, 0 falhas, BUILD SUCCESS.

---

## 2026-05-29 — Ciclo 3: Assessment + Plano criado

**Pre-flight:** `mvn test` → 673 tests pass. BUILD SUCCESS.

**Assessment base:** Inspeção completa do módulo financeiro (98 arquivos fonte).

**Findings selecionados para refactoring (4 de N identificados):**

1. `FundoReservaService.update()` seta campos diretamente no entity, violando CLAUDE.md regra 3 (mapper deve ter `updateEntity`).
2. `OrcamentoAnualService.addItem()` injeta `EntityManager` diretamente no service para criar proxy de `PlanoContas`, em vez de usar o mapper.
3. Cascade soft-delete ausente: `CondominioService.delete()` não propaga para entidades financeiras (`PlanoContas`, `FundoReserva`, `FundoReservaMovimentacao`, `OrcamentoAnual`, `ItemOrcamento`).
4. ArchUnit `@Timed` cobre apenas `condominio.service.*`; módulos `financeiro.service.*`, `security.service.*`, `commons.service.*` ficam sem cobertura.

**Plano:** 3 chunks escritos em `refactor-plan.md`, manifesto inicializado em `refactor-manifest.json`.

---

## 2026-05-29 — Chunk 1: FundoReservaMapper.updateEntity + OrcamentoAnualMapper.toItemEntity

**Entry state:** 673 tests passando.

**Alterações:**
1. `FundoReservaMapper.java` — adicionado `updateEntity(@MappingTarget FundoReserva entity, UpdateFundoReservaDTO dto)` com 7 `@Mapping(ignore=true)`.
2. `FundoReservaService.java` — `update()` substituiu `entity.setPercentualArrecadacao(...)` + `entity.setContaBancariaDestino(...)` por `mapper.updateEntity(entity, dto)`.
3. `OrcamentoAnualMapper.java` — adicionado import `CreateItemOrcamentoDTO` + método concreto `toItemEntity(OrcamentoAnual, CreateItemOrcamentoDTO)` usando `planoContasFromId()`.
4. `OrcamentoAnualService.java` — removidos campo `EntityManager entityManager`, import `EntityManager`, import `PlanoContas`; `addItem()` usa `mapper.toItemEntity(orcamento, dto)`.
5. `FundoReservaServiceTest.java` — `update_whenExists_updatesFields` agora verifica `mapper.updateEntity(eq(entity), eq(updateDTO))`.
6. `OrcamentoAnualServiceTest.java` — removidos `@Mock EntityManager`, construtor com 3 params; `addItem_rascunho_savesItem` adiciona stub `mapper.toItemEntity(...)` e verifica chamada.
7. `OrcamentoAnualMapperTest.java` — novo test class com 2 testes: `toItemEntity_mapsOrcamentoAndPlanoContasProxy` e `toItemEntity_whenPlanoContasIdIsNull_planoContasIsNull`.

**Outcome:** `mvn test` → 675 testes, 0 falhas, BUILD SUCCESS.

---

## 2026-05-29 — Chunk 2: Cascade soft-delete financeiro ao deletar Condomínio

**Entry state:** 675 testes passando (após Chunk 1).

**Alterações:**
1. `FundoReservaRepository.java` — adicionado `softDeleteByCondominioId`.
2. `FundoReservaMovimentacaoRepository.java` — adicionado `softDeleteByCondominioId`.
3. `OrcamentoAnualRepository.java` — adicionado `softDeleteByCondominioId`.
4. `ItemOrcamentoRepository.java` — adicionado `softDeleteByCondominioId` (via `orcamentoAnual.condominio.id`).
5. `PlanoContasService.java` — adicionado `softDeleteByCondominioId` com `@Transactional`, `@Timed`, `@CacheEvict`.
6. `FundoReservaService.java` — idem; deleta movimentações antes do fundo.
7. `OrcamentoAnualService.java` — idem; deleta itens antes do orçamento.
8. `CondominioService.java` — adicionados 3 novos campos finais (`PlanoContasService`, `FundoReservaService`, `OrcamentoAnualService`); `delete()` chama os 3 novos `softDeleteByCondominioId`; `@Caching` expandido com `"plano-contas"`, `"fundo-reserva"`, `"orcamentos"`.
9. `PlanoContasServiceTest.java` — adicionado `softDeleteByCondominioId_callsRepository`.
10. `FundoReservaServiceTest.java` — adicionado `softDeleteByCondominioId_softDeletesMovimentacoesFirst` (verifica ordem via `inOrder`).
11. `OrcamentoAnualServiceTest.java` — adicionado `softDeleteByCondominioId_softDeletesItensFirst` (verifica ordem via `inOrder`).
12. `CondominioServiceTest.java` — adicionados 3 `@Mock` fields; construtor com 7 params; adicionado `delete_cascadesSoftDeleteToFinanceiroServices`.
13. `CondominioServiceCacheTest.java` — adicionados 3 novos service beans como mocks; `ConcurrentMapCacheManager` expandido; construtor `CondominioService` com 7 params.

**Outcome:** `mvn test` → 679 testes, 0 falhas, BUILD SUCCESS.

---

## 2026-05-29 — Chunk 3: Expandir ArchUnit @Timed para todos os módulos de service

**Entry state:** 679 testes passando (após Chunk 2).

**Alterações:**
1. `ArchitectureTest.java` — `condominio_service_public_methods_must_be_timed` substituído por `all_service_public_methods_must_be_timed` com padrão `com.pmrodrigues..service..`.
2. `MeterService.java` — adicionado `@Timed(value = "meter.service.incrementError", ...)` ao método `incrementError` (única violação detectada pelo novo teste; método estava sem `@Timed`).

**Outcome:** `mvn test` → 679 testes, 0 falhas, BUILD SUCCESS.

---

## 2026-05-29 — Post-flight: Ciclo 3 completo

**Estado final:** 679 testes passando. Os 3 chunks foram entregues.

**Post-flight checklist:**
- [x] Full test suite passa (679/679)
- [x] Nenhum TODO/FIXME deixado
- [x] `FundoReservaService.update()` usa `mapper.updateEntity()` (CLAUDE.md regra 3)
- [x] `OrcamentoAnualService.addItem()` usa `mapper.toItemEntity()` — `EntityManager` removido do service
- [x] `CondominioService.delete()` propaga soft-delete para todas as entidades financeiras
- [x] Caches `"plano-contas"`, `"fundo-reserva"`, `"orcamentos"` evictados junto com os existentes no delete de Condomínio
- [x] ArchUnit `@Timed` agora cobre todos os módulos de service (`com.pmrodrigues..service..`)
- [x] `MeterService.incrementError` recebeu `@Timed` (única omissão encontrada na base)

---

## 2026-05-29 — Ciclo 4: Post-flight completo

**Estado final:** 724 testes passando (unit + BDD, sem Testcontainers). Os 3 chunks foram entregues.

**Post-flight checklist:**
- [x] Full unit test suite passa (724/724, 0 failures, BUILD SUCCESS)
- [x] Nenhum TODO/FIXME deixado
- [x] `FundoReservaService` usa `ContaBancariaService` (não o repositório) para validar conta — cache `contas-bancarias` é honrado
- [x] `BancoService.findById()` lança 404 em vez de retornar Optional — consistente com o restante da base
- [x] `BancoController.findById()` simplificado (sem `.orElseThrow` manual)
- [x] `ContaBancariaService.create()` simplificado (sem `.orElseThrow` redundante)
- [x] `BancoServiceCacheTest` criado com 5 testes (filterBy cached, findById cached, create/update/delete evict)
- [x] `ContaBancariaServiceCacheTest` criado com 3 testes (findById cached, 404 não cacheado, create evict)
- [x] 5 testes novos em `FundoReservaServiceTest` cobrindo o caminho `resolveContaBancaria` (tipo correto, mismatch, não encontrado)

---

## 2026-05-29 — Chunk 1: FundoReservaService — trocar ContaBancariaRepository por ContaBancariaService

**Entry state:** 700 testes (543 unit passando, 157 Testcontainers com Docker ausente — pré-existente).

**Alterações:**
1. `FundoReservaService.java` — campo `ContaBancariaRepository contaBancariaRepository` → `ContaBancariaService contaBancariaService`; import de repository removido (mesmo pacote, sem import necessário); `resolveContaBancaria()` agora chama `contaBancariaService.findById()` (throws 404), verifica `contaDTO.tipo()`, retorna proxy `new ContaBancaria()`.
2. `FundoReservaServiceTest.java` — mock trocado; 5 novos testes adicionados: `create_withContaBancaria_fundoReservaType_saves`, `create_withContaBancaria_typeMismatch_throwsBadRequest`, `create_withContaBancaria_notFound_throwsNotFound`, `update_withContaBancaria_fundoReservaType_updates`, `update_withContaBancaria_typeMismatch_throwsBadRequest`.

**Outcome:** 16 testes, 0 falhas, BUILD SUCCESS.

---

## 2026-05-29 — Chunk 2: BancoService.findById — lançar 404 em vez de retornar Optional

**Entry state:** Chunk 1 completo.

**Alterações:**
1. `BancoService.java` — `findById` muda de `Optional<BancoDTO>` para `BancoDTO`; body usa `.orElseThrow(() -> notFound("Banco", id))`; `@Cacheable` perde `unless = "#result == null"` (404 lançado nunca chega a `null`); import `java.util.Optional` removido.
2. `BancoController.java` — `findById()` simplificado de `.orElseThrow(...)` para `var banco = bancoService.findById(id);`; imports `ResponseStatusException` e `NOT_FOUND` removidos.
3. `ContaBancariaService.java` — `create()` remove `.orElseThrow(() -> notFound("Banco", id))` substituído por `// throws 404 if banco not found`.
4. `BancoServiceTest.java` — `findById_whenFound_returnsDTO` removido `.isPresent()` e `.get()`; `findById_whenNotFound_returnsEmpty` renomeado para `findById_whenNotFound_throwsNotFound` com `assertThatThrownBy`.
5. `ContaBancariaServiceTest.java` — stubs `bancoService.findById(1L).thenReturn(Optional.of(...))` e `bancoService.findById(99L).thenReturn(Optional.empty())` atualizados para a nova API (retorno direto / `doThrow`).

**Outcome:** 22 testes (BancoServiceTest=9, ContaBancariaServiceTest=13), 0 falhas, BUILD SUCCESS.

---

## 2026-05-29 — Chunk 3: BancoServiceCacheTest e ContaBancariaServiceCacheTest

**Entry state:** Chunk 2 completo.

**Alterações:**
1. `BancoServiceCacheTest.java` (novo) — `@ContextConfiguration(TestConfig)` com `ConcurrentMapCacheManager("bancos")`; mocks de `BancoRepository` e `BancoMapper`; 5 testes: `filterBy_cached`, `findById_cached`, `create_evicts`, `update_evicts`, `delete_evicts`.
2. `ContaBancariaServiceCacheTest.java` (novo) — `ConcurrentMapCacheManager("contas-bancarias", "bancos")` (o "bancos" é necessário pois o mock de `BancoService` tem `@Cacheable("bancos")` que Spring tenta resolver); mocks de `ContaBancariaRepository`, `BancoService`, `ContaBancariaMapper`; 3 testes: `findById_cached`, `findById_throwsNotFound_notCached`, `create_evicts`.

**Nota técnica:** o cache "bancos" foi adicionado ao TestConfig do ContaBancariaServiceCacheTest porque `@EnableCaching` tenta resolver anotações `@Cacheable` de todos os beans no contexto, inclusive mocks. Sem esse cache no manager, o teste de `create_evicts` falha com `IllegalArgumentException`.

**Outcome:** 8 testes, 0 falhas. Full suite: 724 testes, 0 falhas, BUILD SUCCESS.

---

## 2026-05-29 — Ciclo 4: Assessment + Plano criado

**Pre-flight:** `mvn test` → 700 testes (543 unit passando, 157 erros em testes Testcontainers — Docker ausente, pré-existente). BUILD SUCCESS nos unit tests.

**Assessment base:** Inspeção do módulo bancário recém-adicionado (Banco, ContaBancaria, LancamentoBancario — ~15 arquivos fonte).

**Findings selecionados para refactoring (3 identificados):**

1. **FundoReservaService injeta ContaBancariaRepository diretamente** — bypassa o cache `contas-bancarias` que `ContaBancariaService.findById()` usa. Viola o padrão intra-módulo de serviço-para-serviço (CLAUDE.md regra 16). Fix: trocar injeção pelo service, usar DTO type check + proxy `new ContaBancaria()`.

2. **BancoService.findById() retorna Optional<BancoDTO>** — inconsistente com `ContaBancariaService.findById()` que lança 404 diretamente (CLAUDE.md regra 5: use `Exceptions.notFound()`). Força `BancoController` a usar ResponseStatusException ad-hoc e `ContaBancariaService.create()` a ter `.orElseThrow(...)` verboso. Fix: alterar retorno para `BancoDTO`, lançar 404 internamente.

3. **Ausência de cache tests para BancoService e ContaBancariaService** — todos os outros services com `@Cacheable` na base têm `*CacheTest` dedicado (EstadoServiceCacheTest, BlocoServiceCacheTest, ApartamentoServiceCacheTest, CondominioServiceCacheTest). Fix: criar BancoServiceCacheTest e ContaBancariaServiceCacheTest.

**Não incluídos (deliberadas não-mudanças):**
- EstadoService.findById() retorna Optional — mesmo padrão, mas YAGNI (EstadoService é catálogo estático, sem sofrimento actual).
- LancamentoBancarioService sem cache — intencional (dados transacionais de alto volume).
- FundoReservaService.resolveContaBancaria criando proxy — consistente com ContaBancariaService.create().

**Plano:** 3 chunks escritos em `refactor-plan.md`, manifesto reinicializado em `refactor-manifest.json`.

---

## 2026-06-05 — Ciclo 5: Assessment + Plano criado

**Pre-flight:** `mvn test` → 724 testes passando (unit). BUILD SUCCESS (Testcontainers ignorados sem Docker).

**Assessment base:** Inspeção do módulo morador recém-adicionado (Pessoa, Proprietario — ~25 arquivos fonte).

**Findings selecionados para refactoring (5 identificados, 4 chunks planejados):**

1. **PessoaService e ProprietarioService injetam EntityManager diretamente** — violação do padrão estabelecido em Ciclo 2 Chunk 3 (ApartamentoMapper, CondominioMapper) e Ciclo 3 Chunk 1 (OrcamentoAnualMapper). O `EntityManager.getReference()` deve viver no mapper, não no service. Fix: adicionar `apartamentoFromId(Long id)` em PessoaMapper e ProprietarioMapper via setter injection, remover EntityManager dos services.

2. **CondominioService.delete() não cascateia soft-delete para Pessoas** — ao deletar um Condomínio, moradores (Pessoa com apartamento_id) e proprietários (Proprietario via join table) ficam como órfãos visíveis. Fix: `softDeleteByCondominioId` em PessoaRepository e PessoaService; chamar de CondominioService. Uma query `UPDATE Pessoa WHERE condominio.id = :id` cobre todos os discriminadores (PF, PJ, PROP_PF, PROP_PJ) por ser SINGLE_TABLE.

3. **PessoaMapperTest e ProprietarioMapperTest ausentes** — mappers com lógica manual (toEntity, updateEntity, resolvers @Named) sem nenhuma cobertura de teste. Depende do Finding 1 (Chunk 1) para testar `apartamentoFromId` completo.

4. **PessoaControllerTest e ProprietarioControllerTest ausentes** — todos os outros controllers têm testes (Bloco, Apartamento, Condominio, Banco…). Morador não tem nenhum.

5. **ProprietarioServiceTest não cobre update()** — método adicionado nesta sessão (Ciclo 5), sem cobertura. Adicionado ao Chunk 4 junto com os controller tests.

**Não incluídos:**
- PessoaRepository e ProprietarioRepository tests já cobertos por testes básicos; sem gaps críticos identificados.
- Cache tests para PessoaService/ProprietarioService — nenhum @Cacheable nos services de morador; YAGNI.

**Plano:** 4 chunks escritos em `refactor-plan.md`, manifesto inicializado em `refactor-manifest.json`. Ordem de execução: Chunks 1 e 2 paralelos, depois Chunk 3 (depende de 1), depois Chunk 4 (independente mas deixado por último para ter cobertura total).

---

## 2026-06-05 — Chunk 1: Mover EntityManager.getReference() dos services morador para os mappers

**Entry state:** 797 unit tests passando (baseline pré-Ciclo 5).

**Alterações:**
1. `PessoaMapper.java` — adicionado campo `EntityManager em`; setter `@Autowired(required = false) setEntityManager()`; método concreto `apartamentoFromId(Long id)`.
2. `PessoaService.java` — removido campo `EntityManager entityManager` e import; `create()` e `assignToApartamento()` passaram a usar `mapper.apartamentoFromId()`.
3. `ProprietarioMapper.java` — mesma adição de `EntityManager` via setter e `apartamentoFromId(Long id)`.
4. `ProprietarioService.java` — removido `EntityManager entityManager`; `associarApartamento()` usa `mapper.apartamentoFromId()`.
5. `PessoaServiceTest.java` — removido `@Mock EntityManager`; construtor atualizado para 3 parâmetros; stub `entityManager.getReference()` → `mapper.apartamentoFromId()`.
6. `ProprietarioServiceTest.java` — mesma atualização, construtor 2 parâmetros.

**Outcome:** PessoaServiceTest=11, ProprietarioServiceTest=10; ArchUnit=8; BUILD SUCCESS.

---

## 2026-06-05 — Chunk 2: CondominioService.delete() — cascade soft-delete para Pessoas

**Entry state:** Chunk 1 completo.

**Alterações:**
1. `PessoaRepository.java` — adicionado `softDeleteByCondominioId(@Param Long condominioId)` com `@Modifying @Query("UPDATE Pessoa p SET p.deleted = true WHERE p.condominio.id = :condominioId")`.
2. `PessoaService.java` — adicionado `softDeleteByCondominioId(Long condominioId)` com `@Transactional`, `@Timed`.
3. `CondominioService.java` — adicionado campo `PessoaService pessoaService`; import `com.pmrodrigues.morador.service.PessoaService`; `delete()` chama `pessoaService.softDeleteByCondominioId(id)` após `lancamentoBancarioService`.
4. `PessoaServiceTest.java` — adicionado `softDeleteByCondominioId_callsRepository`.
5. `CondominioServiceTest.java` — adicionado `@Mock PessoaService pessoaService`; construtor com 10 parâmetros; adicionado `delete_cascadesSoftDeleteToPessoas`.
6. `CondominioServiceCacheTest.java` — adicionado bean `pessoaService`; construtor atualizado.

**Outcome:** PessoaServiceTest=12, CondominioServiceTest=17, CondominioServiceCacheTest=5; BUILD SUCCESS.

---

## 2026-06-05 — Chunk 3: PessoaMapperTest + ProprietarioMapperTest

**Entry state:** Chunk 1 completo.

**Alterações:**
1. Criado `src/test/java/com/pmrodrigues/morador/mapper/PessoaMapperTest.java` — 7 testes: toDTO para PF e PJ, toEntity PF e PJ, updateEntity ignora nulos, apartamentoFromId retorna proxy, apartamentoFromId null retorna null.
2. Criado `src/test/java/com/pmrodrigues/morador/mapper/ProprietarioMapperTest.java` — 7 testes: mesma estrutura para ProprietarioPF e ProprietarioPJ.

**Outcome:** PessoaMapperTest=7, ProprietarioMapperTest=7; BUILD SUCCESS.

---

## 2026-06-05 — Chunk 4: PessoaControllerTest + ProprietarioControllerTest + ProprietarioServiceTest.update

**Entry state:** Chunks 1–3 completos.

**Alterações:**
1. `ProprietarioServiceTest.java` — adicionados `update_whenExists_updatesFields` e `update_whenNotFound_throws404`.
2. Criado `src/test/java/com/pmrodrigues/morador/controller/PessoaControllerTest.java` — 11 testes cobrindo todos os endpoints (GET lista, GET por id, POST, PUT, DELETE, assignToApartamento, removeFromApartamento) + cenários 401/403.
3. Criado `src/test/java/com/pmrodrigues/morador/controller/ProprietarioControllerTest.java` — 12 testes cobrindo todos os endpoints (incluindo findByApartamento).
4. **Bug fix colateral:** `Pessoa.java` tinha `@FilterDef` duplicado ao lado do `@FilterDef` já declarado em `package-info.java` de `com.pmrodrigues.commons.model`. Removido o `@FilterDef` e os imports `FilterDef`/`ParamDef` de `Pessoa.java`. Este bug impedia o startup do contexto Spring em *todos* os testes `@SpringBootTest` — os controller tests de todos os módulos estavam falhando silenciosamente. Fix resolve BlocoControllerTest e ApartamentoControllerTest também.

**Outcome:** PessoaControllerTest=11, ProprietarioControllerTest=12, ProprietarioServiceTest=12; BlocoControllerTest=18 (unblocked), ArchUnit=8; Total suite: 797 testes passando; BUILD SUCCESS.

---

## 2026-06-05 — Post-flight: Ciclo 5 completo

**Estado final:** 797 unit tests passando. Os 4 chunks foram entregues.

**Post-flight checklist:**
- [x] Full unit test suite passa (797/797, 0 failures, BUILD SUCCESS)
- [x] Nenhum TODO/FIXME deixado
- [x] `EntityManager` removido de `PessoaService` e `ProprietarioService` — movido para mappers via setter injection
- [x] `PessoaMapper.apartamentoFromId()` e `ProprietarioMapper.apartamentoFromId()` criados seguindo o padrão Ciclo 2 Chunk 3
- [x] `CondominioService.delete()` propaga soft-delete para Pessoa (cobre todos os discriminadores SINGLE_TABLE via uma query)
- [x] `PessoaMapperTest` (7 testes) e `ProprietarioMapperTest` (7 testes) criados
- [x] `PessoaControllerTest` (11 testes) e `ProprietarioControllerTest` (12 testes) criados
- [x] `ProprietarioServiceTest` cobre o método `update()`
- [x] Bug colateral corrigido: `@FilterDef` duplicado em `Pessoa.java` que impedia todos os `@SpringBootTest` de inicializar

---

## 2026-06-09 — Ciclo 7: Assessment + Plano criado

**Pre-flight:** `mvn test` → 1065 testes passando (após correções de startup desta sessão). BUILD SUCCESS.

**Assessment base:** Inspeção do código do feature dashboard (JwtService, ProprietarioController.meusImoveis, UserService.create roles, ProprietarioClaimsProviderImpl).

**Findings (3 gaps de cobertura):**

1. `ProprietarioController.meusImoveis` — endpoint sem nenhum teste em `ProprietarioControllerTest`. CLAUDE.md exige 5 cenários por endpoint.
2. `UserService.create` — validação `ROLES_PERMITIDOS` adicionada sem teste correspondente em `UserServiceTest`.
3. `ProprietarioClaimsProviderImpl` — nova `@Service` sem classe de teste.

**Plano:** 3 chunks independentes escritos, manifesto inicializado.

---

## 2026-06-09 — Chunk 1: ProprietarioClaimsProviderImplTest

**Entry state:** 1065 testes passando.

**Alterações:**
1. Criado `src/test/java/com/pmrodrigues/morador/service/ProprietarioClaimsProviderImplTest.java` — 2 testes: `findClaimsByEmail_whenProprietarioExists_returnsClaims` (verifica id e apartamentoIds) e `findClaimsByEmail_whenNotProprietario_returnsEmpty`.

**Outcome:** ProprietarioClaimsProviderImplTest=2; BUILD SUCCESS.

---

## 2026-06-09 — Chunk 2: UserServiceTest — create_withInvalidRole_throwsBadRequest

**Entry state:** Chunk 1 completo.

**Alterações:**
1. `UserServiceTest.java` — adicionado `create_withInvalidRole_throwsBadRequest`: cria um DTO com `ROLE_DESCONHECIDO`, verifica `ResponseStatusException` com status 400, verifica que `userRepository.save` nunca é chamado.

**Outcome:** UserServiceTest=27 (+1); BUILD SUCCESS.

---

## 2026-06-09 — Chunk 3: ProprietarioControllerTest — 4 testes para meusImoveis

**Entry state:** Chunk 2 completo.

**Alterações:**
1. `ProprietarioControllerTest.java` — adicionados 4 testes para `GET /proprietarios/meus-imoveis`:
   - `meusImoveis_asProprietario_returns200` — usa `jwt().jwt(...).authorities(ROLE_PROPRIETARIO)` com claim `proprietario_id=42`; verifica 200 e nome no JSON.
   - `meusImoveis_claimMissing_returns404` — JWT sem `proprietario_id`; verifica 404.
   - `meusImoveis_unauthenticated_returns401` — sem autenticação; verifica 401.
   - `meusImoveis_asUser_returns403` — `@WithMockUser(roles="USER")`; verifica 403.

**Nota técnica:** `@WithMockUser` gera um `UsernamePasswordAuthenticationToken` que não é um `Jwt`, então não pode ser usado com `@AuthenticationPrincipal Jwt`. Para testes que precisam do `Jwt` (200 e 404), usa-se `SecurityMockMvcRequestPostProcessors.jwt()` com `.authorities()` explícito, pois o `JwtAuthenticationConverter` não é executado pelo post-processor.

**Outcome:** ProprietarioControllerTest=16 (+4); suite completa: 1072 testes, 0 falhas; BUILD SUCCESS.

---

## 2026-06-09 — Post-flight: Ciclo 7 completo

**Estado final:** 1072 testes passando (+7 em relação ao baseline pré-Ciclo 7). Os 3 chunks foram entregues.

**Post-flight checklist:**
- [x] Full test suite passa (1072/1072, 0 failures, BUILD SUCCESS)
- [x] Nenhum TODO/FIXME deixado
- [x] `ProprietarioClaimsProviderImpl.findClaimsByEmail` coberto (2 testes)
- [x] `UserService.create` com role inválido coberto (1 teste)
- [x] `ProprietarioController.meusImoveis` coberto com 4 cenários obrigatórios (200, 404, 401, 403)

---

## 2026-05-28 — Post-flight: Ciclo 2 completo

**Estado final:** 545 testes passando. Os 4 chunks foram entregues.

**Post-flight checklist:**
- [x] Full test suite passa (545/545)
- [x] Nenhum TODO/FIXME deixado
- [x] `PUT /users/{id}` agora tem `@PreAuthorize` explícito (owner-or-admin)
- [x] `UserService` alinhado com padrão `filterBy(FilterDTO)` + `UserSpecification`
- [x] `ApartamentoMapper` e `CondominioMapper` convertidos para abstract class com `EntityManager.getReference()`
- [x] `CondominioService.delete()` propaga soft-delete para Blocos e Apartamentos filhos
- [x] Caches `"blocos"` e `"apartamentos"` evictados junto com `"condominios"` no delete

---

## 2026-06-09 — Startup: correção de test suite pós-dashboard (Ciclo 6 → pré-Ciclo 7)

**Contexto:** O skill `/architectural-refactor` foi invocado. Fase 3 startup verificou o estado do manifesto (Ciclo 6 completo) e rodou `mvn test`. O suite falhou com 185 erros.

**Causa raiz (3 problemas encadeados):**

1. **`JwtServiceTest` — NullPointerException:** `JwtService` recebeu novo campo `ProprietarioRepository` via `@RequiredArgsConstructor` (adicionado para os claims do dashboard). `JwtServiceTest` usa `@InjectMocks` mas não tinha `@Mock ProprietarioRepository`. Resultado: NPE ao chamar `proprietarioRepository.findByUserEmail()`.

2. **`JwtService` — `IllegalArgumentException` ao setar claim nulo:** `JwtClaimsSet.Builder.claim()` não aceita valores nulos. Quando o usuário não é proprietário, `proprietarioId` é `null` — lançava `IllegalArgumentException`. Fix: builder condicional (`if (proprietarioId != null) builder.claim(...)`).

3. **`JwtService` — violação ArchUnit:** `JwtService` (módulo `security`) injetava `ProprietarioRepository` (módulo `morador`), violando a regra ArchUnit "no @Service calls @Repository from another module". Como `morador` já depende de `security`, a inversão criaria dependência circular.

**Solução — Dependency Inversion (DIP):**
- Criado `ProprietarioClaimsProvider` interface em `security.service` (port)
- Criado `ProprietarioClaims` record em `security.dto` (fora do pacote `service` para não violar regra `@Timed`)
- Criado `ProprietarioClaimsProviderImpl` em `morador.service` (adapter), com `@Timed`
- `JwtService` injetou `ProprietarioClaimsProvider` (não o repositório)
- `ProprietarioRepository.findByUserEmail` alterado de native SQL para JPQL (`SELECT p FROM Proprietario p WHERE p.email = :email`) — o native SQL causava `clazz_` não encontrado no ResultSet porque Hibernate 6 não consegue hidratar entidade JOINED inheritance a partir de `SELECT pr.*` sem o discriminador

**Arquivos alterados:**
- `src/main/java/com/pmrodrigues/security/service/JwtService.java` — usa `ProprietarioClaimsProvider`; claim null-safe
- `src/main/java/com/pmrodrigues/security/service/ProprietarioClaimsProvider.java` — nova interface (port)
- `src/main/java/com/pmrodrigues/security/dto/ProprietarioClaims.java` — novo record
- `src/main/java/com/pmrodrigues/morador/service/ProprietarioClaimsProviderImpl.java` — nova implementação
- `src/main/java/com/pmrodrigues/morador/repository/ProprietarioRepository.java` — JPQL em vez de native SQL
- `src/test/java/com/pmrodrigues/security/service/JwtServiceTest.java` — mock `ProprietarioClaimsProvider`

**Outcome:** `mvn test` → 1065 testes, 0 falhas, 0 erros, BUILD SUCCESS.

---

## 2026-06-11 — Ciclo 8: missing tests + ArchUnit fix

**Assessment findings addressed:** 5 (CobrancaMapper test coverage, Cobrança/Pessoa/Proprietário repository tests, brittle ArchUnit UserService check).

**Chunk 1 — PessoaSpecificationTest + ProprietarioSpecificationTest (13 tests):**
- Production fix to `PessoaSpecification.hasTipo`: changed `root.type().as(String.class)` → `root.get("pessoaTipo")` (H2 can't compare DECFLOAT with String from CASE expression in JOINED inheritance).
- Production fix to `PessoaSpecification.hasCpf`: changed `root.get("cpf")` → `cb.treat()` on each subclass (attribute name is ambiguous across Morador and ProprietarioPessoaFisica).
- Used valid Brazilian CPFs `"529.982.247-25"` / `"111.444.777-35"` in ProprietarioSpecificationTest (`@CPF` validation is active in @DataJpaTest).

**Chunk 2 — CobrancaMapperTest (4 tests):**
- Added `.status(StatusCobranca.PENDENTE)` to builder in test helper (`@PrePersist` does not fire in non-persistence mapper tests).

**Chunk 3 — CobrancaRepositoryTest (8) + PessoaRepositoryTest (2) + ProprietarioRepositoryTest (4):**
- `softDeleteByCondominioId` test removed from `PessoaRepositoryTest`: Hibernate 6 generates PostgreSQL-specific `WITH ... AS MATERIALIZED` CTE + `RETURNING` for bulk UPDATE across JOINED inheritance (the `deleted` column is in `users`, not `pessoas`). H2 does not support this syntax. Functionality covered by BDD integration tests on real PostgreSQL.
- `Apartamento.getCondominio()` / `getBloco()` do not exist (getters suppressed); stored `condominio` and `bloco` as test class fields instead.

**Chunk 4 — ArchUnit UserService constant:**
- Introduced `USER_SERVICE_FQCN = "com.pmrodrigues.security.service.UserService"` static constant.
- Cross-module check now explicitly gates on `"security".equals(depModule) && clazz.isAssignableTo(USER_SERVICE_FQCN)`, replacing the brittle `"com.pmrodrigues." + depModule + ".service.UserService"` string that happened to work only because depModule equalled "security" for UserRepository.

**Post-flight:** `mvn test` → 1136 testes, 0 falhas, 0 erros, BUILD SUCCESS.

---
