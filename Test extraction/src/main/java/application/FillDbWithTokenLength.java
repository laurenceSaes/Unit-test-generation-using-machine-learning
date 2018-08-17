package application;

import tokenize.compression.TokenCompression;
import database.MysqlConnect;
import database.MysqlSettings;
import logger.LinkerLogger;
import tokenize.Dictionary;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class FillDbWithTokenLength {
    public static void main(String[] args) throws SQLException {
        if (args.length < 1) {
            LinkerLogger.logDetail("<database>");
            args = new String[]{"unittests"};
            //return
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
        connect.prepareStatement("DELETE FROM registration_tokens").executeUpdate(); //Comment out to debug failure

        for (int sbt = 0; sbt < 2; sbt++) {
            for (Integer compress : Arrays.asList(10, 5, 2, 0)) {
                PreparedStatement statement = connect.prepareStatement("SELECT id, method_code, method_code_sbt, unit_test_code, unit_test_code_sbt " +
                        "FROM registration " +
                        "WHERE method_code IS NOT NULL " +
                        "AND unit_test_code IS NOT NULL");
                ResultSet resultSet = statement.executeQuery();

                List<Integer> codeIds = new ArrayList<>();
                List<Integer> normalTokens = new ArrayList<>();
                List<Integer> sbtTokens = new ArrayList<>();

                Dictionary normalDictionary = new Dictionary();
                Dictionary dictionary = new Dictionary();

                LinkerLogger.logDetail("Creating token lists");
                while (resultSet.next()) {
                    int registrationId = resultSet.getInt(1);
                    String methodCode = resultSet.getString(2);
                    String methodCodeSbt = resultSet.getString(3);
                    String unitTestCode = resultSet.getString(4);
                    String unitTestCodeSbt = resultSet.getString(5);

                    addCodeToTokenList(normalTokens, normalDictionary, methodCode, unitTestCode);
                    addCodeToTokenList(sbtTokens, dictionary, methodCodeSbt, unitTestCodeSbt);
                    codeIds.add(registrationId);
                }


                int insertCounter = 0;
                PreparedStatement compressInsert = connect.prepareStatement("INSERT INTO registration_tokens (registration_id, compression, sbt, amount) VALUES(?,?,?,?)");

                LinkerLogger.logDetail("Working on " + (sbt == 1 ? "sbt" : "normal") + " src.tokenize.compression level " + compress);

                Iterator<Integer> codeIdIterator = codeIds.iterator();
                Iterator<Integer> iterator = TokenCompression.compress(compress, sbt == 1 ? sbtTokens : normalTokens, sbt == 1 ? dictionary : normalDictionary).iterator();
                while (codeIdIterator.hasNext()) {
                    int registrationId = codeIdIterator.next();

                    compressInsert.setInt(1, registrationId);
                    compressInsert.setInt(2, compress);
                    compressInsert.setInt(3, sbt);
                    int tokenMaxSize = getTokensForDoubleExample(iterator);
                    compressInsert.setInt(4, tokenMaxSize);
                    compressInsert.addBatch();

                    if (insertCounter++ > 1000) {
                        System.out.println("Insert: " + insertCounter + " registrations");
                        compressInsert.executeBatch();
                        insertCounter = 0;
                    }
                }

                if (iterator.hasNext()) {
                    LinkerLogger.logError("Token length algorithm did not work correctly");
                }
                compressInsert.executeBatch();
            }
        }
    }

    private static void addCodeToTokenList(List<Integer> normalTokens, Dictionary dictionary, String methodCode, String unitTestCode) {
        normalTokens.addAll(GenerateMultiTokens.createTokenList(methodCode, dictionary));
        normalTokens.add(-1);
        normalTokens.addAll(GenerateMultiTokens.createTokenList(unitTestCode, dictionary));
        normalTokens.add(-1);
    }

    public static int getTokensForDoubleExample(Iterator<Integer> iterator) {
        return Math.max(getTokensForCurrentExample(iterator), getTokensForCurrentExample(iterator));
    }

    private static int getTokensForCurrentExample(Iterator<Integer> iterator) {
        int counter = 0;
        while (iterator.hasNext()) {
            if (iterator.next() == -1)
                return counter;
            counter++;
        }
        return counter;
    }

}
