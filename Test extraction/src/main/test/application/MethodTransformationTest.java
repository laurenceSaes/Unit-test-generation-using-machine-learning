package application;

import application.utilities.MethodTransformation;
import database.MysqlConnect;
import database.MysqlSettings;
import org.junit.Assert;
import org.junit.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class MethodTransformationTest {

    @Test
    public void noStringMethodIsSubPartOfMethod() throws SQLException {

        ResultSet resultSet = getCodeExamples();

        boolean testsRan = false;
        while (resultSet.next()) {
            String methodCode = resultSet.getString("method_code");
            if(methodCode.isEmpty())
                continue;

            String codeWithoutString = MethodTransformation.getCodeWithoutString(methodCode);
            if(codeWithoutString == null)
                continue;

            assertCodeIsContained(methodCode, codeWithoutString);
            testsRan = true;
        }

        Assert.assertTrue(testsRan);
    }


    @Test
    public void codeToSBTToCode() throws SQLException {

        for (int optimizedSBT = 1; optimizedSBT > 0; optimizedSBT--) {
            ResultSet resultSet = getCodeExamples();
            boolean testRan = false;

            while (resultSet.next()) {
                String methodCode = resultSet.getString("method_code");
                String unitTestCode = resultSet.getString("unit_test_code");

                int transformCodeResult = compareTransformationCode(optimizedSBT == 1, methodCode);
                Assert.assertTrue(transformCodeResult != 0);

                int transformUnitTestResult = compareTransformationCode(optimizedSBT == 1, unitTestCode);
                Assert.assertTrue(transformUnitTestResult != 0);

                if(transformCodeResult == 1|| transformUnitTestResult == 1)
                    testRan = true;
            }
            Assert.assertTrue(testRan);
        }
    }


    private void assertCodeIsContained(String methodCode, String subPartOfMethodCode) {
        methodCode = makeCodeComparable(methodCode);
        subPartOfMethodCode = makeCodeComparable(subPartOfMethodCode);
        String cleanedCode = makeCodeComparable(subPartOfMethodCode);
        methodCode = makeCodeComparable(methodCode);
        int counter = 0;

        for(char token : methodCode.toCharArray()) {
            if(cleanedCode.charAt(counter) == token) {
                counter++;
            }
        }

        //All parts of the cleaned method have to be inside the not cleaned version
        Assert.assertEquals(cleanedCode.length(), counter);
    }

    private ResultSet getCodeExamples() throws SQLException {
        MysqlSettings.setDatabaseUrl("unittests_all");
        MysqlConnect mysqlConnect = new MysqlConnect(
                MysqlSettings.databaseDriver,
                MysqlSettings.databaseUrl,
                MysqlSettings.username,
                MysqlSettings.password,
                MysqlSettings.maxPool
        );

        Connection connect = mysqlConnect.connect();

        PreparedStatement statement = connect.prepareStatement("SELECT id, method_code, unit_test_code FROM registration LIMIT 100");
        return statement.executeQuery();
    }

    private int compareTransformationCode(boolean optimizedSBT, String methodCode) {
        String sbtSpecification = MethodTransformation.covertToSBT(methodCode, optimizedSBT);
        if(sbtSpecification == null)
            return -1; //Specification cannot be made because of parse errors

        String codeTransformedBack = MethodTransformation.sbtToCodeTransformation(sbtSpecification, optimizedSBT);
        String cleanedRepresentation = makeCodeComparable(codeTransformedBack);
        String cleanedMethodCode = makeCodeComparable(methodCode);
        if(!cleanedRepresentation.equals(cleanedMethodCode)) {
            int x = 1;
        }
        return cleanedRepresentation.equals(cleanedMethodCode) ? 1 : 0;
    }

    private String makeCodeComparable(String transformedBack) {
        return transformedBack.replaceAll("[\r\n\t ]", "");
    }
}