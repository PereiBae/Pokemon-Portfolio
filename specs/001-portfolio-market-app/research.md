# Research: Pokemon Card Portfolio & Market Analytics

## Decision: Use Thymeleaf for v1 server-rendered UI

**Rationale**: Thymeleaf is aligned with Spring Boot, supports server-rendered
forms and pages with minimal moving parts, and keeps v1 beginner-friendly while
still allowing a professional dashboard UI.

**Alternatives considered**: A separate JavaScript frontend was rejected for v1
because it adds build complexity and creates another project boundary. A mobile
app was rejected because mobile support is a later phase.

## Decision: Use a Spring Boot modular monolith

**Rationale**: The constitution requires a beginner-friendly modular monolith.
Package boundaries provide separation without microservice overhead.

**Alternatives considered**: Microservices were rejected by the constitution and
would add unnecessary deployment, data consistency, and operational complexity.

## Decision: Store source-specific provider results before calculated snapshots

**Rationale**: Provider-specific records preserve auditability and allow the
system to explain how market value was derived. Calculated price snapshots can
then reference source quality and conversion metadata.

**Alternatives considered**: Storing only the final market price was rejected
because it would break auditability, confidence explanations, and source
hierarchy review.

## Decision: Use provider adapters for all external pricing sources

**Rationale**: TCGPlayer, eBay, PriceCharting, mock providers, and manual price
entry must be replaceable. Adapters isolate WebClient usage, credentials,
provider terms, rate limits, and data mapping.

**Alternatives considered**: Calling providers directly from business services
was rejected because it tightly couples valuation logic to provider availability
and makes testing depend on live APIs.

## Decision: Use mock and manual pricing sources in v1 development

**Rationale**: Provider access may be unavailable, paid, blocked, or legally
restricted. Mock data enables deterministic tests and local development. Manual
price entry provides a fallback while still preserving auditability and
confidence metadata.

**Alternatives considered**: Blocking valuation until official provider access
exists was rejected because it would prevent MVP progress and testing.

## Decision: Use append-only snapshots for price, portfolio, exchange-rate,
market signal, forecast, and analysis history

**Rationale**: Historical preservation is required for price graphs, portfolio
graphs, alerts, grading analysis, trade analysis, forecasting, and auditability.

**Alternatives considered**: Updating a current-price row in place was rejected
because it destroys history and prevents trend analysis.

## Decision: Calculate confidence from source quality inputs

**Rationale**: Confidence must reflect source count, recent sale count, latest
sale age, source disagreement, listing liquidity, and volatility. This makes LOW
confidence visible when data is sparse or unreliable.

**Alternatives considered**: A fixed provider-based confidence rating was
rejected because it ignores recency, liquidity, disagreement, and volatility.

## Decision: Use a rules-based/weighted Market Signal Engine in v1

**Rationale**: The constitution requires explainability. A weighted score can
combine supply, demand, and modifiers while producing human-readable reasons.

**Alternatives considered**: Machine learning was rejected for v1 because there
is not yet enough clean historical data and it would reduce explainability.

## Decision: Make scheduled jobs rerunnable with job-run metadata and dedupe

**Rationale**: Daily refreshes must be safe to rerun. Job-run metadata supports
auditability, while append-only snapshots and alert dedupe prevent destructive
updates and duplicate user-facing alerts.

**Alternatives considered**: A single mutable job status field was rejected
because it is less auditable and does not explain partial provider failures.

## Decision: Use updateable PSA grading fee data

**Rationale**: PSA fees and turnaround estimates change. Storing effective fee
records keeps grading recommendations current without code changes.

**Alternatives considered**: Hardcoded fees were rejected by the constitution.

## Decision: Keep forecasting advisory and rules-based

**Rationale**: Forecasting must communicate uncertainty and avoid guaranteed
future value claims. A rules-based model can explain historical trend,
volatility, signal strength, and data gaps.

**Alternatives considered**: Predictive ML was rejected for v1 and retained as a
future enhancement after sufficient clean historical data exists.

