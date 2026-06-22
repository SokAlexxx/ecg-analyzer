package com.ecganalyzer.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager {

    private static final String DATABASE_URL = "jdbc:sqlite:ecg_analyzer.db";

    private DatabaseManager() {
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DATABASE_URL);
    }

    public static void initializeDatabase() {
        String sql = """
                CREATE TABLE IF NOT EXISTS ecg_records (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    record_name TEXT NOT NULL,
                    header_file_path TEXT NOT NULL UNIQUE,
                    signal_file_path TEXT NOT NULL,
                    annotation_file_path TEXT,
                    channels INTEGER NOT NULL,
                    sampling_frequency INTEGER NOT NULL,
                    sample_count INTEGER NOT NULL,
                    lead_names TEXT,
                    signal_format TEXT,
                    gains TEXT,
                    baselines TEXT,
                    annotations_count INTEGER,
                    loaded_at TEXT NOT NULL
                );
                """;

        try (Connection connection = getConnection();
             Statement statement = connection.createStatement()) {

            statement.execute("PRAGMA foreign_keys = ON;");
            statement.execute(sql);
            addColumnIfMissing(statement, "ecg_records", "signal_format", "TEXT");
            addColumnIfMissing(statement, "ecg_records", "gains", "TEXT");
            addColumnIfMissing(statement, "ecg_records", "baselines", "TEXT");
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS ecg_annotations (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        record_id INTEGER NOT NULL,
                        sample_index INTEGER NOT NULL,
                        annotation_type TEXT NOT NULL,
                        description TEXT,
                        FOREIGN KEY(record_id) REFERENCES ecg_records(id) ON DELETE CASCADE
                    );
                    """);
            statement.execute("""
                    DELETE FROM ecg_annotations
                    WHERE id NOT IN (
                        SELECT MIN(id)
                        FROM ecg_annotations
                        GROUP BY record_id, sample_index, annotation_type
                    );
                    """);
            statement.execute("CREATE INDEX IF NOT EXISTS idx_ecg_annotations_record_id ON ecg_annotations(record_id);");
            statement.execute("CREATE UNIQUE INDEX IF NOT EXISTS idx_ecg_annotations_unique ON ecg_annotations(record_id, sample_index, annotation_type);");

        } catch (SQLException e) {
            throw new RuntimeException("Помилка ініціалізації бази даних.", e);
        }
    }

    private static void addColumnIfMissing(Statement statement,
                                           String tableName,
                                           String columnName,
                                           String columnType) throws SQLException {
        try {
            statement.execute("ALTER TABLE " + tableName + " ADD COLUMN " + columnName + " " + columnType);
        } catch (SQLException e) {
            if (!e.getMessage().toLowerCase().contains("duplicate column name")) {
                throw e;
            }
        }
    }
}