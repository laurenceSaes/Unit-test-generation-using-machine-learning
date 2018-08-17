package application;

import database.MysqlConnect;
import database.MysqlSettings;
import logger.LinkerLogger;

import java.io.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class TrainingExamplesToFile {

    private BufferedWriter sourceTrain = null;
    private BufferedWriter targetTrain = null;

    private BufferedWriter sourceValidate = null;
    private BufferedWriter targetValidate = null;

    private BufferedWriter sourceTest = null;
    private BufferedWriter targetTest = null;


    public static void main(String[] args) throws SQLException {

        if (args.length < 3) {
            LinkerLogger.logDetail("<limit_examples> <sbt/normal> <database> <op:useOnlyCommonExamples> <op:bothInsideAstAndByte> <op:noByteAndASTMismatch> <op:max-token-length>");
            args = new String[]{"6000", "sbt", "unittests_all", "1", "0", "0", "100"};
            //return;
        }

        //Settings
        int maxTrainExamples = Integer.parseInt(args[0]);
        String mode = args[1];
        String dataBaseName = args[2];
        boolean useOnlyCommonExamples = args.length >= 4 && args[3].equals("1");
        boolean bothInsideAstAndByte = args.length >= 5 && args[4].equals("1");
        boolean noByteAndASTMismatch = args.length >= 6 && args[5].equals("1");
        int maxTokenLength = args.length >= 7 ? Integer.parseInt(args[6]) : 100;


        MysqlSettings.setDatabaseUrl(dataBaseName);
        MysqlConnect mysqlConnect = new MysqlConnect(
                MysqlSettings.databaseDriver,
                MysqlSettings.databaseUrl,
                MysqlSettings.username,
                MysqlSettings.password,
                MysqlSettings.maxPool
        );

        Connection connect = mysqlConnect.connect();

        TrainingExamplesToFile trainingExamplesToFile = new TrainingExamplesToFile();
        trainingExamplesToFile.store(mode, useOnlyCommonExamples, bothInsideAstAndByte, noByteAndASTMismatch, maxTokenLength, maxTrainExamples, connect);

        mysqlConnect.disconnect();

    }

    private static String getModeQuery(boolean useOnlyCommonExamples, boolean bothInsideAstAndByte, boolean noByteAndASTMismatch, int maxTokenLength, String mode) {
        String testMode = mode.contains("limited") ? "_limited" : "";
        String codeMode = mode.contains("limited") ? "_limited" : "";
        codeMode = mode.contains("mixed") || mode.contains("sbt") ? codeMode + "_sbt" : codeMode;
        testMode = mode.contains("sbt") ? testMode + "_sbt" : testMode;
        return dataQuery(testMode, codeMode, useOnlyCommonExamples, bothInsideAstAndByte, noByteAndASTMismatch, maxTokenLength);
    }

    private static String dataQuery(String tesMode, String codeMode, boolean useOnlyCommonExamples, boolean bothInsideAstAndByte, boolean noByteAndASTMismatch, int maxTokenLength) {

        String selectSBT = " = " + (tesMode.contains("sbt") || codeMode.contains("sbt") ? "1" : "0") + " ";

        String lengthJoin = " INNER JOIN registration_tokens rtc0 ON rtc0.registration_id = rt.registration_id AND rtc0.src.tokenize.compression = 0 AND rtc0.sbt " + selectSBT;
        String commonTokens = "SELECT rt.registration_id FROM (SELECT registration_id FROM registration_tokens rt GROUP BY registration_id) rt " +
                lengthJoin +
                "INNER JOIN registration_tokens rtc10 ON rtc10.registration_id = rt.registration_id AND rtc10.src.tokenize.compression = 10 AND rtc10.sbt " + selectSBT + " " +
                "WHERE rtc10.amount / rtc0.amount  <= 0.2 ";
        String lengthLimit = "rtc0.amount < " + maxTokenLength + " ";


        String subSet = "SELECT rt.registration_id FROM (SELECT id as registration_id FROM unittests.src.registration) rt";

        if (useOnlyCommonExamples && maxTokenLength != 0)
            subSet = commonTokens + " AND " + lengthLimit;
        else if (useOnlyCommonExamples) {
            subSet = commonTokens;
        } else if (maxTokenLength != 0) {
            subSet = subSet + lengthJoin + " WHERE " + lengthLimit;
        }

        String bothInASTAndByte = "SELECT r.* FROM unittests_all.src.registration r\n" +
                "INNER JOIN (SELECT method_class, method_name, unit_test_method_class, unit_test_method_name FROM\n" +
                "(SELECT method_class, method_name, unit_test_method_class, unit_test_method_name \n" +
                "FROM unittests_ast.src.registration \n" +
                "WHERE method_code IS NOT NULL AND unit_test_code IS NOT NULL\n" +
                "GROUP BY method_class, method_name, unit_test_method_class, unit_test_method_name\n" +
                "UNION ALL\n" +
                "SELECT method_class, method_name, unit_test_method_class, unit_test_method_name \n" +
                "FROM unittests.src.registration\n" +
                "WHERE method_code IS NOT NULL AND unit_test_code IS NOT NULL\n" +
                "GROUP BY method_class, method_name, unit_test_method_class, unit_test_method_name) r\n" +
                "GROUP BY method_class, method_name, unit_test_method_class, unit_test_method_name HAVING count(*) > 1) inter ON inter.method_class = r.method_class AND inter.method_name = r.method_name and inter.unit_test_method_class = r.unit_test_method_class AND inter.unit_test_method_name = r.unit_test_method_name";

        if (!bothInsideAstAndByte)
            bothInASTAndByte = "(SELECT * FROM src.registration)";

        String noMismatch = "r.mismatchASTByte = 0 AND\n";
        if (!noByteAndASTMismatch)
            noMismatch = "";

        return "SELECT r.`method_code" + codeMode + "` as method_code, r.`unit_test_code" + tesMode + "` as unit_test_code, r.method_body \n" +
                "            FROM (" + subSet + ") rt \n" +
                "            INNER JOIN (" + bothInASTAndByte + ") r ON rt.registration_id = r.id \n" +
                "           WHERE r.`method_code` IS NOT NULL AND\n" +
                "                 r.`unit_test_code` IS NOT NULL AND" +
                "            " + noMismatch + " " +
                "                  r.method_body != '' \n" +
                //"            GROUP BY r.`method_code" + codeSbt + "`, r.`unit_test_code" + testSbt + "` \n" + //ALl is already non dupl
                "            ORDER BY r.`method_body`\n";

    }

    public void store(String mode, boolean useOnlyCommonExamples, boolean bothInsideAstAndByte, boolean noByteAndASTMismatch, int maxTokenLength, int maxTrainExamples, Connection connect) throws SQLException {
        String sql = getModeQuery(useOnlyCommonExamples, bothInsideAstAndByte, noByteAndASTMismatch, maxTokenLength, mode);

        PreparedStatement preparedStatement = connect.prepareStatement(sql);
        ResultSet trainSet = preparedStatement.executeQuery();

        int rowcount = 0;
        if (trainSet.last()) {
            rowcount = trainSet.getRow();
            trainSet.beforeFirst();
        }
        if (rowcount > maxTrainExamples) {
            rowcount = maxTrainExamples;
        }

        LinkerLogger.logDetail("There are " + rowcount + " examples in total");

        int valItems = (int) (rowcount * 0.1);
        int testItems = (int) (rowcount * 0.2);

        try {
            sourceTrain = getBufferedWriter("src-train.txt");
            targetTrain = getBufferedWriter("targ-train.txt");

            sourceValidate = getBufferedWriter("src-val.txt");
            targetValidate = getBufferedWriter("targ-val.txt");

            sourceTest = getBufferedWriter("src-eval.txt");
            targetTest = getBufferedWriter("targ-eval.txt");

            writeTrainingExamples(trainSet, maxTrainExamples, valItems, testItems);

            closeFiles();
        } catch (IOException e) {
            e.printStackTrace();
        }
        preparedStatement.close();
    }



    private void writeTrainingExamples(ResultSet trainSet, int maxTrainExamples, int valItems, int testItems) throws SQLException, IOException {

        int counter = 0;
        String lastMethodCode = "";
        int lastMode = 0;

        for (int i = 0; trainSet.next(); i++) {
            if (counter + 1 > maxTrainExamples) {
                break;
            }
            ++counter;

            String unit_test_code = trainSet.getString("unit_test_code");
            String method_code = trainSet.getString("method_code");
            String method_body = trainSet.getString("method_body");

            if (unit_test_code == null || method_code == null) {
                continue;
            }

            boolean lastMethodSame = lastMethodCode.equals(method_body);
            boolean exampleForVal = i < valItems;
            boolean keepInsideVal = lastMode == 0 && lastMethodSame;
            boolean exampleForTest = i < testItems;
            boolean keepInsideTest = lastMode == 1 && lastMethodSame;

            if (exampleForVal || keepInsideVal) {
                writeExample(sourceValidate, targetValidate, unit_test_code, method_code);
                lastMode = 0;

                if (!exampleForVal) {
                    valItems++;
                    testItems++;
                }
            } else {
                if (exampleForTest || keepInsideTest) {
                    writeExample(sourceTest, targetTest, unit_test_code, method_code);
                    lastMode = 1;
                    if (!exampleForTest) {
                        testItems++;
                    }
                } else {
                    writeExample(sourceTrain, targetTrain, unit_test_code, method_code);
                    lastMode = 2;
                }
            }

            lastMethodCode = method_body;

            if (i % 100 == 0) {
                flushFiles();
            }
        }

        LinkerLogger.logDetail("Used " + (counter) + " training examples. valItems: " + (valItems - 1) + " testItems: " + (testItems - valItems - 1) + "trainingItems: " + (counter - testItems - 1));
    }

    private void flushFiles() throws IOException {
        sourceTrain.flush();
        targetTrain.flush();
        sourceValidate.flush();
        targetValidate.flush();
        sourceTest.flush();
        targetTest.flush();
    }

    private void closeFiles() throws IOException {
        sourceTrain.close();
        targetTrain.close();
        sourceValidate.close();
        targetValidate.close();
        sourceTest.close();
        targetTest.close();
    }

    private void writeExample(BufferedWriter source, BufferedWriter target, String unitTestCode, String methodCode) throws IOException {
        source.write(methodCode);
        source.newLine();
        target.write(unitTestCode);
        target.newLine();
    }

    private BufferedWriter getBufferedWriter(String s) throws FileNotFoundException {
        return new BufferedWriter(new OutputStreamWriter(new FileOutputStream(new File(s))));
    }

}
