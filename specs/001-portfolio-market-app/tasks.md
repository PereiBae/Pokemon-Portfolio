# Tasks: Pokemon Card Portfolio & Market Analytics

**Input**: Design documents from `specs/001-portfolio-market-app/`

**Prerequisites**: [plan.md](./plan.md), [spec.md](./spec.md), [research.md](./research.md), [data-model.md](./data-model.md), [contracts/](./contracts/), [quickstart.md](./quickstart.md)

**Tests**: Required. Business rules, provider adapters, controllers, repositories, scheduled jobs, and UI flows must be tested without live external API dependencies.

**Task Format**: Every executable task uses `- [ ] T### [P?] [US?] Description with file path`. Metadata below each task provides dependencies, verification, related requirements, and MVP/future status.

## Phase 1: Project Setup

- [X] T001 Initialize Maven Spring Boot project with Java 21 dependencies in `pom.xml`
  - Dependencies: None
  - Verification: Run `./mvnw -version` or `mvn -version` and confirm Java 21 plus Spring Boot dependencies resolve
  - Related: NFR-017
  - Status: MVP-critical

- [X] T002 Create Spring Boot source/resource/test directory structure in `src/main/java/com/pokemonportfolio/`
  - Dependencies: T001
  - Verification: Confirm package folders exist for catalog, portfolio, pricing, market_signal, alerts, grading, trade, forecasting, auth, and config
  - Related: NFR-014, NFR-015
  - Status: MVP-critical

- [X] T003 [P] Add default application configuration in `src/main/resources/application.yml`
  - Dependencies: T001
  - Verification: Confirm database, Thymeleaf, scheduler, and provider toggles are configurable without secrets
  - Related: NFR-003, NFR-017
  - Status: MVP-critical

- [X] T004 [P] Add test profile configuration in `src/test/resources/application-test.yml`
  - Dependencies: T001
  - Verification: Confirm tests can run against an isolated test database/profile with mock providers enabled
  - Related: NFR-007, NFR-008
  - Status: MVP-critical

- [X] T005 Create application entry point in `src/main/java/com/pokemonportfolio/PokemonPortfolioApplication.java`
  - Dependencies: T002
  - Verification: Run application context smoke test after T006
  - Related: NFR-017
  - Status: MVP-critical

- [X] T006 [P] Add application context smoke test in `src/test/java/com/pokemonportfolio/PokemonPortfolioApplicationTests.java`
  - Dependencies: T004, T005
  - Verification: Run `./mvnw test` or `mvn test` and confirm the context loads with test profile
  - Related: NFR-006
  - Status: MVP-critical

## Phase 2: Database and Flyway Setup

- [X] T007 Create initial auth and enum migration in `src/main/resources/db/migration/V1__auth_and_reference_data.sql`
  - Dependencies: T003
  - Verification: Flyway applies migration and creates `app_user` plus reference/check constraints
  - Related: FR-001, NFR-001, NFR-002
  - Status: MVP-critical

- [ ] T008 Create catalog migration in `src/main/resources/db/migration/V2__catalog_tables.sql`
  - Dependencies: T007
  - Verification: Flyway creates `pokemon_set`, `card`, and `sealed_product` tables with language/market and variant fields
  - Related: CA-001, CA-004, CA-005, CA-009, DR-001, DR-004
  - Status: MVP-critical

- [ ] T009 Create portfolio migration in `src/main/resources/db/migration/V3__owned_item.sql`
  - Dependencies: T008
  - Verification: Flyway creates `owned_item` with card/product references and no quantity column
  - Related: FR-006, DR-006, DR-009
  - Status: MVP-critical

- [ ] T010 Create pricing and exchange migration in `src/main/resources/db/migration/V4__pricing_and_exchange_snapshots.sql`
  - Dependencies: T009
  - Verification: Flyway creates `pricing_provider_result`, `manual_price_entry`, `exchange_rate_snapshot`, and `price_snapshot`
  - Related: PR-002, PR-007, PR-010, DR-010, DR-011, DR-012, DR-018
  - Status: MVP-critical

- [ ] T011 Create analytics migration in `src/main/resources/db/migration/V5__portfolio_signal_alerts_trade_grading_forecast.sql`
  - Dependencies: T010
  - Verification: Flyway creates `portfolio_valuation_snapshot`, `market_signal_snapshot`, `alert`, `trade_analysis`, `trade_side`, `trade_item`, `grading_fee`, `grading_analysis`, `grading_scenario`, `forecast_snapshot`, and `scheduled_job_run`
  - Related: PF-012, MS-007, PA-005, TA-001, GA-005, FC-007
  - Status: MVP-critical for portfolio snapshot and alerts; later-v1 for trade/grading/forecast

- [X] T012 Add Flyway repository schema test in `src/test/java/com/pokemonportfolio/config/FlywayMigrationTest.java`
  - Dependencies: T007, T008, T009, T010, T011
  - Verification: Run migration test and confirm all required tables exist
  - Related: NFR-011, NFR-019
  - Status: MVP-critical

## Phase 3: Authentication and Security

- [X] T013 [US1] Create `AppUser` entity and repository in `src/main/java/com/pokemonportfolio/auth/entity/AppUser.java`
  - Dependencies: T007
  - Verification: Repository test can persist and load a hashed-password owner account
  - Related: FR-001, NFR-001, NFR-002
  - Status: MVP-critical

- [X] T014 [US1] Implement owner user details service in `src/main/java/com/pokemonportfolio/auth/service/AppUserDetailsService.java`
  - Dependencies: T013
  - Verification: Service test loads enabled owner and rejects missing/disabled users
  - Related: FR-001, FR-002
  - Status: MVP-critical

- [X] T015 [US1] Configure Spring Security in `src/main/java/com/pokemonportfolio/config/SecurityConfig.java`
  - Dependencies: T014
  - Verification: Controller/security test confirms `/login` is public and app routes require authentication
  - Related: FR-002, NFR-001, NFR-004
  - Status: MVP-critical

- [X] T016 [US1] Add login controller and template in `src/main/java/com/pokemonportfolio/auth/controller/AuthController.java`
  - Dependencies: T015
  - Verification: `GET /login` renders the dark login page and login redirects to dashboard
  - Related: UI-002, UI-003, FR-001
  - Status: MVP-critical

- [X] T017 [US1] Add owner bootstrap workflow in `src/main/java/com/pokemonportfolio/auth/service/OwnerBootstrapService.java`
  - Dependencies: T013, T015
  - Verification: Application can create or seed one owner without committing plaintext secrets
  - Related: NFR-002, NFR-003
  - Status: MVP-critical

- [X] T018 [US1] Add authentication and authorization tests in `src/test/java/com/pokemonportfolio/auth/AuthSecurityTest.java`
  - Dependencies: T014, T015, T016, T017
  - Verification: Tests prove valid login works, invalid login fails, protected routes require auth, and password hash is used
  - Related: FR-001, FR-002, NFR-001, NFR-002
  - Status: MVP-critical

## Phase 4: Core Domain Model

- [X] T019 Create shared domain enums in `src/main/java/com/pokemonportfolio/config/domain/DomainEnums.java`
  - Dependencies: T002
  - Verification: Enums include language market, item type, variants, condition, graded status, confidence, signal classification, alert status, trade side, and forecast horizon
  - Related: DR-001, DR-002, DR-003
  - Status: MVP-critical

- [X] T020 [P] Create auditable base entity support in `src/main/java/com/pokemonportfolio/config/entity/AuditableEntity.java`
  - Dependencies: T002
  - Verification: Entity tests can use created/updated timestamp fields consistently
  - Related: NFR-019
  - Status: MVP-critical

- [X] T021 [P] Create currency and money value helpers in `src/main/java/com/pokemonportfolio/pricing/service/MoneyCalculationSupport.java`
  - Dependencies: T019
  - Verification: Unit tests can represent SGD and source-currency decimal values without floating point
  - Related: FR-016, DR-018, PR-007
  - Status: MVP-critical

- [X] T022 Add core domain unit tests in `src/test/java/com/pokemonportfolio/config/domain/DomainModelTest.java`
  - Dependencies: T019, T020, T021
  - Verification: Tests cover enum values, money rounding expectations, and audit timestamp behavior
  - Related: NFR-006
  - Status: MVP-critical

## Phase 5: Catalogue Module

- [ ] T023 [US2] Create catalog entities in `src/main/java/com/pokemonportfolio/catalog/entity/Card.java`
  - Dependencies: T008, T019, T020
  - Verification: Entities map `PokemonSet`, `Card`, and `SealedProduct` with market and variant fields
  - Related: CA-001, CA-004, CA-005, CA-009, DR-001, DR-004
  - Status: MVP-critical

- [ ] T024 [P] [US2] Create catalog repositories in `src/main/java/com/pokemonportfolio/catalog/repository/CardRepository.java`
  - Dependencies: T023
  - Verification: Repository tests find active English cards and sealed products by search text
  - Related: FR-003, FR-004, CA-007
  - Status: MVP-critical

- [ ] T025 [US2] Implement manual card/product search services in `src/main/java/com/pokemonportfolio/catalog/service/CardSearchService.java`
  - Dependencies: T024
  - Verification: Service tests return English card and sealed product results with visible market/variant/product type
  - Related: FR-003, FR-004, CA-011
  - Status: MVP-critical

- [ ] T026 [US2] Add catalog search controller in `src/main/java/com/pokemonportfolio/catalog/controller/CatalogController.java`
  - Dependencies: T025, T015
  - Verification: Controller test confirms authenticated user can search cards/products and unauthenticated access is denied
  - Related: FR-003, FR-004, FR-002
  - Status: MVP-critical

- [ ] T027 [P] [US2] Add local catalog seed data migration in `src/main/resources/db/migration/V6__seed_local_catalog_examples.sql`
  - Dependencies: T023
  - Verification: Seeded English card and sealed product appear in search results
  - Related: CA-001, CA-004, SC-001
  - Status: MVP-critical

- [ ] T028 [US2] Add catalogue repository/service/controller tests in `src/test/java/com/pokemonportfolio/catalog/CatalogModuleTest.java`
  - Dependencies: T024, T025, T026, T027
  - Verification: Tests cover manual card search, sealed product search, variant visibility, and auth protection
  - Related: FR-003, FR-004, CA-007, CA-011
  - Status: MVP-critical

- [X] T134 [US2] Add official card catalogue provider abstraction in `src/main/java/com/pokemonportfolio/catalog/provider/CardCatalogueProvider.java`
  - Dependencies: T023, T024
  - Verification: Business services depend on a catalogue provider interface and normalized official-card DTOs, not provider-specific response models
  - Related: FR-003, CA-001, CA-011, NFR-007, NFR-008
  - Status: MVP-critical before alerts/trade/grading/forecasting

- [X] T135 [US2] Implement Pokemon TCG API card metadata adapter in `src/main/java/com/pokemonportfolio/catalog/provider/PokemonTcgApiCardCatalogueProvider.java`
  - Dependencies: T134
  - Verification: Adapter uses WebClient, configurable base URL, optional API key from properties/environment, and maps official English card metadata without ingesting pricing data
  - Related: FR-003, CA-001, CA-011, NFR-003, NFR-007, NFR-008
  - Status: MVP-critical before alerts/trade/grading/forecasting

- [X] T136 [US2] Add verified card metadata migration in `src/main/resources/db/migration/V7__card_catalog_verification.sql`
  - Dependencies: T008, T023
  - Verification: Migration adds catalog source, verification status, external card ID, image URL, optional external URL, rarity, last synced timestamp, and unique provider/external-id index while preserving existing manual cards as MANUAL/UNVERIFIED
  - Related: DR-004, CA-001, CA-011, NFR-011
  - Status: MVP-critical before alerts/trade/grading/forecasting

- [X] T137 [US2] Implement official card search/import service and controller in `src/main/java/com/pokemonportfolio/catalog/service/OfficialCardCatalogueService.java` and `src/main/java/com/pokemonportfolio/catalog/controller/CatalogController.java`
  - Dependencies: T134, T135, T136, T015
  - Verification: Authenticated user can search official cards, import a VERIFIED local card, re-import without duplicates, and see a friendly provider-unavailable fallback
  - Related: FR-003, FR-005, CA-001, CA-011, NFR-007, NFR-010
  - Status: MVP-critical before alerts/trade/grading/forecasting

- [X] T138 [US2] Add official catalogue UI and unverified manual-card warning in `src/main/resources/templates/catalog/search.html` and `src/main/resources/templates/catalog/add-card.html`
  - Dependencies: T137, T090, T091
  - Verification: UI shows official card results with thumbnail, name, set, number, rarity, source, Import/Add to Portfolio actions, verified badges, and a clear warning for custom unverified cards
  - Related: UI-001, UI-015, CA-011
  - Status: MVP-critical before alerts/trade/grading/forecasting

- [X] T139 [US2] Add official catalogue provider/service/controller tests in `src/test/java/com/pokemonportfolio/catalog/` and `src/test/java/com/pokemonportfolio/catalog/provider/`
  - Dependencies: T134, T135, T136, T137, T138
  - Verification: Tests cover mocked WebClient mapping, official search DTOs, VERIFIED import, duplicate import reuse, manual MANUAL/UNVERIFIED creation, authenticated search page rendering, provider-unavailable friendly error, and add-to-portfolio for imported verified cards
  - Related: FR-003, FR-005, CA-011, NFR-006, NFR-007, NFR-008
  - Status: MVP-critical before alerts/trade/grading/forecasting

## Phase 6: Portfolio Module

- [X] T029 [US2] Create `OwnedItem` entity and repository in `src/main/java/com/pokemonportfolio/portfolio/entity/OwnedItem.java`
  - Dependencies: T009, T013, T023
  - Verification: Repository test persists separate owned copies for the same card with different purchase details
  - Related: FR-005, FR-006, DR-006, DR-009
  - Status: MVP-critical

- [X] T030 [US2] Create owned item form/view models in `src/main/java/com/pokemonportfolio/portfolio/service/OwnedItemForm.java`
  - Dependencies: T029
  - Verification: Validation test rejects missing item type, purchase price, condition, purchase date, and catalog reference
  - Related: DR-006, DR-007
  - Status: MVP-critical

- [X] T031 [US2] Implement owned item service in `src/main/java/com/pokemonportfolio/portfolio/service/OwnedItemService.java`
  - Dependencies: T029, T030
  - Verification: Service test creates two separate portfolio rows for two copies and never aggregates quantity
  - Related: FR-005, FR-006, PF-010
  - Status: MVP-critical

- [X] T032 [US2] Add portfolio controller for create/edit/archive in `src/main/java/com/pokemonportfolio/portfolio/controller/PortfolioController.java`
  - Dependencies: T031, T026
  - Verification: Controller test creates an owned item from a catalog card and redirects to portfolio detail
  - Related: FR-005, FR-006, FR-002
  - Status: MVP-critical

- [X] T033 [US2] Add portfolio service tests in `src/test/java/com/pokemonportfolio/portfolio/OwnedItemServiceTest.java`
  - Dependencies: T031
  - Verification: Tests cover card item, sealed product item, graded PSA fields, optional notes, and no storage location requirement
  - Related: DR-006, DR-007, DR-008, DR-009
  - Status: MVP-critical

- [X] T034 [US3] Create dashboard summary model skeleton in `src/main/java/com/pokemonportfolio/portfolio/service/PortfolioDashboardView.java`
  - Dependencies: T029
  - Verification: Unit test can assemble empty dashboard summary for authenticated owner
  - Related: FR-017, PF-001, PF-002
  - Status: MVP-critical

## Phase 7: Pricing Provider Abstraction

- [ ] T035 [US4] Create pricing provider result entity and repository in `src/main/java/com/pokemonportfolio/pricing/entity/PricingProviderResult.java`
  - Dependencies: T010, T023
  - Verification: Repository test stores provider name, source price, source currency, observed time, status, and metadata
  - Related: PR-002, PR-007, DR-010
  - Status: MVP-critical

- [X] T036 [US4] Define pricing provider adapter contract in `src/main/java/com/pokemonportfolio/pricing/provider/PricingProviderAdapter.java`
  - Dependencies: T019, T035
  - Verification: Contract exposes provider name, supported contexts, and normalized fetch result without calculating final market value
  - Related: PR-012, NFR-007
  - Status: MVP-critical

- [X] T037 [US4] Implement provider orchestration service in `src/main/java/com/pokemonportfolio/pricing/service/PricingProviderService.java`
  - Dependencies: T036
  - Verification: Service test invokes enabled adapters, stores usable results, records unavailable sources, and does not crash when one adapter fails
  - Related: PR-013, NFR-009, NFR-010
  - Status: MVP-critical

- [X] T038 [US4] Add manual price entry fallback service in `src/main/java/com/pokemonportfolio/pricing/service/ManualPriceEntryService.java`
  - Dependencies: T035
  - Verification: Service test stores manual price as auditable provider-like input with source note and LOW default confidence
  - Related: PR-002, PR-013, DR-019
  - Status: MVP-critical

- [ ] T039 [US4] Add provider adapter contract tests in `src/test/java/com/pokemonportfolio/pricing/provider/PricingProviderAdapterContractTest.java`
  - Dependencies: T036, T037
  - Verification: Tests prove adapters return normalized results and provider failures are mockable without live APIs
  - Related: NFR-007, NFR-008
  - Status: MVP-critical

- [ ] T040 [P] Document future real provider adapter placeholders in `specs/001-portfolio-market-app/contracts/provider-adapters.md`
  - Dependencies: T036
  - Verification: Contract notes TCGPlayer, eBay, and PriceCharting are future disabled adapters until permitted access is configured
  - Related: PR-012, NFR-020
  - Status: Future-phase

## Phase 8: Mock Pricing Provider

- [X] T041 [US4] Implement deterministic mock pricing provider in `src/main/java/com/pokemonportfolio/pricing/provider/MockPricingProviderAdapter.java`
  - Dependencies: T036
  - Verification: Mock provider returns stable source prices for seeded card and sealed product catalog records
  - Related: PR-012, NFR-007, NFR-008
  - Status: MVP-critical

- [ ] T042 [P] [US4] Add mock pricing fixture data in `src/test/resources/fixtures/mock-pricing-results.json`
  - Dependencies: T041
  - Verification: Fixture contains card and sealed product examples with source currency, listing counts, recent sales count, and confidence metadata
  - Related: DR-010, PR-007
  - Status: MVP-critical

- [X] T043 [US4] Add mock provider tests in `src/test/java/com/pokemonportfolio/pricing/provider/MockPricingProviderAdapterTest.java`
  - Dependencies: T041, T042
  - Verification: Tests prove local pricing works with no external network or live provider API
  - Related: NFR-007, NFR-008
  - Status: MVP-critical

- [X] T044 [US4] Add provider enablement configuration in `src/main/java/com/pokemonportfolio/pricing/provider/PricingProviderProperties.java`
  - Dependencies: T041
  - Verification: Test profile enables mock provider and disables real provider adapters by default
  - Related: NFR-003, NFR-008
  - Status: MVP-critical

## Phase 9: Price Snapshot and Valuation Engine

- [X] T045 [US4] Create price snapshot entity and repository in `src/main/java/com/pokemonportfolio/pricing/entity/PriceSnapshot.java`
  - Dependencies: T010, T035
  - Verification: Repository test inserts multiple snapshots for the same item and confirms previous rows remain
  - Related: PR-010, PR-011, DR-012
  - Status: MVP-critical

- [X] T046 [US4] Implement append-only price snapshot service in `src/main/java/com/pokemonportfolio/pricing/service/PriceSnapshotService.java`
  - Dependencies: T045
  - Verification: Service test creates new snapshots on repeated refreshes and never updates prior snapshot values
  - Related: PR-010, PR-011, AC-007
  - Status: MVP-critical

- [ ] T047 [US4] Implement pricing hierarchy rules in `src/main/java/com/pokemonportfolio/pricing/service/PricingHierarchyService.java`
  - Dependencies: T035
  - Verification: Unit tests cover English raw, English graded, Japanese, and sealed product priority order
  - Related: PR-003, PR-004, PR-005, PR-006
  - Status: MVP-critical

- [ ] T048 [US4] Implement confidence rating service in `src/main/java/com/pokemonportfolio/pricing/service/ConfidenceRatingService.java`
  - Dependencies: T035
  - Verification: Unit tests cover source count, recent sales count, sale age, source disagreement, listing liquidity, volatility, and missing data
  - Related: PR-008, PR-009, SC-004
  - Status: MVP-critical

- [X] T049 [US4] Implement market valuation service in `src/main/java/com/pokemonportfolio/pricing/service/MarketValuationService.java`
  - Dependencies: T037, T046, T047, T048
  - Verification: Service test calculates SGD market price from SGD-only mock provider results for Vertical Slice 1 and stores auditable explanation; non-SGD provider conversion is integrated by T127
  - Related: PR-001, PR-002, PR-007, PR-014, PR-015, MS-001
  - Status: MVP-critical

- [ ] T050 [US4] Add valuation business-rule tests in `src/test/java/com/pokemonportfolio/pricing/service/MarketValuationServiceTest.java`
  - Dependencies: T047, T048, T049
  - Verification: Tests cover price calculation, confidence, source unavailability, source disagreement, and snapshot preservation
  - Related: PR-007, PR-008, PR-013, PR-015, NFR-006
  - Status: MVP-critical

## Phase 10: Exchange Rate Handling

- [X] T051 [US4] Create exchange rate snapshot entity and repository in `src/main/java/com/pokemonportfolio/pricing/entity/ExchangeRateSnapshot.java`
  - Dependencies: T010
  - Verification: Repository test stores USD-to-SGD and other source-to-SGD snapshots with effective timestamps
  - Related: FR-016, DR-018, PR-007
  - Status: MVP-critical

- [ ] T052 [US4] Define exchange rate provider adapter in `src/main/java/com/pokemonportfolio/pricing/provider/ExchangeRateProviderAdapter.java`
  - Dependencies: T051
  - Verification: Contract test confirms provider output includes rate, source/provenance, effective timestamp, fetched timestamp, and confidence
  - Related: DR-018, NFR-007
  - Status: MVP-critical

- [ ] T053 [US4] Implement mock exchange rate provider in `src/main/java/com/pokemonportfolio/pricing/provider/MockExchangeRateProviderAdapter.java`
  - Dependencies: T052
  - Verification: Test profile returns deterministic USD-to-SGD rate and requires no live external API
  - Related: FR-016, NFR-008
  - Status: MVP-critical

- [X] T054 [US4] Implement currency conversion service in `src/main/java/com/pokemonportfolio/pricing/service/CurrencyConversionService.java`
  - Dependencies: T051, T052, T053
  - Verification: Unit tests convert source prices to SGD and reject final display when no auditable rate exists
  - Related: FR-016, PR-007, DR-018, EC-003
  - Status: MVP-critical

- [X] T055 [US4] Add currency conversion tests in `src/test/java/com/pokemonportfolio/pricing/service/CurrencyConversionServiceTest.java`
  - Dependencies: T054
  - Verification: Tests cover SGD passthrough, USD conversion, missing rate, stale rate, and metadata preservation
  - Related: FR-016, SC-003, SC-004
  - Status: MVP-critical

- [ ] T127 [US4] Integrate full currency conversion into market valuation service in `src/main/java/com/pokemonportfolio/pricing/service/MarketValuationService.java`
  - Dependencies: T049, T054, T055
  - Verification: Service tests convert USD/non-SGD provider results to SGD, preserve source currency and exchange-rate audit fields on valuation inputs/snapshots, and reduce confidence or reject display when an auditable rate is missing
  - Related: FR-016, PR-007, PR-015, DR-018, SC-003
  - Status: MVP-critical before non-SGD providers; Vertical Slice 1 uses SGD-only mock pricing data

## Phase 11: Portfolio Valuation Snapshots

- [X] T056 [US3] Create portfolio valuation snapshot entity and repository in `src/main/java/com/pokemonportfolio/portfolio/entity/PortfolioValuationSnapshot.java`
  - Dependencies: T011, T029, T045
  - Verification: Repository test stores multiple daily snapshots for the owner without overwriting history
  - Related: FR-008, PF-012, DR-013
  - Status: MVP-critical

- [X] T057 [US3] Implement portfolio valuation service in `src/main/java/com/pokemonportfolio/portfolio/service/PortfolioValuationService.java`
  - Dependencies: T029, T045, T056
  - Verification: Service test calculates total value, cost basis, unrealized gain/loss, percent, item count, and low-confidence count
  - Related: PF-001, PF-002, PF-003, PF-004, PF-008, PF-014
  - Status: MVP-critical

- [X] T058 [US3] Implement dashboard summary service in `src/main/java/com/pokemonportfolio/portfolio/service/PortfolioDashboardService.java`
  - Dependencies: T034, T057
  - Verification: Service test returns total value, cost basis, gain/loss, top gainers, top losers, low-confidence valuations, and alerts placeholders
  - Related: FR-017, PF-001, PF-006, PF-007, PF-008, PF-009
  - Status: MVP-critical

- [X] T059 [US3] Add portfolio valuation tests in `src/test/java/com/pokemonportfolio/portfolio/service/PortfolioValuationServiceTest.java`
  - Dependencies: T057, T058
  - Verification: Tests cover current market value use, SGD totals, low-confidence count, and historical snapshot preservation
  - Related: PR-014, PF-012, SC-002, SC-005
  - Status: MVP-critical

## Phase 12: Market Signal Engine

- [ ] T060 [US4] Create market signal snapshot entity and repository in `src/main/java/com/pokemonportfolio/market_signal/entity/MarketSignalSnapshot.java`
  - Dependencies: T011, T045
  - Verification: Repository test stores expected price separately from market price with classification and explanation
  - Related: MS-002, MS-007, DR-019
  - Status: Later-v1

- [ ] T061 [US4] Implement supply signal service in `src/main/java/com/pokemonportfolio/market_signal/service/SupplySignalService.java`
  - Dependencies: T060
  - Verification: Unit tests cover pull cost, months since release lifecycle, active listing count, and supply shift index
  - Related: MS-010, MS-011, MS-012, MS-013
  - Status: Later-v1

- [ ] T062 [US4] Implement demand signal service in `src/main/java/com/pokemonportfolio/market_signal/service/DemandSignalService.java`
  - Dependencies: T060
  - Verification: Unit tests cover character premium, universal appeal, manual card appeal, and grading intensity placeholders
  - Related: MS-014, MS-015, MS-016, MS-017
  - Status: Later-v1

- [ ] T063 [US4] Implement market modifier service in `src/main/java/com/pokemonportfolio/market_signal/service/MarketModifierService.java`
  - Dependencies: T060
  - Verification: Unit tests cover demand pressure, rip-risk premium, grading lag adjustment, and PSA gem rate
  - Related: MS-018, MS-019, MS-020, MS-021
  - Status: Later-v1

- [ ] T064 [US4] Implement market signal service in `src/main/java/com/pokemonportfolio/market_signal/service/MarketSignalService.java`
  - Dependencies: T061, T062, T063, T045
  - Verification: Service test produces Market Price, Expected Price, SGD difference, percent difference, classification, confidence, and explanation
  - Related: FR-010, FR-011, MS-001, MS-002, MS-005, MS-006, MS-007, MS-022
  - Status: Later-v1

- [ ] T065 [US4] Add expected-price classification tests in `src/test/java/com/pokemonportfolio/market_signal/service/MarketSignalServiceTest.java`
  - Dependencies: T064
  - Verification: Tests cover undervalued, fairly valued, overvalued, low-confidence, and explanation cases
  - Related: MS-006, MS-007, SC-004, AC-009
  - Status: Later-v1

## Phase 13: Price Alerts

- [ ] T066 [US5] Create alert entity and repository in `src/main/java/com/pokemonportfolio/alerts/entity/Alert.java`
  - Dependencies: T011, T029, T045
  - Verification: Repository test stores alert status, trigger snapshot, purchase price, current market value, gain, date, and confidence
  - Related: PA-001, PA-005, DR-014
  - Status: Later-v1

- [ ] T067 [US5] Implement alert evaluation service in `src/main/java/com/pokemonportfolio/alerts/service/AlertEvaluationService.java`
  - Dependencies: T066, T045, T029
  - Verification: Service test triggers SGD 10 gain alert below SGD 100 and SGD 25 gain alert at or above SGD 100
  - Related: PA-002, PA-003, PA-009, PA-010
  - Status: Later-v1

- [ ] T068 [US5] Implement alert view service in `src/main/java/com/pokemonportfolio/alerts/service/AlertViewService.java`
  - Dependencies: T066
  - Verification: Service test separates new, active, and historical alerts
  - Related: PA-006, PA-007, PA-008
  - Status: Later-v1

- [ ] T069 [US5] Add alert controller in `src/main/java/com/pokemonportfolio/alerts/controller/AlertController.java`
  - Dependencies: T068, T015
  - Verification: Controller test lists alerts and acknowledges/dismisses alerts only for authenticated owner
  - Related: PA-006, PA-007, PA-008, FR-002
  - Status: Later-v1

- [ ] T070 [US5] Add alert business-rule tests in `src/test/java/com/pokemonportfolio/alerts/service/AlertEvaluationServiceTest.java`
  - Dependencies: T067
  - Verification: Tests cover thresholds, gain percentage, source confidence, dedupe on rerun, and no alert below threshold
  - Related: PA-002, PA-003, PA-005, EC-008
  - Status: Later-v1

## Phase 14: Trade Analyzer

- [ ] T071 [US6] Create trade entities and repositories in `src/main/java/com/pokemonportfolio/trade/entity/TradeAnalysis.java`
  - Dependencies: T011, T023, T045
  - Verification: Repository test persists trade analysis with exactly USER and OTHER_PARTY sides and trade items
  - Related: TA-001, TA-002, DR-015
  - Status: Later-v1

- [ ] T072 [US6] Implement trade analyzer service in `src/main/java/com/pokemonportfolio/trade/service/TradeAnalyzerService.java`
  - Dependencies: T071, T045
  - Verification: Service test calculates adjusted values, side totals, net difference, fairness, confidence, and LOW-confidence warning
  - Related: TA-003, TA-004, TA-008, TA-009, TA-011, TA-012, TA-013, TA-014, TA-015
  - Status: Later-v1

- [ ] T073 [US6] Add trade controller in `src/main/java/com/pokemonportfolio/trade/controller/TradeController.java`
  - Dependencies: T072, T015
  - Verification: Controller test renders trade form and returns analysis result for authenticated owner
  - Related: FR-013, TA-005, TA-006, TA-007
  - Status: Later-v1

- [ ] T074 [US6] Add trade calculation tests in `src/test/java/com/pokemonportfolio/trade/service/TradeAnalyzerServiceTest.java`
  - Dependencies: T072
  - Verification: Tests cover 80 percent example, independent side percentages, low confidence, and card/sealed product mix
  - Related: TA-002, TA-003, TA-004, AC-012
  - Status: Later-v1

## Phase 15: Grading Analyzer

- [ ] T075 [US7] Create grading entities and repositories in `src/main/java/com/pokemonportfolio/grading/entity/GradingAnalysis.java`
  - Dependencies: T011, T029, T045
  - Verification: Repository test persists grading fee, grading analysis, and PSA 8/9/10 scenarios
  - Related: GA-003, GA-005, GA-006, GA-007, DR-016, DR-017
  - Status: Later-v1

- [ ] T076 [US7] Implement grading fee service in `src/main/java/com/pokemonportfolio/grading/service/GradingFeeService.java`
  - Dependencies: T075
  - Verification: Service test creates and updates PSA fee/turnaround data without hardcoded permanent values
  - Related: GA-012, GA-013
  - Status: Later-v1

- [ ] T077 [US7] Implement manual grading analyzer service in `src/main/java/com/pokemonportfolio/grading/service/GradingAnalyzerService.java`
  - Dependencies: T075, T076, T045
  - Verification: Service test manually produces conservative PSA 8, balanced PSA 9, and aggressive PSA 10 scenarios
  - Related: GA-001, GA-002, GA-004, GA-005, GA-006, GA-007, GA-008
  - Status: Later-v1

- [ ] T078 [US7] Add grading controller in `src/main/java/com/pokemonportfolio/grading/controller/GradingController.java`
  - Dependencies: T077, T015
  - Verification: Controller test renders grading form and runs analysis only on user request
  - Related: FR-014, GA-001, GA-002
  - Status: Later-v1

- [ ] T079 [US7] Add grading scenario tests in `src/test/java/com/pokemonportfolio/grading/service/GradingAnalyzerServiceTest.java`
  - Dependencies: T077
  - Verification: Tests cover profit above SGD 50, not profitable at SGD 50 or below, raw greater than PSA 8/9 warnings, ROI, and confidence
  - Related: GA-009, GA-010, GA-011, GA-014, AC-013
  - Status: Later-v1

## Phase 16: Forecasting v1

- [ ] T080 [US8] Create forecast snapshot entity and repository in `src/main/java/com/pokemonportfolio/forecasting/entity/ForecastSnapshot.java`
  - Dependencies: T011, T023, T045
  - Verification: Repository test stores 30, 90, 180, and 365 day advisory forecast snapshots
  - Related: FC-003, FC-004, FC-005, FC-006, FC-007
  - Status: Later-v1

- [ ] T081 [US8] Implement rules-based forecast service in `src/main/java/com/pokemonportfolio/forecasting/service/ForecastService.java`
  - Dependencies: T080, T045, T060
  - Verification: Service test generates expected value, range, confidence, and explanation without ML
  - Related: FC-001, FC-002, FC-007, FC-008, FC-009, FC-010, FC-012, FC-013
  - Status: Later-v1

- [ ] T082 [US8] Add forecast controller in `src/main/java/com/pokemonportfolio/forecasting/controller/ForecastController.java`
  - Dependencies: T081, T015
  - Verification: Controller test accepts forecast horizon and returns advisory result page for authenticated owner
  - Related: FR-015, FC-001, FC-003, FC-004, FC-005, FC-006
  - Status: Later-v1

- [ ] T083 [US8] Add forecast output tests in `src/test/java/com/pokemonportfolio/forecasting/service/ForecastServiceTest.java`
  - Dependencies: T081
  - Verification: Tests cover all horizons, sparse data confidence reduction, advisory explanation, and range generation
  - Related: FC-001, FC-008, FC-009, FC-010, AC-014
  - Status: Later-v1

## Phase 17: Scheduled Jobs

- [ ] T084 Create scheduled job run entity and repository in `src/main/java/com/pokemonportfolio/config/entity/ScheduledJobRun.java`
  - Dependencies: T011
  - Verification: Repository test records job type, scheduled key, start/end, status, items processed, errors, and summary
  - Related: FR-024, NFR-018
  - Status: MVP-critical

- [X] T085 [US4] Implement daily price refresh job in `src/main/java/com/pokemonportfolio/pricing/scheduler/PriceRefreshJob.java`
  - Dependencies: T036, T037, T041, T044, T049, T084
  - Verification: Scheduled job test runs twice with enabled SGD-only MockPricingProvider data and confirms new snapshots are appended without overwriting prior rows; non-SGD refresh behavior is covered after T127
  - Related: FR-019, FR-024, PR-010, PR-011, PR-012, NFR-018
  - Status: MVP-critical

- [X] T086 [US3] Implement daily portfolio valuation job in `src/main/java/com/pokemonportfolio/portfolio/scheduler/PortfolioValuationJob.java`
  - Dependencies: T057, T084
  - Verification: Scheduled job test creates portfolio valuation snapshot and preserves previous snapshots on rerun
  - Related: FR-020, PF-012, NFR-018
  - Status: MVP-critical

- [ ] T087 [US5] Implement daily alert check job in `src/main/java/com/pokemonportfolio/alerts/scheduler/AlertCheckJob.java`
  - Dependencies: T067, T084
  - Verification: Scheduled job test creates eligible alerts and dedupes repeated runs
  - Related: FR-021, PA-009, NFR-018
  - Status: Later-v1

- [ ] T088 Implement exchange rate refresh job in `src/main/java/com/pokemonportfolio/pricing/scheduler/ExchangeRateRefreshJob.java`
  - Dependencies: T054, T084
  - Verification: Scheduled job test stores exchange-rate snapshots, is safe to rerun, and requires no live external API when mock exchange rates are enabled
  - Related: FR-022, DR-018, NFR-008, NFR-018
  - Status: Later-v1

- [ ] T129 Implement grading fee refresh job in `src/main/java/com/pokemonportfolio/grading/scheduler/GradingFeeRefreshJob.java`
  - Dependencies: T076, T084
  - Verification: Scheduled job test refreshes or updates PSA fee/turnaround data, is safe to rerun, and keeps grading concerns separate from pricing refresh concerns
  - Related: FR-023, GA-012, GA-013, NFR-008, NFR-018
  - Status: Later-v1

- [ ] T089 Add MVP scheduled job rerun-safety tests in `src/test/java/com/pokemonportfolio/config/scheduler/ScheduledJobRerunSafetyTest.java`
  - Dependencies: T085, T086
  - Verification: Tests cover rerunnable price refresh and portfolio valuation jobs with SGD-only mock pricing data, append-only price snapshots, append-only portfolio valuation snapshots, and provider failure handling
  - Related: FR-019, FR-020, FR-024, EC-007, NFR-018
  - Status: MVP-critical

- [ ] T130 Add later-v1 scheduled job regression tests in `src/test/java/com/pokemonportfolio/config/scheduler/ScheduledJobRegressionTest.java`
  - Dependencies: T087, T088, T129
  - Verification: Tests cover alert checks, exchange-rate refresh, grading-fee refresh, rerun safety, dedupe behavior, and no live external API dependency
  - Related: FR-021, FR-022, FR-023, FR-024, PA-009, NFR-018
  - Status: Later-v1

## Phase 18: UI Layout and Styling

- [X] T090 Create Thymeleaf base layout in `src/main/resources/templates/fragments/layout.html`
  - Dependencies: T016
  - Verification: Login and dashboard pages can extend shared dark layout with navigation placeholder
  - Related: UI-001, UI-002, UI-003
  - Status: MVP-critical

- [X] T091 Create dark financial dashboard stylesheet in `src/main/resources/static/css/app.css`
  - Dependencies: T090
  - Verification: CSS defines dark surfaces, compact tables, cards, badges, green/red gain/loss indicators, and avoids toy-like colors
  - Related: UI-001, UI-002, UI-004, UI-008, UI-009, UI-021
  - Status: MVP-critical

- [ ] T092 [P] Create reusable UI fragments in `src/main/resources/templates/fragments/components.html`
  - Dependencies: T090
  - Verification: Fragments exist for summary card, confidence badge, signal badge, alert panel, and compact table header
  - Related: UI-006, UI-010, UI-011, UI-012
  - Status: MVP-critical

- [ ] T093 [P] Create lightweight chart helper in `src/main/resources/static/js/charts.js`
  - Dependencies: T090
  - Verification: Helper can render line chart data from server-provided values without requiring a separate frontend app
  - Related: UI-007, NFR-017
  - Status: MVP-critical

- [ ] T094 Add UI style smoke test in `src/test/java/com/pokemonportfolio/config/ui/UiLayoutSmokeTest.java`
  - Dependencies: T090, T091, T092
  - Verification: Test confirms pages include dark layout, SGD text hooks, badges, and no public app routes render unauthenticated
  - Related: UI-001, UI-002, UI-023, FR-002
  - Status: MVP-critical

## Phase 19: Dashboard UI

- [X] T095 [US3] Add dashboard controller in `src/main/java/com/pokemonportfolio/portfolio/controller/DashboardController.java`
  - Dependencies: T058, T015
  - Verification: Controller test returns dashboard view model for authenticated owner
  - Related: FR-017, PF-001, PF-002, PF-003, PF-004
  - Status: MVP-critical

- [X] T096 [US3] Create dashboard template in `src/main/resources/templates/dashboard/index.html`
  - Dependencies: T090, T091, T092, T093, T095
  - Verification: Page shows total value, cost basis, gain/loss, value chart, top gainers/losers, low-confidence valuations, and alerts in SGD
  - Related: UI-005, UI-006, UI-007, UI-009, PF-001, PF-009
  - Status: MVP-critical

- [X] T097 [US3] Add dashboard controller/UI tests in `src/test/java/com/pokemonportfolio/portfolio/controller/DashboardControllerTest.java`
  - Dependencies: T095, T096
  - Verification: Tests confirm dashboard renders all MVP cards, SGD labels, confidence badges, and dark dashboard layout
  - Related: AC-005, SC-002, UI-023
  - Status: MVP-critical

- [ ] T128 [US3] Compute and display item-level portfolio contribution to growth in `src/main/java/com/pokemonportfolio/portfolio/service/PortfolioDashboardService.java`
  - Dependencies: T057, T058, T096, T097
  - Verification: Service/UI tests compute each owned item's contribution to portfolio growth from stored valuation snapshots and display contribution data on the dashboard without recalculating destructively from current prices only
  - Related: PF-013, FR-017, DR-013, UI-006
  - Status: Later-v1 after Vertical Slice 1 valuation snapshots

## Phase 20: Catalogue and Portfolio UI

- [ ] T098 [US2] Create catalog search and add-to-portfolio templates in `src/main/resources/templates/catalog/search.html`
  - Dependencies: T026, T032, T090, T091
  - Verification: User can search card/product, see market/variant/product type, and open add form
  - Related: FR-003, FR-004, FR-005, CA-011, UI-015
  - Status: MVP-critical

- [X] T099 [US2] Create portfolio list and detail templates in `src/main/resources/templates/portfolio/list.html`
  - Dependencies: T032, T058, T090, T091, T092
  - Verification: Portfolio table shows owned copies separately with condition, purchase price, market value placeholder, gain/loss, confidence, and SGD labels
  - Related: FR-005, FR-006, PF-010, PF-011, UI-008, UI-023
  - Status: MVP-critical

- [ ] T100 [US2] Add catalogue/portfolio UI flow tests in `src/test/java/com/pokemonportfolio/portfolio/controller/PortfolioControllerTest.java`
  - Dependencies: T098, T099
  - Verification: Tests cover search, add card, add sealed product, separate copies, and authenticated-only access
  - Related: SC-001, AC-003, AC-004
  - Status: MVP-critical

## Phase 21: Price History UI

- [ ] T101 [US4] Add pricing controller for history and manual price entry in `src/main/java/com/pokemonportfolio/pricing/controller/PricingController.java`
  - Dependencies: T038, T046, T127, T015
  - Verification: Controller test renders price history and exposes GET/POST manual price entry endpoints only for authenticated owner; POST delegates to append-only snapshot creation
  - Related: FR-009, PR-013, PR-015, UI-016
  - Status: MVP-critical after Vertical Slice 1 for price history/manual fallback; later-v1 for richer history

- [ ] T102 [US4] Create price history template in `src/main/resources/templates/pricing/history.html`
  - Dependencies: T090, T091, T092, T093, T101
  - Verification: Page shows line chart, snapshot table, source currency, exchange rate summary, confidence, Market Price, Expected Price placeholders, and SGD labels
  - Related: FR-009, FR-010, UI-007, UI-011, UI-023, AC-006
  - Status: MVP-critical after Vertical Slice 1

- [X] T131 [US4] Create manual price entry template in `src/main/resources/templates/pricing/manual-entry.html`
  - Dependencies: T090, T091, T092, T101
  - Verification: Page supports source price, source currency, exchange-rate audit fields, source note, confidence metadata, and local fallback submission when external providers are unavailable
  - Related: PR-002, PR-007, PR-013, DR-018, DR-019, UI-016
  - Status: MVP-critical local fallback

- [ ] T103 [US4] Add price history UI tests in `src/test/java/com/pokemonportfolio/pricing/controller/PricingControllerTest.java`
  - Dependencies: T101, T102
  - Verification: Tests confirm historical snapshots render from stored rows and old snapshots remain visible after new snapshot
  - Related: PR-010, PR-011, AC-006, AC-007
  - Status: MVP-critical after Vertical Slice 1

- [X] T132 [US4] Add manual price entry service/controller tests in `src/test/java/com/pokemonportfolio/pricing/service/ManualPriceEntryServiceTest.java` and `src/test/java/com/pokemonportfolio/pricing/controller/ManualPriceEntryControllerTest.java`
  - Dependencies: T038, T046, T054, T101, T131
  - Verification: Tests cover GET and POST manual price entry, append-only price snapshot creation, preserved source currency, preserved exchange-rate audit fields, default LOW confidence when appropriate, and local fallback behavior when external providers are unavailable
  - Related: PR-002, PR-007, PR-010, PR-011, PR-013, DR-018, DR-019
  - Status: MVP-critical local fallback

## Phase 22: Alerts UI

- [ ] T104 [US5] Create alerts template in `src/main/resources/templates/alerts/index.html`
  - Dependencies: T069, T090, T091, T092
  - Verification: Page shows new, active, and historical alert panels with item name, purchase price, current value, gain, date, and confidence
  - Related: PA-005, PA-006, PA-007, PA-008, UI-012
  - Status: Later-v1

- [ ] T105 [US5] Add alerts UI tests in `src/test/java/com/pokemonportfolio/alerts/controller/AlertControllerTest.java`
  - Dependencies: T069, T104
  - Verification: Tests confirm alert panels render and acknowledge/dismiss actions update status
  - Related: AC-010, AC-011, PA-006, PA-007, PA-008
  - Status: Later-v1

## Phase 23: Trade Analyzer UI

- [ ] T106 [US6] Create trade analyzer template in `src/main/resources/templates/trade/index.html`
  - Dependencies: T073, T090, T091, T092
  - Verification: Page shows two trade sides, item selectors, independent trade percentages, adjusted totals, net difference, fairness, confidence, and LOW warning
  - Related: UI-013, TA-001, TA-003, TA-011, TA-014
  - Status: Later-v1

- [ ] T107 [US6] Add trade UI tests in `src/test/java/com/pokemonportfolio/trade/controller/TradeControllerTest.java`
  - Dependencies: T073, T106
  - Verification: Tests submit a two-sided trade and verify adjusted value result and confidence warning display
  - Related: AC-012, TA-004, TA-014
  - Status: Later-v1

## Phase 24: Grading Analyzer UI

- [ ] T108 [US7] Create grading analyzer template in `src/main/resources/templates/grading/index.html`
  - Dependencies: T078, T090, T091, T092
  - Verification: Page shows manual trigger form, PSA 8/9/10 scenario table, ROI, recommendation, confidence, and raw-vs-graded warnings
  - Related: UI-014, GA-005, GA-006, GA-007, GA-008, GA-010, GA-011
  - Status: Later-v1

- [ ] T109 [US7] Add grading UI tests in `src/test/java/com/pokemonportfolio/grading/controller/GradingControllerTest.java`
  - Dependencies: T078, T108
  - Verification: Tests confirm no automatic analysis, manual submit renders scenarios, and non-profitable recommendation is shown correctly
  - Related: GA-001, GA-002, GA-009, AC-013
  - Status: Later-v1

## Phase 25: Forecast UI

- [ ] T110 [US8] Create forecast template in `src/main/resources/templates/forecasting/index.html`
  - Dependencies: T082, T090, T091, T092
  - Verification: Page shows horizon selector, advisory language, expected value, range, confidence, and explanation
  - Related: UI-016, FC-001, FC-003, FC-004, FC-005, FC-006, FC-010
  - Status: Later-v1

- [ ] T111 [US8] Add forecast UI tests in `src/test/java/com/pokemonportfolio/forecasting/controller/ForecastControllerTest.java`
  - Dependencies: T082, T110
  - Verification: Tests cover all horizons and confirm advisory/non-guaranteed wording appears
  - Related: FC-001, FC-002, AC-014
  - Status: Later-v1

## Phase 26: Settings UI

- [X] T112 Add settings controller in `src/main/java/com/pokemonportfolio/config/controller/SettingsController.java`
  - Dependencies: T015, T044, T076
  - Verification: Controller test requires authentication/admin role and never exposes secret values
  - Related: NFR-003, NFR-004
  - Status: Later-v1

- [X] T113 Create provider settings template in `src/main/resources/templates/settings/providers.html`
  - Dependencies: T112, T090, T091
  - Verification: Page shows mock/manual provider status, disabled real provider placeholders, and API key configured/not-configured state without secrets
  - Related: PR-012, NFR-003, NFR-020
  - Status: Later-v1

- [X] T133 Add provider settings service and POST toggle controller tests in `src/main/java/com/pokemonportfolio/config/service/ProviderSettingsService.java`
  - Dependencies: T044, T112, T113
  - Verification: Service/controller tests confirm MockPricingProvider is enabled for local development, real providers are disabled by default, POST toggle behavior updates provider enablement, and no secret values are rendered or persisted in plaintext
  - Related: PR-012, NFR-003, NFR-008, NFR-020
  - Status: Later-v1

- [ ] T114 Create grading fee settings template in `src/main/resources/templates/settings/grading-fees.html`
  - Dependencies: T112, T076, T090, T091
  - Verification: Page allows owner/admin to create or update PSA fee and turnaround records
  - Related: GA-012, GA-013
  - Status: Later-v1

- [ ] T115 Add settings UI tests in `src/test/java/com/pokemonportfolio/config/controller/SettingsControllerTest.java`
  - Dependencies: T112, T113, T114, T133
  - Verification: Tests confirm settings pages are protected, provider toggles require authorization, and secret values are not rendered
  - Related: NFR-003, NFR-004
  - Status: Later-v1

## Phase 27: Testing

- [ ] T116 Add final cross-module smoke/regression suite in `src/test/java/com/pokemonportfolio/businessrules/BusinessRuleRegressionTest.java`
  - Dependencies: T050, T055, T059, T065, T070, T074, T079, T083
  - Verification: Suite samples critical cross-module regressions for price calculation, confidence, currency conversion, snapshot preservation, portfolio valuation, alerts, trade, grading, signal classification, and forecast output without duplicating every module-specific business-rule test deeply
  - Related: NFR-006, XXIII-Testing
  - Status: Later-v1 final regression; MVP-critical only when limited to completed Vertical Slice 1 modules

- [ ] T117 Add repository integration test suite in `src/test/java/com/pokemonportfolio/repository/RepositoryIntegrityTest.java`
  - Dependencies: T012, T023, T029, T035, T045, T051, T056, T060, T066, T071, T075, T080, T084
  - Verification: Tests cover table mappings, required constraints, append-only snapshots, and owned-item separate row behavior
  - Related: DR-009, DR-012, DR-013, NFR-011
  - Status: MVP-critical for completed entities

- [ ] T118 Add controller security regression tests in `src/test/java/com/pokemonportfolio/auth/ControllerSecurityRegressionTest.java`
  - Dependencies: T018, T097, T100, T103, T105, T107, T109, T111, T115
  - Verification: Tests confirm protected routes reject unauthenticated access and login/static assets remain public
  - Related: FR-002, NFR-001, NFR-004
  - Status: MVP-critical for completed routes

- [ ] T119 Add full scheduled job integration tests in `src/test/java/com/pokemonportfolio/scheduler/ScheduledJobIntegrationTest.java`
  - Dependencies: T089, T130
  - Verification: Tests run jobs with mock providers and confirm rerun safety, append-only history, dedupe, and failure resilience
  - Related: FR-019, FR-020, FR-021, FR-022, FR-024, NFR-018
  - Status: Later-v1 final regression; MVP price/portfolio job coverage is handled by T089

- [ ] T120 Add provider adapter mock contract suite in `src/test/java/com/pokemonportfolio/pricing/provider/ProviderAdapterMockContractTest.java`
  - Dependencies: T039, T043, T053
  - Verification: Tests confirm adapters are mockable, normalized, and never call live external APIs
  - Related: PR-012, NFR-007, NFR-008
  - Status: MVP-critical

- [X] T121 Add vertical slice integration test in `src/test/java/com/pokemonportfolio/VerticalSliceOneIntegrationTest.java`
  - Dependencies: T018, T028, T033, T043, T050, T059, T085, T086, T096, T100
  - Verification: Test logs in, creates or selects an English card through the manual catalogue flow, adds it to portfolio, creates an SGD-only mock price snapshot, displays dashboard value in SGD, and confirms a historical portfolio valuation snapshot exists
  - Related: Vertical Slice 1, AC-001, AC-003, AC-004, AC-005, AC-007, AC-015
  - Status: MVP-critical

## Phase 28: Documentation and Final Validation

- [X] T122 Document local setup in `README.md`
  - Dependencies: T001, T003, T017
  - Verification: README explains Java 21, PostgreSQL, secure env vars, mock providers, and no live API requirement
  - Related: NFR-003, NFR-008
  - Status: MVP-critical

- [ ] T123 Update implementation quickstart in `specs/001-portfolio-market-app/quickstart.md`
  - Dependencies: T121
  - Verification: Quickstart matches Vertical Slice 1 commands, login flow, manual card/add-to-portfolio flow, SGD-only mock price refresh, dashboard valuation, and portfolio valuation snapshot validation; later modules append their checks as they are completed
  - Related: AC-001, AC-003, AC-004, AC-005, AC-007, AC-015; later ACs when modules complete
  - Status: MVP-critical for vertical slice; later-v1 for full modules

- [ ] T124 Create constitution compliance checklist in `specs/001-portfolio-market-app/checklists/implementation-readiness.md`
  - Dependencies: T121, T122, T123
  - Verification: Checklist confirms Vertical Slice 1 compliance for personal-use authentication, manual card/add-to-portfolio flow, SGD-only mock valuation display, append-only price snapshots, portfolio valuation snapshots, provider abstraction, and dark financial dashboard UI
  - Related: Constitution v1.1.0, Vertical Slice 1
  - Status: MVP-critical

- [ ] T125 Document future real provider adapter backlog in `docs/future-provider-integrations.md`
  - Dependencies: T040, T113
  - Verification: Document marks TCGPlayer, eBay, and PriceCharting real integrations as future-phase until permitted access and credentials are confirmed
  - Related: PR-012, NFR-020
  - Status: Future-phase

- [ ] T126 Run final validation and record results in `specs/001-portfolio-market-app/checklists/final-validation.md`
  - Dependencies: T121, T122, T123, T124
  - Verification: Vertical Slice 1 build, focused tests, smoke flow, and UI direction review pass for login, manual card creation/selection, add-to-portfolio, SGD-only mock price snapshot, dashboard value in SGD, and historical portfolio valuation snapshot storage; full regression remains T116-T120 after later modules
  - Related: Vertical Slice 1, SC-001, SC-002, AC-001, AC-003, AC-004, AC-005, AC-007, AC-015
  - Status: MVP-critical for vertical slice; later-v1 for all modules

## Dependency Graph

Vertical Slice 1 MVP path:
1. Project setup: T001-T006
2. Database foundations: T007-T012
3. Authentication: T013-T018
4. Core domain: T019-T022
5. Catalogue and portfolio: T023-T034
6. Provider abstraction and SGD-only mock pricing: T035-T050
7. Portfolio valuation: T056-T059
8. Price/portfolio scheduled jobs: T084-T086, T089
9. Dark UI and dashboard/search/portfolio pages: T090-T100
10. Vertical slice test and docs: T121-T124, T126

Expansion order after Vertical Slice 1:
1. Exchange-rate conversion, manual price fallback, and price history: T051-T055, T101-T103, T127, T131-T132
2. Official English card catalogue API integration: T134-T139
3. Item-level portfolio contribution: T128
4. Market Signal Engine: T060-T065
5. Alerts: T066-T070, T087, T104-T105
6. Trade analyzer: T071-T074, T106-T107
7. Grading analyzer: T075-T079, T108-T109, T114, T129
8. Forecasting: T080-T083, T110-T111
9. Settings and provider configuration: T112-T115, T133
10. Full regression and final validation: T116-T120, T130, T123-T126

Future-phase tasks:
- T040 and T125 explicitly keep real TCGPlayer, eBay, and PriceCharting adapter work outside the local-first MVP.
- Japanese, Chinese, mobile, image recognition, machine learning, richer PSA population integration, and automated grading watchlists remain out of scope for this task list.

## Parallel Opportunities

- Phase 1: T003 and T004 can run after T001 while T002/T005 proceed.
- Phase 2: Migrations are ordered, but T012 test planning can begin once migration names are known.
- Phase 4: T020 and T021 can run in parallel after T019.
- Phase 5: T024 and T027 can run after T023.
- Phase 7/8: T040 can run in parallel with T038/T039; mock fixture work T042 can run with T043 setup after T041.
- Phase 10/21: T127 follows currency conversion T054-T055; T131-T132 can run after T101 and shared UI fragments exist.
- Phase 12: T061, T062, and T063 can run in parallel after T060.
- UI phases: T098, T101, T104, T106, T108, T110, and T113 can be designed in parallel after the shared layout T090-T092 and relevant controllers exist.
- Testing phase: T116-T120 can be split by module after their dependencies complete; T116 remains aggregate smoke/regression rather than duplicate deep module coverage.

## Independent Test Criteria by User Story

- US1 Secure Personal Access: T018 proves login works, password hashing is used, and protected routes deny unauthenticated access.
- US2 Add Portfolio Item: T100 proves the user can search a card/product and create separate owned item records.
- US3 Portfolio Dashboard: T097 proves dashboard value, cost basis, gain/loss, confidence, alerts, and SGD labels render.
- US4 Price and Signal Review: T103 proves price history uses stored snapshots; T065 proves Expected Price vs Market Price classification.
- US5 Alerts: T070 and T105 prove threshold rules, dedupe, and alert UI.
- US6 Trade Analysis: T074 and T107 prove independent trade percentages and LOW-confidence warning.
- US7 Manual PSA Grading Analysis: T079 and T109 prove manual-only PSA 8/9/10 scenario calculations.
- US8 Forecast Review: T083 and T111 prove advisory forecast output for all supported horizons.

## Final Implementation Order Recommendation

1. Complete independently executable Vertical Slice 1 first: T001-T050, T056-T059, T084-T086, T089, T090-T100, T121-T124, and T126.
2. Add exchange-rate conversion, manual price fallback, and price history: T051-T055, T101-T103, T127, T131-T132.
3. Add official English card catalogue API integration: T134-T139.
4. Add item-level portfolio contribution: T128.
5. Add Market Signal Engine: T060-T065 so item detail can separate Market Price from Expected Price.
6. Add Alerts: T066-T070, T087, T104-T105.
7. Add Trade Analyzer: T071-T074, T106-T107.
8. Add Grading Analyzer: T075-T079, T108-T109, T114, T129.
9. Add Forecasting: T080-T083, T110-T111.
10. Add Settings, provider toggles, and full regression coverage: T112-T120, T130, T133.
11. Finish documentation and full final validation updates: T123-T126.

## Risks and Scope Notes

- Full v1 is ambitious because it includes portfolio tracking, pricing, alerts, trade, grading, forecasting, scheduled jobs, and UI. The safest delivery is the vertical slice first.
- Real provider integrations are intentionally deferred. Starting with mock/manual providers avoids API access, legal, cost, and test reliability risks.
- Forecasting and Market Signal Engine can become complex quickly. Keep v1 rules-based and explainable with conservative confidence.
- UI polish can expand endlessly. Keep dark financial dashboard components reusable and avoid decorative Pokemon fan-app styling.
- Snapshot tables will grow over time. v1 should index carefully; archival/purge policies should wait for a governed future feature.
