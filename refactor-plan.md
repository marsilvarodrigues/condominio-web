# Refactoring Plan — Ciclo 4

**Source:** Avaliação arquitetural inline — 2026-05-29 (módulo bancário: Banco, ContaBancaria, LancamentoBancario)
**Created:** 2026-05-29
**Target:** Corrigir 3 inconsistências detectadas no módulo bancário recém-adicionado: FundoReservaService bypassing ContaBancariaService, BancoService.findById retornando Optional em vez de lançar 404, e ausência de testes de cache para BancoService e ContaBancariaService.

---

## Pre-flight Checks
- [ ] All tests pass before starting (`mvn test -Dexcludes=**/integration/**,**/bdd/**`)
- [ ] No uncommitted changes
- [ ] Working branch created

---

## Chunk 1: FundoReservaService — trocar ContaBancariaRepository por ContaBancariaService

**Why:**
`FundoReservaService` injeta `ContaBancariaRepository` para validar e resolver o `ContaBancaria` associado ao Fundo de Reserva. Isso:
1. Bypassa o cache `contas-bancarias` (que `ContaBancariaService.findById()` usa)
2. É inconsistente com o padrão estabelecido em `ContaBancariaService.create()`, onde `BancoService.findById()` é chamado para validar a existência do banco antes de criar a associação JPA via proxy

**Entry criteria:** Todos os testes unitários passam, sem chunks pendentes.

**Steps:**

*FundoReservaService.java (`src/main/java/com/pmrodrigues/financeiro/service/FundoReservaService.java`):*

1. Substituir o campo:
   ```java
   private final ContaBancariaRepository contaBancariaRepository;
   ```
   por:
   ```java
   private final ContaBancariaService contaBancariaService;
   ```
2. Remover o import `com.pmrodrigues.financeiro.repository.ContaBancariaRepository`
3. Adicionar o import `com.pmrodrigues.financeiro.service.ContaBancariaService`
4. Substituir o método `resolveContaBancaria(Long contaBancariaId)`:
   ```java
   private ContaBancaria resolveContaBancaria(Long contaBancariaId) {
       var contaDTO = contaBancariaService.findById(contaBancariaId); // throws 404 if not found
       if (contaDTO.tipo() != TipoContaBancaria.FUNDO_RESERVA) {
           log.error("ContaBancaria {} is not of type FUNDO_RESERVA", contaBancariaId);
           throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                   "A conta bancária vinculada ao Fundo de Reserva deve ser do tipo FUNDO_RESERVA");
       }
       var contaRef = new ContaBancaria();
       contaRef.setId(contaBancariaId);
       return contaRef;
   }
   ```
   (The method body is replaced in full; `ContaBancaria` is now only needed as a proxy, not loaded.)

*FundoReservaServiceTest.java (`src/test/java/com/pmrodrigues/financeiro/service/FundoReservaServiceTest.java`):*

5. Substituir:
   ```java
   @Mock ContaBancariaRepository contaBancariaRepository;
   ```
   por:
   ```java
   @Mock ContaBancariaService contaBancariaService;
   ```
6. Remover import `com.pmrodrigues.financeiro.repository.ContaBancariaRepository`
7. Adicionar import `com.pmrodrigues.financeiro.service.ContaBancariaService`
8. Atualizar o construtor no `@BeforeEach`:
   - Trocar `contaBancariaRepository` por `contaBancariaService`
9. Nos testes `create_withContaBancaria_*` e `update_withContaBancaria_*`, trocar stubs de `contaBancariaRepository.findById(...)` por stubs de `contaBancariaService.findById(...)`:
   - O mock retornará um `ContaBancariaDTO` com `tipo = TipoContaBancaria.FUNDO_RESERVA`
   - Para o caso de `type_mismatch`, o mock retornará um DTO com tipo `CORRENTE`
   - Para `not_found`, usar `doThrow(new ResponseStatusException(NOT_FOUND))` em `contaBancariaService.findById(...)`

**Exit criteria:** `mvn test -Dtest="FundoReservaServiceTest"` passa, 0 falhas.

**Commit message:** `fix(financeiro): FundoReservaService uses ContaBancariaService (not repository) to resolve conta bancaria`

---

## Chunk 2: BancoService.findById — lançar 404 em vez de retornar Optional

**Why:**
`BancoService.findById()` retorna `Optional<BancoDTO>`, enquanto `ContaBancariaService.findById()` lança 404 diretamente (conforme CLAUDE.md regra 5: "use `Exceptions.notFound()`"). Esta inconsistência força:
- `BancoController.findById()` a usar `new ResponseStatusException(NOT_FOUND, ...)` em vez de `Exceptions.notFound()`
- `ContaBancariaService.create()` a chamar `bancoService.findById(id).orElseThrow(() -> notFound("Banco", id))` — verboso e duplica a mensagem de erro

**Entry criteria:** Todos os testes unitários passam (independente do Chunk 1).

**Steps:**

*BancoService.java (`src/main/java/com/pmrodrigues/commons/service/BancoService.java`):*

1. Alterar a assinatura de `findById`:
   - Retorno: `Optional<BancoDTO>` → `BancoDTO`
   - Body:
     ```java
     public BancoDTO findById(Long id) {
         log.info("Looking up banco by id: {}", id);
         var result = repository.findById(id)
                 .map(mapper::toDTO)
                 .orElseThrow(() -> notFound("Banco", id));
         log.info("Banco lookup by id {}: found", id);
         return result;
     }
     ```
   - Remover o `unless = "#result == null"` do `@Cacheable` (já não faz sentido quando a ausência lança exception — o cache nunca guarda `null`)
   - Remover import `java.util.Optional` (se ficar órfão)

*BancoController.java (`src/main/java/com/pmrodrigues/commons/controller/BancoController.java`):*

2. Em `findById()`, substituir:
   ```java
   var banco = bancoService.findById(id)
           .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Banco not found: " + id));
   ```
   por:
   ```java
   var banco = bancoService.findById(id);
   ```
3. Remover os imports `java.util.Optional` e `org.springframework.http.HttpStatus.NOT_FOUND` e `org.springframework.web.server.ResponseStatusException` se ficarem órfãos.

*ContaBancariaService.java (`src/main/java/com/pmrodrigues/financeiro/service/ContaBancariaService.java`):*

4. No método `create()`, substituir:
   ```java
   bancoService.findById(dto.bancoId())
           .orElseThrow(() -> notFound("Banco", dto.bancoId()));
   var bancoRef = new Banco();
   bancoRef.setId(dto.bancoId());
   ```
   por:
   ```java
   bancoService.findById(dto.bancoId()); // throws 404 if banco not found
   var bancoRef = new Banco();
   bancoRef.setId(dto.bancoId());
   ```

*BancoServiceTest.java (`src/test/java/com/pmrodrigues/commons/service/BancoServiceTest.java`):*

5. Alterar `findById_whenFound_returnsDTO`:
   - Retorno muda de `Optional.of(...)` para direto: `assertThat(result.codigo()).isEqualTo("341")`
   - Remove `assertThat(result).isPresent()`
6. Renomear `findById_whenNotFound_returnsEmpty` para `findById_whenNotFound_throwsNotFound` e alterar para:
   ```java
   assertThatThrownBy(() -> service.findById(99L))
           .isInstanceOf(ResponseStatusException.class)
           .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode().value()).isEqualTo(404));
   ```

**Exit criteria:** `mvn test -Dtest="BancoServiceTest,ContaBancariaServiceTest"` passa, 0 falhas.

**Commit message:** `fix(commons): BancoService.findById throws 404 instead of returning Optional`

---

## Chunk 3: Criar BancoServiceCacheTest e ContaBancariaServiceCacheTest

**Why:**
`BancoService` tem `@Cacheable` em `filterBy` e `findById`, mais `@CacheEvict` em `create`/`update`/`delete`. `ContaBancariaService` tem `@Cacheable` em `findById`. Nenhum dos dois tem um teste dedicado de comportamento de cache. Todos os outros services com `@Cacheable` na base têm `*CacheTest` (EstadoServiceCacheTest, BlocoServiceCacheTest, ApartamentoServiceCacheTest, CondominioServiceCacheTest).

**Depends on:** Chunk 2 (BancoService.findById mudou de Optional para BancoDTO — o CacheTest precisa refletir a nova API)

**Entry criteria:** Chunk 2 completo e verificado.

**Steps:**

*Criar `src/test/java/com/pmrodrigues/commons/service/BancoServiceCacheTest.java`:*

1. Seguir o padrão de `EstadoServiceCacheTest`:
   - `@ExtendWith(SpringExtension.class)` + `@ContextConfiguration(classes = TestConfig.class)`
   - `@Configuration @EnableCaching static class TestConfig` com:
     - `CacheManager` com `ConcurrentMapCacheManager("bancos")`
     - `BancoRepository` mock
     - `BancoMapper` mock
     - `BancoService` bean construído com ambos
   - Testes:
     - `filterBy_cachedOnSecondCall_repositoryCalledOnce` — chama `filterBy(new BancoFilterDTO(null, null))` duas vezes, verifica `verify(repository, times(1)).findAll(any(Specification.class))`
     - `findById_cachedOnSecondCall_repositoryCalledOnce` — chama `findById(1L)` duas vezes, verifica `verify(repository, times(1)).findById(1L)`
     - `create_evictsCache_repositoryCalledAgainAfterCreate` — faz `filterBy`, depois `create(...)`, depois `filterBy` novamente; verifica `repository.findAll` chamado 2 vezes
     - `update_evictsCache` — idem com `update(1L, dto)`
     - `delete_evictsCache` — idem com `delete(1L)`

*Criar `src/test/java/com/pmrodrigues/financeiro/service/ContaBancariaServiceCacheTest.java`:*

2. Seguir o padrão de `CondominioServiceCacheTest`:
   - `CacheManager` com `ConcurrentMapCacheManager("contas-bancarias")`
   - Mocks: `ContaBancariaRepository`, `BancoService`, `ContaBancariaMapper`
   - `ContaBancariaService` bean
   - Testes:
     - `findById_cachedOnSecondCall_repositoryCalledOnce` — stub `repository.findById(1L)` com uma entidade + `mapper.toDTO(...)` com um DTO; chama `service.findById(1L)` duas vezes; verifica `repository.findById(1L)` chamado 1 vez
     - `findById_throwsNotFound_notCached_repositoryCalledEachTime` — stub `repository.findById(99L)` retornando `Optional.empty()`; duas chamadas a `service.findById(99L)` lançam 404; verifica `repository.findById(99L)` chamado 2 vezes
     - `create_evictsCache_repositoryCalledAgainAfterCreate` — preenche cache com `findById`, depois `create(...)`, depois `findById` de novo; verifica repository chamado 2 vezes

**Exit criteria:** `mvn test -Dtest="BancoServiceCacheTest,ContaBancariaServiceCacheTest"` passa, 0 falhas.

**Commit message:** `test(cache): add BancoServiceCacheTest and ContaBancariaServiceCacheTest`

---

## Post-flight Checks
- [ ] Full unit test suite passes (`mvn test -Dexcludes=**/integration/**,**/bdd/**`)
- [ ] Nenhum TODO/FIXME deixado pelo refactoring
- [ ] Todos os findings do assessment endereçados
- [ ] ArchUnit continua passando (8/8 rules)

---

## Findings fora do escopo (deliberadas não-mudanças)

- **EstadoService.findById() retorna Optional** — mesmo "problema" que BancoService, mas mudar EstadoService exigiria atualizar EstadoController, EstadoServiceCacheTest e EstadoControllerTest. YAGNI: não há código que sofra com isso, e EstadoService é um catálogo de referência estática com uso diferente.
- **LancamentoBancarioService sem cache** — intencional. Lançamentos são dados transacionais que mudam frequentemente e têm alto volume; cachear em nível de service causaria mais invalidações do que hits.
- **FundoReservaService.resolveContaBancaria criando proxy com `new ContaBancaria()`** — consistente com o padrão do `ContaBancariaService.create()` que cria `new Banco()` como proxy. JPA só precisa do FK id para persistência.
