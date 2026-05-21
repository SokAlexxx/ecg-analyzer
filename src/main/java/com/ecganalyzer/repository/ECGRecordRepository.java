package com.ecganalyzer.repository;

import com.ecganalyzer.database.DatabaseManager;
import com.ecganalyzer.model.ECGRecord;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.StringJoiner;

public class ECGRecordRepository {

    public void save(ECGRecord record) {
        String sql = """
                INSERT INTO ecg_records (
                    record_name,
                    header_file_path,
                    signal_file_path,
                    annotation_file_path,
                    channels,
                    sampling_frequency,
                    sample_count,
                    lead_names,
                    annotations_count,
                    loaded_at
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?);
                """;

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, record.getRecordName());
            statement.setString(2, record.getHeaderFile().getAbsolutePath());
            statement.setString(3, record.getSignalFile().getAbsolutePath());
            statement.setString(4, record.getAnnotationFile() != null
                    ? record.getAnnotationFile().getAbsolutePath()
                    : null);
            statement.setInt(5, record.getChannels());
            statement.setInt(6, record.getSamplingFrequency());
            statement.setLong(7, record.getSampleCount());
            statement.setString(8, joinLeadNames(record));
            statement.setInt(9, record.getAnnotations().size());
            statement.setString(10, record.getLoadedAt().toString());

            statement.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Помилка збереження ЕКГ-запису в базу даних.", e);
        }
    }

    public int countRecords() {
        String sql = "SELECT COUNT(*) FROM ecg_records;";

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            return resultSet.next() ? resultSet.getInt(1) : 0;

        } catch (SQLException e) {
            throw new RuntimeException("Помилка підрахунку ЕКГ-записів у базі даних.", e);
        }
    }

    private String joinLeadNames(ECGRecord record) {
        StringJoiner joiner = new StringJoiner(", ");

        for (String leadName : record.getLeadNames()) {
            joiner.add(leadName);
        }

        return joiner.toString();
    }
}