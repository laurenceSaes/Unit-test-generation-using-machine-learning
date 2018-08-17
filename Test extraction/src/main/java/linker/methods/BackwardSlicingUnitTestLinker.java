package linker.methods;

import com.ibm.wala.ipa.callgraph.CallGraphBuilderCancelException;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import linker.*;
import logger.LinkerLogger;
import parser.JavaSourceMapper;
import slicer.Slicer;
import slicer.data.MethodCalls;

import java.io.IOException;
import java.util.*;

public class BackwardSlicingUnitTestLinker extends UnitTestLinker {

    private static List<String> assertStatements = Arrays.asList("assertArrayEquals", "assertEquals", "assertFalse", "assertNotNull", "assertNotSame", "assertNull", "assertSame", "assertThat", "assertTrue");

    private Slicer slicer;

    public BackwardSlicingUnitTestLinker(JavaSourceMapper javaSourceMapper) throws IllegalStateException {
        super(javaSourceMapper);
    }

    @Override
    public void linkToAllMethods(String testMethodClass, SourceCodeMapCallback callback) {

        List<JavaMethodReference> testMethods = this.javaSourceMapper.getTestMethods(testMethodClass);
        Map<String, List<MethodCalls>> methodSlices = createSlices(testMethods);
        String mostCommonTestClass = getMostCommonTestedClass(testMethodClass, testMethods, methodSlices);
        if(mostCommonTestClass == null)
            return;

        reportMethodsUnderTest(mostCommonTestClass, testMethodClass, testMethods, methodSlices, callback);
    }

    @Override
    public void initialize(List<String> classPath, List<String> ignored, List<String> classesToTest) throws IOException, ClassHierarchyException, CallGraphBuilderCancelException {
        this.slicer = getSlicer(classPath, classesToTest);
    }

    @Override
    protected TestMethodCallPerClassManager makeCallOverview(String testMethodClass) {
        return null;
    }

    void reportMethodsUnderTest(String mostCommonTestClass, String testMethodClass, List<JavaMethodReference> testMethods, Map<String, List<MethodCalls>> methodSlices, SourceCodeMapCallback callback) {
        for(JavaMethodReference testMethod : testMethods) {
            List<MethodCalls> methodCalls = methodSlices.get(testMethod.getMethodName());
            List<JavaMethodReference> matchesPerAssert = new ArrayList<>();
            for (MethodCalls methodCall : methodCalls) {
                List<JavaMethodReference> slices = methodCall.getCalls();
                slices.removeIf(p -> !p.getClassName().equals(mostCommonTestClass));

                JavaMethodReference bestSlice = this.fineBestMatchingReferencesBasedOnTestNames(testMethodClass, testMethod.getMethodName(), slices);
                if (bestSlice != null)
                    matchesPerAssert.add(bestSlice);
            }

            reportSlices(testMethod.getMethodName(), testMethodClass, matchesPerAssert, callback);
        }
    }

    private void reportSlices(String testMethodName, String testMethodClass, List<JavaMethodReference> matchesPerAssert, SourceCodeMapCallback callback) {
        LinkerLogger.logDetail("Logging results");
        if (!matchesPerAssert.isEmpty()) {
            JavaMethodReference mostSelected = mostCommon(matchesPerAssert);
            CodeContainer unitTestCodeContainer = new CodeContainer(testMethodClass, testMethodName, this.javaSourceMapper);
            CodeContainer methodCodeContainer = new CodeContainer(mostSelected, this.javaSourceMapper);
            callback.foundMap(mostSelected.getClassName(), mostSelected.getMethodName(), methodCodeContainer.getPlainCode(), methodCodeContainer.getSBTCode(), testMethodClass, testMethodName, unitTestCodeContainer.getPlainCode(), unitTestCodeContainer.getSBTCode());
        } else {
            CodeContainer unitTestCodeContainer = new CodeContainer(testMethodClass, testMethodName, this.javaSourceMapper);
            callback.noMapFound(testMethodClass, testMethodName, unitTestCodeContainer.getPlainCode(), unitTestCodeContainer.getSBTCode());
        }
    }

    String getMostCommonTestedClass(String testMethodClass, List<JavaMethodReference> testMethods, Map<String, List<MethodCalls>> methodSlices) {
        List<String> potentialClassesUnderTest = new ArrayList<>();
        for (JavaMethodReference testMethod : testMethods) {
            List<JavaMethodReference> matchesPerAssert = new ArrayList<>();

            List<MethodCalls> methodCalls = methodSlices.get(testMethod.getMethodName());
            for (MethodCalls methodCall : methodCalls) {
                List<JavaMethodReference> slices = methodCall.getCalls();
                JavaMethodReference bestSlice = this.fineBestMatchingReferencesBasedOnTestNames(testMethodClass, testMethod.getMethodName(), slices);
                if (bestSlice != null)
                    matchesPerAssert.add(bestSlice);
            }

            if (!matchesPerAssert.isEmpty()) {
                JavaMethodReference mostSelected = mostCommon(matchesPerAssert);
                potentialClassesUnderTest.add(mostSelected.getClassName());
            }
        }

        return potentialClassesUnderTest.isEmpty() ? null : mostCommon(potentialClassesUnderTest);
    }

    Map<String, List<MethodCalls>> createSlices(List<JavaMethodReference> testMethods) {
        Map<String, List<MethodCalls>> methodSlices = new HashMap<>();

        //TODO. class of unit test is now full path with package, check implementation
        for (JavaMethodReference testMethod : testMethods) {
            LinkerLogger.logDetail("Slicing " + testMethod.getClassName() + " " + testMethod.getMethodName());
            List<MethodCalls> methodCalls = slicer.sliceCallsInMethod(testMethod.getClassName(), testMethod.getMethodName(), assertStatements, testAnnotation);
            LinkerLogger.logDetail("Done slicing " + testMethod.getClassName() + " " + testMethod.getMethodName());
            methodSlices.put(testMethod.getMethodName(), methodCalls);
        }
        return methodSlices;
    }


}
