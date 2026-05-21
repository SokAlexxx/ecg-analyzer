package com.ecganalyzer.repository;

import com.ecganalyzer.database.DatabaseManager;
import com.ecganalyzer.model.ECGRecord;
import com.ecganalyzer.model.ECGRecordSummary;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.StringJoiner;
import java.util.ArrayList;

public class ECGRecordRepository {

    public void saveOrUpdate(ECGRecord record) {
        if (existsByHeaderPath(record.getHeaderFile().getAbsolutePath())) {
            update(record);
        } else {
            save(record);
        }
    }

    private void save(ECGRecord record) {
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
                    signal_format,
                    gains,
                    baselines,
                    annotations_count,
                    loaded_at
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);
                """;

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            fillStatement(statement, record);
            statement.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Помилка збереження ЕКГ-запису в базу даних.", e);
        }
    }

    private void update(ECGRecord record) {
        String sql = """
                UPDATE ecg_records
                SET record_name = ?,
                    signal_file_path = ?,
                    annotation_file_path = ?,
                    channels = ?,
                    sampling_frequency = ?,
                    sample_count = ?,
                    lead_names = ?,
                    signal_format = ?,
                    gains = ?,
                    baselines = ?,
                    annotations_count = ?,
                    loaded_at = ?
                WHERE header_file_path = ?;
                """;

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, record.getRecordName());
            statement.setString(2, record.getSignalFile().getAbsolutePath());
            statement.setString(3, record.getAnnotationFile() != null
                    ? record.getAnnotationFile().getAbsolutePath()
                    : null);
            statement.setInt(4, record.getChannels());
            statement.setInt(5, record.getSamplingFrequency());
            statement.setLong(6, record.getSampleCount());
            statement.setString(7, joinStrings(record.getLeadNames()));
            statement.setString(8, joinIntegers(record.getSignalFormats()));
            statement.setString(9, joinDoubles(record.getGains()));
            statement.setString(10, joinIntegers(record.getBaselines()));
            statement.setInt(11, record.getAnnotations().size());
            statement.setString(12, record.getLoadedAt().toString());
            statement.setString(13, record.getHeaderFile().getAbsolutePath());

            statement.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Помилка оновлення ЕКГ-запису в базі даних.", e);
        }
    }

    private void fillStatement(PreparedStatement statement, ECGRecord record) throws SQLException {
        statement.setString(1, record.getRecordName());
        statement.setString(2, record.getHeaderFile().getAbsolutePath());
        statement.setString(3, record.getSignalFile().getAbsolutePath());
        statement.setString(4, record.getAnnotationFile() != null
                ? record.getAnnotationFile().getAbsolutePath()
                : null);
        statement.setInt(5, record.getChannels());
        statement.setInt(6, record.getSamplingFrequency());
        statement.setLong(7, record.getSampleCount());
        statement.setString(8, joinStrings(record.getLeadNames()));
        statement.setString(9, joinIntegers(record.getSignalFormats()));
        statement.setString(10, joinDoubles(record.getGains()));
        statement.setString(11, joinIntegers(record.getBaselines()));
        statement.setInt(12, record.getAnnotations().size());
        statement.setString(13, record.getLoadedAt().toString());
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

    public List<ECGRecordSummary> findAllSummaries() {
        String sql = """
                SELECT id,
                       record_name,
                       channels,
                       sampling_frequency,
                       sample_count,
                       lead_names,
                       annotations_count,
                       loaded_at
                FROM ecg_records
                ORDER BY loaded_at DESC;
                """;

        List<ECGRecordSummary> records = new ArrayList<>();

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                records.add(new ECGRecordSummary(
                        resultSet.getInt("id"),
                        resultSet.getString("record_name"),
                        resultSet.getInt("channels"),
                        resultSet.getInt("sampling_frequency"),
                        resultSet.getLong("sample_count"),
                        resultSet.getString("lead_names"),
                        resultSet.getInt("annotations_count"),
                        resultSet.getString("loaded_at")
                ));
            }

            return records;

        } catch (SQLException e) {
            throw new RuntimeException("Помилка отримання списку ЕКГ-записів з бази даних.", e);
        }
    }

    private boolean existsByHeaderPath(String headerFilePath) {
        String sql = "SELECT COUNT(*) FROM ecg_records WHERE header_file_path = ?;";

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, headerFilePath);

            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() && resultSet.getInt(1) > 0;
            }

        } catch (SQLException e) {
            throw new RuntimeException("Помилка перевірки дубліката ЕКГ-запису.", e);
        }
    }

    private String joinStrings(List<String> values) {
        StringJoiner joiner = new StringJoiner(", ");

        for (String value : values) {
            joiner.add(value);
        }

        return joiner.toString();
    }

    private String joinIntegers(List<Integer> values) {
        StringJoiner joiner = new StringJoiner(", ");

        for (Integer value : values) {
            joiner.add(String.valueOf(value));
        }

        return joiner.toString();
    }

    private String joinDoubles(List<Double> values) {
        StringJoiner joiner = new StringJoiner(", ");

        for (Double value : values) {
            joiner.add(String.valueOf(value));
        }

        return joiner.toString();
    }
}