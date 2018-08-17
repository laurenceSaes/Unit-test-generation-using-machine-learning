package registration;

import linker.SourceCodeMapCallback;
import logger.LinkerLogger;

public class CodeLinkToConsole implements SourceCodeMapCallback {
    @Override
    public void foundMap(String methodClass, String methodName, String methodCode,String methodSBT, String testMethodClass, String testMethodName, String testCode, String testSBT) {
        LinkerLogger.logDetail("FOUND: " + methodClass + " - " + methodName + " " + testMethodClass + " - " + testMethodName);
    }

    @Override
    public void noMapFound(String testMethodClass, String testMethodName, String testCode, String testCodeSBT) {
        LinkerLogger.logDetail(" NONE " + testMethodClass + " - " + testMethodName);

    }

    @Override
    public boolean beginClassAnalysis(String testProject, String testClassName) {
        LinkerLogger.logDetail("Start " + testProject + " :: " + testClassName + " logging to console");
        return true;
    }

    @Override
    public void endClassAnalysis() {
        LinkerLogger.logDetail("Stop analysis");
    }

    @Override
    public void endClassAnalysis(boolean succeeded, String message) {
        LinkerLogger.logError("Linking result " + (succeeded ? "success" : "failed") + " : " + message);
    }
}
