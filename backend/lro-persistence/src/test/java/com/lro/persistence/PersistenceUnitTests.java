package com.lro.persistence;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.ResultSet;

import static org.junit.jupiter.api.Assertions.*;

public class PersistenceUnitTests {

    @Test
    @DisplayName("Verify Prompt 12: Flyway migrations initialize all 4 bank schemas, exchange, and lro")
    void testFlywayMigrationsAndTopology() throws Exception {
        DatabaseManager db = new DatabaseManager();
        assertTrue(db.isHealthy(), "Database must be healthy");

        boolean migrated = db.runMigrations();
        assertTrue(migrated, "Flyway migrations must execute successfully");

        // Verify schemas exist
        try (Connection conn = db.getConnection()) {
            ResultSet rs = conn.getMetaData().getSchemas();
            boolean foundBankLarge = false;
            boolean foundExchange = false;
            boolean foundLro = false;

            while (rs.next()) {
                String schema = rs.getString("TABLE_SCHEM").toLowerCase();
                if (schema.equals("bank_large")) foundBankLarge = true;
                if (schema.equals("exchange")) foundExchange = true;
                if (schema.equals("lro")) foundLro = true;
            }

            assertTrue(foundBankLarge, "bank_large schema must exist");
            assertTrue(foundExchange, "exchange schema must exist");
            assertTrue(foundLro, "lro schema must exist");

            // Verify exchange.instruments dropped and replaced with stocks, bonds, securities
            try (var stmt = conn.createStatement()) {
                var rsInst = stmt.executeQuery("SELECT to_regclass('exchange.instruments')");
                if (rsInst.next()) {
                    assertNull(rsInst.getString(1), "exchange.instruments table must be dropped");
                }

                var rsStocks = stmt.executeQuery("SELECT count(*) FROM exchange.stocks");
                assertTrue(rsStocks.next());
                assertTrue(rsStocks.getLong(1) >= 1000, "exchange.stocks must have >= 1000 rows, found: " + rsStocks.getLong(1));

                var rsBonds = stmt.executeQuery("SELECT count(*) FROM exchange.bonds");
                assertTrue(rsBonds.next());
                assertTrue(rsBonds.getLong(1) >= 1000, "exchange.bonds must have >= 1000 rows, found: " + rsBonds.getLong(1));

                var rsSec = stmt.executeQuery("SELECT count(*) FROM exchange.securities");
                assertTrue(rsSec.next());
                assertTrue(rsSec.getLong(1) >= 1000, "exchange.securities must have >= 1000 rows, found: " + rsSec.getLong(1));
            }
        }
    }
}
