# Refactoring Plan — Ciclo 10

**Source:** inline — auditoria N+1 de 2026-06-17 (10 riscos confirmados em 8 entidades)
**Created:** 2026-06-17
**Target:** Eliminar todos os N+1 queries confirmados: @BatchSize nas associações lazy mapeadas em DTOs; JOIN FETCH em queries concretas de repositório; batch morador loading no CobrancaService.

## Pre-flight Checks
- [ ] All tests pass before starting
- [ ] Working branch active (master_data)

## Estratégia de correção

- **`@BatchSize(size = 20)`** em toda associação `@ManyToOne` LAZY e coleção `@OneToMany`/`@ManyToMany` LAZY que o mapper acessa na conversão para DTO. Reduz N queries para ceil(N/20). Compatível com Specification-based `findAll` — nenhuma mudança em repositórios ou services.
- **`JOIN FETCH` em queries concretas** de repositório onde a associação é sempre necessária e a query não é paginada com coleção (sem risco de Cartesian product). Elimina as queries extras completamente.
- **Batch loading no service** para o N+1 de nível de aplicação em `CobrancaService.filterBy → enriquecerMorador → pessoaService.findById × N`.

---

## Chunk 1: `@BatchSize(size = 20)` em todas as associações lazy mapeadas em DTO
**Why:** 8 entidades têm campos `@ManyToOne` ou coleções `@OneToMany`/`@ManyToMany` com `FetchType.LAZY` cujos valores são sistematicamente acessados pelo mapper durante a serialização de listas/páginas. Sem `@BatchSize`, cada entidade na página dispara 1 query extra por associação. Com `@BatchSize(size = 20)`, Hibernate agrupa a inicialização de proxies em batches de 20, reduzindo N queries para ceil(N/20).

**Correção técnica (descoberta durante a execução):** `@BatchSize` em um campo `@ManyToOne`/`@OneToOne` (to-one) não tem efeito no Hibernate — a anotação só é respeitada em (a) coleções (`@OneToMany`/`@ManyToMany`), no próprio campo, ou (b) entidades, na classe alvo, controlando o batch de proxies daquele tipo onde quer que sejam referenciados. A versão original deste chunk colocava `@BatchSize` nos 6 campos `@ManyToOne` — o que compila mas não tem efeito algum em runtime. Os 2 campos de coleção (`Apartamento.moradores`, `OrcamentoAnual.itens`) e o `@ManyToMany` (`Proprietario.apartamentos`) estavam corretos desde o início. Os passos abaixo refletem a colocação correta.

**Entry criteria:** Testes passando (baseline)
**Steps:**
1. Editar `src/main/java/com/pmrodrigues/condominio/model/Apartamento.java`:
   - Adicionar `import org.hibernate.annotations.BatchSize;`
   - Adicionar `@BatchSize(size = 20)` **na classe** `Apartamento` (não no campo `bloco`) — cobre todo proxy de `Apartamento` referenciado lazily (por `Cobranca.apartamento` e `Pessoa.apartamento`)
   - Manter `@BatchSize(size = 20)` no campo `moradores` (OneToMany LAZY) — já correto
2. Editar `src/main/java/com/pmrodrigues/condominio/model/Bloco.java`:
   - Adicionar `import org.hibernate.annotations.BatchSize;`
   - Adicionar `@BatchSize(size = 20)` **na classe** `Bloco` — cobre o proxy referenciado por `Apartamento.bloco`
3. Editar `src/main/java/com/pmrodrigues/financeiro/model/Despesa.java`:
   - Adicionar `import org.hibernate.annotations.BatchSize;`
   - Adicionar `@BatchSize(size = 20)` **na classe** `Despesa` — cobre o proxy referenciado por `RateioExecucao.despesa`
4. Editar `src/main/java/com/pmrodrigues/financeiro/model/GrupoDespesa.java`:
   - Adicionar `import org.hibernate.annotations.BatchSize;`
   - Adicionar `@BatchSize(size = 20)` **na classe** `GrupoDespesa` — cobre o proxy referenciado por `Despesa.grupoDespesa`
5. Editar `src/main/java/com/pmrodrigues/financeiro/model/PlanoContas.java`:
   - Adicionar `import org.hibernate.annotations.BatchSize;`
   - Adicionar `@BatchSize(size = 20)` **na classe** `PlanoContas` — cobre o proxy referenciado por `ItemOrcamento.planoContas`
6. Editar `src/main/java/com/pmrodrigues/financeiro/model/OrcamentoAnual.java`:
   - Adicionar `import org.hibernate.annotations.BatchSize;`
   - Adicionar `@BatchSize(size = 20)` no campo `itens` (OneToMany LAZY) — nota: já tem `@NotAudited`; já correto, sem mudança nesta correção
7. Editar `src/main/java/com/pmrodrigues/morador/model/Proprietario.java`:
   - Adicionar `import org.hibernate.annotations.BatchSize;`
   - Adicionar `@BatchSize(size = 20)` no campo `apartamentos` (@ManyToMany — sem `fetch` explícito, default é LAZY); já correto, sem mudança nesta correção
8. Remover `@BatchSize(size = 20)` (e o import correspondente) dos campos `@ManyToOne` onde não tem efeito: `Cobranca.apartamento`, `ItemOrcamento.planoContas`, `RateioExecucao.despesa`, `Pessoa.apartamento`, `Apartamento.bloco`, `Despesa.grupoDespesa` — a cobertura passa a vir da anotação na classe alvo (passos 1–5).
**Exit criteria:** `mvn compile -q` → BUILD SUCCESS; `mvn test -q` → BUILD SUCCESS (sem regressões)
**Commit message:** `perf: move @BatchSize(20) from to-one fields to target entity classes`

---

## Chunk 2: `JOIN FETCH` em queries concretas de repositório
**Why:** Dois repositórios têm queries derivadas concretas (não-Specification) onde a associação lazy é **sempre** necessária e **sempre** irá disparar N queries extras. JOIN FETCH é mais eficiente que BatchSize nesses casos porque elimina as queries extras completamente em vez de apenas agrupá-las.

- `HistoricoOcupacaoRepository.findByApartamento_IdAndCondominio_Id...`: Pageable + `@ManyToOne` JOIN FETCH é seguro (sem Cartesian product, um historico → uma pessoa).
- `CotaRateioRepository.findByRateioExecucaoId`: Retorna `List<>` (não paginado); dois `@ManyToOne` JOIN FETCH em cadeia são seguros.

**Entry criteria:** Chunk 1 completo
**Steps:**
1. Editar `src/main/java/com/pmrodrigues/morador/repository/HistoricoOcupacaoRepository.java`:
   - Adicionar imports: `import org.springframework.data.jpa.repository.Query;` e `import org.springframework.data.repository.query.Param;`
   - Substituir o método derivado `findByApartamento_IdAndCondominio_IdOrderByDataSaidaDesc` por:
     ```java
     @Query(
         value = "SELECT h FROM HistoricoOcupacao h JOIN FETCH h.pessoa " +
                 "WHERE h.apartamento.id = :apartamentoId AND h.condominio.id = :condominioId " +
                 "ORDER BY h.dataSaida DESC",
         countQuery = "SELECT COUNT(h) FROM HistoricoOcupacao h " +
                      "WHERE h.apartamento.id = :apartamentoId AND h.condominio.id = :condominioId"
     )
     Page<HistoricoOcupacao> findByApartamento_IdAndCondominio_IdOrderByDataSaidaDesc(
         @Param("apartamentoId") Long apartamentoId,
         @Param("condominioId") Long condominioId,
         Pageable pageable);
     ```
   - Nota: manter o mesmo nome de método (sem mudança nos callers); a anotação `@Query` overrides a derivação.
2. Editar `src/main/java/com/pmrodrigues/financeiro/repository/CotaRateioRepository.java`:
   - Adicionar imports: `import org.springframework.data.jpa.repository.Query;` e `import org.springframework.data.repository.query.Param;`
   - Substituir `findByRateioExecucaoId` por:
     ```java
     @Query("SELECT c FROM CotaRateio c JOIN FETCH c.apartamento a JOIN FETCH a.bloco " +
            "WHERE c.rateioExecucao.id = :rateioExecucaoId")
     List<CotaRateio> findByRateioExecucaoId(@Param("rateioExecucaoId") Long rateioExecucaoId);
     ```
**Exit criteria:** `mvn test -q` → BUILD SUCCESS (HistoricoOcupacaoRepositoryTest e todos os demais passam)
**Commit message:** `perf: add JOIN FETCH to HistoricoOcupacaoRepository and CotaRateioRepository`

---

## Chunk 3: Batch morador loading em `CobrancaService.filterBy`
**Why:** `CobrancaService.filterBy()` chama `this.enriquecerMorador(dto)` para **cada** DTO da página. `enriquecerMorador` chama `pessoaService.findById(dto.moradorId())`, que executa uma query JPA por chamada. Com `page_size=20`, isso são 20 queries extras de Pessoa por requisição de listagem — um N+1 de nível de aplicação, não resolvido pelo `@BatchSize` do Chunk 1.

**Fix:** Adicionar `PessoaService.findByIds(Set<Long> ids): Map<Long, PessoaDTO>` que carrega todos os moradores em 1 query, e refatorar `filterBy` para usar esse mapa.

**Entry criteria:** Baseline de testes passando (independente dos outros chunks)
**Steps:**
1. Editar `src/main/java/com/pmrodrigues/morador/service/PessoaService.java`:
   - Após o método `listarPorApartamento`, adicionar:
     ```java
     /**
      * Batch-loads all Pessoas matching the given ids. Used by CobrancaService to avoid N+1 when
      * enriching a page of charges with resident names.
      *
      * @param ids set of pessoa primary keys (may be empty)
      * @return map of id → PessoaDTO for each found entity; absent ids are excluded
      */
     @Transactional(readOnly = true)
     @Timed(value = "pessoa.service.findByIds", description = "Batch find pessoas by ids")
     public Map<Long, PessoaDTO> findByIds(Set<Long> ids) {
         if (ids.isEmpty()) return Map.of();
         log.info("Batch loading {} pessoa ids", ids.size());
         return pessoaRepository.findAllById(ids).stream()
             .collect(java.util.stream.Collectors.toMap(
                 com.pmrodrigues.morador.model.Pessoa::getId,
                 pessoaMapper::toDTO));
     }
     ```
   - Adicionar imports: `java.util.Map`, `java.util.Set`
2. Editar `src/main/java/com/pmrodrigues/cobranca/service/CobrancaService.java`:
   - Refatorar o método `filterBy()` para batch-load moradores:
     ```java
     @Transactional(readOnly = true)
     @Timed(value = "cobranca.service.filterBy", description = "List charges with filters")
     public Page<CobrancaDTO> filterBy(CobrancaFilterDTO filter, Pageable pageable) {
         log.info("filterBy filter={}", filter);
         var page = cobrancaRepository.findAll(
                 Specification.allOf(
                     CobrancaSpecification.hasApartamento(filter.apartamentoId()),
                     CobrancaSpecification.hasStatus(filter.status()),
                     CobrancaSpecification.vencimentoFrom(filter.vencimentoDe()),
                     CobrancaSpecification.vencimentoTo(filter.vencimentoAte()),
                     CobrancaSpecification.emailEnviado(filter.emailEnviado())),
                 pageable)
             .map(mapper::toDTO);
         var moradorIds = page.getContent().stream()
             .map(CobrancaDTO::moradorId)
             .filter(java.util.Objects::nonNull)
             .collect(java.util.stream.Collectors.toSet());
         var moradores = pessoaService.findByIds(moradorIds);
         return page.map(dto -> enriquecerMoradorFromMap(dto, moradores));
     }
     ```
   - Adicionar método privado `enriquecerMoradorFromMap(CobrancaDTO dto, Map<Long, PessoaDTO> moradores)`:
     ```java
     private CobrancaDTO enriquecerMoradorFromMap(CobrancaDTO dto, Map<Long, PessoaDTO> moradores) {
         if (dto.moradorId() == null) return dto;
         var pessoa = moradores.get(dto.moradorId());
         if (pessoa == null) return dto;
         return new CobrancaDTO(
             dto.id(), dto.apartamentoId(), dto.apartamentoNumero(), dto.blocoNome(),
             dto.moradorId(), pessoa.nome(), pessoa.email(),
             dto.valor(), dto.vencimento(), dto.status(),
             dto.boletoUrl(), dto.boletoCodBarras(), dto.pixQrCodeBase64(), dto.pixCopiaCola(),
             dto.emailEnviado(), dto.emailEnviadoEm(), dto.pagoEm(), dto.criadaEm());
     }
     ```
   - Nota: `enriquecerMorador(dto)` (versão antiga, com try-catch por moradorId individual) permanece para uso em `findById`, `cancelar` e `reenviarEmail` — apenas `filterBy` muda para o batch approach.
   - Adicionar imports: `java.util.Map`, `java.util.Objects`, `java.util.stream.Collectors`
3. Editar `src/test/java/com/pmrodrigues/morador/service/PessoaServiceTest.java`:
   - Adicionar seção `// ── findByIds ──────────────────────────────────────────────────────────────`
   - Adicionar teste `findByIds_deveRetornarMapaDTOs_quandoExistemPessoas`:
     - stub `pessoaRepository.findAllById(Set.of(1L, 2L))` → `List.of(morador(1L, "Ana"), morador(2L, "Bia"))`
     - resultado: `Map<Long, PessoaDTO>` com 2 entradas
     - assertar `result.get(1L).nome() == "Ana"` e `result.get(2L).nome() == "Bia"`
   - Adicionar teste `findByIds_deveRetornarMapaVazio_quandoIdsVazios`:
     - chamar `service.findByIds(Set.of())`
     - assertar `result.isEmpty()`
     - verificar que `pessoaRepository.findAllById` NÃO é chamado (never)
4. Editar `src/test/java/com/pmrodrigues/cobranca/service/CobrancaServiceTest.java`:
   - Adicionar `@Mock PessoaService pessoaService` se não existir (verificar se já existe via injeção direta)
   - No `@BeforeEach`, adicionar stub lenient para `pessoaService.findByIds(any())` → `Map.of()`
   - Atualizar o teste existente de `filterBy` para:
     - Stub `pessoaService.findByIds(any())` → mapa com morador mockado
     - Verificar que `pessoaService.findByIds` é chamado UMA vez (não N vezes)
**Exit criteria:** `mvn test -q` → BUILD SUCCESS; PessoaServiceTest e CobrancaServiceTest passam com novos testes
**Commit message:** `perf(cobranca): batch-load moradores in filterBy to eliminate N+1 service calls`

---

## Post-flight Checks
- [ ] Full test suite passes (`mvn test -q`)
- [ ] No TODO/FIXME markers left
- [ ] `@BatchSize(size = 20)` adicionado em: `Cobranca.apartamento`, `Apartamento.bloco`, `Apartamento.moradores`, `Despesa.grupoDespesa`, `ItemOrcamento.planoContas`, `OrcamentoAnual.itens`, `Pessoa.apartamento`, `Proprietario.apartamentos`, `RateioExecucao.despesa`
- [ ] `HistoricoOcupacaoRepository` usa `JOIN FETCH h.pessoa` com countQuery separado
- [ ] `CotaRateioRepository` usa `JOIN FETCH c.apartamento a JOIN FETCH a.bloco`
- [ ] `CobrancaService.filterBy()` usa batch loading — `pessoaService.findByIds()` chamado 1x por requisição
