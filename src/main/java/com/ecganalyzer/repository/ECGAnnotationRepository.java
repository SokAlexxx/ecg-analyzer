package com.ecganalyzer.repository;

import com.ecganalyzer.database.DatabaseManager;
import com.ecganalyzer.model.ECGAnnotation;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

public class ECGAnnotationRepository {

    public void replaceAnnotations(int recordId, List<ECGAnnotation> annotations) {
        String deleteSql = "DELETE FROM ecg_annotations WHERE record_id = ?;";
        String insertSql = """
                INSERT OR REPLACE INTO ecg_annotations (
                    record_id,
                    sample_index,
                    annotation_type,
                    description
                )
                VALUES (?, ?, ?, ?);
                """;

        try (Connection connection = DatabaseManager.getConnection()) {
            connection.setAutoCommit(false);

            try (PreparedStatement deleteStatement = connection.prepareStatement(deleteSql)) {
                deleteStatement.setInt(1, recordId);
                deleteStatement.executeUpdate();
            }

            if (annotations != null && !annotations.isEmpty()) {
                try (PreparedStatement insertStatement = connection.prepareStatement(insertSql)) {
                    for (ECGAnnotation annotation : annotations) {
                        insertStatement.setInt(1, recordId);
                        insertStatement.setLong(2, annotation.getSampleIndex());
                        insertStatement.setString(3, annotation.getType());
                        insertStatement.setString(4, annotation.getDescription());
                        insertStatement.addBatch();
                    }
                    insertStatement.executeBatch();
                }
            }

            connection.commit();
        } catch (SQLException e) {
            throw new RuntimeException("Помилка збереження анотацій ЕКГ у базу даних.", e);
        }
    }
}
