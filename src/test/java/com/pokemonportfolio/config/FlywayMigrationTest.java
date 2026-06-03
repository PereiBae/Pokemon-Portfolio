package com.pokemonportfolio.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.Set;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class FlywayMigrationTest {

    @Autowired
    private DataSource dataSource;

    @Test
    void verticalSliceTablesExist() throws Exception {
        Set<String> expectedTables = Set.of(
                "app_user",
                "pokemon_set",
                "card",
                "owned_item",
                "price_snapshot",
                "portfolio_valuation_snapshot");

        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            for (String table : expectedTables) {
                try (ResultSet resultSet = metaData.getTables(null, null, table, new String[] {"TABLE"})) {
                    assertThat(resultSet.next()).as("table %s should exist", table).isTrue();
                }
            }
        }
    }
}

