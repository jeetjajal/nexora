package com.nexora.phase11;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationState;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Phase 11: Flyway Migration History & Staging Database Validation")
public class FlywayMigrationValidationTest {

    @Test
    @DisplayName("Verify Flyway migrations V1 to V5 apply cleanly and idempotently")
    void testFlywayMigrationsAgainstStagingDatabase() throws Exception {
        // 1. Setup isolated in-memory MySQL-compatible database simulating staging environment
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:nexora_staging_test;DB_CLOSE_DELAY=-1;MODE=MySQL");
        dataSource.setUser("sa");
        dataSource.setPassword("");

        // 2. Configure Flyway instance pointing to classpath:db/migration
        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .load();

        // 3. Execute migrations
        int appliedCount = flyway.migrate().migrationsExecuted;
        assertEquals(5, appliedCount, "Should execute exactly 5 migration scripts (V1, V2, V3, V4 and V5)");

        // 4. Verify Migration Info & History
        MigrationInfo[] appliedMigrations = flyway.info().applied();
        assertEquals(5, appliedMigrations.length);

        assertEquals("1", appliedMigrations[0].getVersion().getVersion());
        assertEquals("init schema", appliedMigrations[0].getDescription());
        assertEquals(MigrationState.SUCCESS, appliedMigrations[0].getState());

        assertEquals("2", appliedMigrations[1].getVersion().getVersion());
        assertEquals("delivery payments disputes analytics", appliedMigrations[1].getDescription());
        assertEquals(MigrationState.SUCCESS, appliedMigrations[1].getState());

        assertEquals("3", appliedMigrations[2].getVersion().getVersion());
        assertEquals("seed catalogue categories", appliedMigrations[2].getDescription());
        assertEquals(MigrationState.SUCCESS, appliedMigrations[2].getState());

        assertEquals("4", appliedMigrations[3].getVersion().getVersion());
        assertEquals("complete quick commerce catalogue", appliedMigrations[3].getDescription());
        assertEquals(MigrationState.SUCCESS, appliedMigrations[3].getState());

        assertEquals("5", appliedMigrations[4].getVersion().getVersion());
        assertEquals("nexora 2 features", appliedMigrations[4].getDescription());
        assertEquals(MigrationState.SUCCESS, appliedMigrations[4].getState());

        // 5. Verify physical schema tables generated in the database
        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();
            Set<String> tableNames = new HashSet<>();
            try (ResultSet rs = metaData.getTables(null, null, "%", new String[]{"TABLE"})) {
                while (rs.next()) {
                    tableNames.add(rs.getString("TABLE_NAME").toLowerCase());
                }
            }

            // Core domain tables from V1
            assertTrue(tableNames.contains("roles"), "Table 'roles' should exist");
            assertTrue(tableNames.contains("users"), "Table 'users' should exist");
            assertTrue(tableNames.contains("user_roles"), "Table 'user_roles' should exist");
            assertTrue(tableNames.contains("categories"), "Table 'categories' should exist");
            assertTrue(tableNames.contains("stores"), "Table 'stores' should exist");
            assertTrue(tableNames.contains("products"), "Table 'products' should exist");
            assertTrue(tableNames.contains("inventory"), "Table 'inventory' should exist");
            assertTrue(tableNames.contains("addresses"), "Table 'addresses' should exist");
            assertTrue(tableNames.contains("orders"), "Table 'orders' should exist");
            assertTrue(tableNames.contains("order_items"), "Table 'order_items' should exist");

            // Operations & reliability tables from V2
            assertTrue(tableNames.contains("delivery_partners"), "Table 'delivery_partners' should exist");
            assertTrue(tableNames.contains("delivery_assignments"), "Table 'delivery_assignments' should exist");
            assertTrue(tableNames.contains("payments"), "Table 'payments' should exist");
            assertTrue(tableNames.contains("payment_transactions"), "Table 'payment_transactions' should exist");
            assertTrue(tableNames.contains("disputes"), "Table 'disputes' should exist");
            assertTrue(tableNames.contains("surge_pricing_rules"), "Table 'surge_pricing_rules' should exist");
            assertTrue(tableNames.contains("notification_logs"), "Table 'notification_logs' should exist");
            assertTrue(tableNames.contains("idempotency_keys"), "Table 'idempotency_keys' should exist");
            assertTrue(tableNames.contains("flyway_schema_history"), "Flyway schema history table should exist");

            // 31-Category Complete Catalogue tables from V4
            assertTrue(tableNames.contains("subcategories"), "Table 'subcategories' should exist");
            assertTrue(tableNames.contains("product_variants"), "Table 'product_variants' should exist");
            assertTrue(tableNames.contains("store_inventory"), "Table 'store_inventory' should exist");

            // NEXORA 2.0 tables from V5
            assertTrue(tableNames.contains("delivery_locations"), "Table 'delivery_locations' should exist");
            assertTrue(tableNames.contains("order_feedback"), "Table 'order_feedback' should exist");
            assertTrue(tableNames.contains("wallet_accounts"), "Table 'wallet_accounts' should exist");
            assertTrue(tableNames.contains("wallet_transactions"), "Table 'wallet_transactions' should exist");

            // Verify count of categories and subcategories in database
            try (Statement stmt = conn.createStatement()) {
                try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM categories")) {
                    assertTrue(rs.next());
                    assertEquals(31, rs.getInt(1), "Database should contain exactly 31 main categories");
                }
                try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM subcategories")) {
                    assertTrue(rs.next());
                    assertEquals(125, rs.getInt(1), "Database should contain exactly 125 subcategories");
                }
                try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM product_variants")) {
                    assertTrue(rs.next());
                    assertTrue(rs.getInt(1) >= 125, "Database should contain product variants for all subcategories");
                }
            }
        }

        // 6. Test Idempotency: Re-running migrate() should execute 0 migrations
        int secondRunCount = flyway.migrate().migrationsExecuted;
        assertEquals(0, secondRunCount, "Subsequent migration run should be idempotent and execute 0 migrations");
    }
}
