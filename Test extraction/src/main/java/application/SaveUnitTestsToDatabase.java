package application;

import cache.ParserCache;
import database.MysqlConnect;
import database.MysqlSettings;
import linker.JavaMethodReference;
import logger.LinkerLogger;
import parser.JavaSourceMapper;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

public class SaveUnitTestsToDatabase {

    private static JavaSourceMapper cachedJavaSourceMapper = null;

    private static ParserCache parserCache = new ParserCache();


    public static void main(String[] args) throws SQLException {

        if (args.length < 3) {
            LinkerLogger.logDetail("<database> <queue_database> <table> given:" + String.join(",", args));
            args = new String[]{"unittests", "unittests", "set1"};
            return;
        }

        String analyzeDatabase = args[0];
        String queueDatabase = args[1];
        String table = args[2];
        MysqlSettings.setDatabaseUrl(analyzeDatabase);

        MysqlConnect mysqlConnect = new MysqlConnect(
                MysqlSettings.databaseDriver,
                MysqlSettings.databaseUrl,
                MysqlSettings.username,
                MysqlSettings.password,
                MysqlSettings.maxPool
        );
        Connection connection = mysqlConnect.connect();


        LinkerLogger.logDetail("Starting");


        String nextElementsToProcess = "SELECT q.`project_name`, q.`test_class`, q.`source_path` " +
                "FROM "+queueDatabase+".`queue` q " +
                "LEFT OUTER JOIN " + analyzeDatabase + "." + table + " a ON a.project = q.project_name AND a.unit_test_method_class = q.test_class " +
                "WHERE a.project IS NULL " +
                "ORDER BY q.source_path ";
        PreparedStatement elementsToProcessStatement = connection.prepareStatement(nextElementsToProcess);
        ResultSet elementsToProcess = elementsToProcessStatement.executeQuery();

        int i = 0;
        try (
                PreparedStatement statement = connection.prepareStatement("INSERT INTO " + analyzeDatabase + "." + table + " (has_method_code, has_unit_test_code,project, unit_test_method_class, unit_test_method_name) VALUE (0,0,?,?,?)")
        ) {
            while (elementsToProcess.next()) {
                String project = elementsToProcess.getString(1);
                String testClass = elementsToProcess.getString(2);
                String sourcePath = elementsToProcess.getString(3);

                //Check exist
                PreparedStatement exist = connection.prepareStatement("SELECT id FROM " + analyzeDatabase + "." + table + " where project = ? AND unit_test_method_class = ? LIMIT 1");
                exist.setString(1, project);
                exist.setString(2, testClass);
                ResultSet resultSetExist = exist.executeQuery();

                if(resultSetExist.next()) {
                    LinkerLogger.logDetail("Already in DB");
                    continue;
                }

                LinkerLogger.logDetail("Adding element");
                if (cachedJavaSourceMapper == null || !sourcePath.equals(cachedJavaSourceMapper.getProjectLocation())) {
                    LinkerLogger.logDetail("Create project src.parser");
                    try {
                        cachedJavaSourceMapper = parserCache.getParser(sourcePath);
                    } catch (IOException e) {
                        e.printStackTrace();
                        continue;
                    }
                }

                List<JavaMethodReference> testMethods = cachedJavaSourceMapper.getTestMethods(testClass);
                if(testMethods == null)
                    continue;

                for (JavaMethodReference testMethod : testMethods) {
                    String testMethodName = testMethod.getMethodName();

                    statement.setString(1, project);
                    statement.setString(2, testClass);
                    statement.setString(3, testMethodName);

                    statement.addBatch();
                    i++;

                    if (i % 500 == 0) {
                        statement.executeBatch();
                    }

                }
            }

            if (i % 500 != 0)
               statement.executeBatch();
        }

        //Update code present
        for(String codeDatabase : Arrays.asList("unittests", "unittests_ast")) {
            for (String column : Arrays.asList("method_code", "unit_test_code")) {
                String update = "UPDATE "+analyzeDatabase+".set1 set `has_" + column + "` = 1 WHERE id IN (\n" +
                        "                SELECT * FROM (\n" +
                        "                        SELECT s1.id FROM "+analyzeDatabase+".set1 s1\n" +
                        "                        INNER JOIN "+codeDatabase+".registration r ON r.unit_test_method_class = s1.`unit_test_method_class` AND r.`unit_test_method_name` = s1.`unit_test_method_name`\n" +
                        "                GROUP BY s1.id, r." + column + " HAVING IF(SUM(" + column + " IS NULL), 0, 1) = 1\n" +
                        "        ) q)";
                PreparedStatement statement = connection.prepareStatement(update);
                statement.executeUpdate();
            }
        }

    }
}
