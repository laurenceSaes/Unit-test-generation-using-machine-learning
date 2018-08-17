package linker.methods;

import com.ibm.wala.ipa.callgraph.CallGraphBuilderCancelException;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import linker.*;
import parser.JavaSourceMapper;

import java.io.IOException;
import java.util.List;

public class ASTAnalysisWithLargestMatchingClass extends UnitTestLinker {

    private ASTAnalysis astAnalysis;

    public ASTAnalysisWithLargestMatchingClass(JavaSourceMapper javaSourceMapper) {
        super(javaSourceMapper);
    }

    @Override
    public void initialize(List<String> classPath, List<String> projectFolders, List<String> classesToTest) throws IOException, ClassHierarchyException, CallGraphBuilderCancelException {
        this.astAnalysis = new ASTAnalysis(projectFolders, classPath);
    }

    @Override
    public void linkToAllMethods(String testMethodClass, SourceCodeMapCallback callback) {
        linkBasedOnMostMatch(testMethodClass, callback);
    }

    protected TestMethodCallPerClassManager makeCallOverview(String testMethodClass) {
        TestMethodCallPerClassManager callsForEachTest = new TestMethodCallPerClassManager();

        List<JavaMethodReference> testMethods = this.javaSourceMapper.getTestMethods(testMethodClass);
        for(JavaMethodReference testMethod : testMethods) {
            try {
                List<JavaMethodReference> methodCalls = this.astAnalysis.findCallsIn(testMethod);
                for(JavaMethodReference call : methodCalls) {
                    if(call == null)
                        continue;
                    callsForEachTest.addCall(testMethod.getMethodName(), call);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return callsForEachTest;
    }

}
