package application;

import application.utilities.MethodTransformation;
import application.utilities.ObjectCreation;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseProblemException;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import database.MysqlConnect;
import database.MysqlSettings;
import logger.LinkerLogger;
import parser.StructureBasedTraversal;
import tokenize.Dictionary;
import tokenize.Dictionary;

import java.sql.*;
import java.util.List;


public class AddSBTToToAllTable {

    private static final boolean optimizations = true;

    private static final boolean cleanUpCode = false;

    public static void main(String[] args) throws SQLException {

        ObjectCreation.setOptimizations(optimizations);

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

        PreparedStatement statement = connect.prepareStatement("SELECT id, method_code, unit_test_code FROM registration  ORDER BY id ");
        ResultSet resultSet = statement.executeQuery();

        PreparedStatement updateBody = connect.prepareStatement("UPDATE registration SET method_code_sbt = ?, unit_test_code_sbt = ? WHERE id = ?");

        System.out.println("Code tokens\tSBT Tokens");
        while (resultSet.next()) {
            int registrationId = resultSet.getInt("id");
            String methodCode = resultSet.getString("method_code");
            String unitTestCode = resultSet.getString("unit_test_code");

            methodCode = getCleanedCode(methodCode);
            unitTestCode = getCleanedCode(unitTestCode);

            //DO the SBT traversal
            String sbtMethodCode = MethodTransformation.covertToSBT(methodCode, optimizations);
            String sbtUnitTestCode = MethodTransformation.covertToSBT(unitTestCode, optimizations);

            updateBody.setString(1, sbtMethodCode);
            updateBody.setString(2, sbtUnitTestCode);
            updateBody.setInt(3, registrationId);
            try {
                //if(inserts++ % 1000 == 0)
                //    LinkerLogger.logDetail("Working");
                updateBody.executeUpdate();
            } catch (Exception ignored) {
                updateBody.setNull(1, Types.VARCHAR);
                updateBody.setNull(2, Types.VARCHAR);
                updateBody.setInt(3, registrationId);
                updateBody.executeUpdate();
            }

            if (methodCode != null && unitTestCode != null) {
                logSizes(registrationId, "code", methodCode, sbtMethodCode);
                logSizes(registrationId, "test", unitTestCode, sbtUnitTestCode);
            }
        }
    }

    private static String getCleanedCode(String methodCode) {
        try {
            if (cleanUpCode && methodCode != null) {
                String cleaned = MethodTransformation.getCodeWithoutString(methodCode);
                if(cleaned != null)
                    return cleaned;
            }
            return methodCode;
        } catch (Exception ex) {
            return methodCode;
        }
    }

    private static void logSizes(int registrationId, String name, String methodCode, String sbtMethodCode) {
        List<Integer> methodTokens = GenerateMultiTokens.createTokenList(methodCode, new Dictionary());
        List<Integer> sbtTokens = GenerateMultiTokens.createTokenList(sbtMethodCode, new Dictionary());
        System.out.println(registrationId + "\t" + (optimizations ? "Optimized" : "notOptimized") + "\t" + name + "\t" + methodTokens.size() + "\t" + sbtTokens.size());
    }



}
