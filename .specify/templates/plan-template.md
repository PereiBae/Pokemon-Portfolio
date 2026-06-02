# Implementation Plan: [FEATURE]

**Branch**: `[###-feature-name]` | **Date**: [DATE] | **Spec**: [link]

**Input**: Feature specification from `/specs/[###-feature-name]/spec.md`

**Note**: This template is filled in by the `/speckit-plan` command. See `.specify/templates/plan-template.md` for the execution workflow.

## Summary

[Extract from feature spec: primary requirement + technical approach from research]

## Technical Context

<!--
  ACTION REQUIRED: Replace the content in this section with the technical details
  for the project. The structure here is presented in advisory capacity to guide
  the iteration process.
-->

**Language/Version**: Java 21

**Primary Dependencies**: Spring Boot 3, Spring Web, Spring Data JPA, Spring
Security, Flyway, Spring Scheduler, WebClient

**Storage**: PostgreSQL with Flyway migrations

**Testing**: JUnit 5, Spring Boot Test, Mockito or equivalent test doubles, and
repository/integration tests that do not depend on live external APIs

**Target Platform**: Personal-use web application

**Project Type**: Modular monolith Spring Boot web application

**Performance Goals**: [domain-specific, e.g., 1000 req/s, 10k lines/sec, 60 fps or NEEDS CLARIFICATION]

**Constraints**: One authenticated personal user first; all user-facing monetary
values in SGD; append-only price and portfolio valuation snapshots; provider
adapters required for external data

**Scale/Scope**: v1 English cards and sealed products, manual search, portfolio
tracking, pricing snapshots, calculated market value, confidence ratings, basic
Market Signal Engine, alerts, trade analyzer, and manual PSA grading analyzer

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- **Personal-use scope**: Feature serves one authenticated owner and avoids
  marketplace, public social, or SaaS behaviors unless a governed exception is
  documented.
- **Market and language separation**: English, Japanese, and Chinese markets stay
  distinct where language affects catalog, pricing, population, or forecasts.
- **Collectible coverage**: Plan states whether it applies to cards, sealed
  products, variants, owned copies, or all of them.
- **Ownership model**: Owned items remain individual records when condition,
  purchase price, grading status, PSA data, purchase date, or language may
  differ.
- **SGD and auditability**: Display values are SGD and source currency, exchange
  rate, source metadata, and conversion time are preserved.
- **Historical preservation**: Price refreshes and portfolio valuations append
  snapshots instead of overwriting history.
- **Pricing hierarchy and confidence**: Provider-specific prices are stored
  separately, hierarchy rules are respected, and HIGH/MEDIUM/LOW confidence is
  calculated.
- **Provider abstraction**: External sources are accessed through adapters and
  degraded gracefully when unavailable.
- **Market Signal Engine**: Market Price and Expected Price remain separate,
  explainable, and classified as undervalued, fairly valued, or overvalued where
  relevant.
- **Alerts, grading, trades, forecasts**: Any feature touching these domains uses
  calculated market values, stored snapshots, confidence ratings, and
  constitution-defined thresholds/scenarios.
- **Security**: Spring Security, hashed passwords, secure configuration, and
  protected admin-only actions are accounted for.
- **Maintainability**: Design keeps a Spring Boot modular monolith with thin
  controllers, service-layer business logic, persistence-only repositories, and
  package boundaries for catalog, portfolio, pricing, market_signal, alerts,
  grading, trade, forecasting, auth, and config.
- **Testing**: Business rules have automated tests and provider interactions are
  mockable without live external APIs.

## Project Structure

### Documentation (this feature)

```text
specs/[###-feature]/
├── plan.md              # This file (/speckit-plan command output)
├── research.md          # Phase 0 output (/speckit-plan command)
├── data-model.md        # Phase 1 output (/speckit-plan command)
├── quickstart.md        # Phase 1 output (/speckit-plan command)
├── contracts/           # Phase 1 output (/speckit-plan command)
└── tasks.md             # Phase 2 output (/speckit-tasks command - NOT created by /speckit-plan)
```

### Source Code (repository root)
<!--
  ACTION REQUIRED: Replace the placeholder tree below with the concrete layout
  for this feature. Delete unused options and expand the chosen structure with
  real paths (e.g., apps/admin, packages/something). The delivered plan must
  not include Option labels.
-->

```text
src/main/java/[base_package]/
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
├── application.yml
└── db/migration/

src/test/java/[base_package]/
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

**Structure Decision**: [Document the selected structure and reference the real
directories captured above]

## Complexity Tracking

> **Fill ONLY if Constitution Check has violations that must be justified**

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| [e.g., 4th project] | [current need] | [why 3 projects insufficient] |
| [e.g., Repository pattern] | [specific problem] | [why direct DB access insufficient] |
