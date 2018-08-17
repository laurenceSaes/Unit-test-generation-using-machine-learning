package linker.methods;

import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.CallGraphBuilderCancelException;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.slicer.Statement;
import linker.*;
import logger.LinkerLogger;
import parser.JavaSourceMapper;
import slicer.AnalyzeUtils;
import slicer.Slicer;

import java.io.IOException;
import java.util.*;

public class BytecodeAnalysisWithLargestMatchingClass extends UnitTestLinker {

    private AnalyzeUtils analyzeUtils = new AnalyzeUtils();

    protected Slicer slicer;


    public BytecodeAnalysisWithLargestMatchingClass(JavaSourceMapper javaSourceMapper) throws IllegalStateException, NullPointerException {
        super(javaSourceMapper);
    }

    public void initialize(List<String> classPath, List<String> ignored, List<String> classesToTest) throws ClassHierarchyException, CallGraphBuilderCancelException, NullPointerException, IOException, IllegalStateException {

        this.slicer = getSlicer(classPath, classesToTest);
    }

    @Override
    public void linkToAllMethods(String testMethodClass, SourceCodeMapCallback callback) {
        linkBasedOnMostMatch(testMethodClass, callback);
    }

    protected TestMethodCallPerClassManager makeCallOverview(String testMethodClass) {
        TestMethodCallPerClassManager callsForEachTest = new TestMethodCallPerClassManager();

        CallGraph callGraph = this.slicer.getCallGraph();
        Map<String, CGNode> testMethodNodes = this.analyzeUtils.getTestMethods(callGraph, testMethodClass, this.testAnnotation);
        for(Map.Entry<String, CGNode> testMethod : testMethodNodes.entrySet()) {
            List<Statement> methodCalls = this.analyzeUtils.findCallTo(testMethod.getValue(), null);
            for(Statement call : methodCalls) {
                List<JavaMethodReference> javaMethodReferences = this.slicer.getJavaMethodReference(call);

                for(JavaMethodReference javaMethodReference : javaMethodReferences)
                    callsForEachTest.addCall(testMethod.getKey(), javaMethodReference);
            }
        }

        return callsForEachTest;
    }

}
