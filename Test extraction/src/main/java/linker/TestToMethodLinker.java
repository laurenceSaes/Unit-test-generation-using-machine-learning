package linker;

import com.ibm.wala.ipa.callgraph.CallGraphBuilderCancelException;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import database.MysqlConnect;
import database.MysqlSettings;
import logger.LinkerLogger;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class TestToMethodLinker {

    public static boolean linkTestCase(String project, String classToLink, String projectFolder, List<String> classPath, UnitTestLinker unitTestLinker, SourceCodeMapCallback sourceCodeMap) {
        try {
            LinkerLogger.logDetail("Starting linking init");
            unitTestLinker.initialize(classPath, Arrays.asList(projectFolder), Arrays.asList(classToLink));
            return linkTestCaseCashedLinker(project, classToLink, projectFolder, unitTestLinker, sourceCodeMap);
        }
       catch (ClassHierarchyException | CallGraphBuilderCancelException | NullPointerException | IOException | IllegalStateException ex) {
           handleError(project, classToLink, projectFolder, sourceCodeMap, ex.getMessage());
           return false;
       }
    }

    public static boolean linkTestCaseCashedLinker(String project, String classToLink, String projectFolder, UnitTestLinker unitTestLinker, SourceCodeMapCallback sourceCodeMap) {
        try {
            LinkerLogger.logDetail("Start " + project + " test class " + classToLink);

            if (!sourceCodeMap.beginClassAnalysis(project, classToLink))
                return true;

            LinkerLogger.logDetail("Starting linking");

            unitTestLinker.linkToAllMethods(classToLink, sourceCodeMap);

            LinkerLogger.logDetail("Linking done for " + project + " test class " + classToLink);

            sourceCodeMap.endClassAnalysis();

            return true;
        } catch (NullPointerException | IllegalStateException ex) {
            handleError(project, classToLink, projectFolder, sourceCodeMap, ex.getMessage());
            return false;
        }
    }

    public static void handleError(String project, String classToLink, String projectFolder, SourceCodeMapCallback sourceCodeMap, String message2) {
        String message = "project: " + project + "\n" + "projectFolder: " + projectFolder + "\n" + "classesToTest: " + classToLink + "\n" + message2;
        LinkerLogger.logError(message);
        sourceCodeMap.endClassAnalysis(false, "Linking error");
    }
}
