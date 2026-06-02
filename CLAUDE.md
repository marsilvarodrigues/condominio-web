# CLAUDE.md

## Commands
`mvn test` | `mvn verify` (80% coverage) | `mvn spring-boot:run` — Integration tests require Docker (Testcontainers). E2E: `mvn verify -Pe2e`.

## New Use Case Checklist
1. **Package**: `com.pmrodrigues.<domain>.{controller,service,repository,mapper,dto,model,specification}`
2. **DTO**: Java `record` in `*.dto` package, Bean Validation annotations; `Create*DTO` for FK fields; Filter DTOs without validation.
3. **Mapper**: MapStruct interface — `toDTO`, `toEntity(Create*DTO)`, `updateEntity(@MappingTarget, DTO)` — `NullValuePropertyMappingStrategy.IGNORE`, never set `condominio`. Use `@Named` default methods for complex mappings (e.g., `Set<Entity>` → `Set<Long>`).
4. **Entity**: `@SQLDelete` + `@SQLRestriction` for soft delete; `@PrePersist` sets `condominio` from `TenantContext`; `condominio` field has `@Getter/@Setter(AccessLevel.NONE)`.
5. **Service**: Constructor inject mapper+repo; `@Timed` on every public method; `log.info` enter/return + `log.error` on exceptions; use `Exceptions.notFound()`; never inject raw `MeterRegistry`.
6. **Controller**: `@Timed`; return `ApiResponse.of(requestId(request), data)`; `@PreAuthorize("hasRole('ADMIN')")` on POST/PUT/DELETE; GET accepts optional `@RequestParam` filters.
7. **Multi-tenancy**: Table HASH-partitioned by `condominio_id` (4 buckets); PK must include `condominio_id`; add `@Filter` + `@FilterDef`; `TenantFilterAspect` activates filter automatically.
8. **Multi-tenancy (user)**: Users have a `@ManyToMany` relationship with `Condominio` (via `user_condominios`). 0 condominios = global access (no filter). 1 condominio = auto-selected. N condominios = `X-Condominio-Id` request header required. JWT carries `condominio_ids` (List<Long>) claim.
9. **Migration**: Liquibase SQL in `src/main/resources/db/changelog/changes/NNNN-description.sql`; register in `db.changelog-master.yaml`. Next number: 0024.
10. **Unit Tests (Service)**: `@ExtendWith(MockitoExtension.class)`; mock repo+mapper with `@Mock`; use `lenient()` stubs in `@BeforeEach`; assert DTO fields, verify repo/mapper calls; test happy path + `notFound` exception per method.
11. **Unit Tests (Mapper)**: `@ExtendWith(SpringExtension.class)` + `@ContextConfiguration(classes = {XxxMapperImpl.class, ...})`; assert every field mapping; verify `updateEntity` ignores `id`, `condominio`, `deleted`, timestamps.
12. **Unit Tests (Repository)**: `@DataJpaTest` + H2 + `@Import(JpaAuditingConfig.class)`; persist fixture chain via `EntityManager`; verify soft-delete with native SQL (`deleted = true`); verify Specification filters return correct subset.
13. **Functional Tests — BDD**: Cucumber + `@SpringBootTest(webEnvironment=RANDOM_PORT)` + Testcontainers PG16+Redis7. Feature files in `src/test/resources/features/<domain>/`; steps in `src/test/java/.../bdd/steps/`; shared Spring-scoped `World` holds JWT + last response. Cenários obrigatórios por endpoint: `admin cria X com sucesso` (201), `dados inválidos retornam 400`, `não autenticado recebe 401`, `sem permissão recebe 403`, `inexistente retorna 404`. Parse `ApiResponse<T>` nos steps; cleanup via `JdbcTemplate` no `@After`. BDD setup hooks must insert into `user_condominios` instead of `users.condominio_id`.
14. **E2E Tests — BDD**: Feature files em `src/test/resources/features/e2e/`; cobrem fluxos completos de negócio (ex: autenticar → criar condomínio → criar bloco → listar apartamentos). Sem mocks — stack real via Testcontainers. Taggear com `@e2e`; excluir do `mvn test` via Surefire `<excludedGroups>e2e</excludedGroups>`; executar com `mvn verify -Pe2e`.
15. **ArchUnit** enforces: no Controller→Repository/Model; DTOs must be records; layered `Controller→Service→Repository`.
16. **ArchUnit** enforces: no `@Autowired` fields; only constructor injection.
17. **ArchUnit** enforces: no @Service calls @Repository from another module, all calls between modules need to be via @Service.
18. **Deprecation**: Avoid any public method deprecation; if possible fix all to avoid it.
19. **SOLID**: Single Responsibility, Open/Closed, Liskov Substitution, Interface Segregation, Dependency Inversion principles must be followed.
20. **DRY**: Don't Repeat Yourself — avoid code duplication by abstracting common logic into reusable methods or components.
21. **KISS**: Keep It Simple, Stupid — prefer simple, straightforward solutions over complex ones; avoid unnecessary abstractions or over-engineering.
22. **YAGNI**: You Aren't Gonna Need It — don't implement features or functionality until they are actually needed; avoid speculative development.
23. **Code Reviews**: All code changes must go through a peer review process to ensure code quality, maintainability, and adherence to best practices.
24. **Documentation**: All public methods and classes must have Javadoc comments explaining their purpose, parameters, return values, and any exceptions thrown. Additionally, maintain an up-to-date README.md with setup instructions, architectural decisions, and any relevant information for developers.
25. **Database**: All tables with foreign keys must have a `condominio_id` column and must be partitioned by `condominio_id`. Exception: `user_condominios` join table (no tenant scope, partitioned by user access rights instead).
