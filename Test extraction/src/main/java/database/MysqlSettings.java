package database;

import logger.LinkerLogger;

public class MysqlSettings {
    public static final String databaseDriver = "com.mysql.cj.jdbc.Driver";
    public static String databaseUrl = "jdbc:mysql://localhost:3306/unittests";
    public static final String username = "root";
    public static final String password = "yourPW";
    public static final String maxPool = "250";

    public static void setDatabaseUrl(String databaseName) {
        MysqlSettings.databaseUrl = "jdbc:mysql://localhost:3306/" + databaseName;
        LinkerLogger.logDetail("Database:" + databaseUrl);
    }
}
