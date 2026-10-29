package com.web.motoridelab3api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.sql.DriverManager;
import java.sql.SQLException;

@Testcontainers
class FlywayMigrationTest {
    @Container static final PostgreSQLContainer DB = new PostgreSQLContainer("postgres:16-alpine");

    Flyway flyway(String schema, String target) {
        return Flyway.configure()
                .dataSource(DB.getJdbcUrl(), DB.getUsername(), DB.getPassword())
                .schemas(schema)
                .defaultSchema(schema)
                .target(target)
                .load();
    }

    @Test
    void freshDatabaseMigratesAndValidates() throws Exception {
        var flyway = flyway("fresh", "latest");
        flyway.migrate();
        flyway.validate();
        assertEquals("14", flyway.info().current().getVersion().getVersion());
        try (var c =
                        DriverManager.getConnection(
                                DB.getJdbcUrl(), DB.getUsername(), DB.getPassword());
                var s = c.createStatement()) {
            try (var rows = s.executeQuery("select count(*) from fresh.motorcycles")) {
                rows.next();
                assertEquals(10, rows.getInt(1));
            }
            assertThrows(
                    SQLException.class,
                    () -> s.executeUpdate("insert into fresh.favorites(motorcycle_id) values (1)"));
            assertThrows(
                    SQLException.class,
                    () ->
                            s.executeUpdate(
                                    "insert into fresh.reviews(motorcycle_id,rating,text)"
                                            + " values(1,5,'No owner')"));
        }
    }

    @Test
    void upgradingV9PreservesLegacyDataWithoutInventingOwners() throws Exception {
        flyway("upgrade", "9").migrate();
        try (var c =
                        DriverManager.getConnection(
                                DB.getJdbcUrl(), DB.getUsername(), DB.getPassword());
                var s = c.createStatement()) {
            s.execute("set search_path to upgrade");
            s.executeUpdate(
                    "insert into reviews(motorcycle_id,rating,text) values (1,5,'Historic"
                            + " review')");
            s.executeUpdate("insert into favorites(motorcycle_id) values (1)");
        }
        var upgraded = flyway("upgrade", "latest");
        upgraded.migrate();
        upgraded.validate();
        try (var c =
                        DriverManager.getConnection(
                                DB.getJdbcUrl(), DB.getUsername(), DB.getPassword());
                var s = c.createStatement()) {
            s.execute("set search_path to upgrade");
            try (var rows = s.executeQuery("select text,user_id from reviews")) {
                assertTrue(rows.next());
                assertEquals("Historic review", rows.getString(1));
                assertNull(rows.getObject(2));
            }
            try (var rows = s.executeQuery("select count(*) from favorites")) {
                rows.next();
                assertEquals(0, rows.getInt(1));
            }
            try (var rows =
                    s.executeQuery(
                            "select motorcycle_id,motorcycle_snapshot->>'brand' from"
                                    + " legacy_favorites")) {
                assertTrue(rows.next());
                assertEquals(1, rows.getLong(1));
                assertEquals("Yamaha", rows.getString(2));
            }
            s.executeUpdate("delete from motorcycles where id=1");
            try (var rows = s.executeQuery("select count(*) from legacy_favorites")) {
                rows.next();
                assertEquals(1, rows.getInt(1));
            }
        }
    }

    @Test
    void rateLimitUpgradePreservesExistingCounters() throws Exception {
        flyway("rate_upgrade", "13").migrate();
        try (var connection =
                        DriverManager.getConnection(
                                DB.getJdbcUrl(), DB.getUsername(), DB.getPassword());
                var statement = connection.createStatement()) {
            statement.executeUpdate(
                    "insert into rate_upgrade.auth_rate_limits(bucket_key,window_start,attempts)"
                            + " values ('"
                            + "ab".repeat(32)
                            + "',now(),7)");
        }
        var upgraded = flyway("rate_upgrade", "latest");
        upgraded.migrate();
        upgraded.validate();
        try (var connection =
                        DriverManager.getConnection(
                                DB.getJdbcUrl(), DB.getUsername(), DB.getPassword());
                var statement = connection.createStatement();
                var rows =
                        statement.executeQuery(
                                "select attempts,lock_id from rate_upgrade.auth_rate_limits")) {
            assertTrue(rows.next());
            assertEquals(7, rows.getInt(1));
            assertEquals(43, rows.getInt(2));
        }
    }
}
