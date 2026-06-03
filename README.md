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

