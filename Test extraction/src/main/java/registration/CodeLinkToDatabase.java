package registration;

import database.MysqlConnect;
import linker.SourceCodeMapCallback;
import logger.LinkerLogger;

import java.sql.*;

public class CodeLinkToDatabase implements SourceCodeMapCallback {

    private MysqlConnect mysqlConnect;

    private Connection connection;

    private int lastTestMethodIndex = 0;

    public CodeLinkToDatabase(MysqlConnect mysqlConnect) {
        this.mysqlConnect = mysqlConnect;
        this.connection = this.mysqlConnect.connect();
    }

    public void disconnect() {
        this.mysqlConnect.disconnect();
    }

    @Override
    public boolean beginClassAnalysis(String testProject, String testClassName) {
        try {

            String register = "INSERT IGNORE INTO test_method (id, project, class, complete, log) VALUES (NULL, ?, ?, false, ?)";
            PreparedStatement registerStatement = this.connection.prepareStatement(register);
            registerStatement.setString(1, testProject);
            registerStatement.setString(2, testClassName);
            registerStatement.setString(3, "Started");
            registerStatement.executeUpdate();

            String getId = "SELECT id FROM test_method WHERE project=? AND class=?;";
            PreparedStatement getIdStatement = this.connection.prepareStatement(getId);
            getIdStatement.setString(1, testProject);
            getIdStatement.setString(2, testClassName);
            ResultSet resultSet = getIdStatement.executeQuery();
            if (!resultSet.next()) {
                LinkerLogger.logError("Cannot get test_method id: " + getId + " param: " + testProject + " - " + testClassName + " reg: " + register.toString());
                return false;
            }

            this.lastTestMethodIndex = resultSet.getInt("id");
            LinkerLogger.logDetail("Setting lastTestMethodIndex: " + this.lastTestMethodIndex );
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public void endClassAnalysis() {
        try {
            String updateStatus = "UPDATE test_method SET complete = 1, log = '' WHERE id = ?";
            PreparedStatement statusUpdate = this.connection.prepareStatement(updateStatus);
            statusUpdate.setInt(1, this.lastTestMethodIndex);
            statusUpdate.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        this.lastTestMethodIndex = 0;
    }

    @Override
    public void endClassAnalysis(boolean succeeded, String message) {
        try {
            String updateStatus = "UPDATE test_method SET complete = 1, error = ?, log = ? WHERE id = ?";
            PreparedStatement statusUpdate = this.connection.prepareStatement(updateStatus);
            statusUpdate.setInt(1, succeeded ? 1 : 0);
            statusUpdate.setString(2, message);
            statusUpdate.setInt(3, this.lastTestMethodIndex);
            statusUpdate.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        this.lastTestMethodIndex = 0;
    }

    public void addRegistration(int testClassID, String methodClass, String methodName, String methodCode, String methodCodeSBT, String testMethodClass, String testMethodName, String testCode, String testCodeSBT) {

        String sql = "INSERT INTO registration (`test_method_id`, `method_class`, `method_code`, `method_code_sbt`, `unit_test_method_class`, `unit_test_method_name`, `unit_test_code`, `unit_test_code_sbt`, `method_name`) VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try {
            String exist = "SELECT * FROM registration WHERE `method_code` = ? AND `unit_test_code` = ?";
            PreparedStatement existStatement = this.connection.prepareStatement(exist);
            existStatement.setString(1, methodCode);
            existStatement.setString(2, testCode);
            ResultSet resultSet = existStatement.executeQuery();
            if(resultSet.next()) {
                LinkerLogger.logDetail("Match not added in database because it already exists");
                return;
            }

            PreparedStatement statement = this.connection.prepareStatement(sql);
            statement.setInt(1, testClassID);
            statement.setString(2, methodClass);
            statement.setString(3, methodCode);
            statement.setString(4, methodCodeSBT);
            statement.setString(5, testMethodClass);
            statement.setString(6, testMethodName);
            statement.setString(7, testCode);
            statement.setString(8, testCodeSBT);
            statement.setString(9, methodName);
            statement.executeUpdate();
        } catch (SQLException e) {
            LinkerLogger.logError("Cannot add src.registration " + sql + " for testClassID " + testClassID);
            e.printStackTrace();
        }
    }


    public void addNoLink(int testMethodID, String testMethodClass, String testMethodName, String testCode, String testCodeSBT) {
        String sql = "INSERT INTO registration (`test_method_id`, `method_class`, `method_code`, `method_code_sbt`, `unit_test_method_class`, `unit_test_method_name`, `unit_test_code`, `unit_test_code_sbt`, `method_name`) VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?)";
        PreparedStatement statement = null;
        try {
            String exist = "SELECT * FROM registration WHERE `unit_test_method_class` = ? AND `unit_test_method_name` = ? AND `unit_test_code` = ?";
            PreparedStatement existStatement = this.connection.prepareStatement(exist);
            existStatement.setString(1, testMethodClass);
            existStatement.setString(2, testMethodName);
            existStatement.setString(3, testCode);
            ResultSet resultSet = existStatement.executeQuery();
            if(resultSet.next()) {
                LinkerLogger.logDetail("None match not added in database because it already exists");
                return;
            }

            statement = this.connection.prepareStatement(sql);
            statement.setInt(1, testMethodID);
            statement.setNull(2, Types.VARCHAR);
            statement.setNull(3, Types.VARCHAR);
            statement.setNull(4, Types.VARCHAR);
            statement.setString(5, testMethodClass);
            statement.setString(6, testMethodName);
            statement.setString(7, testCode);
            statement.setString(8, testCodeSBT);
            statement.setNull(9, Types.VARCHAR);
            statement.executeUpdate();
        } catch (SQLException e) {
            LinkerLogger.logError("Cannot add none src.registration " + sql + " for " + testMethodID + " - " + (statement != null ? statement.toString() : ""));
            e.printStackTrace();
        }
    }

    @Override
    public void foundMap(String methodClass, String methodName, String methodCode,String methodSBT, String testMethodClass, String testMethodName, String testCode, String testSBT) {
        this.addRegistration(this.lastTestMethodIndex, methodClass, methodName, methodCode, methodSBT, testMethodClass, testMethodName, testCode, testSBT);

    }


    @Override
    public void noMapFound(String testMethodClass, String testMethodName, String testCode, String testCodeSBT) {
        this.addNoLink(this.lastTestMethodIndex, testMethodClass, testMethodName, testCode, testCodeSBT);

    }
}
