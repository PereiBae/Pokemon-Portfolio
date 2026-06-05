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

## pokemon-api.com Pricing Provider Spike

The `PokemonApiPricingProvider` spike is present but intentionally not wired into
portfolio valuation. It does not replace the official Pokemon TCG API catalogue
integration, mock pricing, or manual price entry.

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
  data is third-party aggregated, marketplace field availability varies, and
  this spike does not yet store source-specific provider results or convert
  currencies to SGD.
- Recommendation: promote to a production provider only after validating several
  real cards and sealed products, documenting exact currency/source semantics,
  adding provider-result persistence, adding exchange-rate conversion, and
  defining confidence rules for missing PSA/sample-size data.
