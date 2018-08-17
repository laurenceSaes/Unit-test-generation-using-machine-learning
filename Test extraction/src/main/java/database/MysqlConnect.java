package database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;


public class MysqlConnect {

    // init database constants
    private String databaseDriver;
    private String databaseUrl;
    private String username;
    private String password;
    private String maxPool;

    // init connection object
    private Connection connection;

    // init properties object
    private Properties properties;

    public MysqlConnect(String databaseDriver, String databaseUrl, String username, String password, String maxPool) {
        this.databaseDriver = databaseDriver;
        this.databaseUrl = databaseUrl;
        this.username = username;
        this.password = password;
        this.maxPool = maxPool;
    }

    // create properties
    private Properties getProperties() {
        if (properties == null) {
            properties = new Properties();
            properties.setProperty("user", username);
            properties.setProperty("password", password);
            properties.setProperty("MaxPooledStatements", maxPool);
            properties.setProperty("serverTimezone", "UTC");
        }
        return properties;
    }

    // connect database
    public Connection connect() {
        if (connection == null) {
            try {
                Class.forName(databaseDriver);
                connection = DriverManager.getConnection(databaseUrl, getProperties());
            } catch (ClassNotFoundException | SQLException e) {
                e.printStackTrace();
            }
        }
        return connection;
    }

    // disconnect database
    public void disconnect() {
        if (connection != null) {
            try {
                connection.close();
                connection = null;
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}
