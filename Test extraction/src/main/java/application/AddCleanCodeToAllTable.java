package application;

import application.utilities.MethodTransformation;
import database.MysqlConnect;
import database.MysqlSettings;
import logger.LinkerLogger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;


public class AddCleanCodeToAllTable {

    public static void main(String[] args) throws SQLException {

        if (args.length < 1) {
            LinkerLogger.logDetail("<database>");
            args = new String[]{"unittests_all"};
            return;
        }

        MysqlSettings.setDatabaseUrl(args[0]);
        MysqlConnect mysqlConnect = new MysqlConnect(
                MysqlSettings.databaseDriver,
                MysqlSettings.databaseUrl,
                MysqlSettings.username,
                MysqlSettings.password,
                MysqlSettings.maxPool
        );

        Connection connect = mysqlConnect.connect();

        PreparedStatement statement = connect.prepareStatement("SELECT id, method_code FROM registration WHERE method_body IS NULL OR method_body = ''");
        ResultSet resultSet = statement.executeQuery();

        PreparedStatement updateBody = connect.prepareStatement("UPDATE registration SET method_body = ? WHERE id = ?");
        int inserts = 0;
        while(resultSet.next()) {
            int registrationId = resultSet.getInt("id");
            String methodCode = resultSet.getString("method_code");

            try {
                String methodBodyCode = MethodTransformation.getCleanCode(methodCode);
                updateBody.setString(1, methodBodyCode);
                updateBody.setInt(2, registrationId);
                updateBody.addBatch();

                if( inserts++ % 100 == 0) {
                    LinkerLogger.logDetail("Insert");
                    updateBody.executeBatch();
                }
            } catch (Exception ex) {
                LinkerLogger.logError(ex.getMessage());
            }

        }
        updateBody.executeBatch();
    }

}
