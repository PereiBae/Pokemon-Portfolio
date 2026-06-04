# Data Model: Pokemon Card Portfolio & Market Analytics

## Overview

The data model preserves market separation, individual ownership records,
source-specific pricing data, SGD conversion auditability, append-only
historical snapshots, and explainable analysis outputs.

## Enumerations

- `LanguageMarket`: ENGLISH, JAPANESE, CHINESE
- `ItemType`: CARD, SEALED_PRODUCT
- `CardVariant`: STANDARD, REVERSE_HOLO, HOLO, PROMO, STAMPED, ALTERNATE_ART,
  SECRET_RARE, MASTER_BALL, POKE_BALL, ERROR
- `SealedProductType`: BOOSTER_PACK, BOOSTER_BOX, ELITE_TRAINER_BOX,
  COLLECTION_BOX, PROMO_PRODUCT, OTHER
- `Condition`: RAW_NEAR_MINT, RAW_LIGHTLY_PLAYED, RAW_MODERATELY_PLAYED,
  RAW_HEAVILY_PLAYED, RAW_DAMAGED, SEALED_NEW, SEALED_DAMAGED
- `GradedStatus`: UNGRADED, PSA_GRADED
- `ConfidenceRating`: HIGH, MEDIUM, LOW
- `SignalClassification`: UNDERVALUED, FAIRLY_VALUED, OVERVALUED
- `AlertStatus`: NEW, ACTIVE, ACKNOWLEDGED, DISMISSED
- `TradeSideType`: USER, OTHER_PARTY
- `TradeTransactionMode`: ANALYSIS_ONLY, EXECUTE_TRADE
- `TradeTransactionStatus`: DRAFT, ANALYZED, EXECUTED, CANCELLED
- `ForecastHorizon`: DAYS_30, DAYS_90, DAYS_180, DAYS_365

## Entities

### AppUser

Fields:
- id
- username or email
- password hash
- display name
- role
- enabled
- created at
- updated at
- last login at

Relationships:
- One user owns many owned items, alerts, portfolio snapshots, trade
  transactions, grading analyses, and forecast snapshots.

Validation:
- Password hash is required.
- Plaintext password is never persisted.

### PokemonSet

Fields:
- id
- set name
- set code
- language market
- release date
- product line
- external references
- created at
- updated at

Relationships:
- One set has many cards.

Validation:
- Set identity is market-specific.

### Card

Fields:
- id
- set id
- name
- card number
- rarity
- language market
- variant
- character tags
- release date
- image URL
- external references
- active flag

Relationships:
- Many cards belong to one set.
- One card may have many owned items, provider results, price snapshots, market
  signal snapshots, grading analyses, and forecast snapshots.

Validation:
- Japanese and Chinese records remain separate from English records.
- Variant is explicit where known.

### SealedProduct

Fields:
- id
- name
- product type
- language market
- release date
- image URL
- external references
- active flag

Relationships:
- One sealed product may have many owned items, provider results, price
  snapshots, market signal snapshots, trade items, and forecast snapshots.

Validation:
- Product type is required.

### OwnedItem

Fields:
- id
- app user id
- item type
- card id
- sealed product id
- language market
- condition
- purchase price SGD
- purchase date
- graded status
- PSA grade
- PSA certification number
- notes
- created at
- updated at
- archived at

Relationships:
- Belongs to one user.
- References exactly one card or sealed product.
- May have many alerts and trade items.

Validation:
- No quantity field.
- Exactly one catalog reference is required.
- PSA grade/certification number apply only when graded status is PSA_GRADED.
- Storage location is not required.

### PricingProviderResult

Fields:
- id
- provider name
- provider type
- item type
- card id
- sealed product id
- graded status
- PSA grade
- source price
- source currency
- source timestamp
- observed at
- source URL/reference where allowed
- listing count
- recent sales count
- provider confidence
- fetch status
- error code
- raw metadata summary

Relationships:
- May contribute to one or more calculated price snapshots.

Validation:
- Source currency and observed timestamp are required for usable price results.

### ExchangeRateSnapshot

Fields:
- id
- base currency
- target currency
- rate
- rate source
- effective at
- fetched at
- confidence

Validation:
- Target currency is SGD for v1 display.
- Missing rate prevents unaudited final SGD display.

### PriceSnapshot

Fields:
- id
- item type
- card id
- sealed product id
- graded status
- PSA grade
- market price SGD
- source currency summary
- exchange-rate summary
- confidence rating
- calculation timestamp
- pricing basis
- explanation
- latest sale at
- source disagreement score
- volatility score

Relationships:
- References source provider results through an association or calculation
  metadata.
- Can have one market signal snapshot.
- Can trigger alerts.

Validation:
- Append-only.
- Market price is SGD.

### MarketSignalSnapshot

Fields:
- id
- price snapshot id
- expected price SGD
- market price SGD
- difference SGD
- percentage difference
- classification
- confidence rating
- explanation
- supply score
- demand score
- modifier score
- created at

Validation:
- Market Price and Expected Price remain separate.
- Explanation is required.

### PortfolioValuationSnapshot

Fields:
- id
- app user id
- total value SGD
- total cost basis SGD
- unrealized gain/loss SGD
- unrealized gain/loss percent
- item count
- low confidence count
- snapshot date
- calculated at
- explanation

Validation:
- Graphs use stored snapshots.
- Snapshot data is not destructively recalculated.

### Alert

Fields:
- id
- owned item id
- price snapshot id
- alert type
- threshold rule
- purchase price SGD
- current market value SGD
- gain amount SGD
- gain percent
- source confidence
- status
- triggered at
- acknowledged at

State transitions:
- NEW -> ACTIVE -> ACKNOWLEDGED
- NEW or ACTIVE -> DISMISSED

Validation:
- Default threshold rules are fixed in v1.
- Reruns must not create duplicate alerts for the same trigger.

### TradeTransaction

Fields:
- id
- app user id
- name
- mode
- status
- notes
- total user market value SGD
- total other market value SGD
- total user adjusted value SGD
- total other adjusted value SGD
- total outgoing agreed value SGD
- total incoming agreed value SGD
- trade imbalance SGD
- net difference SGD
- fairness result
- confidence rating
- executed at
- created at

Relationships:
- Has two trade transaction sides.

Validation:
- Exactly one USER side and one OTHER_PARTY side.
- Analysis-only mode does not mutate portfolio, disposal, realised gain/loss, or
  cost-basis records.
- Execute-trade mode links all outgoing, incoming, and disposal records.

### TradeTransactionSide

Fields:
- id
- trade transaction id
- side type
- trade percentage
- total market value SGD
- total adjusted value SGD
- total agreed value SGD

Relationships:
- Has many trade transaction items.

Validation:
- Trade percentage is positive and can be independently set per side.

### TradeTransactionItem

Fields:
- id
- trade transaction side id
- item type
- card id
- sealed product id
- quantity for proposed trade display only
- market value SGD
- adjusted trade value SGD
- agreed trade value SGD
- allocated cost basis SGD
- outgoing owned item id, nullable
- incoming owned item id, nullable
- disposal record id, nullable
- confidence rating
- warning

Validation:
- Uses calculated market value.
- Outgoing item realised gain/loss is trade value received minus original
  purchase price.
- Incoming allocated cost basis uses agreed incoming item values when totals
  match, or proportional allocation when incoming and outgoing totals differ.

### GradingFee

Fields:
- id
- grading company
- service level
- fee amount
- fee currency
- turnaround days estimate
- effective from
- effective to
- source/provenance

Validation:
- PSA only in v1.
- Fees are updateable data.

### GradingAnalysis

Fields:
- id
- app user id
- owned item id
- card id
- raw value SGD
- PSA gem rate
- grading intensity
- grading lag warning
- confidence rating
- warnings
- created at

Relationships:
- Has three grading scenarios.

Validation:
- Manually triggered only.

### GradingScenario

Fields:
- id
- grading analysis id
- assumed PSA grade
- expected value SGD
- total cost SGD
- expected profit SGD
- ROI percent
- recommendation
- confidence rating

Validation:
- PSA 8, PSA 9, and PSA 10 scenarios are required.
- Profitable recommendation requires expected profit above SGD 50.

### ForecastSnapshot

Fields:
- id
- app user id
- item type
- card id
- sealed product id
- horizon days
- expected future value SGD
- lower bound SGD
- upper bound SGD
- confidence rating
- explanation
- model type
- created at

Validation:
- Advisory only.
- Horizon must be 30, 90, 180, or 365 days.

### ScheduledJobRun

Fields:
- id
- job type
- scheduled key
- started at
- completed at
- status
- items processed
- errors count
- summary

Validation:
- Used for rerun safety and auditability.

### ManualPriceEntry

Fields:
- id
- app user id
- item type
- card id
- sealed product id
- price SGD
- source note
- entered at
- confidence rating

Validation:
- Treated as manual provider fallback.
- Default confidence is LOW unless user marks supporting context.
