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
        }
    }
}
