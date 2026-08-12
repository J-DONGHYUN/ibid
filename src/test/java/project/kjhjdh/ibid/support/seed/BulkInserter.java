package project.kjhjdh.ibid.support.seed;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BulkInserter {

    private static final Logger log = LoggerFactory.getLogger(BulkInserter.class);
    private static final int PROGRESS_INTERVAL = 200_000;

    public interface RowBinder {
        void bind(PreparedStatement statement, int rowIndex) throws SQLException;
    }

    private final Connection connection;
    private final int batchSize;

    public BulkInserter(Connection connection, int batchSize) {
        this.connection = connection;
        this.batchSize = batchSize;
    }

    public long insert(String label, String sql, int totalRows, RowBinder binder) throws SQLException {
        long startedAt = System.nanoTime();
        int inserted = 0;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int rowIndex = 0; rowIndex < totalRows; rowIndex++) {
                binder.bind(statement, rowIndex);
                statement.addBatch();
                inserted++;
                if (inserted % batchSize == 0) {
                    statement.executeBatch();
                    connection.commit();
                }
                if (inserted % PROGRESS_INTERVAL == 0) {
                    logProgress(label, inserted, totalRows, startedAt);
                }
            }
            statement.executeBatch();
            connection.commit();
        }
        return elapsedMillis(startedAt);
    }

    public void execute(String sql) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
        connection.commit();
    }

    public long countOf(String table) throws SQLException {
        try (Statement statement = connection.createStatement();
             var resultSet = statement.executeQuery("SELECT COUNT(*) FROM `" + table + "`")) {
            return resultSet.next() ? resultSet.getLong(1) : 0L;
        }
    }

    private void logProgress(String label, int inserted, int totalRows, long startedAt) {
        long elapsed = Math.max(1L, elapsedMillis(startedAt));
        log.info("  {} {}/{} rows ({} rows/sec)", label, inserted, totalRows, inserted * 1000L / elapsed);
    }

    private long elapsedMillis(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000L;
    }
}
