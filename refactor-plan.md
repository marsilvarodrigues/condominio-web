# Refactoring Plan — Ciclo 6

**Source:** inline — avaliação arquitetural 2026-06-08 (módulos cobranca, gateway, financeiro.CotaRateio)
**Created:** 2026-06-08
**Target:** Cobertura de testes completa para os módulos cobranca e gateway recém-adicionados, alinhados com o padrão estabelecido nos Ciclos 2–5. `CondominioService.delete()` propagando soft-delete para `Cobranca`.

## Pre-flight Checks
- [ ] All tests pass before starting (`mvn test`)
- [ ] No uncommitted changes
- [ ] Working branch confirmed

---

## Chunk 1: CotaRateioServiceTest + AsaasGatewayServiceTest

**Why:** `CotaRateioService` é a fronteira de módulo que `CobrancaService` usa para acessar dados do financeiro — 3 métodos públicos, zero testes. `AsaasGatewayService` encapsula todo o fluxo Asaas (lookup/criação de customer, emissão de cobrança, geração de QR Code) — 2 métodos públicos, zero testes.

**Entry criteria:** All tests pass, no prior chunks pending.

**Steps:**

1. Criar `src/test/java/com/pmrodrigues/financeiro/service/CotaRateioServiceTest.java`:
   - `@ExtendWith(MockitoExtension.class)`, `@Mock CotaRateioRepository`, `@Mock RateioExecucaoRepository`
   - `@BeforeEach`: `service = new CotaRateioService(cotaRateioRepository, rateioExecucaoRepository)`
   - Testes:
     - `findExecucaoById_deveRetornarEntidade_quandoExiste` — stub `findById` → Optional.of(entity); assert retorno
     - `findExecucaoById_deveLancar404_quandoNaoExiste` — stub → Optional.empty(); assertThatThrownBy 404
     - `findExecucaoOptional_deveRetornarOptionalPreenchido` — stub → Optional.of(entity)
     - `findExecucaoOptional_deveRetornarOptionalVazio` — stub → Optional.empty()
     - `findByExecucaoId_deveRetornarListaDeCotas` — stub `findByRateioExecucaoId(42L)` → List.of(cota); assert hasSize(1)

2. Criar `src/test/java/com/pmrodrigues/gateway/service/AsaasGatewayServiceTest.java`:
   - `@ExtendWith(MockitoExtension.class)`, `@Mock AsaasClient`, `@Mock PixQrCodeGenerator`, `@Mock AsaasCustomerRepository`
   - `@BeforeEach`: `service = new AsaasGatewayService(asaasClient, qrCodeGenerator, asaasCustomerRepository)`
   - Helper `emissaoRequest()` que retorna um `AsaasEmissaoRequest` válido com pessoaId=1L, valor, vencimento, nome, cpf, email
   - Helper `asaasResp(String pixPayload)` que retorna `AsaasCobrancaResponse` com id, bankSlipUrl, nossoNumero, e `AsaasPixResponse(pixPayload)`
   - Testes:
     - `emitir_deveReutilizarCustomerExistente_semCriarNovo` — stub `asaasCustomerRepository.findByPessoaId(1L)` → Optional.of(customer "cus_111"); stub `asaasClient.criarCobranca(any())` → resp; verify `asaasClient.criarCustomer(any())` never; assert result.asaasCustomerId() == "cus_111"
     - `emitir_deveCriarNovoCustomer_quandoNaoExiste` — stub `findByPessoaId(1L)` → Optional.empty(); stub `criarCustomer(any())` → "cus_novo"; stub `criarCobranca(any())` → resp; verify `asaasCustomerRepository.save(any())` once
     - `emitir_deveGerarQrCodeBase64_quandoPixPayloadPresente` — pixPayload="pix.payload"; stub `qrCodeGenerator.gerarBase64("pix.payload")` → "base64data"; assert result.pixQrCodeBase64() == "base64data"
     - `emitir_naoDeveGerarQrCode_quandoPixPayloadAusente` — pixPayload=null (AsaasPixResponse null); assert result.pixQrCodeBase64() == null; verify qrCodeGenerator never called
     - `cancelar_deveDelegarParaAsaasClient` — `service.cancelar("pay_123")`; verify `asaasClient.cancelarCobranca("pay_123")`

**Exit criteria:** `mvn test` passa sem falhas.

**Commit message:** `test: CotaRateioServiceTest e AsaasGatewayServiceTest`

---

## Chunk 2: CondominioService.delete() — cascade soft-delete para Cobrança

**Why:** `CondominioService.delete()` propaga soft-delete para Bloco, Apartamento, PlanoContas, FundoReserva, OrcamentoAnual, ContaBancaria, LancamentoBancario e Pessoa — mas não para `Cobranca`. Ao deletar um Condomínio, cobranças ficam como órfãos visíveis para o tenant. Padrão estabelecido nos Ciclos 2, 3 e 5.

**Depends on:** nenhum (independente do Chunk 1).

**Entry criteria:** All tests pass.

**Steps:**

1. `CobrancaRepository.java` — adicionar método:
   ```java
   @Modifying
   @Query("UPDATE Cobranca c SET c.deleted = true WHERE c.apartamento.id IN " +
          "(SELECT a.id FROM Apartamento a WHERE a.condominio.id = :condominioId)")
   void softDeleteByCondominioId(@Param("condominioId") Long condominioId);
   ```
   Adicionar imports `@Modifying`, `@Query`, `@Param`.
   *(Nota: `Cobranca` associa-se ao `condominio` através do `Apartamento`, não diretamente. A query navega via join.)*

2. `CobrancaService.java` — adicionar método público:
   ```java
   @Transactional
   @Timed(value = "cobranca.service.softDeleteByCondominioId", description = "Soft-delete cobrancas by condominio")
   public void softDeleteByCondominioId(Long condominioId) {
       log.info("softDeleteByCondominioId condominioId={}", condominioId);
       cobrancaRepository.softDeleteByCondominioId(condominioId);
   }
   ```

3. `CondominioService.java` — adicionar campo `private final CobrancaService cobrancaService` (via `@Lazy` se necessário para evitar ciclo); no método `delete()`, adicionar linha `cobrancaService.softDeleteByCondominioId(id)` após `pessoaService.softDeleteByCondominioId(id)`.

4. `CondominioServiceTest.java` — adicionar `@Mock CobrancaService cobrancaService`; atualizar construtor com o novo campo; adicionar teste `delete_cascadesSoftDeleteToCobrancas`: verifica `cobrancaService.softDeleteByCondominioId(id)` chamado com o id correto.

5. `CondominioServiceCacheTest.java` — adicionar bean `cobrancaService` como mock no `TestConfig`; atualizar construtor do `CondominioService`.

**Exit criteria:** `mvn test` passa sem falhas.

**Commit message:** `feat: cascade soft-delete de Condomínio para Cobranças`

---

## Chunk 3: CobrancaServiceTest

**Why:** `CobrancaService` é o service mais complexo da base — 8 métodos públicos, transações `REQUIRES_NEW`, idempotência em `processarCota`, processamento de webhook. Zero testes. Além disso, o método `softDeleteByCondominioId` adicionado no Chunk 2 precisa de cobertura.

**Depends on:** Chunk 2 (`softDeleteByCondominioId` deve existir em `CobrancaService` antes de criar o teste).

**Entry criteria:** Chunk 2 completo e verificado.

**Steps:**

1. Criar `src/test/java/com/pmrodrigues/cobranca/service/CobrancaServiceTest.java`:
   - `@ExtendWith(MockitoExtension.class)`
   - Mocks: `@Mock CobrancaRepository`, `@Mock CobrancaConfiguracaoRepository`, `@Mock AsaasGatewayService`, `@Mock MailService`, `@Mock CobrancaMapper`, `@Mock CotaRateioService`, `@Mock PessoaService`
   - `@BeforeEach`: `service = new CobrancaService(cobrancaRepository, configuracaoRepo, gatewayService, mailService, mapper, cotaRateioService, pessoaService)`
   - `lenient()` stubs no `@BeforeEach`:
     - `mapper.toDTO(any(Cobranca.class))` → `cobrancaDTO(1L, StatusCobranca.PENDENTE)`
     - `mapper.toResumoDTO(any(Cobranca.class))` → `resumoDTO()`
     - `configuracaoRepo.findByCondominioId(any())` → `Optional.of(CobrancaConfiguracao.builder().condominioId(1L).build())`
     - `pessoaService.listarPorApartamento(any())` → `List.of(pessoaDTO(1L))`
     - `gatewayService.emitir(any())` → `emissaoResult()`
     - `cobrancaRepository.save(any(Cobranca.class))` → answer que seta id=10L na cobrança salva
   - Helpers privados: `cobrancaDTO(Long id, StatusCobranca status)`, `resumoDTO()`, `pessoaDTO(Long id)`, `emissaoResult()`, `cota(Long id, Long aptId)`, `cobranca(Long id, String asaasId)`
   - Testes:
     - `gerarCobrancas_deveRetornarListaDeCobrancasGeradas` — 2 cotas; stub `findByCotaRateioId` → empty (sem duplicata); assert lista de size 2
     - `gerarCobrancas_deveLancar404_quandoExecucaoNaoExiste` — `cotaRateioService.findExecucaoById(99L)` lança 404; assertThatThrownBy 404
     - `processarCota_deveCriarCobrancaEEnviarEmail_quandoMoradorExiste` — happy path; verify `cobrancaRepository.save(any())` twice (1x antes do email, 1x após setEmailEnviado); verify `mailService.sendEmail(eq("m@t.com"), any())` once
     - `processarCota_deveRetornarNull_quandoSemMorador` — `pessoaService.listarPorApartamento(any())` → emptyList(); assert retorno null
     - `processarCota_deveRetornarNull_quandoCobrancaJaExiste_idempotencia` — `cobrancaRepository.findByCotaRateioId(any())` → Optional.of(cobranca existente); assert retorno null; verify `cobrancaRepository.save(any())` never
     - `filterBy_deveRetornarPaginaFiltrada` — stub `findAll(any(Specification.class), any(Pageable.class))` → PageImpl com 1 cobrança; assert size 1
     - `findById_deveRetornarDTO` — stub `findById(1L)` → Optional.of(cobranca); assert retorno não nulo
     - `findById_deveLancar404_quandoNaoExiste` — stub → empty; assertThatThrownBy 404
     - `porApartamento_deveRetornarPagina` — stub `findByApartamentoIdOrderByCreatedAtDesc(any(), any())` → PageImpl; assert não vazio
     - `cancelar_deveCancelarNaAsaasEAtualizarStatus` — cobrança com asaasId="pay_1"; stub `findById(1L)` → cobrança; verify `gatewayService.cancelar("pay_1")`; assert status == CANCELADA
     - `cancelar_deveLancar404_quandoNaoExiste` — stub → empty; assertThatThrownBy 404
     - `reenviarEmail_deveReenviarEmail_quandoMoradorExiste` — stub `findById(1L)` → cobrança com apartamento; verify `mailService.sendEmail(any(), any())`
     - `reenviarEmail_deveLancarIllegalState_quandoSemMorador` — `pessoaService.listarPorApartamento(any())` → emptyList(); assertThatThrownBy `IllegalStateException`
     - `processarWebhook_deveMarcarComoPaga_quandoPaymentReceived` — event="PAYMENT_RECEIVED"; stub `findByAsaasIdNative("pay_1")` → cobrança PENDENTE; assert status PAGA
     - `processarWebhook_deveIgnorar_quandoEventoNaoEhPagamento` — event="PAYMENT_OVERDUE"; verify `cobrancaRepository.findByAsaasIdNative(any())` never
     - `processarWebhook_deveIgnorar_quandoCobrancaJaPaga` — cobrança com status PAGA; verify `cobrancaRepository.save(any())` never
     - `softDeleteByCondominioId_callsRepository` — verify `cobrancaRepository.softDeleteByCondominioId(42L)`

**Exit criteria:** `mvn test` passa sem falhas.

**Commit message:** `test: CobrancaServiceTest`

---

## Chunk 4: CobrancaSpecificationTest

**Why:** Todos os Specifications na base têm testes `@DataJpaTest` dedicados (ContaBancariaSpecificationTest, LancamentoBancarioSpecificationTest, OrcamentoAnualSpecificationTest, PlanoContasSpecificationTest). `CobrancaSpecification` tem 5 factories estáticas sem nenhuma cobertura.

**Depends on:** nenhum (independente dos Chunks 1–3).

**Entry criteria:** All tests pass.

**Steps:**

1. Criar `src/test/java/com/pmrodrigues/cobranca/specification/CobrancaSpecificationTest.java`:
   - `@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")`
   - `@Import(JpaAuditingConfig.class)`
   - `@Autowired CobrancaRepository`, `@Autowired CondominioRepository`, `@Autowired ApartamentoRepository`, `@Autowired BlocoRepository`
   - Persistir via fixture helper: `condominio`, `bloco`, `apartamento`
   - `@BeforeEach`: criar 2–3 cobranças com atributos distintos (status, vencimento, emailEnviado); `TenantContext.setCondominioId(condominio.getId())`
   - `@AfterEach`: `TenantContext.clear()`
   - Testes:
     - `hasApartamento_filtrarPorApartamento` — spec retorna apenas cobranças do apartamento correto
     - `hasApartamento_comNullRetornaTodasAsCobrancas` — null predicado aceita todas
     - `hasStatus_filtrarPorStatus` — PENDENTE vs PAGA
     - `hasStatus_comNullRetornaTodasAsCobrancas`
     - `vencimentoFrom_filtrarPorDataInicio` — data inicial exclui cobranças anteriores
     - `vencimentoTo_filtrarPorDataFim` — data final exclui cobranças posteriores
     - `emailEnviado_filtrarPorEmailEnviado` — true/false

**Exit criteria:** `mvn test` passa sem falhas.

**Commit message:** `test: CobrancaSpecificationTest`

---

## Chunk 5: CobrancaControllerTest

**Why:** `CobrancaController` expõe 7 endpoints e é o único controller do projeto sem cobertura de testes. CLAUDE.md regra 13 exige cenários obrigatórios: 201/200/401/403/404 por endpoint.

**Depends on:** nenhum (independente dos Chunks 1–4).

**Entry criteria:** All tests pass.

**Steps:**

1. Criar `src/test/java/com/pmrodrigues/cobranca/controller/CobrancaControllerTest.java`:
   - `@SpringBootTest(webEnvironment = RANDOM_PORT)`, `@AutoConfigureMockMvc`, `@ActiveProfiles("test")`
   - `@MockitoBean CobrancaService cobrancaService`
   - `@MockitoBean AsaasProperties asaasProperties` (stub `asaasProperties.apiKey()` → "valid-key")
   - `@MockitoBean JwtDecoder jwtDecoder`, `@MockitoBean TokenBlacklistService tokenBlacklistService`, `@MockitoBean UserDetailsServiceImpl userDetailsService`
   - Testes:
     - `gerar_asAdmin_returns201` — `@WithMockUser(roles="ADMIN")`, POST `/api/v1/cobrancas/gerar`, body GerarCobrancasDTO válido; stub `gerarCobrancas` → emptyList(); verifica 201
     - `gerar_unauthenticated_returns401` — sem @WithMockUser; verifica 401
     - `gerar_asUser_returns403` — `@WithMockUser` sem ADMIN; verifica 403
     - `filterBy_returns200` — `@WithMockUser`; GET `/api/v1/cobrancas`; stub `filterBy` → Page.empty(); verifica 200
     - `findById_returns200` — GET `/api/v1/cobrancas/1`; stub → cobrancaDTO; verifica 200
     - `findById_notFound_returns404` — stub lança 404; verifica 404
     - `cancelar_asAdmin_returns200` — `@WithMockUser(roles="ADMIN")`, POST `/api/v1/cobrancas/1/cancelar`; verifica 200
     - `cancelar_unauthenticated_returns401`
     - `cancelar_asUser_returns403`
     - `reenviarEmail_asAdmin_returns204` — `@WithMockUser(roles="ADMIN")`, POST `/api/v1/cobrancas/1/reenviar-email`; verifica 204
     - `reenviarEmail_unauthenticated_returns401`
     - `reenviarEmail_asUser_returns403`
     - `porApartamento_returns200` — GET `/api/v1/cobrancas/apartamentos/1/cobrancas`; verifica 200
     - `webhook_comTokenValido_returns200` — POST `/api/v1/cobrancas/webhook`, header `access_token: valid-key`; verifica 200
     - `webhook_comTokenInvalido_returns403` — header `access_token: wrong-key`; verifica 403

**Exit criteria:** `mvn test` passa sem falhas.

**Commit message:** `test: CobrancaControllerTest`

---

## Post-flight Checks
- [ ] Full test suite passes
- [ ] No TODO/FIXME markers left from refactoring
- [ ] `CotaRateioServiceTest` criado (5 testes)
- [ ] `AsaasGatewayServiceTest` criado (5 testes)
- [ ] `CobrancaService.softDeleteByCondominioId()` existe e é chamado por `CondominioService.delete()`
- [ ] `CobrancaServiceTest` criado (17 testes)
- [ ] `CobrancaSpecificationTest` criado (7 testes)
- [ ] `CobrancaControllerTest` criado (15 testes)
