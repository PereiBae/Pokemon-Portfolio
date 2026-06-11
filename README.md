# Pokemon Portfolio

Personal-use Spring Boot dashboard for tracking Pokemon cards as collectible investments.

## Vertical Slice 1

Implemented scope:
- Login with one owner account
- Manual English card creation
- Add a card copy to the portfolio as a separate owned record
- Generate SGD-only mock price snapshots
- Calculate dashboard value in SGD
- Store append-only portfolio valuation snapshots

Later-phase features such as real pricing providers, Japanese/Chinese cards, trade analysis, grading analysis, and forecasting are intentionally not implemented yet.

## Local Run

Requirements:
- Java 21
- Maven
- PostgreSQL database named `pokemon_portfolio`

Set local configuration:

```sh
export DATABASE_URL=jdbc:postgresql://localhost:5432/pokemon_portfolio
export DATABASE_USERNAME=pokemon_portfolio
export DATABASE_PASSWORD=your_database_password
export APP_OWNER_USERNAME=owner@example.com
export APP_OWNER_BOOTSTRAP_PASSWORD=choose-a-local-password
```

Run:

```sh
mvn spring-boot:run
```

Open `http://localhost:8080/login` and log in with the configured owner username and bootstrap password.

Tests run with an H2 database in PostgreSQL compatibility mode and use only mock pricing data:

```sh
mvn test
```

## pokemon-api.com Pricing Provider

The `PokemonApiPricingProvider` remains separate from the official Pokemon TCG
API catalogue integration, mock pricing, and manual price entry. It can be
tested from the UI and, when explicitly enabled/configured, used by the real
price refresh flow.

Configuration:

```sh
export POKEMON_API_PRICING_ENABLED=false
export POKEMON_API_PRICING_BASE_URL=https://pokemon-tcg-api.p.rapidapi.com
export POKEMON_API_RAPIDAPI_KEY=your_rapidapi_key
```

Keep `POKEMON_API_PRICING_ENABLED=false` for normal local runs. To manually spike
the provider from the app UI, enable it in a development-only profile or local
environment, start the app, log in, and open:

```text
http://localhost:8080/settings/providers/pokemon-api/test
```

The test harness supports:

- `fetchOneCard("3852")` for numeric internal card IDs
- `fetchOneCard("Scizor Obsidian Flames 205")` for search text
- `fetchProductById(20039L)` for sealed product checks

Example local run:

```sh
export DATABASE_URL=jdbc:postgresql://localhost:5432/pokemon_portfolio
export DATABASE_USERNAME=pokemon_portfolio
export DATABASE_PASSWORD=your_database_password
export APP_OWNER_USERNAME=owner@example.com
export APP_OWNER_BOOTSTRAP_PASSWORD=choose-a-local-password
export POKEMON_API_PRICING_ENABLED=true
export POKEMON_API_RAPIDAPI_KEY=your_rapidapi_key
mvn spring-boot:run
```

Tests for this provider use mocked WebClient responses only and must not call the
live RapidAPI endpoint.

Research notes from pokemon-api.com docs:

- Raw card support: likely usable for USD TCGPlayer `market_price` and `mid_price`.
- PSA support: likely usable for eBay sold graded median prices and sample sizes,
  including PSA 8, PSA 9, and PSA 10 when data exists.
- Sealed support: likely usable for sealed product records. The docs clearly show
  Cardmarket sealed fields, while TCGPlayer sealed fields should be treated as
  optional until verified with real responses.
- Known limitations: RapidAPI key and plan required, rate limits apply, source
  data is third-party aggregated, and marketplace field availability varies.
- Recommendation: promote to a production provider only after validating several
  real cards and sealed products, documenting exact currency/source semantics,
  adding provider-result persistence, adding exchange-rate conversion, and
  defining confidence rules for missing PSA/sample-size data.

## Candidate Pricing Provider Spikes

PokeTrace and PokemonPriceTracker are available as read-only spike providers.
They are disabled by default, do not create price snapshots, and do not change
dashboard valuation, alerts, trades, grading, or manual price entry.

Configuration:

```sh
export POKETRACE_PRICING_ENABLED=false
export POKETRACE_PRICING_BASE_URL=https://api.poketrace.com/v1
export POKETRACE_API_KEY=your_poketrace_key

export POKEMON_PRICE_TRACKER_ENABLED=false
export POKEMON_PRICE_TRACKER_BASE_URL=https://www.pokemonpricetracker.com/api/v2
export POKEMON_PRICE_TRACKER_API_KEY=your_pokemon_price_tracker_key
```

Manual spike run:

```sh
export DATABASE_URL=jdbc:postgresql://localhost:5432/pokemon_portfolio
export DATABASE_USERNAME=pokemon_portfolio
export DATABASE_PASSWORD=your_database_password
export APP_OWNER_USERNAME=owner@example.com
export APP_OWNER_BOOTSTRAP_PASSWORD=choose-a-local-password

export POKETRACE_PRICING_ENABLED=true
export POKETRACE_API_KEY=your_poketrace_key

export POKEMON_PRICE_TRACKER_ENABLED=true
export POKEMON_PRICE_TRACKER_API_KEY=your_pokemon_price_tracker_key

mvn spring-boot:run
```

Browser URLs after login:

```text
http://localhost:8080/settings/providers/poketrace/test
http://localhost:8080/settings/providers/pokemon-price-tracker/test
http://localhost:8080/settings/providers/comparison/test
```

Implementation notes:

- PokeTrace sends `X-API-Key` and maps card identity, set, card number, market,
  print variant, Near Mint raw price, low/average/high, sale count, source
  market/currency, updated timestamp, and PSA 8/9/10 tiers where returned.
- PokemonPriceTracker sends `Authorization: Bearer <key>` and maps card identity,
  set, card number, language, print variant, raw market/Near Mint price,
  source currency, updated timestamp, PSA 8/9/10 fields, sample size, and
  plan/quota diagnostics where returned.
- The comparison page compares pokemon-api.com, PokeTrace, and
  PokemonPriceTracker for one exact card and shows a converted SGD preview only
  when a matching exchange rate already exists.
- Automated tests use mocked responses only and never call live provider APIs.

Suggested live validation set:

1. Modern normal card
2. Modern holofoil
3. Modern reverse holofoil
4. Vintage holo
5. PSA 8 card
6. PSA 9 card
7. PSA 10 card
8. Japanese card
9. Promo
10. Obscure card with limited sales

For each card, record exact identity match, exact variant match, raw price
availability, PSA availability, currency, sale/sample count, plan restrictions,
and whether the result is safe for production use.

Promotion rules:

- Promote a raw provider only if live responses consistently return exact card
  identity, exact print variant, Near Mint value, recent update date, and useful
  sales/sample count.
- Promote a PSA provider only if PSA 8, PSA 9, and PSA 10 are returned under the
  active plan with clearly separated grades and sales/sample context.
- If neither candidate satisfies those rules, keep pokemon-api.com graded values
  and manual PSA entries until a stronger provider is available.
