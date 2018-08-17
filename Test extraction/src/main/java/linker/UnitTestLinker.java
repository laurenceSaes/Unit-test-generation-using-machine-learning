package linker;

import com.ibm.wala.ipa.callgraph.CallGraphBuilderCancelException;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import logger.LinkerLogger;
import parser.JavaSourceMapper;
import slicer.Slicer;

import java.io.IOException;
import java.util.*;

public abstract class UnitTestLinker {

    protected final List<String> testAnnotation = Arrays.asList("Lorg/junit/Test");

    protected JavaSourceMapper javaSourceMapper;

    public UnitTestLinker(JavaSourceMapper javaSourceMapper) {
        this.javaSourceMapper = javaSourceMapper;
    }

    public abstract void linkToAllMethods(String testMethodClass, SourceCodeMapCallback callback);

    public abstract void initialize(List<String> classPath, List<String> projectFolder, List<String> classesToTest) throws IOException, ClassHierarchyException, CallGraphBuilderCancelException;

    protected abstract TestMethodCallPerClassManager makeCallOverview(String testMethodClass);

    /**
     * Return the last slice witch matches the unit test name and class the best
     */
    protected JavaMethodReference fineBestMatchingReferencesBasedOnTestNames(String unitTestClassName, String unitTestMethodName, List<JavaMethodReference> references) {
        int methodNameDistance = Integer.MAX_VALUE;

        JavaMethodReference bestMatch = null;
        for (int i = references.size() - 1; i >= 0; i--) {
            JavaMethodReference reference = references.get(i);

            if(reference.getClassName().startsWith("java/"))
                continue;

            //Exclude calls from the test itself
            String sliceClassName = reference.getClassName();
            if (unitTestClassName.toLowerCase().equals(sliceClassName.toLowerCase()))
                continue;

            String sliceMethodName = reference.getMethodName().toLowerCase();
            if (!unitTestMethodName.toLowerCase().contains(sliceMethodName) || sliceMethodName.isEmpty())
                continue;

            int newMethodNameDistance = unitTestMethodName.length() - sliceMethodName.length();
            if (methodNameDistance <= newMethodNameDistance)
                continue;

            methodNameDistance = newMethodNameDistance;
            bestMatch = reference;
        }

        return bestMatch;
    }

    protected static <T> T mostCommon(List<T> list) {
        Map<T, Integer> map = new HashMap<>();

        for (T t : list) {
            Integer val = map.get(t);
            map.put(t, val == null ? 1 : val + 1);
        }

        Map.Entry<T, Integer> max = null;

        for (Map.Entry<T, Integer> e : map.entrySet()) {
            if (max == null || e.getValue() > max.getValue())
                max = e;
        }

        return max.getKey();
    }


    protected void reportFoundMappings(List<UnitTestMethodLinkManager.MethodLink> itemsToReport, List<JavaMethodReference> leftToReportQueue,SourceCodeMapCallback callback) {
        for(UnitTestMethodLinkManager.MethodLink link : itemsToReport) {

            JavaMethodReference bestMatch = link.getMethod();
            CodeContainer methodCodeContainer = new CodeContainer(bestMatch, this.javaSourceMapper);

            JavaMethodReference testReference = link.getUnitTest();
            CodeContainer unitTestCodeContainer = new CodeContainer(testReference, this.javaSourceMapper);

            if(methodCodeContainer.getPlainCode() == null) {
                methodCodeContainer = new CodeContainer(bestMatch, this.javaSourceMapper);
            }


            LinkerLogger.logDetail((methodCodeContainer.getPlainCode() != null ? "Code found " : "No code found ") + bestMatch.getClassName() + "-" + bestMatch.getMethodName() + " with " + testReference.getClassName() + "-" + testReference.getMethodName());
            callback.foundMap(bestMatch.getClassName(), bestMatch.getMethodName(), methodCodeContainer.getPlainCode(), methodCodeContainer.getSBTCode(), testReference.getClassName(), testReference.getMethodName(), unitTestCodeContainer.getPlainCode(), unitTestCodeContainer.getSBTCode());

            leftToReportQueue.remove(testReference);
        }
    }

    protected void reportNotFoundMappings(List<JavaMethodReference> leftToReportMethods, SourceCodeMapCallback callback) {
        for(JavaMethodReference leftToReportMethod : leftToReportMethods) {
            LinkerLogger.logDetail("No link for: " + leftToReportMethod.getClassName() + "-" + leftToReportMethod.getMethodName());
            CodeContainer unitTestCodeContainer = new CodeContainer(leftToReportMethod, this.javaSourceMapper);
            callback.noMapFound(leftToReportMethod.getClassName(), leftToReportMethod.getMethodName(), unitTestCodeContainer.getPlainCode(), unitTestCodeContainer.getSBTCode());
        }
    }


    protected Slicer getSlicer(List<String> classPath, List<String> onlyUseTestClasses) throws IOException, ClassHierarchyException, CallGraphBuilderCancelException, NullPointerException {
        List<String> byteCodeAnalysisNodes = new ArrayList<>();
        byteCodeAnalysisNodes.addAll(this.javaSourceMapper.getBaseClasses(onlyUseTestClasses));
        byteCodeAnalysisNodes.addAll(onlyUseTestClasses);

        LinkerLogger.logDetail("Create src.slicer");
        return new Slicer(classPath, byteCodeAnalysisNodes);
    }

    protected void linkBasedOnMostMatch(String testMethodClass, SourceCodeMapCallback callback) {
        UnitTestMethodLinkManager unitTestMethodLinkManager = new UnitTestMethodLinkManager(this.javaSourceMapper);
        List<JavaMethodReference> testMethodNames = this.javaSourceMapper.getTestMethods(testMethodClass);
        if(testMethodNames == null)
            return;

        TestMethodCallPerClassManager callsPerMethod = makeCallOverview(testMethodClass);

        for(String className : callsPerMethod.getClasses()) {
            TestMethodCallPerClassManager callsForClass = callsPerMethod.filterCallsToClass(className);

            for(JavaMethodReference unitTestMethod : testMethodNames) {
                //Contains for example all calls to class A in unit test method B
                String unitTestMethodMethodName = unitTestMethod.getMethodName();
                TestMethodCallPerClassManager referenceForClassAndTest = callsForClass.filterOnUnitTestNames(unitTestMethodMethodName);
                Set<JavaMethodReference> calls = referenceForClassAndTest.getAllReferences();
                if(calls.isEmpty())
                    continue;

                JavaMethodReference filteredReferences = fineBestMatchingReferencesBasedOnTestNames(testMethodClass, unitTestMethodMethodName, new ArrayList<>(calls));
                if(filteredReferences == null)
                    continue;

                TestMethodCall bestCall = new TestMethodCall(unitTestMethodMethodName, filteredReferences);
                unitTestMethodLinkManager.addLink(unitTestMethod, bestCall);
            }
        }

        List<JavaMethodReference> leftToReportMethods = new ArrayList<>(testMethodNames);
        List<UnitTestMethodLinkManager.MethodLink> classWithMostLinks = unitTestMethodLinkManager.getParserContainedClassWithMostLinks();
        if(classWithMostLinks == null) {
            reportNotFoundMappings(leftToReportMethods, callback);
            return;
        }

        reportFoundMappings(classWithMostLinks, leftToReportMethods, callback);
        reportNotFoundMappings(leftToReportMethods, callback);
    }

    public void setJavaSourceMapper(JavaSourceMapper javaSourceMapper) {
        this.javaSourceMapper = javaSourceMapper;
    }
}
