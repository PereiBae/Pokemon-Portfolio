# Product Specification v1.0: Pokemon Card Portfolio & Market Analytics

**Feature Branch**: `001-portfolio-market-app`

**Created**: 2026-06-02

**Status**: Ready for Planning

**Input**: Product specification for a personal-use Pokemon card and sealed
product portfolio, pricing, and market analytics web application.

## Constitution Alignment *(mandatory)*

- **Personal-use scope**: The product serves one authenticated personal user in
  v1. Public marketplace, social, SaaS, subscription, and multi-tenant features
  are excluded.
- **Market/language handling**: English is the v1 market. Japanese and Chinese
  are separate future markets and must not be collapsed into English records.
- **Collectible coverage**: The product covers individual cards, card variants,
  sealed products, and separately tracked owned copies.
- **SGD and currency auditability**: All user-facing values are shown in SGD.
  Source currency, exchange rate, converted value, and calculation time are
  retained for valuation auditability.
- **Historical preservation**: Price snapshots and portfolio valuation snapshots
  are append-only and must not be overwritten by refresh jobs.
- **Pricing and confidence**: Valuations use source-specific prices, the
  constitution source hierarchy, calculated market value, and HIGH/MEDIUM/LOW
  confidence.
- **Market Signal Engine**: Market Price and Expected Price are separate outputs
  with explainable undervalued, fairly valued, and overvalued classifications.
- **UI/UX direction**: The UI is dark-mode-first and uses a professional
  financial dashboard style, not childish or cartoon-heavy styling.
- **Security and authentication**: The product requires secure authentication,
  protected administrative actions, hashed passwords, and no committed secrets.
- **External data legality/reliability**: Pricing and market data sources are
  accessed through replaceable providers and must degrade gracefully when data is
  unavailable or unreliable.
- **Automated test coverage**: Pricing, confidence, signals, alerts, grading,
  trades, portfolio valuation, and currency conversion rules require automated
  tests without live external API dependencies.

## 1. Product Overview

The application is a personal collectibles investment dashboard for Pokemon
cards and sealed products. It allows one authenticated owner to track holdings,
monitor market value, preserve price history, analyze value signals, receive
alerts, evaluate trades, and manually run PSA grading analysis.

The v1 product is a server-rendered web application first, with future mobile
support. It uses a serious financial dashboard presentation inspired by stock
broker and portfolio analytics products. Pokemon imagery may appear as asset
context, but the product experience is investment-focused rather than fan-site
focused.

MVP scope includes English cards, sealed product support, manual search,
portfolio tracking, source-specific pricing snapshots, calculated market price,
confidence ratings, a basic explainable Market Signal Engine, price alerts,
portfolio value graph, trade analyzer, manual PSA grading analyzer, and a dark
financial dashboard UI.

## 2. Target User

The target user is a single collector-investor who owns Pokemon cards and sealed
products and wants to manage them like a financial portfolio.

The user cares about current market value, cost basis, unrealized gains, price
history, grading economics, trade fairness, and future price outlook. The user is
comfortable reviewing dense financial information and wants analytical
confidence indicators rather than decorative collection browsing alone.

## 3. User Goals

- **UG-001**: Track every owned card and sealed product with purchase, condition,
  language, and grading details.
- **UG-002**: Understand current portfolio value, total cost basis, unrealized
  gain/loss, and portfolio movement over time.
- **UG-003**: Inspect individual card and product price history.
- **UG-004**: Compare current Market Price against model-derived Expected Price.
- **UG-005**: Identify undervalued, fairly valued, and overvalued items with an
  explanation.
- **UG-006**: Receive alerts when portfolio items move meaningfully above
  purchase price.
- **UG-007**: Analyze proposed trades using adjusted trade percentages and
  valuation confidence.
- **UG-008**: Manually evaluate whether a card may be worth grading with PSA.
- **UG-009**: Review advisory forecasts for selected time horizons.
- **UG-010**: Use a professional dashboard that feels like a financial platform.

## 4. Core User Journeys

### User Story 1 - Secure Personal Access (Priority: P1)

As the owner, I want to log in securely so that my portfolio, purchase prices,
and analysis data remain private.

**Independent Test**: A user can authenticate, reach the dashboard, and cannot
access protected pages while logged out.

**Acceptance Scenarios**:

1. **Given** the owner has valid credentials, **When** they log in, **Then** the
   dashboard is displayed.
2. **Given** a visitor is not authenticated, **When** they request a protected
   portfolio page, **Then** access is denied or redirected to login.

### User Story 2 - Add Portfolio Item (Priority: P1)

As the owner, I want to search for a card or sealed product and add an owned copy
to my portfolio so that I can track my cost basis and value.

**Independent Test**: A searched English card can be selected, entered with
purchase details, and shown as a distinct portfolio record.

**Acceptance Scenarios**:

1. **Given** a card exists in the catalogue, **When** the owner adds it with
   purchase price, condition, and purchase date, **Then** one owned item is
   created.
2. **Given** the owner owns two copies with different purchase details, **When**
   both are added, **Then** the portfolio shows two separate records.

### User Story 3 - Portfolio Dashboard (Priority: P1)

As the owner, I want a financial dashboard view so that I can quickly understand
portfolio value, gains/losses, alerts, and valuation quality.

**Independent Test**: The dashboard displays total value, cost basis, gain/loss,
value over time, top gainers, top losers, low-confidence valuations, and alerts.

**Acceptance Scenarios**:

1. **Given** portfolio records have current valuations, **When** the owner opens
   the dashboard, **Then** all summary values are displayed in SGD.
2. **Given** some valuations have LOW confidence, **When** the dashboard loads,
   **Then** those items are surfaced in a low-confidence section.

### User Story 4 - Price and Signal Review (Priority: P1)

As the owner, I want to view price history and value signals for an item so that
I can understand whether it appears undervalued, fairly valued, or overvalued.

**Independent Test**: An item detail page shows historical price graph, Market
Price, Expected Price, difference, classification, confidence, and explanation.

**Acceptance Scenarios**:

1. **Given** an item has price snapshots, **When** the owner opens the item page,
   **Then** a price history graph is available.
2. **Given** Expected Price and Market Price differ, **When** the signal is
   shown, **Then** the difference in SGD and percent is displayed.

### User Story 5 - Alerts (Priority: P2)

As the owner, I want price movement alerts so that I can notice meaningful gains
without checking every item manually.

**Independent Test**: A portfolio item that crosses the default gain threshold
generates an alert with purchase price, current value, gain, date, and confidence.

**Acceptance Scenarios**:

1. **Given** an item was purchased below SGD 100, **When** current market value
   rises at least SGD 10 above purchase price, **Then** an alert is created.
2. **Given** an item was purchased at SGD 100 or more, **When** current market
   value rises at least SGD 25 above purchase price, **Then** an alert is created.

### User Story 6 - Trade Analysis (Priority: P2)

As the owner, I want to compare two sides of a proposed trade so that I can
understand adjusted value, fairness, and confidence risk.

**Independent Test**: The owner can enter cards/products on each side, set
independent trade percentages, and view adjusted totals and fairness result.

**Acceptance Scenarios**:

1. **Given** a side has SGD 100 market value at 80%, **When** the trade is
   analyzed, **Then** adjusted trade value is SGD 80.
2. **Given** any trade item has LOW valuation confidence, **When** results are
   shown, **Then** the analysis displays a warning.
3. **Given** the owner runs trade analysis-only mode, **When** the comparison is
   completed, **Then** no portfolio items are changed and no realised gain/loss
   is recorded.
4. **Given** the owner confirms execute trade mode, **When** outgoing and
   incoming items are supplied with agreed trade values, **Then** outgoing items
   are marked TRADED, disposal records are created, incoming items are added to
   the portfolio with allocated cost basis, and all items are linked to one trade
   transaction.

### User Story 7 - Manual PSA Grading Analysis (Priority: P2)

As the owner, I want to manually run grading analysis on a card so that I can
compare raw value against PSA 8, PSA 9, and PSA 10 scenarios.

**Independent Test**: A selected card returns conservative, balanced, and
aggressive scenarios with expected value, total cost, profit, ROI, and
recommendation.

**Acceptance Scenarios**:

1. **Given** grading analysis is manually requested, **When** the analyzer runs,
   **Then** PSA 8, PSA 9, and PSA 10 scenarios are displayed.
2. **Given** expected profit is SGD 50 or below, **When** the recommendation is
   calculated, **Then** the scenario is not marked profitable.

### User Story 8 - Forecast Review (Priority: P3)

As the owner, I want advisory forecast outputs so that I can review possible
future value ranges without treating them as guaranteed.

**Independent Test**: The owner selects 30, 90, 180, or 365 days and receives an
expected value, range, confidence level, and explanation.

**Acceptance Scenarios**:

1. **Given** a forecast horizon is selected, **When** forecast data is shown,
   **Then** the UI labels it as advisory and not guaranteed.
2. **Given** the forecast has limited historical data, **When** results are
   shown, **Then** confidence is reduced and explained.

## 5. Functional Requirements

- **FR-001**: The system MUST allow one authenticated personal user to access the
  application.
- **FR-002**: The system MUST prevent unauthenticated access to portfolio,
  pricing, alert, trade, grading, and forecast pages.
- **FR-003**: The system MUST allow the user to search Pokemon cards manually in
  v1.
- **FR-004**: The system MUST allow the user to search sealed products manually
  in v1.
- **FR-005**: The system MUST allow the user to add a card or sealed product to
  the portfolio.
- **FR-006**: The system MUST store each owned copy as a separate portfolio item.
- **FR-007**: The system MUST allow the user to view current portfolio value.
- **FR-008**: The system MUST allow the user to view portfolio value over time.
- **FR-009**: The system MUST allow the user to view individual card/product
  price history.
- **FR-010**: The system MUST compare Market Price against Expected Price for
  valued items.
- **FR-011**: The system MUST classify valued items as Undervalued, Fairly
  Valued, or Overvalued where signal inputs are available.
- **FR-012**: The system MUST support price movement alerts.
- **FR-013**: The system MUST allow the user to run two-sided trade analysis and,
  when explicitly confirmed, execute a full trade transaction with auditable
  portfolio accounting.
- **FR-014**: The system MUST allow the user to manually run PSA grading analysis
  for a card.
- **FR-015**: The system MUST allow the user to view advisory forecasting output.
- **FR-016**: The system MUST display all user-facing money values in SGD.
- **FR-017**: The system MUST provide a dashboard home page with portfolio,
  market, alert, and valuation-confidence summaries.
- **FR-018**: The system MUST support a detailed card/product page with item
  metadata, portfolio ownership records, pricing history, and signal output.
- **FR-019**: The system MUST support scheduled daily price refreshes.
- **FR-020**: The system MUST support scheduled daily portfolio valuation
  snapshots.
- **FR-021**: The system MUST support scheduled daily alert checks.
- **FR-022**: The system MUST support exchange rate refreshes.
- **FR-023**: The system MUST support PSA fee/turnaround refresh or manual update
  workflow.
- **FR-024**: Scheduled jobs MUST be safe to rerun without overwriting historical
  snapshots.
- **FR-025**: The v1 application MUST support server-rendered web UI first.

## 6. Data Requirements

- **DR-001**: The system MUST distinguish catalogue item type as card or sealed
  product.
- **DR-002**: The system MUST store language/market separately from item identity.
- **DR-003**: The system MUST distinguish English, Japanese, and Chinese markets
  even when only English is active in v1.
- **DR-004**: The system MUST store card variants distinctly where data exists.
- **DR-005**: The system MUST represent sealed products distinctly from cards.
- **DR-006**: Each portfolio record MUST store item type, language/market,
  condition, purchase price, purchase date, graded/ungraded status, and optional
  notes.
- **DR-007**: Each graded portfolio record MUST allow PSA grade and PSA
  certification number.
- **DR-008**: Storage location MUST NOT be required in v1.
- **DR-009**: Multiple owned copies MUST remain separate records when purchase
  price, condition, grading status, purchase date, or language may differ.
- **DR-010**: Source-specific price records MUST store provider/source name,
  source price, source currency, source timestamp, allowed source URL/reference,
  and provider confidence metadata.
- **DR-011**: Calculated valuation records MUST store market price in SGD,
  exchange rate used, source currency, confidence rating, calculation timestamp,
  and explanation metadata.
- **DR-012**: Price history MUST be stored as append-only snapshots.
- **DR-013**: Portfolio valuation history MUST be stored as daily snapshots.
- **DR-014**: Alert records MUST store item name, purchase price, current market
  value, gain amount, gain percentage, date triggered, status, and source
  confidence.
- **DR-015**: Trade analysis records SHOULD store both sides, item lists, trade
  percentages, adjusted values, net difference, fairness result, confidence, and
  timestamp when the user saves an analysis.
- **DR-016**: Grading analysis records SHOULD store input values, PSA scenario
  outputs, recommendation, confidence, warnings, and timestamp when the user
  saves an analysis.
- **DR-017**: PSA fees and turnaround estimates MUST be stored as updateable data,
  not permanent constants.
- **DR-018**: Exchange rate data MUST include rate, source/provenance where
  available, effective timestamp, and currency pair.
- **DR-019**: Important calculation outputs MUST retain enough input metadata to
  explain how the result was produced.
- **DR-020**: Executed trade transaction records MUST store one transaction that
  links outgoing owned items, incoming portfolio items, agreed trade values,
  allocated incoming cost basis, trade imbalance, disposal records, and execution
  timestamp.
- **DR-021**: Incoming items created by an executed trade MUST receive cost basis
  from allocated agreed trade values so future unrealised gain/loss starts from
  that allocated basis.

## 7. Pricing and Valuation Requirements

- **PR-001**: The application MUST NOT rely on a single pricing source when more
  than one source is available.
- **PR-002**: The application MUST store source-specific prices separately before
  calculating a unified market price.
- **PR-003**: English raw card pricing priority MUST be TCGPlayer Market Price,
  eBay sold listings, eBay lowest active listing, PriceCharting, then internal
  historical data.
- **PR-004**: English graded card pricing priority MUST be eBay sold listings,
  PriceCharting, then internal historical data.
- **PR-005**: Japanese card pricing priority MUST be eBay sold listings, eBay
  lowest active listing, PriceCharting, then internal historical data.
- **PR-006**: Sealed product pricing priority MUST be TCGPlayer, eBay sold
  listings, eBay lowest active listing, PriceCharting, then internal historical
  data.
- **PR-007**: Each valuation MUST include source-specific price values, source
  currency, exchange rate used, calculated market price in SGD, confidence
  rating, and calculation timestamp.
- **PR-008**: Confidence rating MUST use HIGH, MEDIUM, or LOW.
- **PR-009**: Confidence MUST consider source count, recent sale count, age of
  latest sale, source disagreement, listing liquidity, and market volatility.
- **PR-010**: Each daily price refresh MUST create a new price snapshot.
- **PR-011**: Price history MUST never be overwritten.
- **PR-012**: Pricing providers MUST be replaceable so TCGPlayer, eBay,
  PriceCharting, or future sources can be swapped.
- **PR-013**: When a source is unavailable, valuation MUST continue with other
  available sources where possible and reduce confidence accordingly.
- **PR-014**: Current portfolio value MUST be calculated from latest available
  calculated market value, not from a single raw provider price.
- **PR-015**: Valuation output MUST explain how the SGD market price was derived.

## 8. Market Signal Engine Requirements

- **MS-001**: The system MUST calculate Market Price for valued cards and sealed
  products.
- **MS-002**: The system MUST calculate Expected Price for valued cards and sealed
  products where enough signal inputs exist.
- **MS-003**: Market Price MUST represent the current calculated market value from
  available pricing sources.
- **MS-004**: Expected Price MUST represent a model-derived fair value based on
  supply, demand, and market modifier signals.
- **MS-005**: The system MUST compare Expected Price against Market Price.
- **MS-006**: The system MUST classify each signal as Undervalued, Fairly Valued,
  or Overvalued.
- **MS-007**: Each signal result MUST show expected price, market price, SGD
  difference, percentage difference, classification, confidence rating, and
  explanation.
- **MS-008**: The v1 Market Signal Engine MUST be explainable and MUST NOT be a
  black-box machine learning requirement.
- **MS-009**: The v1 engine SHOULD use a weighted scoring or rules-based model.
- **MS-010**: Supply signals MUST include pull cost, months since release, active
  listing count, and supply shift index where data exists.
- **MS-011**: Pull cost MUST consider pack price and pull rate where data exists.
- **MS-012**: Months since release MUST identify lifecycle stage such as launch
  volatility, price discovery, stabilization, or mature market.
- **MS-013**: Supply shift index MUST compare recent active listings against a
  longer-term active listing baseline.
- **MS-014**: Demand signals MUST include character premium, universal appeal,
  card appeal, and grading intensity.
- **MS-015**: Character premium scores MUST be configurable.
- **MS-016**: Card appeal scoring MUST support manual scoring in v1.
- **MS-017**: Grading intensity SHOULD use PSA population growth over 30 or 90
  days where data exists.
- **MS-018**: Market modifiers MUST include demand pressure, rip-risk premium,
  grading lag adjustment, and PSA gem rate where data exists.
- **MS-019**: Rip-risk premium MUST strengthen Expected Price only when demand
  signals are also strong.
- **MS-020**: Grading lag adjustment MUST warn when newly released cards have
  sparse or potentially inflated graded sales/population data.
- **MS-021**: PSA gem rate MUST be calculated as PSA 10 population divided by
  total PSA graded population where data exists.
- **MS-022**: Signal explanations MUST identify the main factors behind the
  classification.

## 9. Portfolio Requirements

- **PF-001**: The dashboard MUST show total portfolio value.
- **PF-002**: The dashboard MUST show total cost basis.
- **PF-003**: The dashboard MUST show unrealized gain/loss in SGD.
- **PF-004**: The dashboard MUST show unrealized gain/loss percentage.
- **PF-005**: The dashboard MUST show portfolio value over time.
- **PF-006**: The dashboard MUST show top gainers.
- **PF-007**: The dashboard MUST show top losers.
- **PF-008**: The dashboard MUST show low-confidence valuations.
- **PF-009**: The dashboard MUST show triggered alerts.
- **PF-010**: The portfolio MUST support cards and sealed products.
- **PF-011**: Portfolio item detail MUST show owned-copy details separately from
  catalogue item metadata.
- **PF-012**: Portfolio value graphs MUST use stored valuation snapshots.
- **PF-013**: Portfolio analytics MUST identify item-level contribution to
  portfolio growth where data exists.
- **PF-014**: Portfolio values MUST be displayed in SGD.

## 10. Price Alert Requirements

- **PA-001**: The application MUST support price movement alerts.
- **PA-002**: For portfolio items purchased below SGD 100, an alert MUST trigger
  when current market value increases by at least SGD 10 from purchase price.
- **PA-003**: For portfolio items purchased at SGD 100 or above, an alert MUST
  trigger when current market value increases by at least SGD 25 from purchase
  price.
- **PA-004**: Alert thresholds MAY become configurable later; fixed default rules
  are acceptable for v1.
- **PA-005**: Each alert MUST include item name, purchase price, current market
  value, gain amount, gain percentage, date triggered, and source confidence.
- **PA-006**: The app MUST show new alerts.
- **PA-007**: The app MUST show active alerts.
- **PA-008**: The app MUST show historical alerts.
- **PA-009**: Alerts MUST be generated from stored price snapshots and portfolio
  records.
- **PA-010**: Alert calculations MUST use calculated market value, not a single
  raw provider price.

## 11. Trade Analyzer Requirements

- **TA-001**: The trade analyzer MUST support a user side and an other party
  side.
- **TA-002**: Each trade side MUST support cards and sealed products.
- **TA-003**: Each trade side MUST support its own trade percentage.
- **TA-004**: Adjusted trade value MUST equal market value multiplied by the
  trade percentage.
- **TA-005**: The analyzer MUST display item list per side.
- **TA-006**: The analyzer MUST display market value per item.
- **TA-007**: The analyzer MUST display adjusted trade value per item.
- **TA-008**: The analyzer MUST display total market value per side.
- **TA-009**: The analyzer MUST display total adjusted trade value per side.
- **TA-010**: The analyzer MUST display trade percentage per side.
- **TA-011**: The analyzer MUST display net difference.
- **TA-012**: The analyzer MUST display a fairness result.
- **TA-013**: The analyzer MUST display confidence rating.
- **TA-014**: The analyzer MUST warn when any valuation confidence is LOW.
- **TA-015**: The analyzer MUST use calculated market value, not a single raw
  pricing source.
- **TA-016**: The trade analyzer MUST support analysis-only mode.
- **TA-017**: Analysis-only mode MUST compare both trade sides without changing
  portfolio records, owned item status, disposal records, realised gain/loss, or
  cost basis.
- **TA-018**: The trade analyzer MUST support execute trade mode after explicit
  user confirmation.
- **TA-019**: Execute trade mode MUST mark outgoing owned items as TRADED.
- **TA-020**: Execute trade mode MUST create disposal records for outgoing owned
  items.
- **TA-021**: Outgoing trade realised gain/loss MUST equal Trade Value Received
  minus Original Purchase Price.
- **TA-022**: Execute trade mode MUST add incoming cards/products to the
  portfolio as separate owned records.
- **TA-023**: Execute trade mode MUST link all outgoing items, incoming items,
  and disposal records to one trade transaction.
- **TA-024**: Incoming items MUST receive allocated cost basis based on their
  agreed trade values.
- **TA-025**: If total incoming agreed value differs from total outgoing trade
  value, the system MUST allocate incoming cost basis proportionally and display
  the difference as trade imbalance.
- **TA-026**: The existing simple Trade Away disposal flow MUST remain available
  for cases where the owner does not want to record incoming cards yet.
- **TA-027**: Full Trade Transaction MUST be the preferred workflow when incoming
  cards/products are known.

## 12. Grading Analyzer Requirements

- **GA-001**: The grading analyzer MUST be manually triggered by the user.
- **GA-002**: The app MUST NOT automatically analyze grading candidates in v1.
- **GA-003**: PSA MUST be the only grading company in v1.
- **GA-004**: The analyzer MUST consider raw value, PSA 8 value, PSA 9 value, PSA
  10 value, grading cost, turnaround time, opportunity cost, PSA gem rate,
  grading intensity, grading lag adjustment, and confidence rating.
- **GA-005**: The analyzer MUST show a conservative scenario assuming PSA 8.
- **GA-006**: The analyzer MUST show a balanced scenario assuming PSA 9.
- **GA-007**: The analyzer MUST show an aggressive scenario assuming PSA 10.
- **GA-008**: Each scenario MUST show expected value, total cost, expected profit,
  ROI, and recommendation.
- **GA-009**: A recommendation is profitable only if expected profit exceeds SGD
  50.
- **GA-010**: The system MUST detect and warn when raw value is greater than PSA
  8 value.
- **GA-011**: The system MUST detect and warn when raw value is greater than PSA
  9 value.
- **GA-012**: PSA fees and turnaround estimates MUST be stored as updateable
  data.
- **GA-013**: PSA fees and turnaround estimates MUST NOT be permanently
  hardcoded.
- **GA-014**: Grading analysis MUST include confidence rating and explanation.

## 13. Forecasting Requirements

- **FC-001**: Forecasting MUST be advisory only.
- **FC-002**: Forecasting MUST never be shown as guaranteed future value.
- **FC-003**: The user MUST be able to select a 30-day forecast horizon.
- **FC-004**: The user MUST be able to select a 90-day forecast horizon.
- **FC-005**: The user MUST be able to select a 180-day forecast horizon.
- **FC-006**: The user MUST be able to select a 365-day forecast horizon.
- **FC-007**: Forecast output MUST include expected future value.
- **FC-008**: Forecast output MUST include forecast range.
- **FC-009**: Forecast output MUST include confidence level.
- **FC-010**: Forecast output MUST include explanation.
- **FC-011**: Forecasting SHOULD consider historical prices, price volatility,
  trend consistency, pull cost, months since release, active listing count,
  supply shift index, character premium, universal appeal, card appeal, grading
  intensity, demand pressure, rip-risk premium, grading lag adjustment, PSA gem
  rate, and product type where data exists.
- **FC-012**: v1 forecasting MAY be rules-based or weighted-score based.
- **FC-013**: Machine learning MUST be treated as a later-phase enhancement after
  enough clean historical data exists.

## 14. Card and Sealed Product Catalogue Requirements

- **CA-001**: The catalogue MUST support English cards first.
- **CA-002**: The catalogue MUST support Japanese cards in a later phase.
- **CA-003**: The catalogue MUST support Chinese cards as lower priority than
  Japanese.
- **CA-004**: The catalogue MUST support sealed products.
- **CA-005**: The catalogue MUST distinguish English, Japanese, and Chinese
  markets.
- **CA-006**: Japanese cards MUST NOT be forced into English equivalent cards.
- **CA-007**: Manual card/product search MUST be available in v1.
- **CA-008**: Image recognition MUST be treated as a later-phase feature.
- **CA-009**: Card variants MUST be represented distinctly where data exists,
  including reverse holo, holo, promo, stamped, alternate art, secret rare,
  Master Ball variant, Poke Ball variant, and error cards.
- **CA-010**: Sealed product types MUST include booster packs, booster boxes,
  Elite Trainer Boxes, collection boxes, promo products, and Japanese sealed
  products in later phases.
- **CA-011**: Search results MUST make language/market and variant visible enough
  for the user to choose the correct item.
- **CA-012**: Catalogue detail pages MUST show available item identity,
  language/market, variant, product type, and relevant pricing status.

## 15. UI/UX Requirements

- **UI-001**: The UI MUST use a modern financial dashboard style.
- **UI-002**: The UI MUST be dark mode first.
- **UI-003**: The UI MUST feel tech-focused, professional, analytical, and
  investment-focused.
- **UI-004**: The UI MUST NOT feel childish, toy-like, or cartoon-heavy.
- **UI-005**: The application MUST include a dashboard home page.
- **UI-006**: The dashboard MUST use portfolio summary cards.
- **UI-007**: The UI MUST include line charts for price and portfolio movement.
- **UI-008**: The UI MUST use compact financial-style data tables.
- **UI-009**: The UI MUST use green/red gain-loss indicators.
- **UI-010**: The UI MUST use market signal badges.
- **UI-011**: The UI MUST use confidence badges.
- **UI-012**: The UI MUST include alert panels.
- **UI-013**: The UI MUST include a trade analyzer screen.
- **UI-014**: The UI MUST include a grading analyzer screen.
- **UI-015**: The UI MUST include search and add-to-portfolio workflow.
- **UI-016**: The UI MUST include a detailed card/product page.
- **UI-017**: The UI SHOULD use watchlist-style layouts where they improve
  monitoring and comparison.
- **UI-018**: Pokemon card images and set symbols MAY be used as item context.
- **UI-019**: Pokemon imagery MUST NOT dominate the interface or replace
  analytics context.
- **UI-020**: The UI MUST avoid excessive bright colors, unnecessary animations,
  and cluttered card grids without analytics context.
- **UI-021**: Screens with financial data MUST keep high information density
  without clutter.
- **UI-022**: Gain/loss, confidence, and signal states MUST be visually
  distinguishable without relying only on text.
- **UI-023**: All monetary displays MUST clearly show SGD.

## 16. Non-Functional Requirements

- **NFR-001**: The application MUST use secure authentication.
- **NFR-002**: Passwords MUST be hashed.
- **NFR-003**: Secrets MUST NOT be committed to code or repository-managed
  configuration.
- **NFR-004**: Administrative actions MUST be protected.
- **NFR-005**: Important calculations MUST be explainable.
- **NFR-006**: Business rules MUST be testable.
- **NFR-007**: External pricing providers MUST be mockable.
- **NFR-008**: Automated tests MUST NOT depend on live external APIs.
- **NFR-009**: The application MUST tolerate missing, delayed, inconsistent, or
  unavailable provider data.
- **NFR-010**: Source-provider failures MUST reduce confidence or show a clear
  unavailable state rather than corrupting valuation.
- **NFR-011**: Historical price and portfolio valuation data MUST preserve data
  integrity across refreshes.
- **NFR-012**: The application SHOULD feel responsive for personal portfolio
  workflows, with search, dashboard, and detail views loading quickly enough for
  repeated daily use.
- **NFR-013**: Dashboard summaries SHOULD be understandable at a glance within 5
  seconds for an existing portfolio.
- **NFR-014**: The project MUST remain maintainable as a modular monolith, not a
  microservices system in v1.
- **NFR-015**: Suggested module boundaries are catalog, portfolio, pricing,
  market_signal, alerts, grading, trade, forecasting, auth, and config.
- **NFR-016**: Business logic MUST remain separate from presentation and
  persistence responsibilities.
- **NFR-017**: The v1 delivery is constrained to Java 21, Spring Boot 3, Spring
  Web, Spring Data JPA, Spring Security, PostgreSQL, Flyway, Spring Scheduler,
  WebClient, and server-rendered web UI first.
- **NFR-018**: Scheduled jobs MUST be idempotent from the user's perspective and
  safe to rerun.
- **NFR-019**: The system MUST store enough metadata to audit market prices,
  expected prices, confidence ratings, portfolio values, alerts, grading results,
  trade values, forecasts, and currency conversion.
- **NFR-020**: The system MUST prefer official, permitted, or documented provider
  access over brittle scraping.

## 17. Out-of-Scope Features

- **OOS-001**: Multi-user public features are out of scope for v1.
- **OOS-002**: Public social feeds, public profiles, collection sharing, and
  community features are out of scope for v1.
- **OOS-003**: Marketplaces, selling workflows, order processing, subscriptions,
  billing, tenancy, and commercial SaaS features are out of scope for v1.
- **OOS-004**: Japanese card support is a later phase.
- **OOS-005**: Chinese card support is a later phase and lower priority than
  Japanese support.
- **OOS-006**: Image recognition is a later phase.
- **OOS-007**: Mobile app support is a later phase.
- **OOS-008**: Advanced forecasting and machine learning are later-phase
  enhancements.
- **OOS-009**: Richer PSA population integration is a later phase.
- **OOS-010**: Automated grading candidate watchlist is a later phase.
- **OOS-011**: Automatic grading recommendations are out of scope for v1.
- **OOS-012**: Storage location tracking is not required for v1.
- **OOS-013**: Childish, cartoon-heavy, toy-like UI is out of scope.
- **OOS-014**: Non-SGD user-facing valuation display is out of scope for v1.

## 18. Assumptions

- **AS-001**: The owner is the only expected v1 user.
- **AS-002**: English cards and sealed products are sufficient for the first
  production-usable release.
- **AS-003**: Pricing data availability will vary by source and item type.
- **AS-004**: Some items will have LOW confidence due to sparse sales or provider
  gaps.
- **AS-005**: Manual search is acceptable for v1 and image recognition can be
  deferred.
- **AS-006**: PSA is the only grading company considered for v1 analysis.
- **AS-007**: Forecasts are advisory decision support and not investment
  guarantees.
- **AS-008**: The user accepts server-rendered web UI first, with mobile support
  deferred.
- **AS-009**: Exchange rates are available from a permitted source or manual
  update workflow.
- **AS-010**: Provider source selection and credentials will be finalized during
  planning without changing product scope.

## 19. Acceptance Criteria

### Measurable Outcomes

- **SC-001**: The owner can search for an item and add an owned portfolio record
  with required purchase details in under 2 minutes.
- **SC-002**: The owner can understand total value, cost basis, unrealized
  gain/loss, and alert status from the dashboard within 5 seconds of viewing it.
- **SC-003**: 100% of reviewed user-facing monetary values are displayed in SGD.
- **SC-004**: 100% of calculated market values reviewed show confidence rating,
  calculation timestamp, and source/currency audit context.
- **SC-005**: A daily refresh preserves prior historical price snapshots for all
  refreshed items.
- **SC-006**: A proposed trade with at least one item on each side produces
  market totals, adjusted totals, net difference, fairness result, and confidence.
- **SC-007**: A manually triggered grading analysis produces PSA 8, PSA 9, and
  PSA 10 scenarios with recommendation and confidence.
- **SC-008**: Forecast views always display advisory language, forecast range,
  confidence, and explanation.

### Edge Cases Covered

- **EC-001**: When a pricing source is unavailable, valuation continues with
  other usable sources and confidence is reduced.
- **EC-002**: When recent sales data is sparse, confidence is LOW or the system
  shows that valuation is unavailable.
- **EC-003**: When exchange-rate data is missing, the system does not display an
  unaudited converted value as final.
- **EC-004**: When Japanese or Chinese catalogue data exists later, it remains in
  its own market and is not merged into an English equivalent.
- **EC-005**: When the owner adds multiple copies of the same card with different
  purchase details, separate portfolio records are created.
- **EC-006**: When raw value exceeds PSA 8 or PSA 9 value, grading analysis shows
  a warning.
- **EC-007**: When a scheduled job is rerun, it does not overwrite historical
  price or portfolio valuation snapshots.
- **EC-008**: When a trade contains any LOW-confidence item, the trade result
  shows a valuation confidence warning.

### Major Flow Criteria

- **AC-001**: Given the owner has valid credentials, when they log in, then they
  can access the dashboard.
- **AC-002**: Given a user is unauthenticated, when they attempt to access a
  protected portfolio page, then the system denies access or requests login.
- **AC-003**: Given the owner searches for a card, when matching English records
  exist, then results show enough identity, market, and variant information to
  choose the correct card.
- **AC-004**: Given the owner adds a card to the portfolio, when they enter
  purchase price, date, condition, language/market, and grading status, then a
  separate owned item is created.
- **AC-005**: Given portfolio items have valuations, when the owner views the
  dashboard, then total value, cost basis, gain/loss, gain/loss percentage, value
  over time, top gainers, top losers, low-confidence valuations, and alerts are
  visible.
- **AC-006**: Given an item has price snapshots, when the owner views the item
  detail page, then the system displays price history without recalculating over
  current prices only.
- **AC-007**: Given a daily price refresh runs, when source data is available,
  then a new price snapshot is stored and prior snapshots remain unchanged.
- **AC-008**: Given pricing sources are available, when valuation is calculated,
  then source-specific prices, source currency, exchange rate, SGD market price,
  confidence, and timestamp are recorded.
- **AC-009**: Given Market Price and Expected Price are calculated, when the
  owner views the item signal, then the system shows classification, SGD
  difference, percentage difference, confidence, and explanation.
- **AC-010**: Given an item purchased below SGD 100 rises by at least SGD 10, when
  alert checks run, then a price alert is generated.
- **AC-011**: Given an item purchased at SGD 100 or more rises by at least SGD 25,
  when alert checks run, then a price alert is generated.
- **AC-012**: Given the owner enters two trade sides and trade percentages, when
  trade analysis runs, then market totals, adjusted totals, net difference,
  fairness, confidence, and LOW-confidence warnings are shown.
- **AC-012A**: Given the owner runs analysis-only trade mode, when the result is
  displayed, then no portfolio item, disposal record, realised gain/loss, or cost
  basis value is changed.
- **AC-012B**: Given the owner executes a trade, when one outgoing item bought for
  SGD 200 is traded at agreed value SGD 500 for incoming items agreed at SGD 250,
  SGD 150, and SGD 100, then realised gain/loss is SGD 300 and the incoming
  portfolio items receive cost basis of SGD 250, SGD 150, and SGD 100.
- **AC-012C**: Given incoming agreed value differs from outgoing agreed trade
  value, when the trade is executed, then incoming cost basis is allocated
  proportionally and trade imbalance is shown.
- **AC-013**: Given the owner manually runs grading analysis, when PSA scenario
  data exists, then conservative, balanced, and aggressive scenarios show
  expected value, total cost, expected profit, ROI, recommendation, confidence,
  and relevant raw-value warnings.
- **AC-014**: Given the owner selects a forecast horizon, when forecast output is
  displayed, then expected value, range, confidence, and explanation are shown as
  advisory only.
- **AC-015**: Given any monetary value is displayed, when the owner views it, then
  it is shown in SGD.
- **AC-016**: Given historical price, valuation, alert, grading, trade, or
  forecast outputs exist, when the system refreshes data, then historical records
  needed for auditability are preserved.
- **AC-017**: Given any UI screen is implemented, when it is reviewed, then it
  follows dark-mode-first professional financial dashboard styling and avoids
  childish or cartoon-heavy presentation.

## 20. Open Questions

- **OQ-001**: Which permitted provider integrations will be available first for
  TCGPlayer, eBay, PriceCharting, exchange rates, and PSA-related data?
- **OQ-002**: Which exchange-rate source will be used for v1 audit metadata?
- **OQ-003**: What initial character premium and card appeal scoring values will
  be seeded for the first release?
- **OQ-004**: Which charting and table interaction patterns best fit
  server-rendered UI while preserving the financial dashboard direction?
- **OQ-005**: Should analysis-only trade comparisons and grading analyses be
  saved by default, or remain on-demand with optional save? Executed trades MUST
  be saved as trade transactions.

## Constitution Conflicts

- **CC-001**: No conflicts found. The specification follows the current
  constitution on disk, including the v1.1.0 Modern Financial Dashboard UI
  amendment, while this product specification remains versioned as v1.0.
