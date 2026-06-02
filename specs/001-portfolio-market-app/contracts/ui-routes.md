# Contract: Server-Rendered UI Routes

All application routes except login and static assets require authentication.

## Authentication

- `GET /login`: show login page
- `POST /login`: authenticate owner
- `POST /logout`: log out owner

## Dashboard

- `GET /`: redirect to dashboard
- `GET /dashboard`: show portfolio summary, value chart, alerts, top gainers,
  top losers, low-confidence valuations, and market movement/watchlist panels

## Portfolio

- `GET /portfolio`: list owned items
- `GET /portfolio/add`: show add card/product workflow
- `POST /portfolio/items`: create owned item
- `GET /portfolio/items/{id}`: show owned item detail
- `GET /portfolio/items/{id}/edit`: edit owned item form
- `POST /portfolio/items/{id}`: update owned item
- `POST /portfolio/items/{id}/archive`: archive owned item

## Catalog

- `GET /catalog/search`: search cards and sealed products
- `GET /catalog/cards/{id}`: card detail
- `GET /catalog/sealed-products/{id}`: sealed product detail

## Pricing

- `GET /pricing/items/{itemType}/{id}/history`: price history graph and
  snapshot table
- `GET /pricing/manual-entry`: manual price entry form
- `POST /pricing/manual-entry`: create manual price entry

## Alerts

- `GET /alerts`: new, active, and historical alerts
- `POST /alerts/{id}/acknowledge`: acknowledge alert
- `POST /alerts/{id}/dismiss`: dismiss alert

## Trade Analyzer

- `GET /trade`: trade analyzer form
- `POST /trade/analyze`: calculate trade result
- `POST /trade/save`: save trade analysis when requested

## Grading Analyzer

- `GET /grading`: grading analyzer form
- `POST /grading/analyze`: run manual PSA grading analysis
- `POST /grading/save`: save grading analysis when requested

## Forecasting

- `GET /forecast`: forecast item/horizon form
- `POST /forecast/run`: create forecast snapshot and show result

## Settings / Provider Configuration

- `GET /settings`: owner/admin settings
- `GET /settings/providers`: provider status and configuration
- `POST /settings/providers/{provider}/toggle`: enable or disable provider
- `GET /settings/grading-fees`: PSA fee and turnaround maintenance
- `POST /settings/grading-fees`: create or update PSA fee data

## UI Rules

- Pages use dark-mode-first financial dashboard styling.
- Monetary values display SGD.
- Gain/loss uses green/red semantics plus text labels.
- Confidence and signal states use badges.
- Pokemon imagery is contextual and does not replace analytics.

