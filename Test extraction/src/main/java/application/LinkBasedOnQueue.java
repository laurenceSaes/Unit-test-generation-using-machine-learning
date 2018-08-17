package application;

import application.linking.QueueElement;
import cache.ParserCache;
import com.ibm.wala.ipa.callgraph.CallGraphBuilderCancelException;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import linker.SourceCodeMapCallback;
import linker.TestToMethodLinker;
import database.MysqlConnect;
import database.MysqlSettings;
import linker.UnitTestLinker;
import linker.methods.ASTAnalysisWithLargestMatchingClass;
import linker.methods.BytecodeAnalysisWithLargestMatchingClass;
import linker.methods.LinkMethods;
import logger.LinkerLogger;
import parser.JavaSourceMapper;
import registration.CodeLinkToDatabase;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;


public class LinkBasedOnQueue {

    private static JavaSourceMapper cachedJavaSourceMapper = null;

    private static LinkMethods linkMethod = LinkMethods.AST;

    private static MysqlConnect mysqlConnect;

    private static SourceCodeMapCallback sourceCodeMap;

    private static ParserCache parserCache = new ParserCache();

    private static Map.Entry<String, UnitTestLinker> cache = new AbstractMap.SimpleEntry<>("", null);

    private static boolean onePass = false;

    private static boolean onlyOneProject = false;

    private static final int onePassMaxItemPerTime = 100;

    private static Connection connection;


    public static void main(String[] args) {

        if (args.length < 2) {
            LinkerLogger.logDetail("<AST/BYTECODE> <database> given:" + String.join(",", args));
            args = new String[]{"BYTE", "unittests", "1", "1"};
            return;
        }

        killWhenInactive();

        linkMethod = args[0].equals("AST") ? LinkMethods.AST : LinkMethods.BYTE_CODE;
        MysqlSettings.setDatabaseUrl(args[1]);

        if (args.length >= 3) {
            onePass = args[2].equals("1");
        }

        if (args.length >= 4) {
            onlyOneProject = args[3].equals("1");
        }

        mysqlConnect = new MysqlConnect(
                MysqlSettings.databaseDriver,
                MysqlSettings.databaseUrl,
                MysqlSettings.username,
                MysqlSettings.password,
                MysqlSettings.maxPool
        );
        connection = mysqlConnect.connect();

        sourceCodeMap = new CodeLinkToDatabase(mysqlConnect);
        //sourceCodeMap = new CodeLinkToConsole();

        LinkerLogger.logDetail("Starting");

        if (onePass) {
            doOnePassIteration();
            return;
        }

        String projectName = "";

        while (true) {
            try {
                String sql = getNextElementQuery();

                PreparedStatement getElementToProcess = connection.prepareStatement(sql);
                getElementToProcess.setString(1, projectName);
                ResultSet elementToProcess = getElementToProcess.executeQuery();
                if (!elementToProcess.next()) {
                    LinkerLogger.logDetail("DONE!");
                    return;
                }

                if (onePass && projectName.isEmpty()) {
                    boolean onePassStatus = updateOnePassStatus(elementToProcess.getString(2));
                    if (!onePassStatus) {
                        continue;
                    }
                }

                if (onlyOneProject && !projectName.isEmpty() && !projectName.equals(elementToProcess.getString(2)))
                    return;

                projectName = elementToProcess.getString(2);

                LinkerLogger.logDetail("Found element on the queue");
                if (!processTest(elementToProcess)) {
                    projectName = "";
                }

            } catch (SQLException | IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    /*
    * We combine multiple test classes and link them at once. This is faster than processing each class separate
    * */
    private static void doOnePassIteration() {

        try {
            String projectName = getNextProjectToProcess();
            if (projectName == null) {
                LinkerLogger.logDetail("There are no projects to process");
                return;
            }

            String nextElementsToProcess = "SELECT `id`, `project_name`, `test_class`, `source_path`, `class_path` FROM `queue` q WHERE q.`complete` = 0 AND q.`bussy` = 0 AND project_name = ? ORDER BY q.source_path LIMIT " + onePassMaxItemPerTime;
            PreparedStatement elementsToProcessStatement = connection.prepareStatement(nextElementsToProcess);
            elementsToProcessStatement.setString(1, projectName);
            ResultSet elementsToProcess = elementsToProcessStatement.executeQuery();

            Set<QueueElement> testClasses = new HashSet<>();
            Set<String> classPaths = new HashSet<>();
            Set<String> sourcePaths = new HashSet<>();

            while(elementsToProcess.next()) {
                int id = elementsToProcess.getInt(1);
                String testClass = elementsToProcess.getString(3);

                String sourcePath = elementsToProcess.getString(4);
                if (sourcePath != null && !sourcePath.isEmpty())
                    sourcePaths.add(sourcePath);

                String classField = elementsToProcess.getString(5);
                if (classField != null && !classField.isEmpty())
                    classPaths.addAll(Arrays.asList(classField.split(";")));

                testClasses.add(new QueueElement(id, testClass, sourcePath));
            }

            ArrayList<String> sourcePathsAsList = new ArrayList<>(sourcePaths);
            ArrayList<String> classPathsAsList = new ArrayList<>(classPaths);

            for(QueueElement queueElement : testClasses) {
                boolean normalStatus = updateTestStatus(queueElement.getId(), 1, 0);
                LinkerLogger.logDetail("Updating status");
                if (!normalStatus)
                    continue;

                LinkerLogger.logDetail("Working");
                if (cachedJavaSourceMapper == null || !queueElement.getSourcePath().equals(cachedJavaSourceMapper.getProjectLocation())) {
                    LinkerLogger.logDetail("Create project src.parser");
                    try {
                        cachedJavaSourceMapper = parserCache.getParser(queueElement.getSourcePath());
                    } catch (IOException e) {
                        e.printStackTrace();
                        continue;
                    }
                }

                if (cachedJavaSourceMapper.isTestClassesEmpty()) {
                    continue;
                }

                UnitTestLinker unitTestLinker = getUnitTestLinker(projectName, sourcePathsAsList, classPathsAsList);

                boolean linkDone = TestToMethodLinker.linkTestCaseCashedLinker(projectName, queueElement.getTestClassName(), queueElement.getSourcePath(), unitTestLinker, sourceCodeMap);
                if (linkDone)
                    updateTestStatus(queueElement.getId(), 0, 1);
                }

            unsetOnePassWhenElementsAvailable(projectName);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void unsetOnePassWhenElementsAvailable(String projectName) throws SQLException {
        String projectFree = "SELECT `project_name` FROM `queue` q WHERE q.`complete` = 0 AND q.`bussy` = 0 AND project_name = ?";
        PreparedStatement freeProjectStatement = connection.prepareStatement(projectFree);
        freeProjectStatement.setString(1, projectName);
        ResultSet freeProjectResult = freeProjectStatement.executeQuery();
        if (!freeProjectResult.next()) {
            return;
        }

        String clean = "DELETE FROM onePass WHERE project_name = ?";
        PreparedStatement statement = connection.prepareStatement(clean);
        statement.setString(1, projectName);
        statement.executeUpdate();
    }

    private static String getNextProjectToProcess() throws SQLException {
        while (true) {
            String projectClaim = "SELECT `project_name` FROM `queue` q WHERE q.`complete` = 0 AND q.`bussy` = 0 AND project_name NOT IN (SELECT project_name FROM onePass) ORDER BY q.project_name";
            PreparedStatement freeProjectStatement = connection.prepareStatement(projectClaim);
            ResultSet freeProjectResult = freeProjectStatement.executeQuery();
            if (!freeProjectResult.next()) {
                return null;
            }

            String projectName = freeProjectResult.getString(1);

            if (updateOnePassStatus(projectName))
                return projectName;
        }
    }

    private static void killWhenInactive() {

        Thread keepAliveThread = new Thread(() -> {
            while (true) {
                Calendar minDate = Calendar.getInstance();
                minDate.setTime(new Date());
                minDate.add(Calendar.MINUTE, -30);

                if (!LinkerLogger.getLastLog().after(minDate.getTime())) {
                    LinkerLogger.logDetail("Took too long");
                    System.exit(0);
                }

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    return;
                }
            }
        });
        keepAliveThread.setDaemon(true);
        keepAliveThread.start();

    }

    private static String getNextElementQuery() {
        if (onePass) {
            return "SELECT `id`, `project_name`, `test_class`, `source_path`, `class_path` FROM `queue` q WHERE q.`complete` = 0 AND q.`bussy` = 0 AND project_name NOT IN (SELECT project_name FROM onePass WHERE project_name != ?) ORDER BY q.project_name, q.source_path LIMIT 1";
        } else {
            String doNotIncludeWhenToMuchError = "(q.project_name = ? OR q.project_name\n" +
                    "IN (\n" +
                    "SELECT fq.project_name\n" +
                    "FROM queue fq\n" +
                    "GROUP BY fq.project_name \n" +
                    "HAVING COUNT(CASE WHEN fq.bussy = 1 THEN fq.bussy END) - 35 < COUNT(CASE WHEN fq.complete = 1 THEN fq.complete END)\n" +
                    "))\n" +
                    " ";

            return "SELECT `id`, `project_name`, `test_class`, `source_path`, `class_path` FROM `queue` q WHERE q.`complete` = 0 AND q.`bussy` = 0 AND " + doNotIncludeWhenToMuchError + " ORDER BY q.project_name, q.source_path LIMIT 1";
        }
    }

    private static boolean processTest(ResultSet elementToProcess) throws SQLException, IOException {

        int id = elementToProcess.getInt("id");
        String projectName = elementToProcess.getString(2);

        LinkerLogger.logDetail("Updating status");
        boolean normalStatus = updateTestStatus(id, 1, 0);
        if (!normalStatus)
            return true;

        LinkerLogger.logDetail("Working");
        String testClass = elementToProcess.getString(3);
        String sourcePath = elementToProcess.getString(4);
        List<String> classPath = new ArrayList<>();
        String classField = elementToProcess.getString(5);
        if (classField != null && !classField.isEmpty())
            classPath.addAll(Arrays.asList(classField.split(";")));

        if (cachedJavaSourceMapper == null || !sourcePath.equals(cachedJavaSourceMapper.getProjectLocation())) {
            LinkerLogger.logDetail("Create project src.parser");
            cachedJavaSourceMapper = parserCache.getParser(sourcePath);
        }

        if (cachedJavaSourceMapper.isTestClassesEmpty()) {
            return true;
        }

        List<String> sourcePaths = new ArrayList<>();
        sourcePaths.add(sourcePath);
        if (onePass) {
            classPath = getProjectClassPath(projectName);
            sourcePaths = getProjectSourcePath(projectName);
        }

        UnitTestLinker unitTestLinker = getUnitTestLinker(projectName, sourcePaths, classPath);

        boolean linkDone;
        if (onePass)
            linkDone = TestToMethodLinker.linkTestCaseCashedLinker(projectName, testClass, sourcePath, unitTestLinker, sourceCodeMap);
        else
            linkDone = TestToMethodLinker.linkTestCase(projectName, testClass, sourcePath, classPath, unitTestLinker, sourceCodeMap);


        if (linkDone)
            updateTestStatus(id, 0, 1);

        return true;
    }

    private static List<String> getProjectSourcePath(String projectName) throws SQLException {
        PreparedStatement sStatement = connection.prepareStatement("select `source_path` from queue where `project_name` = ?");
        sStatement.setString(1, projectName);
        ResultSet resultSet = sStatement.executeQuery();
        HashSet<String> sourceSet = new HashSet<>();
        while (resultSet.next()) {
            sourceSet.add(resultSet.getString(1));
        }
        return new ArrayList<>(sourceSet);
    }

    private static UnitTestLinker getUnitTestLinker(String projectName, List<String> sourcePaths, List<String> classPath) throws SQLException {
        UnitTestLinker unitTestLinker = null;
        if (onePass && cache.getKey().equals(projectName)) {
            unitTestLinker = cache.getValue();
            unitTestLinker.setJavaSourceMapper(cachedJavaSourceMapper);
            return unitTestLinker;
        } else {
            if (linkMethod == LinkMethods.AST) {
                unitTestLinker = new ASTAnalysisWithLargestMatchingClass(cachedJavaSourceMapper);
            } else if (linkMethod == LinkMethods.BYTE_CODE) {
                unitTestLinker = new BytecodeAnalysisWithLargestMatchingClass(cachedJavaSourceMapper);
            }
            if (!onePass)
                return unitTestLinker;

            cache = new AbstractMap.SimpleEntry<>(projectName, null);

            List<String> testClasses = getTestClasses(projectName);

            try {
                unitTestLinker.initialize(classPath, sourcePaths, testClasses);
            } catch (IOException | ClassHierarchyException | CallGraphBuilderCancelException e) {
                TestToMethodLinker.handleError(projectName, "ALL", "ALL", sourceCodeMap, e.getMessage());
                return null;
            }

            cache = new AbstractMap.SimpleEntry<>(projectName, unitTestLinker);
            return unitTestLinker;
        }
    }

    private static List<String> getTestClasses(String projectName) throws SQLException {
        PreparedStatement sql = connection.prepareStatement("SELECT DISTINCT `test_class` FROM `queue` WHERE `project_name` = ?");
        sql.setString(1, projectName);
        ResultSet resultSet = sql.executeQuery();
        List<String> testClasses = new ArrayList<>();
        while (resultSet.next()) {
            testClasses.add(resultSet.getString(1));
        }
        return testClasses;
    }

    private static List<String> getProjectClassPath(String projectName) throws SQLException {
        List<String> classPath;
        PreparedStatement cpStatement = connection.prepareStatement("select `class_path` from queue where `project_name` = ?");
        cpStatement.setString(1, projectName);
        ResultSet resultSet1 = cpStatement.executeQuery();
        HashSet classPathStack = new HashSet();
        while (resultSet1.next()) {
            String[] split = resultSet1.getString(1).split(";");
            classPathStack.addAll(Arrays.asList(split));
        }
        classPath = new ArrayList<>(classPathStack);
        return classPath;
    }

    private static boolean updateTestStatus(int id, int busy, int complete) throws SQLException {
        PreparedStatement notifyBegin = connection.prepareStatement("UPDATE queue SET bussy = " + busy + ", complete = " + complete + " WHERE complete = 0 AND id = ?");
        notifyBegin.setInt(1, id);
        return notifyBegin.executeUpdate() != 0;
    }

    private static boolean updateOnePassStatus(String projectName) throws SQLException {
        String insertQ = "INSERT INTO onePass (project_name)\n" +
                "SELECT * FROM (SELECT ?) AS tmp\n" +
                "WHERE NOT EXISTS (\n" +
                "    SELECT project_name FROM onePass WHERE project_name = ?\n" +
                ") LIMIT 1; ";
        PreparedStatement statement = connection.prepareStatement(insertQ);
        statement.setString(1, projectName);
        statement.setString(2, projectName);
        return statement.executeUpdate() != 0;
    }
}
