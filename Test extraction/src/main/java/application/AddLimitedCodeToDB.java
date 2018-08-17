package application;

import application.utilities.MethodTransformation;
import database.MysqlConnect;
import database.MysqlSettings;
import logger.LinkerLogger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static application.utilities.MethodTransformation.getCodeWithoutString;

/*
ALTER TABLE `src.registration` ADD `method_code_limited` TEXT NOT NULL AFTER `method_code_sbt`, ADD `unit_test_code_limited` TEXT NOT NULL AFTER `method_code_limited`, ADD `method_code_limited_sbt` TEXT NOT NULL AFTER `unit_test_code_limited`, ADD `unit_test_code_limited_sbt` TEXT NOT NULL AFTER `method_code_limited_sbt`;
 UPDATE `src.registration` SET `method_limited` = "",`unit_test_limited`="",`method_sbt_limited`="",`unit_test_sbt_limited`=""
 */

public class AddLimitedCodeToDB {

    public static void main(String[] args) throws SQLException {

        if (args.length < 1) {
            LinkerLogger.logDetail("<database>");
            args = new String[]{"unittests_all"};
            //return;
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

        PreparedStatement statement = connect.prepareStatement("SELECT id, method_code, unit_test_code FROM registration WHERE method_body IS NOT NULL AND (method_code_limited IS NULL OR method_code_limited = '')");
        ResultSet resultSet = statement.executeQuery();

        PreparedStatement updateBody = connect.prepareStatement("UPDATE registration SET method_code_limited = ?, unit_test_code_limited = ?, method_code_limited_sbt = ?, unit_test_code_limited_sbt = ? WHERE id = ?");
        int inserts = 0;
        while(resultSet.next()) {
            int registrationId = resultSet.getInt("id");
            String methodCode = resultSet.getString("method_code");
            String unitTestCode = resultSet.getString("unit_test_code");

            try {
                String limitedMethodCode = getCodeWithoutString(methodCode);
                String limitedTestCode = getCodeWithoutString(unitTestCode);
                if(limitedMethodCode == null || limitedTestCode == null) {
                    LinkerLogger.logError("Cannot covert ID:" + registrationId);
                    continue;
                }

                String limitedMethodCodeSBT = MethodTransformation.covertToSBT(limitedMethodCode, true);
                String limitedTestCodeSBT = MethodTransformation.covertToSBT(limitedTestCode, true);
                if(limitedMethodCodeSBT == null || limitedTestCodeSBT == null) {
                    LinkerLogger.logError("Cannot covert SBT ID:" + registrationId);
                    continue;
                }

                updateBody.setString(1, limitedMethodCode);
                updateBody.setString(2, limitedTestCode);
                updateBody.setString(3, limitedMethodCodeSBT);
                updateBody.setString(4, limitedTestCodeSBT);
                updateBody.setInt(5, registrationId);
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
