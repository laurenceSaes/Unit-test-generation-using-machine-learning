package linker;

public interface SourceCodeMapCallback {
    void foundMap(String methodClass, String methodName, String methodCode, String methodCodeSBT, String testMethodClass, String testMethodName, String testCode, String testCodeSBT);
    void noMapFound(String testMethodClass, String testMethodName, String testCode, String testCodeSBT);
    boolean beginClassAnalysis(String testProject, String testClassName);
    void endClassAnalysis();

    void endClassAnalysis(boolean succeeded, String message);
}
