# Condominio Web

API REST para gestão de condomínios, construída com Spring Boot 3.5 / Java 21. O sistema é multi-tenant, particionado por `condominio_id` ao nível de banco de dados, com cache em dois níveis, autenticação JWT + OAuth2, soft delete em todas as entidades de domínio e observabilidade completa via Prometheus + Grafana + Loki.

---

## O que está implementado

### Módulo Commons
Infraestrutura transversal compartilhada por todos os módulos de negócio.

- **Cache em dois níveis** — `TwoLevelCacheManager` encadeia L1 (Caffeine in-process) e L2 (Redis compartilhado). Chaves incluem `condominioId` do `TenantContext` para isolamento multi-tenant.
- **Multi-tenancy** — `TenantContext` (ThreadLocal) transporta o `condominioId` da requisição. `TenantFilterAspect` ativa o filtro Hibernate (`@Filter`) automaticamente em todas as queries.
- **Validação de CNPJ** — anotação `@CNPJ` + `CnpjValidator` integrada ao Bean Validation.
- **Resposta padronizada** — `ApiResponse<T>` como envelope de todas as respostas (`request_id`, `timestamp`, `data`).
- **MDC estruturado** — `MdcContextFilter` injeta `correlationId` e `requestId` em cada requisição; `RequestIdInterceptor` propaga o ID entre camadas.
- **API Versioning** — `@ApiVersion`, `ApiVersionHandlerMapping` e `ApiVersionRequestCondition` para versionamento de endpoints via URL.
- **Estados brasileiros** — entidade `Estado` (27 UFs) com seed SQL; `EstadoService` com `filterBy(EstadoFilterDTO)` + `JpaSpecificationExecutor`.
- **Bancos** — entidade `Banco` (lista de bancos brasileiros); CRUD completo via `BancoService`/`BancoController`.
- **MeterService** — abstração sobre `MeterRegistry` para incrementar contadores de erro sem expor Micrometer diretamente.
- **MailService** — envio de e-mail via SMTP configurável.

### Módulo Condomínio
Gestão da hierarquia física: Condomínio → Bloco → Apartamento.

- CRUD completo para **Condomínio**, **Bloco** e **Apartamento**.
- Filtros dinâmicos via `JpaSpecification` em todos os recursos (nome, CNPJ, número, etc.).
- **Soft delete em cascata** — ao deletar um condomínio, blocos e apartamentos filhos são soft-deletados; entidades financeiras também são cascateadas (ver módulo Financeiro).
- `@PrePersist` seta `condominio_id` automaticamente a partir do `TenantContext`.
- `@Getter/@Setter(AccessLevel.NONE)` no campo `condominio` para evitar vazamento entre tenants.
- `ApartamentoMapper` e `CondominioMapper` usam `EntityManager.getReference()` para criar proxies JPA sem carregar entidades desnecessariamente.
- Cache por tenant com eviction em escrita (`@Cacheable`, `@CacheEvict`, `@Caching`).
- **Tabelas particionadas** — `blocos` e `apartamentos` com HASH por `condominio_id` (4 buckets).
- Área construída em `apartamentos` (coluna `area_construida`).

### Módulo Security
Autenticação e autorização com JWT + OAuth2.

- **Autenticação** — `POST /api/auth/login` retorna access token (RS256, 1 h) + refresh token (UUID, 24 h).
- **Refresh** — `POST /api/auth/refresh` rotaciona o refresh token; o token anterior é invalidado.
- **Logout** — `POST /api/auth/logout` blacklista o JTI do access token atual no Redis.
- **Blacklist** — `CustomBearerTokenFilter` verifica o Redis antes que o Spring Security processe o token; tokens revogados recebem 401.
- **Multi-condomínio** — usuários podem ter acesso a 0 ou N condomínios (relação `@ManyToMany` via `user_condominios`). O JWT carrega a lista `condominio_ids`. Usuários com 0 condominios têm acesso global; com 1, o tenant é auto-selecionado; com N, o header `X-Condominio-Id` é obrigatório (403 se inválido, 400 se não numérico).
- **CRUD de usuários** — `UserController`/`UserService` com filtros dinâmicos (`UserFilterDTO` + `UserSpecification`).
- **Ativação de conta** — fluxo de activation code via e-mail.
- **Troca de senha** — `PATCH /api/users/{id}/password` com histórico; senhas recentes são rejeitadas (`PasswordHistory`).
- **Roles** — `ROLE_ADMIN` obrigatório para operações de escrita; `ROLE_USER` lê dados do próprio tenant.
- **OAuth2 Authorization Server** — configurado via `AuthorizationServerConfig` para emissão de tokens.
- Soft delete em usuários; `UserRepository` não usa `@Filter` (Spring Security consulta usuários globalmente; isolamento por service).

### Módulo Moradores
Gestão de moradores e proprietários de apartamentos.

- **Hierarquia de herança** — `Pessoa` herda de `User` via JPA JOINED inheritance (`@Inheritance(InheritanceType.JOINED)` em `User`, `@PrimaryKeyJoinColumn(name="id")` em `Pessoa`). A tabela `pessoas` tem `id` como FK para `users.id`. Dentro de `pessoas`, as subclasses usam SINGLE_TABLE inheritance com discriminador `pessoa_tipo`.
- **Subclasses de Pessoa** (discriminadores):
  - `PessoaFisica` (`"PF"`) — campo `cpf`; papel `ROLE_MORADOR`
  - `PessoaJuridica` (`"PJ"`) — campos `cnpj`, `razao_social`; papel `ROLE_MORADOR`
  - `Proprietario` (abstract, `"PROP"`) — estende Pessoa; papel `ROLE_PROPRIETARIO`
  - `ProprietarioPessoaFisica` (`"PROP_PF"`) — campo `cpf`
  - `ProprietarioPessoaJuridica` (`"PROP_PJ"`) — campos `cnpj`, `razao_social`
- **`@PrePersist`** em `Pessoa` — chama `super.prePersist()` e adiciona automaticamente `ROLE_MORADOR` ao set de papéis do `User`. `Proprietario.prePersist()` adiciona também `ROLE_PROPRIETARIO`. O `roles` em `User` foi alterado de `Set.of(...)` imutável para `new HashSet<>(Set.of(...))` para suportar `add()`.
- **Moradores (1:N)** — `Apartamento` tem `@OneToMany(mappedBy = "apartamento") List<Pessoa> moradores`. A FK `apartamento_id` reside diretamente na tabela `pessoas`. Não há tabela de junção separada nem histórico de ocupação.
- **Proprietários (N:M)** — `Proprietario` tem `@ManyToMany @JoinTable(name="proprietario_apartamentos") Set<Apartamento> apartamentos`. A join table armazena `proprietario_id`, `apartamento_id`, `condominio_id`, `data_inicio`, `data_fim`.
- **Sem HASH partitioning** em `pessoas` — JOINED inheritance exige que `pessoas.id` seja FK única para `users.id` (PK simples), o que é incompatível com a restrição do PostgreSQL de incluir a chave de partição na PK. A tabela `pessoas` usa índices simples em `condominio_id`.
- CRUD de **Pessoas** (`PF`/`PJ`) via `PessoaController` (`/api/pessoas`).
- CRUD de **Proprietários** (`PROP_PF`/`PROP_PJ`) via `ProprietarioController` (`/api/proprietarios`).
- Associação/desassociação de moradores: `POST /api/pessoas/{id}/apartamento?apartamentoId=X`, `DELETE /api/pessoas/{id}/apartamento`.
- Associação/desassociação de proprietários: `POST /api/proprietarios/{id}/apartamentos?apartamentoId=X`, `DELETE /api/proprietarios/{id}/apartamentos/{aptId}`.
- Listagem de proprietários por apartamento: `GET /api/apartamentos/{aptId}/proprietarios`.
- Filtros dinâmicos por `nome`, `tipo`, `email` via `JpaSpecificationExecutor`.
- **Frontend** — `PessoasPage` em `/pessoas` (CRUD com chips de tipo e documento), `ApartamentoDetailPage` em `/hierarquia/apartamentos/:id` (abas Moradores e Proprietários com dialogs de atribuição/criação).

### Módulo Financeiro
Gestão financeira completa do condomínio.

- **Plano de Contas** — estrutura hierárquica (`pai_id`) com tipos `RECEITA`/`DESPESA`, `TipoRateio` (`IGUALITARIO`/`FRACAO_IDEAL`) e `EscopoRateio` (`TODOS`/`POR_BLOCO`). Endpoints: CRUD + tree (`GET /api/plano-contas/arvore`).
- **Fundo de Reserva** — entidade única por condomínio. Operações: criar, atualizar percentual/conta, creditar, debitar, listar movimentações paginadas. Saldo calculado e validado no service (rejeita débito se saldo insuficiente).
- **Orçamento Anual** — ciclo de vida: `RASCUNHO → APROVADO → ENCERRADO`. Operações: criar, atualizar exercício, aprovar (calcula taxa estimada por unidade = total previsto / 12 / nUnidades), encerrar, deletar. Itens: adicionar, atualizar valor previsto, remover.
- **Contas Bancárias** — CRUD completo (`/api/contas-bancarias`). Associação com `Banco` (referência global) e integração com `FundoReserva`. Filtros por banco, tipo e status.
- **Lançamentos Bancários** — CRUD completo (`/api/lancamentos-bancarios`). Tipos: `CREDITO`/`DEBITO`. Origens: `MANUAL`/`EXTRATO`. Status de conciliação: `PENDENTE`/`CONCILIADO`/`DIVERGENTE`.
- **Extrato Importação** — importação de extratos bancários (`ExtratoImportacao` + `ItemExtrato`). Formatos suportados via `FormatoExtrato`.
- **Conciliação Bancária** — associação de itens de extrato a itens de orçamento via `AssociacaoOrcamentoController` (`/api/conciliacao/associacao`). Sugestão automática por score (`SugestaoScoreCalculator`); cálculo de contribuição por item de orçamento.
- **Soft delete em cascata** — ao deletar um condomínio, todas as entidades financeiras são cascateadas: itens de orçamento → orçamentos → movimentações de fundo → fundo de reserva → plano de contas (nessa ordem para respeitar FKs).
- **Tabelas particionadas** — `plano_contas`, `fundo_reserva`, `fundo_reserva_movimentacao`, `orcamento_anual`, `item_orcamento`, `contas_bancarias`, `lancamentos_bancarios` com HASH por `condominio_id` (4 buckets). PKs compostas incluem `condominio_id`; FKs entre tabelas particionadas carregam a chave de partição.
- `OrcamentoAnualMapper` usa `EntityManager.getReference()` via `planoContasFromId()` para criar proxies de `PlanoContas` sem carregar entidades.

---

## Stack Tecnológica

| Camada | Tecnologia | Versão |
|---|---|---|
| Linguagem | Java (Virtual Threads / Project Loom) | 21 |
| Framework | Spring Boot | 3.5.14 |
| Servidor HTTP | Undertow (substitui Tomcat) | via Spring Boot |
| Persistência | Spring Data JPA + Hibernate | via Spring Boot |
| Banco de dados | PostgreSQL | 16 |
| Migrações | Liquibase | via Spring Boot |
| Cache L1 | Caffeine (in-process) | via Spring Boot |
| Cache L2 | Redis + Lettuce | 7 |
| Segurança | Spring Security + OAuth2 Resource Server + Authorization Server | via Spring Boot |
| JWT | RS256 (assimétrico) | via Spring Boot |
| Mapeamento | MapStruct | 1.6.3 |
| Redução de boilerplate | Lombok | via Spring Boot |
| Métricas | Micrometer + Prometheus | via Spring Boot |
| Logging estruturado | Logback + Loki Appender + Logstash Encoder | 1.6.0 / 8.0 |
| Build | Apache Maven | 3.9 |
| Testes unitários | JUnit 5 + Mockito | via Spring Boot |
| Testes de integração | Testcontainers (PG 16 + Redis 7) | via Spring Boot |
| Testes BDD | Cucumber | 7.18.0 |
| Arquitetura | ArchUnit | 1.3.0 |
| Cobertura | JaCoCo (mínimo 80%) | 0.8.12 |
| Performance | Apache JMeter | 5.6.3 |
| Containerização | Docker (multi-stage, Ubuntu Jammy + ZGC) | — |
| Monitoramento | Prometheus + Grafana | v2.52.0 / 11.0.0 |
| Log aggregation | Grafana Loki | 3.0.0 |
| GC | ZGC Generacional (Java 21) | — |

---

## Estrutura do Projeto

```
src/main/java/com/pmrodrigues/
├── CondominiApplication.java
├── commons/
│   ├── cache/          # TwoLevelCache, TwoLevelCacheManager
│   ├── config/         # CacheConfig, JpaAuditingConfig, MetricsConfig, WebConfig, TenantFilterAspect, ApiVersioningConfig
│   ├── controller/     # BancoController, EstadoController
│   ├── dto/            # ApiResponse, BancoDTO, BancoFilterDTO, CreateBancoDTO, UpdateBancoDTO,
│   │                   # EnderecoDTO, ErrorResponse, EstadoDTO, EstadoFilterDTO
│   ├── embeddable/     # Endereco (JPA @Embeddable)
│   ├── filter/         # MdcContextFilter
│   ├── interceptor/    # RequestIdInterceptor
│   ├── mapper/         # BancoMapper, EnderecoMapper, EstadoMapper
│   ├── model/          # Banco, Estado
│   ├── repository/     # BancoRepository, EstadoRepository
│   ├── service/        # BancoService, EstadoService, MailService, MeterService
│   ├── specification/  # BancoSpecification, EstadoSpecification
│   ├── tenant/         # TenantContext
│   ├── util/           # Exceptions
│   ├── validation/     # @CNPJ, CnpjValidator
│   └── versioning/     # @ApiVersion, ApiVersionHandlerMapping, ApiVersionRequestCondition
├── condominio/
│   ├── controller/     # ApartamentoController, BlocoController, CondominioController
│   ├── dto/            # Create*, Update*, Filter* DTOs
│   ├── mapper/         # ApartamentoMapper, BlocoMapper, CondominioMapper
│   ├── model/          # Apartamento, Bloco, Condominio
│   ├── repository/     # ApartamentoRepository, BlocoRepository, CondominioRepository
│   ├── service/        # ApartamentoService, BlocoService, CondominioService
│   └── specification/  # ApartamentoSpecification, BlocoSpecification, CondominioSpecification
├── morador/
│   ├── controller/     # PessoaController, ProprietarioController
│   ├── dto/            # PessoaDTO, CreatePessoaDTO, UpdatePessoaDTO, PessoaFilterDTO,
│   │                   # ProprietarioDTO, CreateProprietarioDTO, ProprietarioFilterDTO,
│   │                   # AssociarProprietarioApartamentoDTO
│   ├── mapper/         # PessoaMapper, ProprietarioMapper
│   ├── model/          # Pessoa (abstract), PessoaFisica, PessoaJuridica,
│   │                   # Proprietario (abstract), ProprietarioPessoaFisica, ProprietarioPessoaJuridica
│   ├── repository/     # PessoaRepository, PessoaFisicaRepository,
│   │                   # PessoaJuridicaRepository, ProprietarioRepository
│   ├── service/        # PessoaService, ProprietarioService
│   └── specification/  # PessoaSpecification, ProprietarioSpecification
├── financeiro/
│   ├── controller/     # AssociacaoOrcamentoController, ContaBancariaController,
│   │                   # FundoReservaController, LancamentoBancarioController,
│   │                   # OrcamentoAnualController, PlanoContasController
│   ├── dto/            # Create*, Update*, Filter* DTOs para todas as entidades;
│   │                   # Aprovar*, Creditar*, Debitar*, Associar*, Desassociar* DTOs;
│   │                   # ItemExtratoComOrcamentoResponse, SugestaoItemOrcamentoResponse,
│   │                   # ContribuicaoResponse, ItemOrcamentoResumoResponse
│   ├── mapper/         # ContaBancariaMapper, FundoReservaMapper, ItemExtratoMapper,
│   │                   # LancamentoBancarioMapper, OrcamentoAnualMapper, PlanoContasMapper
│   ├── model/          # ContaBancaria, ExtratoImportacao, FundoReserva, FundoReservaMovimentacao,
│   │                   # ItemExtrato, ItemOrcamento, LancamentoBancario, OrcamentoAnual, PlanoContas
│   │                   # Enums: EscopoRateio, FormatoExtrato, OrigemLancamento, StatusExtrato,
│   │                   # StatusItemExtrato, StatusLancamento, StatusOrcamento, TipoContaBancaria,
│   │                   # TipoConta, TipoLancamento, TipoMovimentacao, TipoRateio
│   ├── repository/     # ContaBancariaRepository, FundoReservaRepository,
│   │                   # FundoReservaMovimentacaoRepository, ItemExtratoRepository,
│   │                   # ItemOrcamentoRepository, LancamentoBancarioRepository,
│   │                   # OrcamentoAnualRepository, PlanoContasRepository
│   ├── service/        # AssociacaoOrcamentoService, ContaBancariaService, FundoReservaService,
│   │                   # LancamentoBancarioService, OrcamentoAnualService, PlanoContasService
│   ├── specification/  # ContaBancariaSpecification, LancamentoBancarioSpecification,
│   │                   # OrcamentoAnualSpecification, PlanoContasSpecification
│   └── util/           # SugestaoScoreCalculator
└── security/
    ├── config/         # AuthorizationServerConfig, JwtConfig, JwtProperties, SecurityConfig
    ├── controller/     # AuthController, AuthExceptionHandler, UserController
    ├── dto/            # ActivateAccountDTO, AuthRequestDTO, AuthResponseDTO, ChangePasswordDTO,
    │                   # CreateUserDTO, RefreshRequestDTO, UserDTO, UserFilterDTO
    ├── filter/         # CustomBearerTokenFilter
    ├── mapper/         # UserMapper
    ├── model/          # User, PasswordHistory
    ├── repository/     # PasswordHistoryRepository, UserRepository
    ├── service/        # JwtService, TokenBlacklistService, UserDetailsServiceImpl, UserService
    └── specification/  # UserSpecification
```

---

## Banco de Dados

23 migrações Liquibase em `src/main/resources/db/changelog/changes/` (SQL, `dbms:postgresql`):

| # | Arquivo | Conteúdo |
|---|---|---|
| 0001 | create-users-tables | Tabela `users` (email, password_hash) |
| 0002 | add-soft-delete-and-activation | Colunas `deleted`, `activation_code` |
| 0003 | add-audit-timestamps | `created_at`, `updated_at`, `created_by`, `updated_by` |
| 0004 | create-estados-table | Tabela `estados` (UF brasileiras) |
| 0005 | create-condominios-tables | `condominios`, `blocos`, `apartamentos` |
| 0006 | insert-estados | Seed com 27 estados brasileiros |
| 0007 | add-condominio-to-apartamentos | FK `condominio_id` em `apartamentos` |
| 0008 | partition-blocos-apartamentos | HASH partition por `condominio_id` (4 buckets) em `blocos` e `apartamentos` |
| 0009 | add-condominio-id-to-users | *(removido pela 0023; mantido para histórico de migração)* |
| 0010 | insert-admin-user | Seed do usuário master admin |
| 0011 | create-password-history | Histórico de senhas para prevenção de reutilização |
| 0012 | create-financeiro-tables | `plano_contas`, `fundo_reserva`, `fundo_reserva_movimentacao`, `orcamento_anual`, `item_orcamento` |
| 0013 | add-tipo-rateio-to-plano-contas | Coluna `tipo_rateio` |
| 0014 | add-escopo-rateio-to-plano-contas | Coluna `escopo_rateio` |
| 0015 | partition-financeiro-tables | HASH partition das 5 tabelas financeiras; PKs compostas; FKs inter-particionadas |
| 0016 | alter-apartamento-add-areaConstruida | Coluna `area_construida` em `apartamentos` |
| 0017 | create-bancos | Tabela `bancos` (código COMPE, nome, ISPB) |
| 0018 | create-contas-bancarias | Tabela `contas_bancarias` particionada por `condominio_id` |
| 0019 | create-lancamentos-bancarios | Tabela `lancamentos_bancarios` particionada por `condominio_id` |
| 0020 | create-extrato-importacao | Tabelas `extrato_importacao` e `itens_extrato` |
| 0021 | refactor-fundo-reserva-conta-bancaria | Associa `fundo_reserva` a `conta_bancaria` |
| 0022 | add-item-orcamento-to-itens-extrato | FK de `itens_extrato` para `item_orcamento` (conciliação) |
| 0023 | user-condominios-many-to-many | Cria `user_condominios`; migra dados; remove `condominio_id` de `users` |
| 0024 | add-roles-to-users | Coluna `roles` (array texto) em `users` para suportar `ROLE_MORADOR`/`ROLE_PROPRIETARIO` |
| 0025 | add-dtype-to-users | Coluna `dtype VARCHAR(31) DEFAULT 'USER'` em `users` (discriminator JPA para JOINED inheritance) |
| 0026 | create-pessoas | Tabela `pessoas` com `id` FK → `users.id`; discriminador `pessoa_tipo`; FK `apartamento_id`; sem HASH partitioning (incompatível com JOINED inheritance) |
| 0027 | add-cpf-cnpj-to-pessoas | Colunas `cpf`, `cnpj`, `razao_social` em `pessoas` |
| 0028 | create-proprietario-apartamentos | Join table N:M `proprietario_apartamentos` (`proprietario_id`, `apartamento_id`, `condominio_id`, `data_inicio`, `data_fim`) |
| 0029 | drop-historico-ocupacao | Remove tabelas legadas `historico_ocupacao` e `proprietarios` |

Para adicionar uma migração: criar `NNNN-descricao.sql` (próximo: `0030`) e registrar no `db.changelog-master.yaml`.

---

## Pré-requisitos

- Java 21+
- Maven 3.9+
- Docker (obrigatório para Testcontainers nos testes de integração e BDD)
- PostgreSQL 16 (para executar localmente)
- Redis 7 (para executar localmente)

---

## Variáveis de Ambiente

| Variável | Padrão | Descrição |
|---|---|---|
| `DB_USERNAME` | `postgres` | Usuário PostgreSQL |
| `DB_PASSWORD` | `postgres` | Senha PostgreSQL |
| `DB_URL` | `jdbc:postgresql://localhost:5432/condominio` | URL JDBC |
| `REDIS_HOST` | `localhost` | Host Redis |
| `REDIS_PORT` | `6379` | Porta Redis |
| `REDIS_PASSWORD` | *(vazio)* | Senha Redis (opcional) |
| `JWT_SECRET` | *(chave dev)* | Base64-encoded RS256 — **substituir em produção** |
| `JWT_ISSUER` | `http://localhost:8080` | Claim `iss` dos tokens |
| `JWT_ACCESS_EXPIRATION` | `3600` | TTL do access token (segundos) |
| `JWT_REFRESH_EXPIRATION` | `86400` | TTL do refresh token (segundos) |
| `MAIL_HOST` | `localhost` | Host SMTP |
| `MAIL_PORT` | `587` | Porta SMTP |
| `SERVER_PORT` | `8080` | Porta HTTP |
| `GRAFANA_USER` | `admin` | Usuário Grafana |
| `GRAFANA_PASSWORD` | `admin` | Senha Grafana |

---

## Executando

### Localmente

```bash
# Clonar e compilar
git clone <repo-url>
cd condominio-web
mvn package -DskipTests

# Subir dependências (PostgreSQL + Redis)
docker compose up -d postgres redis

# Executar a aplicação
mvn spring-boot:run
```

A API está disponível em `http://localhost:8080/api`.

### Stack completa (aplicação + observabilidade)

```bash
docker compose up -d
```

Serviços disponíveis:

| Serviço | URL | Descrição |
|---|---|---|
| API | http://localhost:8080/api | Endpoints REST |
| Prometheus | http://localhost:9090 | Métricas |
| Grafana | http://localhost:3000 | Dashboards |
| Loki | http://localhost:3100 | Log aggregation |

### Build Docker isolado

```bash
docker build -t condominio-web .
docker run -p 8080:8080 \
  -e DB_USERNAME=postgres \
  -e DB_PASSWORD=postgres \
  -e REDIS_HOST=redis \
  condominio-web
```

---

## Testes

```bash
# Testes unitários + integração (requer Docker para Testcontainers)
mvn test

# Verificação completa com cobertura mínima de 80%
mvn verify

# Testes E2E BDD (fluxos completos, sem mocks, Testcontainers)
mvn verify -Pe2e

# Testes de performance (Apache JMeter)
mvn test -Pperformance -Dapp.host=localhost -Dapp.port=8080 \
  -Dapp.user=admin@condominio.com -Dapp.pass=admin123
```

### Cobertura de testes

| Tipo | Frameworks | Quantidade |
|---|---|---|
| Unitários (service, mapper, repository) | JUnit 5 + Mockito + H2 | ~620 testes |
| BDD (integração full-stack) | Cucumber 7 + Testcontainers PG16 + Redis7 | ~117 cenários |
| Arquitetura | ArchUnit 1.3 | 7 regras |
| Performance | Apache JMeter 5.6 | profile dedicado |

### Regras de arquitetura (ArchUnit)

- Controllers não acessam repositórios nem modelos diretamente.
- Todos os DTOs devem ser Java `record`.
- Classes terminando em `DTO` residem em pacotes `*.dto.*`.
- Arquitetura em camadas: `Controller → Service → Repository`.
- Sem `@Autowired` em campos (apenas injeção por construtor).
- Nenhum `@Service` acessa repositórios de outro módulo (sempre via service).
- Todos os métodos públicos de qualquer `*.service.*` devem ter `@Timed`.

### Feature files BDD

| Feature | Cenários |
|---|---|
| `auth.feature` | Login, refresh, logout, token inválido |
| `condominios.feature` | CRUD, filtros, 401/403/404 |
| `blocos.feature` | CRUD, filtros, multi-tenant |
| `apartamentos.feature` | CRUD, filtros, área construída |
| `usuarios.feature` | CRUD, filtros, ativação, troca de senha, multi-condomínio |
| `estados.feature` | Listagem, filtros |
| `plano-contas.feature` | CRUD, hierarquia, arvore |
| `fundo-reserva.feature` | Criar, creditar, debitar, movimentações |
| `orcamento-anual.feature` | CRUD, adicionar item, aprovar, encerrar |
| `pessoas.feature` | Criar PF/PJ, filtros, atribuir/remover apartamento, 401/403/404 |
| `proprietarios.feature` | Criar PROP_PF/PROP_PJ, associar/desassociar apartamento, 401/403/404 |

---

## Endpoints da API

Todos os endpoints exigem `Authorization: Bearer <access_token>`. `POST`, `PUT` e `DELETE` exigem `ROLE_ADMIN`.

Usuários com acesso a múltiplos condomínios devem enviar o header `X-Condominio-Id: <id>` para selecionar o tenant ativo. Retorna 403 se o ID não constar na lista do token.

### Autenticação

| Método | Endpoint | Descrição |
|---|---|---|
| `POST` | `/api/auth/login` | Login; retorna access + refresh tokens |
| `POST` | `/api/auth/refresh` | Rotaciona refresh token |
| `POST` | `/api/auth/logout` | Invalida access token atual |

### Usuários

| Método | Endpoint | Descrição |
|---|---|---|
| `GET` | `/api/users` | Listar (filtros: `nome`, `email`, `enabled`, `role`) |
| `POST` | `/api/users` | Criar usuário (`condominioIds`: lista de IDs ou vazio para acesso global) |
| `GET` | `/api/users/{id}` | Buscar por ID |
| `PUT` | `/api/users/{id}` | Atualizar (admin ou dono) |
| `DELETE` | `/api/users/{id}` | Soft-delete |
| `PATCH` | `/api/users/{id}/password` | Trocar senha |
| `POST` | `/api/users/{id}/activate` | Ativar conta |

### Estados

| Método | Endpoint | Descrição |
|---|---|---|
| `GET` | `/api/estados` | Listar (filtros: `uf`, `nome`) |
| `GET` | `/api/estados/{id}` | Buscar por ID |

### Bancos

| Método | Endpoint | Descrição |
|---|---|---|
| `GET` | `/api/bancos` | Listar (filtros: `codigo`, `nome`) |
| `POST` | `/api/bancos` | Criar |
| `GET` | `/api/bancos/{id}` | Buscar por ID |
| `PUT` | `/api/bancos/{id}` | Atualizar |
| `DELETE` | `/api/bancos/{id}` | Soft-delete |

### Condomínios

| Método | Endpoint | Descrição |
|---|---|---|
| `GET` | `/api/condominios` | Listar (filtros: `nome`, `cnpj`) |
| `POST` | `/api/condominios` | Criar |
| `GET` | `/api/condominios/{id}` | Buscar por ID |
| `PUT` | `/api/condominios/{id}` | Atualizar |
| `DELETE` | `/api/condominios/{id}` | Soft-delete (cascata para blocos, apartamentos e dados financeiros) |

### Blocos

| Método | Endpoint | Descrição |
|---|---|---|
| `GET` | `/api/blocos` | Listar (filtros: `nome`) |
| `POST` | `/api/blocos` | Criar |
| `GET` | `/api/blocos/{id}` | Buscar por ID |
| `PUT` | `/api/blocos/{id}` | Atualizar |
| `DELETE` | `/api/blocos/{id}` | Soft-delete |

### Apartamentos

| Método | Endpoint | Descrição |
|---|---|---|
| `GET` | `/api/apartamentos` | Listar (filtros: `numero`, `blocoId`) |
| `POST` | `/api/apartamentos` | Criar |
| `GET` | `/api/apartamentos/{id}` | Buscar por ID |
| `PUT` | `/api/apartamentos/{id}` | Atualizar |
| `DELETE` | `/api/apartamentos/{id}` | Soft-delete |

### Pessoas (Moradores)

| Método | Endpoint | Descrição |
|---|---|---|
| `GET` | `/api/pessoas` | Listar (filtros: `nome`, `tipo`, `email`) |
| `POST` | `/api/pessoas` | Criar pessoa PF (`tipo: "PF"`) ou PJ (`tipo: "PJ"`) |
| `GET` | `/api/pessoas/{id}` | Buscar por ID |
| `PUT` | `/api/pessoas/{id}` | Atualizar |
| `DELETE` | `/api/pessoas/{id}` | Soft-delete |
| `POST` | `/api/pessoas/{id}/apartamento` | Atribuir ao apartamento (`?apartamentoId=X`) |
| `DELETE` | `/api/pessoas/{id}/apartamento` | Remover do apartamento (seta FK para null) |

### Proprietários

| Método | Endpoint | Descrição |
|---|---|---|
| `GET` | `/api/proprietarios` | Listar (filtros: `nome`, `tipo`, `email`) |
| `POST` | `/api/proprietarios` | Criar proprietário PF (`tipo: "PROP_PF"`) ou PJ (`tipo: "PROP_PJ"`) |
| `GET` | `/api/proprietarios/{id}` | Buscar por ID (inclui lista de apartamentos) |
| `PUT` | `/api/proprietarios/{id}` | Atualizar (tipo não pode ser alterado após criação) |
| `DELETE` | `/api/proprietarios/{id}` | Soft-delete |
| `POST` | `/api/proprietarios/{id}/apartamentos` | Associar ao apartamento (`?apartamentoId=X`) |
| `DELETE` | `/api/proprietarios/{id}/apartamentos/{aptId}` | Desassociar do apartamento |
| `GET` | `/api/apartamentos/{aptId}/proprietarios` | Listar proprietários de um apartamento |

### Plano de Contas

| Método | Endpoint | Descrição |
|---|---|---|
| `GET` | `/api/plano-contas` | Listar (filtros: `tipo`, `paiId`) |
| `GET` | `/api/plano-contas/arvore` | Retornar árvore hierárquica completa |
| `POST` | `/api/plano-contas` | Criar |
| `GET` | `/api/plano-contas/{id}` | Buscar por ID |
| `PUT` | `/api/plano-contas/{id}` | Atualizar |
| `DELETE` | `/api/plano-contas/{id}` | Soft-delete (rejeita se tiver filhos) |

### Fundo de Reserva

| Método | Endpoint | Descrição |
|---|---|---|
| `GET` | `/api/fundo-reserva` | Obter fundo do condomínio atual |
| `POST` | `/api/fundo-reserva` | Criar (apenas um por condomínio) |
| `PUT` | `/api/fundo-reserva` | Atualizar percentual / conta bancária |
| `POST` | `/api/fundo-reserva/creditar` | Creditar valor |
| `POST` | `/api/fundo-reserva/debitar` | Debitar valor (rejeita se saldo insuficiente) |
| `GET` | `/api/fundo-reserva/movimentacoes` | Listar movimentações (paginado, mais recente primeiro) |

### Orçamento Anual

| Método | Endpoint | Descrição |
|---|---|---|
| `GET` | `/api/orcamentos` | Listar (filtros: `exercicio`, `status`) |
| `POST` | `/api/orcamentos` | Criar (status inicial: RASCUNHO) |
| `GET` | `/api/orcamentos/{id}` | Buscar por ID |
| `PUT` | `/api/orcamentos/{id}` | Atualizar exercício (apenas RASCUNHO) |
| `DELETE` | `/api/orcamentos/{id}` | Soft-delete (apenas RASCUNHO) |
| `POST` | `/api/orcamentos/{id}/aprovar` | Aprovar; calcula taxa por unidade |
| `POST` | `/api/orcamentos/{id}/encerrar` | Encerrar (apenas APROVADO) |
| `POST` | `/api/orcamentos/{id}/itens` | Adicionar item |
| `PUT` | `/api/orcamentos/{id}/itens/{itemId}` | Atualizar valor previsto |
| `DELETE` | `/api/orcamentos/{id}/itens/{itemId}` | Remover item |

### Contas Bancárias

| Método | Endpoint | Descrição |
|---|---|---|
| `GET` | `/api/contas-bancarias` | Listar (filtros: `bancoId`, `tipo`, `status`) |
| `POST` | `/api/contas-bancarias` | Criar |
| `GET` | `/api/contas-bancarias/{id}` | Buscar por ID |
| `PUT` | `/api/contas-bancarias/{id}` | Atualizar |
| `DELETE` | `/api/contas-bancarias/{id}` | Soft-delete |

### Lançamentos Bancários

| Método | Endpoint | Descrição |
|---|---|---|
| `GET` | `/api/lancamentos-bancarios` | Listar (filtros: `contaId`, `tipo`, `status`, `dataInicio`, `dataFim`) |
| `POST` | `/api/lancamentos-bancarios` | Criar lançamento manual |
| `GET` | `/api/lancamentos-bancarios/{id}` | Buscar por ID |
| `PUT` | `/api/lancamentos-bancarios/{id}` | Atualizar |
| `DELETE` | `/api/lancamentos-bancarios/{id}` | Soft-delete |

### Conciliação Bancária

| Método | Endpoint | Descrição |
|---|---|---|
| `POST` | `/api/conciliacao/associacao/{itemExtratoId}` | Associar item de extrato a item de orçamento |
| `DELETE` | `/api/conciliacao/associacao/{itemExtratoId}` | Desassociar (com justificativa) |
| `GET` | `/api/conciliacao/associacao/sugestoes/{itemExtratoId}` | Sugestões por score (até 5 candidatos) |
| `GET` | `/api/conciliacao/associacao/contribuicao/{itemOrcamentoId}` | Contribuição realizada por item de orçamento |

### Formato de resposta

```json
{
  "request_id": "550e8400-e29b-41d4-a716-446655440000",
  "timestamp": "2026-05-29T10:00:00Z",
  "data": { }
}
```

---

## Arquitetura

### Multi-tenancy

Todas as tabelas de domínio têm `condominio_id`. Nove tabelas são HASH-particionadas por `condominio_id` em 4 buckets (PostgreSQL table partitioning): `blocos`, `apartamentos`, `plano_contas`, `fundo_reserva`, `fundo_reserva_movimentacao`, `orcamento_anual`, `item_orcamento`, `contas_bancarias`, `lancamentos_bancarios`. PKs compostas incluem `condominio_id`; FKs entre tabelas particionadas carregam a chave de partição para respeitar a restrição do PostgreSQL.

**Exceção — tabela `pessoas`**: JPA JOINED inheritance exige que `pessoas.id` seja uma FK simples para `users.id` (PK de coluna única). PostgreSQL HASH partitioning requer que a chave de partição (`condominio_id`) faça parte da PK — o que cria uma PK composta `(id, condominio_id)` incompatível com a FK `REFERENCES users(id)`. Por essa razão, `pessoas` e `proprietario_apartamentos` **não são particionadas**; isolamento multi-tenant é garantido pelo `@Filter` Hibernate e pelo índice `idx_pessoas_condominio_id`.

O `TenantContext` (ThreadLocal) é populado pelo `CustomBearerTokenFilter` a partir da claim `condominio_ids` do JWT e do header `X-Condominio-Id`:

- **0 condominios no token** → acesso global (sem filtro Hibernate; comportamento de master admin).
- **1 condominio no token** → tenant auto-selecionado, header não obrigatório.
- **N condominios no token** → `X-Condominio-Id` obrigatório; 403 se o valor não constar na lista; 400 se não numérico.

O `TenantFilterAspect` ativa o filtro Hibernate (`condominioFilter`) em toda query. Entidades têm `@PrePersist` que seta `condominio_id` automaticamente — o caller nunca precisa setar manualmente.

A tabela `user_condominios` (join table `@ManyToMany`) é global (sem particionamento por tenant), pois representa direitos de acesso, não dados de negócio.

### Hierarquia de herança — Módulo Moradores

```
User (users)
└── Pessoa (pessoas — JOINED, PK = FK → users.id)
    ├── PessoaFisica        discriminator="PF"
    ├── PessoaJuridica      discriminator="PJ"
    └── Proprietario (abstract)  discriminator="PROP"
        ├── ProprietarioPessoaFisica   discriminator="PROP_PF"
        └── ProprietarioPessoaJuridica discriminator="PROP_PJ"
```

- `User → Pessoa`: **JOINED** (`InheritanceType.JOINED`). Tabelas separadas; `pessoas.id` é FK para `users.id`. Cada `Pessoa` tem login e credenciais próprios.
- `Pessoa → subclasses`: **SINGLE_TABLE** dentro de `pessoas`, discriminadas pela coluna `pessoa_tipo`.
- **Roles atribuídas em `@PrePersist`**: `Pessoa` adiciona `ROLE_MORADOR`; `Proprietario` adiciona também `ROLE_PROPRIETARIO`.
- **Relacionamentos**:
  - **1:N moradores** — `Apartamento.moradores` (`@OneToMany mappedBy="apartamento"`); FK `pessoas.apartamento_id` (nullable, setada a null ao remover o morador).
  - **N:M proprietários** — `Proprietario.apartamentos` (`@ManyToMany @JoinTable(name="proprietario_apartamentos")`); a join table armazena também `condominio_id` e datas de início/fim.

### Cache em dois níveis

```
Request → L1 (Caffeine, in-process, sub-ms) → L2 (Redis, compartilhado, ms) → DB
```

- **L1** — Caffeine, TTL configurável por cache (5–10 min), máx 100–2000 entradas.
- **L2** — Redis com Lettuce, TTL 30 min–1 h.
- **Chaves** — prefixadas com `condominioId` do `TenantContext` para isolamento entre tenants.
- **Eviction** — write-through: qualquer escrita invalida L1 e L2 via `@CacheEvict`/`@Caching`.

### JWT + OAuth2

- Tokens RS256 (assimétrico). O access token (1 h) carrega claims `roles` e `condominio_ids` (lista de IDs dos condomínios acessíveis ao usuário).
- Refresh token: UUID opaco, armazenado no Redis. Rotacionado a cada refresh; o anterior é imediatamente invalidado.
- Blacklist: o JTI do access token é armazenado no Redis no logout. `CustomBearerTokenFilter` rejeita JTIs na blacklist com 401 antes que o Spring Security processe.

### Soft Delete

Todas as entidades de domínio usam `@SQLDelete` (UPDATE ... SET deleted = true) + `@SQLRestriction("deleted = false")`. Queries automaticamente excluem linhas deletadas sem exigir cláusula `WHERE` explícita no caller.

Cascata de delete ao remover um Condomínio:
1. Apartamentos → Blocos (módulo condomínio)
2. Itens de orçamento → Orçamentos anuais (módulo financeiro)
3. Movimentações de fundo → Fundo de reserva (módulo financeiro)
4. Plano de contas (módulo financeiro)
5. Lançamentos bancários → Contas bancárias (módulo financeiro)
6. Pessoas (moradores e proprietários do módulo moradores)
7. Condomínio

### Observabilidade

- **Métricas** — `@Timed` em todos os métodos públicos de todos os services. Endpoint Prometheus: `GET /api/actuator/prometheus`.
- **Logging** — SLF4J + Logback. `MdcContextFilter` injeta `correlationId` e `requestId` em cada linha de log. Loki Appender envia logs estruturados (JSON) ao Grafana Loki.
- **Health** — `GET /api/actuator/health` com probes de liveness/readiness para Kubernetes.
- **Grafana** — dashboard pré-configurado com métricas de latência, taxa de erro e cache hit rate.

### Servidor e JVM

- **Undertow** no lugar do Tomcat — 1,5× melhor throughput para workloads REST.
- **Virtual Threads** (Java 21 / Project Loom) — habilitados via `spring.threads.virtual.enabled=true`.
- **ZGC Generacional** — configurado no Dockerfile para latência de GC sub-1 ms em produção (requer Ubuntu/glibc; Alpine/musl não suporta ZGC).
- **Dockerfile multi-stage** — build (Maven + Alpine), extração de layers (Spring Boot layertools), runtime (Ubuntu Jammy). Camadas de dependências separadas da aplicação para rebuild incremental.
- **Perfil `prod`** — `spring.datasource.hikari.auto-commit=false` + `hibernate.connection.provider_disables_autocommit=true` elimina 2 round-trips JDBC por transação.
