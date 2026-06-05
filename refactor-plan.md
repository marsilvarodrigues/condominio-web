# Refactoring Plan — Ciclo 5

**Source:** inline — avaliação arquitetural 2026-06-05 (módulo morador: Pessoa, Proprietario)
**Created:** 2026-06-05
**Target:** Módulo morador alinhado com padrões estabelecidos nos Ciclos 2–4: EntityManager nos mappers (não nos services), cascade soft-delete ao deletar Condomínio, e cobertura de testes completa (mapper tests + controller tests + método update do ProprietarioServiceTest).

## Pre-flight Checks
- [ ] All tests pass before starting (`mvn test`)
- [ ] No uncommitted changes
- [ ] Working branch confirmed

## Chunk 1: Mover EntityManager.getReference() de PessoaService/ProprietarioService para PessoaMapper/ProprietarioMapper

**Why:** `PessoaService` e `ProprietarioService` injetam `EntityManager` diretamente, violando o padrão estabelecido nos Ciclos 2 e 3 onde `ApartamentoMapper`, `CondominioMapper` e `OrcamentoAnualMapper` recebem o `EntityManager` via setter `@Autowired(required = false)` e expõem métodos `*FromId()`. Os services não devem conhecer `EntityManager`.

**Entry criteria:** All tests pass, no prior chunks pending.

**Steps:**

1. `PessoaMapper.java` — adicionar campo `EntityManager em`; adicionar setter `@Autowired(required = false) public void setEntityManager(EntityManager em)` (não campo — ArchUnit proíbe campos @Autowired); adicionar método concreto `apartamentoFromId(Long id)` que retorna `em.getReference(Apartamento.class, id)` quando id não é null, ou null quando id é null; adicionar imports `Apartamento`, `EntityManager`, `org.springframework.beans.factory.annotation.Autowired`.

2. `PessoaService.java` — remover campo `EntityManager entityManager` e import `EntityManager`; em `create()`: substituir `entity.setApartamento(entityManager.getReference(Apartamento.class, dto.apartamentoId()))` por `entity.setApartamento(mapper.apartamentoFromId(dto.apartamentoId()))`; em `assignToApartamento()`: substituir `entityManager.getReference(Apartamento.class, apartamentoId)` por `mapper.apartamentoFromId(apartamentoId)`; atualizar construtor (3 parâmetros: repository, mapper, proprietarioRepository).

3. `ProprietarioMapper.java` — adicionar campo `EntityManager em`; adicionar setter `@Autowired(required = false) public void setEntityManager(EntityManager em)`; adicionar método concreto `apartamentoFromId(Long id)`; adicionar imports `Apartamento`, `EntityManager`, `Autowired`.

4. `ProprietarioService.java` — remover campo `EntityManager entityManager` e import `EntityManager`; em `associarApartamento()`: substituir `entityManager.getReference(Apartamento.class, apartamentoId)` por `mapper.apartamentoFromId(apartamentoId)`; atualizar construtor (2 parâmetros: repository, mapper).

5. `PessoaServiceTest.java` — remover `@Mock EntityManager entityManager`; atualizar construtor em `@BeforeEach` para 3 parâmetros (repository, mapper, proprietarioRepository); no teste `assignToApartamento_deveAssociarApartamento`: substituir stub `entityManager.getReference(...)` por stub `mapper.apartamentoFromId(10L)`.

6. `ProprietarioServiceTest.java` — remover `@Mock EntityManager entityManager`; atualizar construtor em `@BeforeEach` para 2 parâmetros (repository, mapper); no teste `associarApartamento_deveAdicionarApartamento`: substituir stub `entityManager.getReference(...)` por stub `mapper.apartamentoFromId(5L)`.

**Exit criteria:** `mvn test` passa sem falhas.

**Commit message:** `refactor: mover EntityManager.getReference() dos services morador para os mappers`

---

## Chunk 2: CondominioService.delete() — cascade soft-delete para Pessoas

**Why:** `CondominioService.delete()` propaga soft-delete para Blocos, Apartamentos, PlanoContas, FundoReserva, OrcamentoAnual, ContaBancaria e LancamentoBancario — mas não para `Pessoa`. Ao deletar um Condomínio, moradores e proprietários ficam como órfãos visíveis. Padrão: adicionar `softDeleteByCondominioId` ao repositório e service de Pessoa, depois chamar via CondominioService.

**Depends on:** nenhum (independente do Chunk 1).

**Entry criteria:** All tests pass.

**Steps:**

1. `PessoaRepository.java` — adicionar método:
   ```java
   @Modifying
   @Query("UPDATE Pessoa p SET p.deleted = true WHERE p.condominio.id = :condominioId")
   void softDeleteByCondominioId(@Param("condominioId") Long condominioId);
   ```
   Adicionar imports `@Modifying`, `@Query`, `@Param`.

2. `PessoaService.java` — adicionar método `softDeleteByCondominioId(Long condominioId)` com `@Transactional`, `@Timed(value = "pessoa.service.softDeleteByCondominioId")`, `log.info` entrada e saída, chamando `repository.softDeleteByCondominioId(condominioId)`.

3. `CondominioService.java` — adicionar campo `private final PessoaService pessoaService`; em `delete()`, adicionar `pessoaService.softDeleteByCondominioId(id)` após a linha de `lancamentoBancarioService.softDeleteByCondominioId(id)`; adicionar import `PessoaService` do pacote `com.pmrodrigues.morador.service`.

4. `PessoaServiceTest.java` — adicionar teste `softDeleteByCondominioId_callsRepository`: verifica `verify(repository).softDeleteByCondominioId(42L)`.

5. `CondominioServiceTest.java` — adicionar `@Mock PessoaService pessoaService`; atualizar construtor do service com o novo campo; adicionar teste `delete_cascadesSoftDeleteToPessoas`: verificar que `pessoaService.softDeleteByCondominioId(id)` é chamado.

6. `CondominioServiceCacheTest.java` — adicionar `pessoaService` como mock bean no `TestConfig`; atualizar construtor do `CondominioService` com o novo parâmetro.

**Exit criteria:** `mvn test` passa sem falhas.

**Commit message:** `feat: cascade soft-delete de Condomínio para Pessoas (moradores e proprietários)`

---

## Chunk 3: PessoaMapperTest + ProprietarioMapperTest

**Why:** Todos os mappers com EntityManager e lógica manual têm teste dedicado (ApartamentoMapperTest, CondominioMapperTest, OrcamentoAnualMapperTest). PessoaMapper e ProprietarioMapper têm `toEntity` manual, `updateEntity` manual, e após o Chunk 1, também `apartamentoFromId` — sem teste algum.

**Depends on:** Chunk 1 (PessoaMapper e ProprietarioMapper precisam ter `apartamentoFromId` para que o teste seja completo).

**Entry criteria:** Chunk 1 completo e verificado.

**Steps:**

1. Criar `src/test/java/com/pmrodrigues/morador/mapper/PessoaMapperTest.java`:
   - `@ExtendWith(SpringExtension.class)` + `@ContextConfiguration(classes = PessoaMapperImpl.class)`
   - `@MockitoBean EntityManager em`
   - Em `@BeforeEach`: stub `when(em.getReference(Apartamento.class, 10L)).thenReturn(aptRef)`
   - Testes:
     - `toDTO_mapsAllFields_forPessoaFisica` — verifica id, nome, tipo="PF", cpf, apartamentoId
     - `toDTO_mapsAllFields_forPessoaJuridica` — verifica tipo="PJ", cnpj, razaoSocial
     - `toEntity_createsPessoaFisica` — dto com tipo "PF"; assert resultado é `PessoaFisica` com cpf preenchido
     - `toEntity_createsPessoaJuridica` — dto com tipo "PJ"; assert resultado é `PessoaJuridica` com cnpj e razaoSocial
     - `updateEntity_updatesNonNullFields_ignoresNullFields` — aplica nome novo; campo email null não sobrescreve email original
     - `apartamentoFromId_returnsProxy` — `assertThat(mapper.apartamentoFromId(10L)).isSameAs(aptRef)`
     - `apartamentoFromId_whenNull_returnsNull` — `assertThat(mapper.apartamentoFromId(null)).isNull()`

2. Criar `src/test/java/com/pmrodrigues/morador/mapper/ProprietarioMapperTest.java`:
   - Mesma estrutura com `ProprietarioMapperImpl.class`
   - Testes:
     - `toDTO_mapsAllFields_forProprietarioPF` — verifica tipo="PROP_PF", cpf, lista de apartamentos
     - `toDTO_mapsAllFields_forProprietarioPJ` — verifica tipo="PROP_PJ", cnpj, razaoSocial
     - `toEntity_createsProprietarioPF` — dto com tipo "PROP_PF"; assert resultado é `ProprietarioPessoaFisica`
     - `toEntity_createsProprietarioPJ` — dto com tipo "PROP_PJ"; assert resultado é `ProprietarioPessoaJuridica`
     - `updateEntity_updatesNonNullFields` — verifica que campos null no DTO não sobrescrevem campos do entity
     - `apartamentoFromId_returnsProxy`
     - `apartamentoFromId_whenNull_returnsNull`

**Exit criteria:** `mvn test` passa sem falhas.

**Commit message:** `test: adicionar PessoaMapperTest e ProprietarioMapperTest`

---

## Chunk 4: PessoaControllerTest + ProprietarioControllerTest + ProprietarioServiceTest.update

**Why:** Todos os controllers do projeto têm testes de controller (BlocoControllerTest, ApartamentoControllerTest, CondominioControllerTest, BancoControllerTest…). PessoaController e ProprietarioController não têm nenhum. Além disso, o `ProprietarioService.update()` — método adicionado nesta sessão — não tem cobertura em `ProprietarioServiceTest`.

**Depends on:** nenhum (independente dos Chunks 1–3).

**Entry criteria:** All tests pass.

**Steps:**

1. `ProprietarioServiceTest.java` — adicionar 2 testes:
   - `update_whenExists_updatesFields`: stub `repository.findById(1L)` → proprietário; stub `repository.save(any())` → mesmo entity; `doNothing().when(mapper).updateEntity(any(), any())`; verifica `mapper.updateEntity(eq(entity), any(UpdateProprietarioDTO.class))` chamado.
   - `update_whenNotFound_throws404`: `repository.findById(99L)` → empty; assert `ResponseStatusException` 404.

2. Criar `src/test/java/com/pmrodrigues/morador/controller/PessoaControllerTest.java`:
   - `@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test")`
   - `@MockitoBean PessoaService pessoaService`, `@MockitoBean JwtDecoder jwtDecoder`, `@MockitoBean TokenBlacklistService tokenBlacklistService`, `@MockitoBean UserDetailsServiceImpl userDetailsService`
   - Testes (cenários obrigatórios — CLAUDE.md regra 13):
     - `filterBy_returns200` — `@WithMockUser`, stub `Page.empty()`, GET `/api/v1/pessoas`, verifica 200
     - `findById_returns200` — GET `/api/v1/pessoas/1`, stub → pessoaDTO, verifica 200 e campo nome
     - `findById_notFound_returns404` — stub lança ResponseStatusException 404, verifica 404
     - `create_asAdmin_returns201` — `@WithMockUser(roles="ADMIN")`, POST, verifica 201
     - `create_unauthenticated_returns401` — sem @WithMockUser, verifica 401
     - `create_asUser_returns403` — `@WithMockUser` (sem ADMIN), verifica 403
     - `update_asAdmin_returns200` — `@WithMockUser(roles="ADMIN")`, PUT `/api/v1/pessoas/1`, verifica 200
     - `update_notFound_returns404` — stub lança 404, verifica 404
     - `delete_asAdmin_returns204` — `@WithMockUser(roles="ADMIN")`, DELETE, verifica 204
     - `assignToApartamento_asAdmin_returns200` — POST `/api/v1/pessoas/1/apartamentos?apartamentoId=5`, verifica 200
     - `removeFromApartamento_asAdmin_returns200` — DELETE `/api/v1/pessoas/1/apartamentos`, verifica 200

3. Criar `src/test/java/com/pmrodrigues/morador/controller/ProprietarioControllerTest.java`:
   - Mesma estrutura com `@MockitoBean ProprietarioService proprietarioService`
   - Testes:
     - `filterBy_returns200`
     - `findById_returns200`
     - `findById_notFound_returns404`
     - `create_asAdmin_returns201`
     - `create_unauthenticated_returns401`
     - `create_asUser_returns403`
     - `update_asAdmin_returns200` — PUT `/api/v1/proprietarios/1`
     - `update_notFound_returns404`
     - `delete_asAdmin_returns204`
     - `associarApartamento_asAdmin_returns200` — POST `/api/v1/proprietarios/1/apartamentos?apartamentoId=5`
     - `desassociarApartamento_asAdmin_returns204` — DELETE `/api/v1/proprietarios/1/apartamentos/5`
     - `findByApartamento_returns200` — GET `/api/v1/apartamentos/1/proprietarios`

**Exit criteria:** `mvn test` passa sem falhas.

**Commit message:** `test: adicionar PessoaControllerTest, ProprietarioControllerTest e ProprietarioServiceTest.update`

---

## Post-flight Checks
- [ ] Full test suite passes
- [ ] No TODO/FIXME markers left from refactoring
- [ ] Assessment findings are all resolved
- [ ] `EntityManager` removido de PessoaService e ProprietarioService
- [ ] `CondominioService.delete()` propaga para Pessoa
- [ ] PessoaMapperTest e ProprietarioMapperTest existem
- [ ] PessoaControllerTest e ProprietarioControllerTest existem
- [ ] `ProprietarioServiceTest` cobre o método `update()`
