package slicer;

import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ipa.slicer.NormalStatement;
import com.ibm.wala.ipa.slicer.Statement;
import com.ibm.wala.ssa.*;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.annotations.Annotation;
import com.ibm.wala.util.intset.IntIterator;
import com.ibm.wala.util.intset.IntSet;
import logger.LinkerLogger;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AnalyzeUtils {

    public IClass findClass(CallGraph callGraph, String findClassName) {
        IClassHierarchy classHierarchy = callGraph.getClassHierarchy();
        for (IClass iClass : classHierarchy) {
            String walaName = iClass.getName().toString();
            if(!findClassName.equals(getJavaClassName(walaName)))
                continue;

            if (!iClass.getClassLoader().getReference().equals(ClassLoaderReference.Application))
                continue;

            return iClass;
        }

        return null;
    }

    public Map<String, CGNode> getTestMethods(CallGraph callGraph, String findClassName, List<String> annotations) {
        IClass iClass = this.findClass(callGraph, findClassName);
        return getClassTestMethods(iClass, annotations, callGraph);
    }

    private Map<String, CGNode> getClassTestMethods(IClass iClass, List<String> annotations, CallGraph callGraph) {
        if(iClass == null) {
            return new HashMap<>();
        }

        Map<String, CGNode> methods = new HashMap<>();
        for (IMethod method : iClass.getAllMethods()) {
            if (!classHasAnnotations(annotations, method))
                continue;

            CGNode methodNode = getMethodNode(callGraph, method);
            if(methodNode != null)
                methods.put(method.getName().toString(), methodNode);
        }


        methods.putAll(this.getClassTestMethods(iClass.getSuperclass(), annotations, callGraph));

        return methods;
    }

    private boolean classHasAnnotations(List<String> annotations, IMethod method) {
        return annotations.size() == 0 || hasAnnotation(annotations, method);
    }

    public List<CGNode> getMethod(CallGraph callGraph, String findClassName, String findMethodName, List<String> annotations) {

        List<CGNode> methods = new ArrayList<CGNode>();

        IClass iClass = this.findClass(callGraph, findClassName);
        if(iClass != null) {
            for (IMethod method : iClass.getAllMethods()) {
                if (classHasAnnotations(annotations, method)) continue;

                if(!findMethodName.equals(method.getName().toString()))
                    continue;

                CGNode methodNode = getMethodNode(callGraph, method);
                if(methodNode != null)
                    methods.add(methodNode);
            }
        }

        return methods;
    }

    private boolean hasAnnotation(List<String> annotations, IMethod method) {
        Collection<Annotation> methodAnnotations = method.getAnnotations();
        for(Annotation annotation : methodAnnotations) {
            if(annotations.contains(annotation.getType().getName().toString()))
                return true;
        }

        return false;
    }

    private CGNode getMethodNode(CallGraph callGraph, IMethod method) {
        MethodReference reference = method.getReference();
        Set<CGNode> nodes = callGraph.getNodes(reference);
        Iterator<CGNode> iterator = nodes.iterator();
        return iterator.hasNext() ? iterator.next() : null;
    }

    public List<CallSiteReference> getCallSites(CGNode node, List<String> methodNames) {
        IR ir = node.getIR();
        if(ir == null)
            return new ArrayList<>();

        List<CallSiteReference> callSiteReferences = new ArrayList<>();
        for (Iterator<SSAInstruction> iterator = ir.iterateAllInstructions(); iterator.hasNext(); ) {
            SSAInstruction instruction = iterator.next();
            if (!(instruction instanceof SSAAbstractInvokeInstruction))
                continue;

            SSAAbstractInvokeInstruction call = (SSAAbstractInvokeInstruction) instruction;
            CallSiteReference callSite = call.getCallSite();
            MethodReference declaredTarget = callSite.getDeclaredTarget();
            if (methodNames == null || methodNames.contains(declaredTarget.getName().toString())) {
                callSiteReferences.add(callSite);
            }
        }
        return callSiteReferences;
    }

    public List<Statement> findCallTo(CGNode node, List<String> call) {
        List<Statement> statements = new ArrayList<>();

        IR ir = node.getIR();
        List<CallSiteReference> callSites = getCallSites(node, call);
        for (CallSiteReference callSite : callSites) {
            IntSet indices = ir.getCallInstructionIndices(callSite);
            IntIterator callIterator = indices.intIterator();
            while (callIterator.hasNext()) {

                //This is how you return the method
                NormalStatement statement = new NormalStatement(node, callIterator.next());
                statements.add(statement);
            }
        }

        return statements;
    }

    public String getJavaClassName(String walaName) {

        Pattern walaExtractPattern = Pattern.compile("[A-Z](.*)");
        Matcher matcher = walaExtractPattern.matcher(walaName);

        if(!matcher.find()) {
            LinkerLogger.logError("Cannot convert " + walaName + " to java name");
            return null;
        }

        return matcher.group(1).replace("/", ".");
    }
}
