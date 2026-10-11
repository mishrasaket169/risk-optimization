package com.lro.persistence;

import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Manages database connection pool, Flyway schema migrations,
 * and multi-schema topology verification for PostgreSQL and H2 test profiles.
 */
public class DatabaseManager {

    private static final Logger log = LoggerFactory.getLogger(DatabaseManager.class);

    private final String jdbcUrl;
    private final String username;
    private final String password;

    public DatabaseManager() {
        this(
                System.getenv().getOrDefault("LRO_DB_URL", "jdbc:h2:mem:lro_sim;DB_CLOSE_DELAY=-1;MODE=PostgreSQL"),
                System.getenv().getOrDefault("LRO_DB_USER", "sa"),
                System.getenv().getOrDefault("LRO_DB_PASSWORD", "")
        );
    }

    public DatabaseManager(String jdbcUrl, String username, String password) {
        this.jdbcUrl = jdbcUrl;
        this.username = username;
        this.password = password;
    }

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(jdbcUrl, username, password);
    }

    public boolean runMigrations() {
        try {
            log.info("Running Flyway migrations on {}", jdbcUrl);
            Flyway flyway = Flyway.configure()
                    .dataSource(jdbcUrl, username, password)
                    .locations("classpath:db/migration")
                    .baselineOnMigrate(true)
                    .load();
            int applied = flyway.migrate().migrationsExecuted;
            log.info("Successfully executed {} database migrations.", applied);
            return true;
        } catch (Exception ex) {
            log.error("Failed to execute database migrations: {}", ex.getMessage(), ex);
            return false;
        }
    }

    public boolean isHealthy() {
        try (Connection conn = getConnection()) {
            return conn.isValid(2);
        } catch (Exception e) {
            return false;
        }
    }

    public String getJdbcUrl() {
        return jdbcUrl;
    }
}
