<!--
Sync Impact Report
Version change: unversioned template -> 1.0.0
Modified principles: placeholder principles -> 24 enforceable project principles
Added sections: Scope Boundaries; Compliance Verification Checklist
Removed sections: placeholder Additional Constraints and Development Workflow sections
Templates requiring updates:
- updated: .specify/templates/plan-template.md
- updated: .specify/templates/spec-template.md
- updated: .specify/templates/tasks-template.md
- updated: .specify/templates/checklist-template.md
- not applicable: .specify/templates/commands/*.md (directory absent)
- updated: AGENTS.md
Follow-up TODOs: none
-->

# Pokemon Card Portfolio & Market Analytics Constitution

## Core Principles

### I. Personal-Use Scope

Non-negotiable rules:
- The application MUST serve one authenticated personal user first.
- Multi-user marketplaces, public social features, public profiles, seller tools,
  commercial SaaS billing, tenancy, and public community features are out of scope
  for v1.
- Personal use MUST NOT weaken security, test coverage, data quality, or
  maintainability requirements.
- Admin-only actions MUST be protected even when the owner is the only expected
  user.

Rationale: A personal finance-adjacent collection tool needs tight scope, but
portfolio data, purchase prices, authentication, and external API credentials
still require production-quality defaults.

### II. Market and Language Separation

Non-negotiable rules:
- English, Japanese, and Chinese collectibles MUST be modeled as separate
  markets.
- Japanese cards MUST NOT be coerced into English card equivalents when pricing,
  history, population, demand, release, or forecasting data differs.
- Each market MAY have separate pricing sources, price histories, demand
  signals, population data, and forecast models.
- v1 MUST prioritize English. Japanese support MAY be added in a later phase.
  Chinese support MAY be added after Japanese unless a future amendment changes
  priority.

Rationale: Language markets behave differently. Combining them would make prices,
signals, and forecasts misleading.

### III. Complete Collectible Coverage

Non-negotiable rules:
- The catalog and portfolio MUST support both individual cards and sealed
  products.
- Card variants MUST be distinct records where data exists, including reverse
  holo, holo, promo, stamped, alternate art, secret rare, Master Ball variant,
  Poke Ball variant, and error cards.
- Sealed products MUST support booster packs, booster boxes, Elite Trainer
  Boxes, collection boxes, promo products, and Japanese sealed products in later
  phases.
- Specifications MUST identify whether a feature applies to cards, sealed
  products, or both.

Rationale: Treating collectible variants or sealed products as generic duplicates
would corrupt valuation, ownership, and analytics.

### IV. Individual Ownership Records

Non-negotiable rules:
- Every owned card or sealed product copy MUST be persisted as a separate
  portfolio record.
- A count-only model such as one row with quantity 5 MUST NOT be used when
  condition, purchase price, grading status, PSA grade, PSA certification number,
  purchase date, or language may differ.
- Each owned item MAY store condition, language, purchase price, purchase date,
  graded or ungraded status, PSA grade, and PSA certification number.
- Storage location MUST NOT be required for v1.

Rationale: Individual copy records preserve the inputs needed for gain/loss,
grading analysis, alerts, and trade decisions.

### V. SGD-Only Display and Currency Auditability

Non-negotiable rules:
- All user-facing monetary values MUST be displayed in SGD.
- Source prices MAY arrive in USD or other currencies, but displayed values MUST
  be converted before presentation.
- The system MUST store source currency, converted SGD value, exchange rate,
  exchange-rate source or provenance where available, and conversion timestamp.
- Calculations MUST preserve enough source-currency information to explain how
  SGD values were derived.

Rationale: A single display currency keeps the experience consistent while
source-currency audit fields prevent opaque financial calculations.

### VI. Historical Price Preservation

Non-negotiable rules:
- Price history MUST never be overwritten by refresh jobs or manual updates.
- Each refresh MUST create a new price snapshot.
- Historical snapshots MUST support card price graphs, sealed product price
  graphs, portfolio value graphs, price alerts, grading analysis, trade analysis,
  forecasting, and expected price modeling.
- Features requiring historical analysis MUST NOT rely only on current prices.

Rationale: Destructive price updates would make trend, alert, gain/loss, and
forecast explanations impossible to audit.

### VII. Pricing Accuracy, Source Hierarchy, and Confidence

Non-negotiable rules:
- The application MUST NOT rely on a single pricing source when multiple sources
  are available.
- Source-specific prices MUST be stored separately before calculating a unified
  market value.
- English raw card pricing priority MUST be: TCGPlayer Market Price, eBay sold
  listings, eBay lowest active listing, PriceCharting, internal historical data.
- English graded card pricing priority MUST be: eBay sold listings,
  PriceCharting, internal historical data.
- Japanese card pricing priority MUST be: eBay sold listings, eBay lowest active
  listing, PriceCharting, internal historical data.
- Sealed product pricing priority MUST be: TCGPlayer, eBay sold listings, eBay
  lowest active listing, PriceCharting, internal historical data.
- Every calculated market value MUST include confidence rated HIGH, MEDIUM, or
  LOW.
- Confidence MUST consider source count, recent sale count, age of latest sale,
  disagreement between sources, listing liquidity, and market volatility.

Rationale: A hierarchy plus confidence metadata makes valuation useful without
pretending imperfect market data is exact.

### VIII. Pricing Source Abstraction

Non-negotiable rules:
- Pricing providers MUST be replaceable through provider interfaces and adapters.
- Business services MUST NOT be tightly coupled to TCGPlayer, eBay,
  PriceCharting, or any future provider implementation.
- Provider failures, unavailable sources, paid access limits, blocked requests,
  unreliable responses, or legal restrictions MUST degrade valuation confidence
  instead of failing the entire valuation flow when remaining data is usable.
- External provider tests MUST use mocks, fakes, fixtures, or contract-style
  test doubles rather than live external APIs.

Rationale: Market data access changes over time. Adapter boundaries keep the
core model stable and testable.

### IX. Market Signal Engine

Non-negotiable rules:
- The application MUST include a Market Signal Engine that calculates both
  Market Price and Expected Price.
- Market Price MUST mean the current calculated market value derived from
  available pricing sources.
- Expected Price MUST mean a model-derived fair value based on combined supply,
  demand, and modifier signals.
- The engine MUST classify each asset as Undervalued, Fairly Valued, or
  Overvalued.
- The classification MUST include expected price, market price, SGD difference,
  percentage difference, confidence rating, and a short explanation.
- v1 MUST use an explainable weighted scoring model. Machine learning MAY be
  introduced only after sufficient clean historical data exists.

Rationale: Separating observed market value from model-derived fair value makes
analytics actionable without hiding the reasoning.

### X. Supply Signals

Non-negotiable rules:
- The Market Signal Engine MUST support pull cost, months since release, active
  listing count, and supply shift index where data exists.
- Pull cost MUST consider pack price and pull rate where data exists and MUST be
  treated as a supply pressure signal, not a guaranteed fair price.
- Months since release MUST classify lifecycle stage using 0-3 months launch
  volatility, 4-12 months price discovery, 13-24 months stabilization, and 24+
  months mature market unless a feature documents a justified alternative.
- Active listing count MUST be tracked where available.
- Supply shift index MUST compare recent active listing count against a
  longer-term baseline, such as 7-day average divided by 30-day average.

Rationale: Supply conditions explain why a card or product may deserve a price
premium or discount beyond recent sale price alone.

### XI. Demand Signals

Non-negotiable rules:
- The Market Signal Engine MUST support configurable character premium scores.
- High-premium Pokemon examples MAY include Pikachu, Charizard, Umbreon,
  Rayquaza, Gengar, Lugia, Mew, Mewtwo, Eevee evolutions, Greninja, Latias, and
  Latios.
- Universal appeal MUST account for factors such as iconic Pokemon, popular
  artwork, playable card demand, trainer demand, nostalgia, and
  cross-generation appeal when data exists.
- Card appeal MUST support manual scoring in v1. Automated image-based appeal
  estimation MAY be added later.
- Grading intensity MUST track PSA population growth over 30 or 90 days where
  data exists.

Rationale: Demand is not captured by sales price alone. Configurable, explainable
signals allow the owner to tune the model as collector behavior changes.

### XII. Market Modifiers

Non-negotiable rules:
- The Market Signal Engine MUST support demand pressure, rip-risk premium,
  grading lag adjustment, and PSA gem rate where data exists.
- Demand pressure MUST measure how quickly available supply is being absorbed,
  such as sales volume divided by active listings.
- Rip-risk premium MUST compare estimated pull cost against current raw market
  price and MUST strengthen expected price only when demand signals are also
  strong.
- Newly released cards under a defined threshold such as 6 months MUST show a
  grading lag warning when graded data is sparse or likely inflated.
- PSA gem rate MUST be calculated as PSA 10 population divided by total PSA
  graded population where population data exists.
- Gem rate MUST be used in grading analysis and forecasting where available.

Rationale: Modifiers prevent simplistic price calls, especially for scarce cards,
new releases, and PSA 10 premiums.

### XIII. Expected Price vs Market Price Presentation

Non-negotiable rules:
- User interfaces and APIs MUST clearly separate Market Price from Expected
  Price.
- Valuation output MUST show whether an asset appears undervalued, fairly valued,
  or overvalued.
- Every signal result MUST include a human-readable reason, such as tightening
  supply, high demand pressure, grading lag, few recent sales, or source
  disagreement.
- The system MUST NOT show only a final signal label without traceable inputs.

Rationale: A signal is useful only when the owner can understand why the model
reached that result.

### XIV. Portfolio Valuation

Non-negotiable rules:
- Portfolio value MUST be calculated from the latest available calculated market
  value.
- The application MUST store daily portfolio valuation snapshots.
- Portfolio value graphs MUST be based on stored snapshots, not destructive
  recalculation from current prices only.
- Portfolio analytics MUST include total portfolio value, unrealized gain/loss,
  gain/loss percentage, value over time, and item-level contribution to
  portfolio growth.

Rationale: Stored valuation snapshots preserve the history required to explain
portfolio performance over time.

### XV. Price Alerts

Non-negotiable rules:
- The application MUST support price movement alerts.
- For portfolio items purchased below SGD 100, the default alert MUST trigger
  when current market value is at least SGD 10 above purchase price.
- For portfolio items purchased at SGD 100 or above, the default alert MUST
  trigger when current market value is at least SGD 25 above purchase price.
- Alert thresholds MAY become configurable later, but v1 defaults MUST be
  implemented as stated.
- Alerts MUST include item name, purchase price, current market value, gain
  amount, gain percentage, date triggered, and source confidence.
- Alerts MUST be generated from stored price snapshots and portfolio records.

Rationale: Alerts must be predictable, auditable, and tied to the owner's actual
purchase basis.

### XVI. Manual PSA Grading Analysis

Non-negotiable rules:
- Grading analysis MUST be manually triggered by the user in v1.
- The app MUST NOT automatically generate grading recommendations unless a
  future constitution-compliant specification explicitly adds that behavior.
- PSA MUST be the only grading company for v1.
- Grading analysis MUST consider raw value, PSA 8 value, PSA 9 value, PSA 10
  value, grading cost, turnaround time, opportunity cost, PSA gem rate, grading
  intensity, grading lag adjustment, and confidence rating.
- Conservative, balanced, and aggressive scenarios MUST assume PSA 8, PSA 9, and
  PSA 10 respectively.
- A grading recommendation MUST be considered profitable only when expected
  profit exceeds SGD 50.
- Modern raw cards that may be worth more than PSA 8 or PSA 9 copies MUST
  penalize grading recommendations accordingly.
- PSA fees and turnaround estimates MUST be stored in updateable database tables,
  not hardcoded permanently.

Rationale: Grading can destroy value when fees, timing, or grade outcomes are
unfavorable. Manual, scenario-based analysis keeps recommendations conservative.

### XVII. Trade Analysis

Non-negotiable rules:
- Trade analysis MUST support two sides: user side and other party side.
- Both sides MUST support cards and sealed products.
- Each side MUST support an independent trade percentage.
- Trade analysis MUST show raw market value, adjusted trade value, trade
  percentage applied, net difference, fairness result, and confidence rating.
- Trade analysis MUST use calculated market value rather than a single raw source
  price.
- Trade analysis MUST warn the user when valuation confidence is LOW.

Rationale: Trades depend on adjusted value and confidence, not just sticker
prices.

### XVIII. Advisory Forecasting

Non-negotiable rules:
- Forecasting MUST be presented as advisory only and MUST NOT be described as
  guaranteed future value.
- The user MUST be able to choose 30-day, 90-day, 180-day, and 365-day forecast
  horizons.
- Forecasts MUST include expected value, forecast range, confidence level, and
  explanation.
- v1 forecasting MUST be explainable and rules-based or weighted-score based.
- Forecasting MAY consider historical prices, volatility, trend consistency,
  pull cost, months since release, active listing count, supply shift index,
  character premium, universal appeal, card appeal, grading intensity, demand
  pressure, rip-risk premium, grading lag adjustment, PSA gem rate, and product
  type as data becomes available.
- Machine learning MAY be introduced later only after sufficient clean historical
  data exists.

Rationale: Forecasts can guide decisions, but they must communicate uncertainty
and preserve trust.

### XIX. Data Ingestion Legality and Reliability

Non-negotiable rules:
- Data ingestion MUST prefer official, permitted, or publicly documented APIs.
- Brittle scraping MUST be avoided when a safer API, export, manual import, or
  provider integration exists.
- Any scraping used during development MUST be isolated behind provider adapters
  and MUST respect source terms, rate limits, and robots restrictions.
- The system MUST NOT store copyrighted marketplace descriptions unnecessarily.
- The system MUST store numerical prices, dates, source names, URLs where
  allowed, source currency, exchange rate, and confidence metadata.
- Ingestion jobs MUST tolerate missing, delayed, inconsistent, or unreliable
  provider data.

Rationale: Market data is useful only when collected legally, reliably, and in a
form the app can audit.

### XX. Security and Authentication

Non-negotiable rules:
- The application MUST use Spring Security.
- Passwords MUST be hashed with a secure password hashing mechanism.
- Plaintext secrets MUST NOT be committed to code or configuration.
- API keys and credentials MUST come from environment variables or secure
  configuration.
- Admin-only actions MUST be authorization-protected.
- Personal-use scope MUST NOT remove secure defaults.

Rationale: Portfolio and credential data are private even when the application
has only one expected user.

### XXI. Auditability and Explainability

Non-negotiable rules:
- Important financial calculations MUST be explainable.
- The system MUST store enough inputs and metadata to explain market prices,
  expected prices, confidence ratings, portfolio values, alerts, grading results,
  trade values, and forecasts.
- Recommendations MUST include reasons.
- The app MUST NOT show final financial numbers without traceable calculation
  inputs.

Rationale: The owner needs to trust the numbers, inspect disagreements, and
understand why recommendations changed.

### XXII. Beginner-Friendly Maintainability

Non-negotiable rules:
- The project MUST remain understandable for a learner using Spring Boot.
- v1 MUST use a modular monolith, not microservices.
- The required stack is Java 21, Spring Boot 3, Spring Web, Spring Data JPA,
  Spring Security, PostgreSQL, Flyway, Spring Scheduler, and WebClient.
- Package boundaries MUST stay clear across catalog, portfolio, pricing,
  market_signal, alerts, grading, trade, forecasting, auth, and config.
- Controllers MUST be thin. Business logic MUST live in services. Repositories
  MUST handle persistence only.
- v1 MUST avoid unnecessary abstractions, distributed systems, and framework
  complexity that do not serve the MVP.

Rationale: A clean modular monolith protects learning speed while still allowing
future growth.

### XXIII. Testing Requirement

Non-negotiable rules:
- Business rules MUST be covered by automated tests.
- Tests MUST cover price calculation, confidence rating, expected price
  calculation, undervalued/fairly valued/overvalued classification, supply signal
  calculations, demand signal calculations, alert triggering, grading scenarios,
  trade percentage calculations, portfolio valuation, and currency conversion.
- External pricing providers MUST be mockable.
- Automated tests MUST NOT depend on live external APIs.

Rationale: Most project risk lives in calculations and data interpretation.
Automated tests protect those rules as the model evolves.

### XXIV. MVP Discipline

Non-negotiable rules:
- v1 MUST focus on English cards, sealed product support, manual search,
  portfolio tracking, pricing snapshots, calculated market value, confidence
  ratings, a basic Market Signal Engine, Expected Price vs Market Price,
  portfolio value graph, price alerts, trade analyzer, and manual grading
  analyzer.
- Later phases MAY include Japanese support, Chinese support, image recognition,
  advanced forecasting, machine learning, richer PSA population analysis, and a
  mobile app.
- Any v1 feature that expands beyond the listed MVP MUST document why it is
  necessary and MUST pass the Governance compliance review.

Rationale: MVP discipline keeps the first build coherent and prevents advanced
analytics from arriving before the data foundation exists.

## Scope Boundaries

In scope for v1:
- One authenticated personal user.
- English cards and English sealed products first.
- Manual search and portfolio tracking.
- Individual ownership records.
- Pricing snapshots with source-specific prices, SGD conversion, calculated
  market value, and confidence ratings.
- Basic explainable Market Signal Engine with Expected Price vs Market Price.
- Portfolio value snapshots and graph-ready history.
- Price movement alerts using the default thresholds.
- Trade analyzer for cards and sealed products.
- Manually triggered PSA grading analyzer.
- Java 21 Spring Boot 3 modular monolith backed by PostgreSQL and Flyway.

Later-phase scope:
- Japanese card and sealed product support.
- Chinese support after Japanese priority unless amended.
- Image recognition.
- Advanced forecasting and machine learning.
- Richer PSA population analysis.
- Mobile app.
- Configurable alert thresholds beyond the v1 defaults.

Out of scope for v1:
- Multi-user marketplace features.
- Public social features, public profiles, or collection sharing.
- Commercial SaaS features such as billing, tenant management, public onboarding,
  or subscription plans.
- Automatic grading recommendations.
- Storage location tracking as a required field.
- Valuation displays in non-SGD currencies.
- Tests that require live external APIs.
- Provider-specific business logic embedded directly in services or controllers.
- Scraping-first ingestion designs when safer permitted alternatives exist.

## Compliance Verification Checklist

Use this checklist before approving any future specification, plan, task list, or
implementation:
- Does the feature stay inside personal-use v1 scope or document a governed
  exception?
- Does it preserve separate English, Japanese, and Chinese market handling where
  language matters?
- Does it model cards, variants, sealed products, and owned copies without
  collapsing meaningful differences?
- Does every displayed financial value use SGD with auditable source currency
  and exchange-rate metadata?
- Does it append historical snapshots instead of overwriting price or portfolio
  history?
- Does valuation use provider abstraction, source-specific data, hierarchy rules,
  and HIGH/MEDIUM/LOW confidence?
- Does Market Signal output separate Market Price from Expected Price and explain
  undervalued/fairly valued/overvalued results?
- Does the feature preserve auditability for calculations, alerts, grading,
  trades, forecasts, and portfolio analytics?
- Does it use Spring Security, hashed passwords, secure configuration, and
  protected admin actions?
- Are all business rules covered by automated tests without live external API
  dependencies?

## Governance

This Constitution supersedes conflicting preferences in specifications, plans,
tasks, implementation notes, and runtime guidance. When a conflict exists, the
Constitution wins unless it is formally amended.

Future specifications MUST include constitution alignment notes covering scope,
market/language handling, pricing history, currency, security, auditability,
testing, and any external data source assumptions. Scope expansions MUST be
called out explicitly.

Implementation plans MUST pass a Constitution Check before Phase 0 research and
again after Phase 1 design. Any violation MUST be listed in Complexity Tracking
with a justification and a simpler alternative that was considered.

Task lists MUST include automated tests for constitution-governed business rules.
Tasks that touch financial data MUST include persistence, auditability, and
historical snapshot work where applicable. Tasks that touch provider data MUST
include provider adapter and mock/test-double work.

Implementation MUST keep controllers thin, keep business logic in services, keep
repositories persistence-only, and preserve the modular monolith package
boundaries named in Principle XXII.

Amendments require an updated Sync Impact Report, a semantic version change,
updates to affected templates or guidance files, and a concise rationale. Major
versions are required for backward-incompatible governance changes or principle
removals. Minor versions are required for new principles, new governed feature
areas, or materially expanded rules. Patch versions are for clarifications and
wording fixes that do not change obligations.

Compliance review is required before accepting any generated spec, plan, task
list, or code change. For this personal project, the authenticated owner may be
the reviewer, but the review MUST still check the Compliance Verification
Checklist.

**Version**: 1.0.0 | **Ratified**: 2026-06-02 | **Last Amended**: 2026-06-02
