# Quickstart: Pokemon Card Portfolio & Market Analytics

## Purpose

This quickstart describes how to validate the planned v1 application once tasks
are implemented. It is written for local development and smoke testing.

## Prerequisites

- Java 21
- PostgreSQL available locally
- Environment variables for database connection
- Optional provider credentials for permitted TCGPlayer, eBay, PriceCharting,
  exchange-rate, and PSA-related sources

## Local Setup

1. Create a local PostgreSQL database for the app.
2. Configure database URL, username, and password through environment variables
   or secure local configuration.
3. Configure provider credentials through environment variables only. Leave
   providers disabled or use mock/manual providers when credentials are absent.
4. Run the application build.
5. Run automated tests.
6. Start the application.
7. Open the login page.

## Smoke Test Flow

1. Log in as the owner.
2. Confirm the dark financial dashboard loads.
3. Search for an English card.
4. Add one owned card copy with condition, purchase price, purchase date, and
   grading status.
5. Add a sealed product copy.
6. Run or trigger mock pricing refresh.
7. Confirm source-specific provider results are stored.
8. Confirm a new calculated price snapshot is created in SGD.
9. Confirm the portfolio dashboard shows total value, cost basis, gain/loss,
   low-confidence valuations, and alerts.
10. Open an item detail page and confirm price history, Market Price, Expected
    Price, signal classification, confidence, and explanation.
11. Run alert checks and confirm default thresholds.
12. Run trade analysis with one item on each side and independent trade
    percentages.
13. Run manual PSA grading analysis and verify PSA 8, PSA 9, and PSA 10
    scenarios.
14. View a 30-day forecast and confirm advisory language, range, confidence, and
    explanation.
15. Confirm all displayed monetary values are in SGD.

## Expected Validation Results

- Historical price snapshots are not overwritten by refreshes.
- Portfolio valuation snapshots are stored for graphing.
- Provider failures lower confidence or show unavailable states rather than
  stopping the entire valuation flow.
- No test or smoke workflow requires live external APIs when mock providers are
  enabled.
- UI remains professional, compact, dark-mode-first, and analytics-focused.

