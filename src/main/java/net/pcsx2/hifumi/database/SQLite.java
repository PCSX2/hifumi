package net.pcsx2.hifumi.database;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import net.pcsx2.hifumi.util.Log;
import net.pcsx2.hifumi.util.Messaging;

public class SQLite {

    private Connection connection;

    public SQLite(String dataDirectory) {
        try {
            // NOTE: this shouldn't be needed for modern versions of java, it should just dynamically look
            // at the classpath for you, but leaving it here incase im wrong
            // Class.forName("org.sqlite.JDBC");
            var jbdcString = String.format("jdbc:sqlite:%s/hifumibot.db", dataDirectory);
            Log.info("Opening database with JBDC string: " + jbdcString);
            this.connection = DriverManager.getConnection(jbdcString);
            ensureDatabaseIsInitialized();
        } catch (Exception e) {
            Messaging.logException("SQlite", "(constructor)", e);
        }
    }

    // NOTE: order is potentially important here
    // each file should contain a single valid SQL statement
    private String[] schemaMigrations = {
        "000-create-user-table.sql",
        "001-create-channel-table.sql",
        "002-create-message-table.sql",
        "003-create-message-attachment-table.sql",
        "004-create-message-embed-table.sql",
        "005-create-message-event-table.sql",
        "006-create-user-displayname-event-table.sql",
        "007-create-user-username-event-table.sql",
        "008-create-warez-event-table.sql",
        "009-create-member-event-table.sql",
        "010-create-interaction-event-table.sql",
        "011-create-filter-event-table.sql",
        "012-create-counter-table.sql",
        "013-create-command-table.sql",
        "014-create-command-event-table.sql",
        "015-create-command-event-option-table.sql",
        "016-create-automod-event-table.sql"
    };

    private void ensureDatabaseIsInitialized() {
        var conn = this.connection;
        try {
            conn.setAutoCommit(false); // begin transaction
            for (var migrationFile : schemaMigrations) {
                var resourcePath = String.format("db/migrations/%s", migrationFile);
                var is = SQLite.class.getClassLoader().getResourceAsStream(resourcePath);
                if (is == null) {
                    throw new RuntimeException("Resource not found: " + resourcePath);
                }
                String sql = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                try (var statement = conn.createStatement()) {
                    statement.execute(sql.trim());
                }
            }

            conn.commit(); // commit all statements
            conn.setAutoCommit(true);
        } catch (Exception e) {
            try {
                conn.rollback(); // rollback if anything fails
            } catch (SQLException rollbackEx) {
                e.addSuppressed(rollbackEx); // don't lose original exception
            }
            throw new RuntimeException("Unable to ensure database is initialized properly", e);
        }
    }

    public Connection getConnection() {
        return this.connection;
    }

    public void shutdown() {
        try {
            this.connection.close();
        } catch (SQLException e) {
            Messaging.logException("SQLite", "shutdown", e);
        }
    }
}
