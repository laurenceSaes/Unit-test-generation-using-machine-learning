package application;

import database.MysqlConnect;
import database.MysqlSettings;
import logger.LinkerLogger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;

public class Statistics {
    private static MysqlConnect mysqlConnect;

    public static void main(String[] args) throws SQLException {

        Connection connection = null;
        for(String databaseName : Arrays.asList("unittests", "unittests_ast")) {
            MysqlSettings.setDatabaseUrl(databaseName);
            mysqlConnect = new MysqlConnect(
                    MysqlSettings.databaseDriver,
                    MysqlSettings.databaseUrl,
                    MysqlSettings.username,
                    MysqlSettings.password,
                    MysqlSettings.maxPool
            );

            connection = mysqlConnect.connect();

            doStatisticQuery(connection, "SELECT count(*) FROM (SELECT 1 from src.registration GROUP BY test_method_id, unit_test_method_class, `unit_test_method_name`) as q;", databaseName + ": Total test registrations: ");

            doStatisticQuery(connection, "SELECT count(*) FROM (SELECT 1 FROM `src.registration` INNER JOIN test_method tm ON tm.id = `test_method_id` GROUP BY tm.project) as q", databaseName + ": Total projects: ");

            doStatisticQuery(connection, "SELECT count(*) FROM (SELECT 1 FROM src.registration WHERE method_name IS NOT NULL GROUP BY test_method_id, unit_test_method_class, unit_test_method_name) as q", databaseName + ": Total matches: ");

            //doStatisticQuery(connection, "SELECT count(*) FROM (SELECT 1 FROM src.registration WHERE method_name IS NOT NULL AND method_code IS NOT NULL AND unit_test_code IS NOT NULL GROUP BY test_method_id, unit_test_method_class, unit_test_method_name) as q", databaseName + ": Total matches with code: ");

            doStatisticQuery(connection, "SELECT count(*) FROM (SELECT 1 FROM src.registration WHERE method_name IS NOT NULL AND method_code IS NOT NULL AND unit_test_code IS NOT NULL GROUP BY method_code, unit_test_code) as q", databaseName + ": Total matches with code: ");
        }

        if(connection != null)
            doStatisticQuery(connection, "SELECT count(*) FROM (SELECT 1 FROM (SELECT * FROM unittests.src.registration UNION ALL SELECT * FROM unittests_ast.src.registration) q2 WHERE method_code IS NOT NULL AND unit_test_code IS NOT NULL GROUP BY method_code, unit_test_code) as q", "Total none duplicate training examples: ");

    }

    private static void doStatisticQuery(Connection connection, String query, String title) throws SQLException {
        PreparedStatement statement;
        ResultSet resultSet;
        statement = connection.prepareStatement(query);
        resultSet = statement.executeQuery();
        resultSet.next();
        LinkerLogger.logDetail(title + resultSet.getInt(1));
    }
}
