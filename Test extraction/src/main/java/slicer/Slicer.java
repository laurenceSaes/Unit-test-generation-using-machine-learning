package slicer;

import cache.ClassAnalyzerCache;
import com.ibm.wala.ipa.callgraph.*;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.slicer.Statement;
import com.ibm.wala.ipa.slicer.StatementWithInstructionIndex;
import com.ibm.wala.ssa.*;
import com.ibm.wala.util.CancelException;
import linker.JavaMethodReference;
import logger.LinkerLogger;
import slicer.data.MethodCalls;

import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Slicer {

    private AnalyzeUtils analyzeUtils = new AnalyzeUtils();

    private ClassAnalyzer classAnalyzer;
    private ClassAnalyzerCache classAnalyzerCache = new ClassAnalyzerCache();

    public Slicer(List<String> classPath, List<String> includeClassesInSlice) throws IOException, IllegalStateException, NullPointerException, ClassHierarchyException, CallGraphBuilderCancelException {
        this.classAnalyzer = this.classAnalyzerCache.getCachedClassAnalyzer(classPath, includeClassesInSlice);
        if (this.classAnalyzer == null) {
            this.classAnalyzer = new ClassAnalyzer(classPath, includeClassesInSlice);
        }
    }

    /**
     * @param calls NULL to slice every call
     */
    public List<MethodCalls> sliceCallsInMethod(String methodClass, String methodName, List<String> calls, List<String> annotations) {

        List<MethodCalls> result = new ArrayList<>();

        LinkerLogger.logInfo("Get methods");
        CallGraph callGraph = classAnalyzer.getCallGraph();
        List<CGNode> foundMethods = this.analyzeUtils.getMethod(callGraph, methodClass, methodName, annotations);

        for(CGNode method : foundMethods) {
            String foundMethodName = method.getMethod().getName().toString();
            LinkerLogger.logDetail("Processing " + foundMethodName);
            List<Statement> assertStatements = this.analyzeUtils.findCallTo(method, calls);
            List<Statement> allStatements = this.analyzeUtils.findCallTo(method, null);

            Set<JavaMethodReference> methodCalls = getSlicesBySlicingForward(assertStatements, allStatements);

            printResult(methodCalls, "Test slice forward!");

            result.add(new MethodCalls(methodClass, foundMethodName, new ArrayList<>(methodCalls)));
        }

        return result;
    }

    Set<JavaMethodReference> getSlicesBySlicingBackward(List<Statement> assertStatements, List<Statement> allStatements) {
        Set<JavaMethodReference> methodCalls = new HashSet<>();
        for (Statement statement : assertStatements) {
            List<JavaMethodReference> sliceAndFilter = getSliceAndFilter(statement, allStatements, classAnalyzer);
            methodCalls.addAll(sliceAndFilter);

            if(!sliceAndFilter.isEmpty())
                System.out.println("add!");
        }
        return methodCalls;
    }

    Set<JavaMethodReference> getSlicesBySlicingForward(List<Statement> assertStatements, List<Statement> allStatements) {
        Set<JavaMethodReference> methodCalls = new HashSet<>();
        for (Statement statement : allStatements) {
            List<JavaMethodReference> referenceToStatements = getJavaMethodReference(statement);
            for(JavaMethodReference referenceToStatement : referenceToStatements) {
                if (referenceToStatement != null && statementsInfluenceOnOtherStatement(statement, assertStatements, classAnalyzer)) {
                    System.out.println("add!");
                    methodCalls.add(referenceToStatement);
                }
            }
        }
        return methodCalls;
    }

    private boolean statementsInfluenceOnOtherStatement(Statement statement, List<Statement> influence, ClassAnalyzer classAnalyzer) {

        Collection<Statement> slice = slice(statement, SliceType.Forward, classAnalyzer);
        for (Statement currentStatement : slice) {
            if (influence.contains(currentStatement))
                return true;
        }
        return false;
    }

    private List<JavaMethodReference> getSliceAndFilter(Statement statement, List<Statement> sliceCanOnlyContain, ClassAnalyzer classAnalyzer) {
        List<JavaMethodReference> validSlices = new ArrayList<>();
        Collection<Statement> slice = slice(statement, SliceType.Backward, classAnalyzer);
        for (Statement currentStatement : slice) {
            if (sliceCanOnlyContain.contains(currentStatement)) {
                List<JavaMethodReference> javaMethodReferences = getJavaMethodReference(currentStatement);
                System.out.println(javaMethodReferences);
                validSlices.addAll(javaMethodReferences);
            }
        }
        return validSlices;
    }

    public List<JavaMethodReference> getJavaMethodReference(Statement statement) {

        if(!(statement instanceof StatementWithInstructionIndex))
            return new ArrayList<>();

        SSAInstruction instruction = ((StatementWithInstructionIndex) statement).getInstruction();
        return getInstructionReference(instruction, statement);
    }

    private List<JavaMethodReference> getInstructionReference(SSAInstruction instruction, Statement statement) {
        if(!(instruction instanceof SSAInvokeInstruction)) {
            return null;
        }

        Set<JavaMethodReference> javaMethodReferences = new HashSet<>();

        SSAInvokeInstruction ssaInvokeInstruction = (SSAInvokeInstruction)instruction;
        String methodCallName = ssaInvokeInstruction.getDeclaredTarget().getSelector().getName().toString();

        //Add the type of the instruction
        String walaClassName = ssaInvokeInstruction.getDeclaredTarget().getDeclaringClass().getName().toString();
        javaMethodReferences.add(new JavaMethodReference(getJavaClassName(walaClassName), methodCallName));

        //Find the possible targets of a call to a method reference. Add them
        Set<CGNode> possibleTargets = this.classAnalyzer.getCallGraph().getPossibleTargets(statement.getNode(), ((SSAInvokeInstruction) instruction).getCallSite());
        for(CGNode node : possibleTargets) {
            String className = node.getMethod().getDeclaringClass().getName().toString();
            javaMethodReferences.add(new JavaMethodReference(getJavaClassName(className), methodCallName));
        }

        return new ArrayList<>(javaMethodReferences);
    }

    private String getJavaClassName(String walaClassName) {
        return walaClassName.replace("/", ".").substring(1);
    }

    private JavaMethodReference getInstructionReferenceWithRegex(SSAInstruction instruction) {
        Pattern callPattern = Pattern.compile("L([a-zA-Z0-9\\/_]+)[, ]+(.*?)[^a-zA<_>-Z0-9].*");
        String input = instruction.toString();
        Matcher m = callPattern.matcher(input);
        if (!m.find()) {
            return null;
        }

        String className = m.group(1);
        String methodCallName = m.group(2);
        return new JavaMethodReference(className, methodCallName);
    }

    private Collection<Statement> slice(Statement statement, SliceType sliceType, ClassAnalyzer classAnalyzer) {
        CallGraph callGraph = classAnalyzer.getCallGraph();
        PointerAnalysis pointerAnalysis = classAnalyzer.getPointerAnalysis();
        Collection<Statement> slice = new ArrayList<>();
        if(sliceType == SliceType.Forward)
            slice = forwardSlice(callGraph, statement, pointerAnalysis);
        else if (sliceType == SliceType.Backward)
            slice = backwardsSlice(callGraph, statement, pointerAnalysis);
        return slice;
    }

    private Collection<Statement> forwardSlice(CallGraph callGraph, Statement statement, PointerAnalysis pointerAnalysis) {
        try {
            return com.ibm.wala.ipa.slicer.Slicer.computeForwardSlice(statement, callGraph, pointerAnalysis, com.ibm.wala.ipa.slicer.Slicer.DataDependenceOptions.FULL, com.ibm.wala.ipa.slicer.Slicer.ControlDependenceOptions.FULL);
        } catch (CancelException e) {
            return new ArrayList<>();
        }
    }
    private Collection<Statement> backwardsSlice(CallGraph callGraph, Statement statement, PointerAnalysis pointerAnalysis) {
        try {
            return com.ibm.wala.ipa.slicer.Slicer.computeBackwardSlice(statement, callGraph, pointerAnalysis, com.ibm.wala.ipa.slicer.Slicer.DataDependenceOptions.NO_BASE_PTRS, com.ibm.wala.ipa.slicer.Slicer.ControlDependenceOptions.NONE);
        } catch (CancelException e) {
            return new ArrayList<>();
        }
    }

    private void printResult(Set<JavaMethodReference> slice, String title) {
        LinkerLogger.logDetail(title);
        for(JavaMethodReference reference : slice)
            LinkerLogger.logDetail(reference.toString());
        LinkerLogger.logDetail("\n\n\n\n");
    }

    public CallGraph getCallGraph() {
        return classAnalyzer.getCallGraph();
    }
}
