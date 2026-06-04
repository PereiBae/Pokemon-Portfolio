# Implementation Plan: Pokemon Card Portfolio & Market Analytics

**Branch**: `001-portfolio-market-app` | **Date**: 2026-06-02 | **Spec**: [spec.md](./spec.md)

**Input**: Product Specification v1.0 for the Pokemon Card Portfolio & Market
Analytics application.

## Summary

Build a personal-use Spring Boot 3 modular monolith for tracking Pokemon cards
and sealed products as collectibles/investments. v1 focuses on English cards,
sealed product support, manual search, portfolio tracking, append-only pricing
snapshots, SGD valuation, confidence ratings, a rules-based Market Signal
Engine, alerts, trade analysis, manual PSA grading analysis, advisory forecasts,
and a dark-mode-first financial dashboard UI.

## Technical Context

**Language/Version**: Java 21

**Primary Dependencies**: Spring Boot 3, Spring Web, Spring Data JPA, Spring
Security, Thymeleaf, Flyway, Spring Scheduler, WebClient

**Storage**: PostgreSQL with Flyway migrations

**Testing**: JUnit 5, Spring Boot Test, Mockito or equivalent test doubles,
repository tests, controller tests, scheduled job tests, and provider adapter
tests without live external APIs

**Target Platform**: Personal-use server-rendered web application first, future
mobile/API support later

**Project Type**: Modular monolith Spring Boot web application

**UI Direction**: Dark-mode-first modern financial dashboard with compact
tables, portfolio summary cards, market movement indicators, price graphs,
watchlists, alert panels, trade analysis panels, grading opportunity panels, and
professional green/red gain-loss indicators

**Performance Goals**: Dashboard and detail views should feel responsive for a
personal portfolio; item search/add workflows should complete comfortably within
the 2-minute acceptance target; scheduled jobs prioritize correctness and
auditability over raw speed.

**Constraints**: One authenticated personal user first; all user-facing monetary
values in SGD; source currency and exchange-rate metadata preserved;
price/portfolio history append-only; external data behind replaceable provider
adapters; rules-based/weighted-score analytics in v1; UI must remain
professional, analytical, investment-focused, and non-childish.

**Scale/Scope**: v1 English cards and sealed products, manual search, portfolio
tracking, pricing snapshots, calculated market value, confidence ratings, basic
Market Signal Engine, alerts, trade analyzer, manual PSA grading analyzer,
forecast view, and settings/provider configuration.

## 1. Technical Summary

The implementation will use a single Spring Boot application with clear package
boundaries. Controllers render Thymeleaf pages and delegate to services. Services
own business rules. Repositories handle persistence only. Pricing providers,
exchange-rate providers, and PSA fee/turnaround sources are accessed through
interfaces so TCGPlayer, eBay, PriceCharting, mock providers, and manual entry
can be swapped without changing valuation logic.

Financial calculations are stored as auditable snapshots. Source-specific
provider results are stored before calculated market values. Market Price and
Expected Price are separate persisted concepts. Scheduled jobs are rerunnable,
append-only where history is required, and protected from duplicate user-facing
alerts.

## 2. Architecture Overview

Architecture style:
- Modular monolith; no microservices in v1.
- Server-rendered UI first with Thymeleaf templates and static assets.
- Spring Security protects all application pages except login/static assets.
- Spring Scheduler runs daily refresh, valuation, alert, exchange-rate, and PSA
  fee/turnaround workflows.
- WebClient is used only inside provider adapters or provider-support clients.
- PostgreSQL stores portfolio, catalog, snapshots, analysis outputs, and audit
  metadata.
- Flyway owns all schema changes and seed/reference data migrations.

Primary flow:
1. User logs in.
2. User searches catalog records or manually adds data.
3. User creates an `owned_item` record for each owned copy.
4. Pricing refresh gathers provider-specific results through adapters.
5. Valuation service converts source prices to SGD, applies source hierarchy, and
   creates a calculated `price_snapshot`.
6. Market Signal Engine creates an explainable `market_signal_snapshot`.
7. Portfolio valuation job creates a `portfolio_valuation_snapshot`.
8. Alert job evaluates default thresholds and creates deduplicated alerts.
9. User runs trade, grading, or forecast analysis on demand.

## 3. Package / Module Structure

Use base package `com.pokemonportfolio` unless changed during implementation.

```text
src/main/java/com/pokemonportfolio/
├── catalog/
│   ├── controller/
│   ├── entity/
│   ├── repository/
│   └── service/
├── portfolio/
│   ├── controller/
│   ├── entity/
│   ├── repository/
│   └── service/
├── pricing/
│   ├── controller/
│   ├── entity/
│   ├── provider/
│   ├── repository/
│   ├── scheduler/
│   └── service/
├── market_signal/
│   ├── entity/
│   ├── repository/
│   └── service/
├── alerts/
│   ├── controller/
│   ├── entity/
│   ├── repository/
│   ├── scheduler/
│   └── service/
├── grading/
│   ├── controller/
│   ├── entity/
│   ├── repository/
│   └── service/
├── trade/
│   ├── controller/
│   ├── entity/
│   ├── repository/
│   └── service/
├── forecasting/
│   ├── controller/
│   ├── entity/
│   ├── repository/
│   └── service/
├── auth/
│   ├── controller/
│   ├── entity/
│   ├── repository/
│   └── service/
└── config/
```

```text
src/main/resources/
├── templates/
│   ├── auth/
│   ├── dashboard/
│   ├── portfolio/
│   ├── catalog/
│   ├── alerts/
│   ├── trade/
│   ├── grading/
│   ├── forecasting/
│   ├── settings/
│   └── fragments/
├── static/
│   ├── css/
│   ├── js/
│   └── images/
├── application.yml
└── db/migration/
```

## 4. Database Model

Core tables/entities:
- `app_user`: authenticated owner account.
- `pokemon_set`: set metadata. Represents the product specification's set
  concept while avoiding a reserved or ambiguous table name.
- `card`: individual card catalog records.
- `sealed_product`: sealed product catalog records.
- `owned_item`: each owned card/product copy.
- `pricing_provider_result`: source-specific provider pricing result.
- `price_snapshot`: calculated market price snapshot in SGD.
- `exchange_rate_snapshot`: source currency to SGD conversion snapshot.
- `market_signal_snapshot`: expected price, signal classification, and
  explanation snapshot.
- `portfolio_valuation_snapshot`: portfolio-level valuation snapshot.
- `alert`: price movement alert.
- `trade_transaction`: saved trade analysis or executed trade header, including
  mode, status, totals, imbalance, and execution metadata.
- `trade_transaction_side`: user/outgoing side and other-party/incoming side.
- `trade_transaction_item`: item included in a trade side, including agreed
  value, allocated incoming cost basis, linked owned item, linked disposal
  record, and linked incoming owned item where applicable.
- `grading_fee`: updateable PSA fee and turnaround data.
- `grading_analysis`: manually triggered grading analysis header.
- `grading_scenario`: PSA 8, PSA 9, PSA 10 scenario output.
- `forecast_snapshot`: advisory forecast output.
- `scheduled_job_run`: job execution audit and rerun protection.
- `manual_price_entry`: optional manual valuation input, represented as a
  provider-like source in valuation.

Important database rules:
- `owned_item` has no quantity field. Each row is one owned copy.
- `owned_item` references exactly one of `card` or `sealed_product`.
- `pricing_provider_result` stores raw source data separately from
  `price_snapshot`.
- `price_snapshot`, `market_signal_snapshot`, `portfolio_valuation_snapshot`,
  `forecast_snapshot`, and `exchange_rate_snapshot` are append-only for
  historical analysis.
- Alerts are deduplicated by owned item, threshold rule, and triggering snapshot
  so reruns do not create duplicate user-facing alerts.
- Executed trade transactions link outgoing owned items, incoming owned items,
  and disposal records; incoming owned items use allocated trade cost basis so
  dashboard realised and unrealised gain/loss remain auditable.
- Monetary columns use decimal precision suitable for currency, not floating
  point.
- Timestamps are stored with timezone.

## 5. Entity Design

`AppUser`:
- Fields: id, username/email, passwordHash, displayName, role, enabled,
  createdAt, updatedAt, lastLoginAt.
- Rules: one owner account in v1; password never stored as plaintext.

`PokemonSet`:
- Fields: id, name, code, languageMarket, releaseDate, productLine,
  externalRefs, createdAt, updatedAt.
- Rules: market-specific; Japanese/Chinese sets remain separate later.

`Card`:
- Fields: id, setId, name, cardNumber, rarity, languageMarket, variant,
  characterTags, releaseDate, imageUrl, externalRefs, active.
- Rules: variants such as reverse holo, holo, promo, stamped, alternate art,
  secret rare, Master Ball, Poke Ball, and error are distinct where data exists.

`SealedProduct`:
- Fields: id, name, productType, languageMarket, releaseDate, imageUrl,
  externalRefs, active.
- Rules: supports booster packs, booster boxes, Elite Trainer Boxes, collection
  boxes, promo products, and later Japanese sealed products.

`OwnedItem`:
- Fields: id, userId, itemType, cardId, sealedProductId, languageMarket,
  condition, purchasePriceSgd, purchaseDate, gradedStatus, psaGrade,
  psaCertificationNumber, notes, createdAt, updatedAt, archivedAt.
- Rules: exactly one catalog reference; PSA fields allowed only for graded card
  items; no storage location required.

`PricingProviderResult`:
- Fields: id, providerName, providerType, itemType, cardId, sealedProductId,
  gradedStatus, psaGrade, sourcePrice, sourceCurrency, sourceTimestamp,
  observedAt, sourceUrl, listingCount, recentSalesCount, providerConfidence,
  rawMetadataSummary, fetchStatus, errorCode.
- Rules: source-specific and immutable after capture except for internal
  processing status.

`ExchangeRateSnapshot`:
- Fields: id, baseCurrency, targetCurrency, rate, rateSource, effectiveAt,
  fetchedAt, confidence.
- Rules: target currency is SGD for v1 displays.

`PriceSnapshot`:
- Fields: id, itemType, cardId, sealedProductId, gradedStatus, psaGrade,
  marketPriceSgd, sourceCurrencySummary, exchangeRateSummary, confidence,
  calculationTimestamp, pricingBasis, explanation, latestSaleAt,
  sourceDisagreementScore, volatilityScore.
- Rules: append-only; stores enough explanation metadata to audit valuation.

`MarketSignalSnapshot`:
- Fields: id, priceSnapshotId, expectedPriceSgd, marketPriceSgd, differenceSgd,
  percentageDifference, classification, confidence, explanation, supplyScore,
  demandScore, modifierScore, createdAt.
- Rules: classification is Undervalued, Fairly Valued, or Overvalued.

`PortfolioValuationSnapshot`:
- Fields: id, userId, totalValueSgd, totalCostBasisSgd, unrealizedGainLossSgd,
  unrealizedGainLossPercent, itemCount, lowConfidenceCount, snapshotDate,
  calculatedAt, explanation.
- Rules: graph views use stored snapshots.

`Alert`:
- Fields: id, ownedItemId, priceSnapshotId, alertType, thresholdRule,
  purchasePriceSgd, currentMarketValueSgd, gainAmountSgd, gainPercent,
  sourceConfidence, status, triggeredAt, acknowledgedAt.
- Rules: default v1 gain thresholds; status supports NEW, ACTIVE, ACKNOWLEDGED,
  DISMISSED.

`TradeTransaction`, `TradeTransactionSide`, `TradeTransactionItem`:
- Fields: transaction name/notes/mode/status/timestamp, execution timestamp,
  side type and trade percentage, item references, market value, adjusted value,
  agreed trade value, allocated cost basis, trade imbalance, confidence,
  warnings, linked outgoing owned item, linked incoming owned item, and linked
  disposal record.
- Rules: two modes are supported. Analysis-only mode compares both sides and
  MUST NOT change portfolio records or realised gain/loss. Execute trade mode
  marks outgoing owned items as TRADED, creates disposal records, creates
  incoming owned items, links all records to one transaction, and allocates
  incoming cost basis from agreed trade values.

`GradingFee`, `GradingAnalysis`, `GradingScenario`:
- Fields: PSA service level, fee, currency, turnaround estimate, effective dates;
  analysis card, raw value, gem rate, confidence, warnings; scenario grade,
  expected value, total cost, expected profit, ROI, recommendation.
- Rules: PSA only in v1; profitable only when expected profit exceeds SGD 50.

`ForecastSnapshot`:
- Fields: id, itemType, cardId, sealedProductId, horizonDays,
  expectedFutureValueSgd, lowerBoundSgd, upperBoundSgd, confidence, explanation,
  modelType, createdAt.
- Rules: advisory only; horizons 30, 90, 180, 365 days.

## 6. Service Design

Catalog services:
- `CardSearchService`: manual search across active English cards in v1.
- `SealedProductSearchService`: manual search across sealed products.
- `CatalogAdminService`: optional owner/admin maintenance for catalog records.

Portfolio services:
- `OwnedItemService`: create/update/archive owned copies; enforces individual
  record model.
- `PortfolioDashboardService`: assembles dashboard summaries, top gainers/losers,
  low-confidence valuations, and alerts.
- `PortfolioValuationService`: creates portfolio valuation snapshots from latest
  calculated market values.

Pricing services:
- `PricingProviderService`: coordinates enabled provider adapters.
- `MarketValuationService`: applies hierarchy, currency conversion, and
  confidence calculation to provider results.
- `CurrencyConversionService`: selects exchange-rate snapshots and converts
  source prices to SGD.
- `ConfidenceRatingService`: computes HIGH/MEDIUM/LOW based on source count,
  recent sales, sale age, disagreement, liquidity, and volatility.
- `ManualPriceEntryService`: stores manual price fallback as provider-like input.

Market signal services:
- `MarketSignalService`: calculates Expected Price and classification.
- `SupplySignalService`: pull cost, months since release, active listing count,
  supply shift index.
- `DemandSignalService`: character premium, universal appeal, card appeal,
  grading intensity.
- `MarketModifierService`: demand pressure, rip-risk premium, grading lag
  adjustment, PSA gem rate.

Alerts:
- `AlertEvaluationService`: evaluates default gain thresholds and dedupes alerts.
- `AlertViewService`: lists new, active, and historical alerts.

Trade:
- `TradeAnalyzerService`: calculates analysis-only market totals, adjusted
  totals, net difference, fairness, confidence, and LOW-confidence warnings.
- `TradeExecutionService`: executes confirmed trade transactions, marks outgoing
  items as TRADED, creates linked disposal records, creates incoming owned
  items, allocates incoming cost basis, calculates trade imbalance, and leaves
  the existing simple Trade Away disposal flow available for partial/manual
  records.

Grading:
- `GradingAnalyzerService`: manually runs PSA 8/9/10 scenario analysis.
- `GradingFeeService`: manages updateable PSA fees and turnaround estimates.

Forecasting:
- `ForecastService`: creates advisory rules-based/weighted forecast snapshots.

Scheduling:
- `PriceRefreshJob`, `PortfolioValuationJob`, `AlertCheckJob`,
  `ExchangeRateRefreshJob`, and `GradingFeeRefreshJob` orchestrate services and
  record `scheduled_job_run` metadata.

## 7. Controller Design

Controllers are thin and only handle request mapping, input validation, view
model assembly delegation, redirects, and error display.

Controllers:
- `AuthController`: login page and logout redirect support.
- `DashboardController`: dashboard home.
- `PortfolioController`: portfolio list, add/edit/archive owned items.
- `CatalogController`: card/product search and detail pages.
- `PricingController`: price history view and manual price entry fallback.
- `AlertController`: new/active/historical alerts and acknowledgment.
- `TradeController`: trade analyzer form/results, analysis-only save, and
  explicit execute-trade confirmation workflow.
- `GradingController`: manual grading analyzer form/results and optional save.
- `ForecastController`: forecast horizon selection/results.
- `SettingsController`: provider configuration, exchange-rate status, PSA fee
  maintenance, and owner/admin settings.

## 8. UI Page Design

Visual system:
- Dark mode first.
- Compact, high-density dashboard layout.
- Financial typography, muted surfaces, clear hierarchy.
- Green/red gain-loss indicators.
- Badges for market signal and confidence.
- Pokemon card images and set symbols used as supporting asset context only.
- Avoid toy-like colors, cartoon-heavy layouts, decorative animations, and
  card grids without analytics context.

Pages:
- Login: focused dark login panel, no public signup for v1 unless owner setup is
  needed.
- Dashboard: portfolio summary cards, value chart, gain/loss indicators, top
  gainers/losers, low-confidence valuations, recent alerts, watchlist-style
  market movement table.
- Portfolio: compact table of owned items with value, cost basis, gain/loss,
  confidence, language/market, condition, grading status, and filters.
- Add Card/Product: search-first workflow with tabs for cards and sealed
  products, then owned-copy purchase details form.
- Card/Product Detail: metadata, latest market value, expected price signal,
  price graph, source breakdown, owned-copy records, alerts, forecast access.
- Price History: graph plus snapshot table with source/currency/confidence
  metadata.
- Alerts: new, active, and historical alert panels.
- Trade Analyzer: two-side layout, item picker, trade percentage controls,
  adjusted values, fairness result, confidence warnings.
- Grading Analyzer: card selector, PSA scenario table, raw-vs-graded warnings,
  recommendation panel, fee/turnaround assumptions.
- Forecast View: horizon selector, expected value, range, confidence, advisory
  language, explanation.
- Settings / Provider Configuration: provider enablement/status, mock/manual
  data controls, API key status display without exposing secrets, exchange-rate
  source, PSA fee updates.

## 9. External Provider Integration Design

Provider abstraction:
- `PricingProviderAdapter` interface returns normalized `ProviderPriceResult`
  records.
- `ExchangeRateProviderAdapter` returns source-to-SGD exchange rates.
- `GradingMetadataProviderAdapter` may refresh PSA fee/turnaround or population
  metadata when permitted.

Adapters:
- TCGPlayer adapter: raw English card and sealed product market price source.
- eBay adapter: sold listing and lowest active listing source.
- PriceCharting adapter: fallback/graded pricing source.
- Mock pricing provider: deterministic local development and tests.
- Manual price entry provider: owner-entered fallback source with LOW or
  configurable confidence.

Reliability:
- Adapters isolate WebClient, authentication, rate limits, source terms, retry
  behavior, and response mapping.
- Provider failures are captured in `pricing_provider_result` with fetch status
  and error metadata.
- Valuation continues with remaining usable results and confidence is reduced.
- Automated tests use mocked adapters/fixtures only.

## 10. Scheduled Job Design

Scheduled jobs:
- Daily price refresh: fetch provider results, store source-specific results,
  calculate new price snapshots.
- Daily portfolio valuation snapshot: calculate total value/cost/gain/loss from
  latest market values and store a snapshot.
- Daily alert checks: evaluate default thresholds from portfolio records and
  latest price snapshots.
- Exchange-rate refresh: store source-to-SGD exchange-rate snapshots.
- PSA fee/turnaround refresh or manual update workflow: update/insert effective
  fee data.

Rerun safety:
- Every run records `scheduled_job_run` type, scheduled date, start/end time,
  status, and summary.
- Price refreshes append snapshots; they never overwrite.
- Portfolio valuation reruns either append a new calculation record or are
  explicitly linked to the same daily run without deleting prior results.
- Alerts dedupe by owned item, threshold rule, and trigger snapshot.
- Failed provider calls do not fail the whole job when other sources are usable.

## 11. Pricing and Valuation Design

Flow:
1. Resolve catalog asset and valuation context: card/sealed product, language,
   raw/graded, PSA grade where applicable.
2. Query enabled provider adapters.
3. Store each provider result separately.
4. Convert each usable source price to SGD using latest valid exchange-rate
   snapshot or newly refreshed rate.
5. Apply pricing source hierarchy based on asset category.
6. Calculate market price in SGD.
7. Calculate confidence.
8. Persist append-only `price_snapshot` with explanation metadata.

Hierarchy:
- English raw: TCGPlayer Market Price, eBay sold, eBay active lowest,
  PriceCharting, internal historical.
- English graded: eBay sold, PriceCharting, internal historical.
- Japanese: eBay sold, eBay active lowest, PriceCharting, internal historical.
- Sealed: TCGPlayer, eBay sold, eBay active lowest, PriceCharting, internal
  historical.

Missing data:
- No source: mark valuation unavailable or LOW confidence with explanation.
- One weak source: compute only if source is usable; confidence LOW.
- Source disagreement: widen uncertainty and lower confidence.
- Stale latest sale: lower confidence and explain age.

## 12. Market Signal Engine Design

The v1 engine is rules-based/weighted-score based, not black-box ML.

Inputs:
- Market Price from latest calculated `price_snapshot`.
- Supply signals: pull cost, months since release, active listing count, supply
  shift index.
- Demand signals: character premium, universal appeal, manual card appeal,
  grading intensity.
- Modifiers: demand pressure, rip-risk premium, grading lag adjustment, PSA gem
  rate.

Output:
- Expected Price in SGD.
- Market Price in SGD.
- Difference in SGD.
- Percentage difference.
- Classification: Undervalued, Fairly Valued, Overvalued.
- Confidence: HIGH, MEDIUM, LOW.
- Explanation naming strongest factors.

Classification:
- Undervalued when Expected Price is meaningfully above Market Price and
  confidence is adequate.
- Overvalued when Market Price is meaningfully above Expected Price.
- Fairly Valued when difference is within configured tolerance or confidence
  prevents a stronger call.

## 13. Portfolio Valuation Design

Portfolio valuation uses latest calculated market value per owned item.

For each owned item:
- Resolve card/sealed product current price snapshot.
- Apply graded status/PSA grade context when relevant.
- Calculate current value and item-level gain/loss against purchase price.
- Flag LOW confidence valuations.

Snapshot:
- Total value SGD.
- Total cost basis SGD.
- Unrealized gain/loss SGD and percent.
- Item count.
- Low-confidence count.
- Item-level contribution metadata where available.

Graphs always read `portfolio_valuation_snapshot`, not destructive
recalculation from current prices only.

## 14. Price Alert Design

Default thresholds:
- Purchase price below SGD 100: alert when gain is at least SGD 10.
- Purchase price SGD 100 or above: alert when gain is at least SGD 25.

Evaluation:
- Uses owned item purchase price and latest calculated market value.
- Calculates gain amount and gain percentage.
- Includes source confidence from valuation.
- Creates NEW/ACTIVE alert when threshold crossed.
- Dedupes alerts on reruns.

Future configurability:
- Thresholds can later move into updateable settings without changing v1 default
  behavior.

## 15. Trade Analyzer Design

Model:
- One `trade_transaction` has exactly two sides: USER and OTHER_PARTY.
- Each side has independent trade percentage.
- Each side contains card and/or sealed product trade items.
- Transaction mode is either ANALYSIS_ONLY or EXECUTE_TRADE.
- ANALYSIS_ONLY transactions are comparison-only and create no owned item,
  disposal, status, realised gain/loss, or cost-basis changes.
- EXECUTE_TRADE transactions link outgoing owned items, incoming owned items,
  and outgoing disposal records to one auditable transaction.

Calculation:
- Market value per item comes from calculated market value.
- Adjusted item value = market value x trade percentage.
- Side totals sum raw and adjusted values.
- Net difference compares adjusted totals.
- Fairness result labels the trade as favorable, balanced, or unfavorable within
  a configured tolerance.
- Confidence is the lowest or aggregated confidence across included items.
- LOW confidence produces a visible warning.
- For execution accounting, outgoing realised gain/loss equals Trade Value
  Received minus Original Purchase Price.
- Incoming items receive allocated cost basis based on agreed incoming trade
  values. If total incoming agreed value differs from total outgoing trade value,
  allocate proportionally and display the difference as trade imbalance.
- Example: a Magikarp IR bought for SGD 200 and traded at agreed value SGD 500
  records SGD 300 realised gain; incoming items agreed at SGD 250, SGD 150, and
  SGD 100 receive those cost bases. If the incoming side total is not SGD 500,
  the allocation is scaled proportionally and the imbalance is shown.
- Full Trade Transaction is the preferred workflow when incoming items are known;
  the simpler Trade Away disposal flow remains available when the owner does not
  want to record incoming items yet.

## 16. Grading Analyzer Design

Manual-only v1 flow:
1. User selects card/owned item.
2. System resolves raw value and PSA 8/9/10 values.
3. System loads updateable PSA fee and turnaround assumptions.
4. System applies opportunity cost, gem rate, grading intensity, and grading lag
   adjustment where data exists.
5. System produces PSA 8 conservative, PSA 9 balanced, and PSA 10 aggressive
   scenarios.

Scenario output:
- Expected value.
- Total cost.
- Expected profit.
- ROI.
- Recommendation.
- Confidence.
- Warnings.

Rules:
- Profitable only if expected profit exceeds SGD 50.
- Warn when raw value exceeds PSA 8 or PSA 9 value.
- PSA only for v1.

## 17. Forecasting Design

Forecasting is advisory only and always labeled as non-guaranteed.

Horizons:
- 30, 90, 180, 365 days.

Inputs:
- Historical prices, volatility, trend consistency, pull cost, months since
  release, active listing count, supply shift index, character premium,
  universal appeal, card appeal, grading intensity, demand pressure, rip-risk
  premium, grading lag adjustment, PSA gem rate, and product type where data
  exists.

Output:
- Expected future value.
- Forecast range.
- Confidence level.
- Explanation.

v1 uses rules/weighted score. Machine learning is explicitly later-phase only.

## 18. Security Design

Security:
- Spring Security form login for the personal owner.
- Password hashing using a secure Spring Security password encoder.
- No plaintext secrets in source code or checked-in config.
- Provider API keys read from environment variables or secure runtime config.
- Admin/settings/provider actions require owner/admin role.
- CSRF protection enabled for forms.
- Error pages avoid exposing provider credentials or internal details.
- Static assets and login are public; all app pages require authentication.

Personal-use does not reduce secure defaults.

## 19. Testing Strategy

Required test layers:
- Unit tests for pure business rules: confidence, valuation, expected price,
  classification, alert thresholds, trade percentage, grading profitability,
  currency conversion, and forecast calculations.
- Service tests for pricing, portfolio valuation, alert evaluation, trade,
  grading, market signal, and forecasting flows.
- Repository tests for Flyway schema mappings, append-only snapshots, and key
  query behavior.
- Controller tests for login access rules, dashboard, portfolio, search, detail,
  alerts, trade, grading, forecast, and settings pages.
- Scheduled job tests for rerun safety, append-only behavior, provider failure,
  and alert deduplication.
- Provider adapter tests with mocked WebClient responses or fixtures; no live
  external API calls.
- UI smoke tests can verify dark dashboard pages render key panels and financial
  indicators once implementation exists.

Test data:
- Use deterministic mock provider data for TCGPlayer/eBay/PriceCharting.
- Include sparse data, conflicting data, stale sales, missing exchange rates,
  LOW confidence, and graded/raw edge cases.

## 20. Migration Strategy

Flyway migration phases:
- V1: base auth/user tables and reference enums.
- V2: catalog tables for sets, cards, sealed products.
- V3: portfolio owned item table.
- V4: exchange rate, provider result, and price snapshot tables.
- V5: market signal and portfolio valuation snapshot tables.
- V6: alert tables.
- V7: trade transaction, trade transaction side, and trade transaction item
  tables, including links to owned items and disposal records for executed
  trades.
- V8: grading fee, grading analysis, and grading scenario tables.
- V9: forecast snapshot tables.
- V10: scheduled job run table and operational indexes.

Seed data:
- Owner account bootstrap path or documented initial-user setup.
- Baseline grading fees/turnaround assumptions.
- Initial character premium/card appeal reference values.
- Mock/local pricing provider fixtures for development.

Migration rules:
- Never drop historical snapshots in v1 migrations.
- Additive changes preferred.
- Any destructive migration requires explicit user backup/migration procedure.

## 21. Development Milestones

1. Project foundation: Spring Boot, security, PostgreSQL, Flyway, Thymeleaf dark
   shell, package boundaries.
2. Catalog and manual search: cards, sealed products, variants, markets.
3. Portfolio tracking: owned item records, dashboard basics, SGD display.
4. Pricing foundation: provider interfaces, mock provider, manual price entry,
   exchange-rate snapshots, source-specific results.
5. Valuation: market price calculation, hierarchy, confidence, price snapshots.
6. Portfolio valuation snapshots and portfolio graphs.
7. Market Signal Engine: expected price, classification, explanations.
8. Alerts: scheduled checks, default thresholds, alert panels.
9. Trade analyzer.
10. Manual PSA grading analyzer with updateable fees.
11. Advisory forecasting.
12. Provider adapter hardening for TCGPlayer/eBay/PriceCharting as permitted.
13. UI polish for dark financial dashboard style and end-to-end tests.

## 22. Risks and Mitigations

| Risk | Impact | Mitigation |
|------|--------|------------|
| Provider access is paid, limited, blocked, or legally restricted | Pricing gaps | Use adapters, mock/manual fallback, confidence reduction, and permitted sources only |
| Sparse recent sales data | Misleading valuation | LOW confidence, explanation, internal historical fallback |
| Source disagreement | Incorrect market price | Store source-specific values, disagreement score, confidence downgrade |
| Exchange rate unavailable | Unaudited SGD display | Block final converted display or mark unavailable until auditable rate exists |
| Snapshot growth | Larger database over time | Index carefully and archive only through future governed process |
| UI drifts into fan-site styling | Violates constitution | Use dark financial dashboard components and UI review gates |
| Business rules become scattered | Hard to maintain | Keep controllers thin and centralize rules in services |
| Scheduled job reruns create duplicate alerts | User confusion | Alert dedupe keys and job-run audit |
| Grading analysis over-recommends | Bad decisions | Conservative thresholds, PSA 8/9 penalties, confidence warnings |

## 23. Constitution Compliance Check

Pre-design gate:
- Personal-use scope: PASS. One authenticated owner; public/social/SaaS features
  are excluded.
- Market/language separation: PASS. English v1; Japanese and Chinese remain
  distinct future markets.
- Collectible coverage: PASS. Cards, variants, sealed products, and owned copies
  are modeled separately.
- Ownership model: PASS. `owned_item` stores each copy as a separate row with no
  quantity shortcut.
- SGD and auditability: PASS. All displays use SGD with source currency,
  exchange-rate snapshots, and calculation metadata.
- Historical preservation: PASS. Price, market signal, forecast, exchange-rate,
  and portfolio valuation snapshots are append-only.
- Pricing hierarchy and confidence: PASS. Hierarchies are encoded in valuation
  service design and confidence uses source quality inputs.
- Provider abstraction: PASS. TCGPlayer, eBay, PriceCharting, mock, and manual
  sources are behind adapters.
- Market Signal Engine: PASS. Market Price and Expected Price are separate and
  explainable.
- Alerts/grading/trades/forecasting: PASS. Default alert thresholds, manual PSA
  grading, two-sided trades, and advisory forecasts are preserved.
- Security: PASS. Spring Security, password hashing, secure config, and protected
  settings/admin actions are planned.
- Maintainability: PASS. Modular monolith with thin controllers, services, and
  repositories.
- Testing: PASS. Business-rule automated tests and mock provider tests are
  required.
- Modern financial dashboard UI: PASS. Dark-mode-first, analytical, professional
  UI is planned and childish presentation is excluded.

Post-design gate:
- PASS. No constitution violations were introduced by the database model,
  service boundaries, provider integration design, scheduled jobs, UI plan, or
  testing strategy.

## Project Structure

### Documentation (this feature)

```text
specs/001-portfolio-market-app/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   ├── provider-adapters.md
│   ├── scheduled-jobs.md
│   └── ui-routes.md
└── tasks.md
```

### Source Code (repository root)

```text
src/main/java/com/pokemonportfolio/
├── catalog/
├── portfolio/
├── pricing/
├── market_signal/
├── alerts/
├── grading/
├── trade/
├── forecasting/
├── auth/
└── config/

src/main/resources/
├── templates/
├── static/
├── application.yml
└── db/migration/

src/test/java/com/pokemonportfolio/
├── catalog/
├── portfolio/
├── pricing/
├── market_signal/
├── alerts/
├── grading/
├── trade/
├── forecasting/
└── auth/
```

**Structure Decision**: Use a single Spring Boot modular monolith with package
boundaries matching the constitution. No separate frontend or microservice
projects are introduced for v1.

## Complexity Tracking

No constitution violations or exceptional complexity are required.
