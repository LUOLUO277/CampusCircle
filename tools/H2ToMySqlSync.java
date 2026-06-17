import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class H2ToMySqlSync {
    private static final int BATCH_SIZE = 200;

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            System.err.println("Usage: java H2ToMySqlSync <h2-jdbc-url> <mysql-jdbc-url>");
            System.exit(1);
        }

        String h2Url = args[0];
        String mysqlUrl = args[1];

        try (Connection h2 = DriverManager.getConnection(h2Url, "sa", "");
             Connection mysql = DriverManager.getConnection(mysqlUrl)) {
            mysql.setAutoCommit(false);
            setForeignKeyChecks(mysql, false);

            List<String> tables = loadTables(h2);
            for (String table : tables) {
                syncTable(h2, mysql, table);
            }

            setForeignKeyChecks(mysql, true);
            mysql.commit();
            System.out.println("Sync completed for " + tables.size() + " tables at " + LocalDateTime.now());
        }
    }

    private static List<String> loadTables(Connection h2) throws SQLException {
        List<String> tables = new ArrayList<>();
        String sql = """
            SELECT TABLE_NAME
            FROM INFORMATION_SCHEMA.TABLES
            WHERE LOWER(TABLE_SCHEMA) = 'public'
            ORDER BY TABLE_NAME
            """;
        try (Statement statement = h2.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            while (resultSet.next()) {
                tables.add(resultSet.getString(1));
            }
        }
        return tables;
    }

    private static void syncTable(Connection h2, Connection mysql, String table) throws SQLException {
        if (!targetTableExists(mysql, table)) {
            System.out.println("Skip missing target table: " + table);
            return;
        }

        int sourceCount = countRows(h2, table);
        try (Statement deleteStatement = mysql.createStatement()) {
            deleteStatement.execute("DELETE FROM `" + table + "`");
        }

        if (sourceCount == 0) {
            mysql.commit();
            System.out.println("Synced " + table + ": 0 rows");
            return;
        }

        String selectSql = "SELECT * FROM \"" + table + "\"";
        try (Statement selectStatement = h2.createStatement();
             ResultSet resultSet = selectStatement.executeQuery(selectSql)) {
            ResultSetMetaData metaData = resultSet.getMetaData();
            int columnCount = metaData.getColumnCount();
            String insertSql = buildInsertSql(table, metaData, columnCount);

            try (PreparedStatement insertStatement = mysql.prepareStatement(insertSql)) {
                int rowCount = 0;
                while (resultSet.next()) {
                    bindRow(resultSet, metaData, insertStatement, columnCount);
                    insertStatement.addBatch();
                    rowCount++;

                    if (rowCount % BATCH_SIZE == 0) {
                        insertStatement.executeBatch();
                    }
                }
                insertStatement.executeBatch();
                mysql.commit();
                System.out.println("Synced " + table + ": " + rowCount + " rows");
            }
        }
    }

    private static boolean targetTableExists(Connection mysql, String table) throws SQLException {
        try (ResultSet resultSet = mysql.getMetaData().getTables(mysql.getCatalog(), null, table, new String[]{"TABLE"})) {
            return resultSet.next();
        }
    }

    private static int countRows(Connection connection, String table) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT COUNT(*) FROM \"" + table + "\"")) {
            resultSet.next();
            return resultSet.getInt(1);
        }
    }

    private static String buildInsertSql(String table, ResultSetMetaData metaData, int columnCount) throws SQLException {
        StringBuilder columns = new StringBuilder();
        StringBuilder placeholders = new StringBuilder();
        for (int i = 1; i <= columnCount; i++) {
            if (i > 1) {
                columns.append(", ");
                placeholders.append(", ");
            }
            columns.append("`").append(metaData.getColumnName(i)).append("`");
            placeholders.append("?");
        }
        return "INSERT INTO `" + table + "` (" + columns + ") VALUES (" + placeholders + ")";
    }

    private static void bindRow(ResultSet resultSet, ResultSetMetaData metaData, PreparedStatement statement, int columnCount)
            throws SQLException {
        for (int i = 1; i <= columnCount; i++) {
            Object value = resultSet.getObject(i);
            int sqlType = metaData.getColumnType(i);
            String typeName = metaData.getColumnTypeName(i);

            if (value == null) {
                statement.setNull(i, sqlType);
                continue;
            }

            if ("JSON".equalsIgnoreCase(typeName)) {
                statement.setString(i, resultSet.getString(i));
                continue;
            }

            if (sqlType == Types.TIMESTAMP || value instanceof Timestamp) {
                statement.setTimestamp(i, resultSet.getTimestamp(i));
                continue;
            }

            if (sqlType == Types.BOOLEAN || value instanceof Boolean) {
                statement.setBoolean(i, resultSet.getBoolean(i));
                continue;
            }

            statement.setObject(i, value);
        }
    }

    private static void setForeignKeyChecks(Connection mysql, boolean enabled) throws SQLException {
        try (Statement statement = mysql.createStatement()) {
            statement.execute("SET FOREIGN_KEY_CHECKS=" + (enabled ? "1" : "0"));
        }
    }
}
