package application;


import cache.ClassAnalyzerCache;
import cache.ParserCache;
import database.MysqlConnect;
import database.MysqlSettings;
import logger.LinkerLogger;
import parser.JavaSourceMapper;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class StartCaching {

    private static MysqlConnect mysqlConnect;

    private static ParserCache parserCache = new ParserCache();
    private static ClassAnalyzerCache classAnalyzerCache = new ClassAnalyzerCache();

    public static void main(String[] args) throws SQLException, IOException {

        int instance, maxInstances;
        if(args.length != 3) {
            LinkerLogger.logDetail("<instance> <maxInstances> <database>");
            instance = 0;
            maxInstances = 1;
            return;
        } else {
            instance = Integer.parseInt(args[0]);
            maxInstances = Integer.parseInt(args[1]);
            MysqlSettings.setDatabaseUrl(args[2]);
        }

        mysqlConnect = new MysqlConnect(
                MysqlSettings.databaseDriver,
                MysqlSettings.databaseUrl,
                MysqlSettings.username,
                MysqlSettings.password,
                MysqlSettings.maxPool
        );
        Connection connect = mysqlConnect.connect();


        LinkerLogger.logDetail("Starting src.parser src.cache");

        processParser(connect, instance, maxInstances);
    }

    private static void processSlicer(Connection connect) throws SQLException {
        PreparedStatement getElementToProcess = connect.prepareStatement("SELECT `id`, `project_name`, `test_class`, `source_path`, `class_path` FROM `queue` GROUP BY `id`, `project_name`, `test_class`, `source_path`, `class_path`");
        ResultSet elementToProcess = getElementToProcess.executeQuery();
        for (int i = 0; elementToProcess.next(); i++) {
            LinkerLogger.logDetail("("+i+") Create src.slicer");
            processSliceCache(elementToProcess);
        }
    }

    private static void processParser(Connection connect, int instance, int maxInstances) throws SQLException {
        PreparedStatement getElementToProcess = connect.prepareStatement("SELECT DISTINCT `source_path` FROM `queue`");
        ResultSet elementToProcess = getElementToProcess.executeQuery();
        for (int i = 0; elementToProcess.next(); i++) {
            if(i % maxInstances != instance )
                continue;

            LinkerLogger.logDetail("("+i+") Create src.parser");
            processParseCache(elementToProcess);
        }
    }

    private static void processSliceCache(ResultSet elementToProcess) throws SQLException {

        String testClass = elementToProcess.getString(3);
        String sourcePath = elementToProcess.getString(4);
        List<String> classPath = new ArrayList<>(Arrays.asList(elementToProcess.getString(5).split(";")));

        LinkerLogger.logDetail("Processing: " + testClass + " - " + sourcePath);

        try {
            JavaSourceMapper mapper = new JavaSourceMapper(sourcePath);
            List<String> byteCodeAnalysisNodes = new ArrayList<>();
            byteCodeAnalysisNodes.addAll(mapper.getBaseClasses(Arrays.asList(testClass)));
            byteCodeAnalysisNodes.addAll(Arrays.asList(testClass));
            classAnalyzerCache.getClassAnalyzer(classPath, byteCodeAnalysisNodes);
        } catch (Exception e) {
            LinkerLogger.logError(e.getMessage());
        }

    }

    private static void processParseCache(ResultSet elementToProcess) {
        try {
            String sourcePath = elementToProcess.getString(1);
            LinkerLogger.logDetail("processing " + sourcePath);
            parserCache.getParser(sourcePath); //Start caching
        } catch (Exception ex) {
            LinkerLogger.logDetail("Cannot create src.parser. Error: " + ex.getMessage());
        }
    }
}

