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
                    header_file_path TEXT NOT NULL,
                    signal_file_path TEXT NOT NULL,
                    annotation_file_path TEXT,
                    channels INTEGER NOT NULL,
                    sampling_frequency INTEGER NOT NULL,
                    sample_count INTEGER NOT NULL,
                    lead_names TEXT,
                    annotations_count INTEGER,
                    loaded_at TEXT NOT NULL
                );
                """;

        try (Connection connection = getConnection();
             Statement statement = connection.createStatement()) {

            statement.execute(sql);

        } catch (SQLException e) {
            throw new RuntimeException("Помилка ініціалізації бази даних.", e);
        }
    }
}