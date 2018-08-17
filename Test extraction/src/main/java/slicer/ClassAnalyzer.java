package slicer;

import com.ibm.wala.classLoader.ClassLoaderFactoryImpl;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.*;
import com.ibm.wala.ipa.callgraph.impl.DefaultEntrypoint;
import com.ibm.wala.ipa.callgraph.impl.Util;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.cha.ClassHierarchyFactory;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.config.AnalysisScopeReader;
import com.ibm.wala.util.debug.UnimplementedError;
import logger.LinkerLogger;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.*;

public class ClassAnalyzer {

    private CallGraph callGraph = null;
    private AnalyzeUtils analyzeUtils = new AnalyzeUtils();
    private PointerAnalysis pointerAnalysis = null;

    public ClassAnalyzer(List<String> appJar, List<String> classes) throws ClassHierarchyException, CallGraphBuilderCancelException, IOException, IllegalStateException, NullPointerException {
        this.initialize(appJar, classes);
    }

    public CallGraph getCallGraph() {
        return callGraph;
    }

    public PointerAnalysis getPointerAnalysis() {
        return pointerAnalysis;
    }

    private void initialize(List<String> appJar, List<String> mainMethods) throws IOException, ClassHierarchyException, CallGraphBuilderCancelException, IllegalStateException, NullPointerException {
        if(appJar.size() == 0 || mainMethods.size() == 0)
            return;

        appJar = ClassPathHelper.resolvePathAndFixMaven(appJar);

        LinkerLogger.logInfo("Loading class paths");
        AnalysisScope scope = getAnalysisScope(appJar);

        LinkerLogger.logInfo("Creating class hierarchy");
        ClassLoaderFactoryImpl factory = new ClassLoaderFactoryImpl(scope.getExclusions());

        LinkerLogger.logInfo("Loading entry points");
        ClassHierarchy classHierarchy = ClassHierarchyFactory.make(scope, factory);
        Iterable<Entrypoint> entryPoints = getEntryPoint(classHierarchy, new HashSet<>(mainMethods));

        LinkerLogger.logInfo("Get options");
        AnalysisOptions options = new AnalysisOptions(scope, entryPoints);
        options.setEntrypoints(entryPoints);
        options.setReflectionOptions(AnalysisOptions.ReflectionOptions.FULL);

        LinkerLogger.logInfo("Create call graph builder");
        AnalysisCache cache = new AnalysisCacheImpl();

        LinkerLogger.logInfo("Create call graph");
        CallGraphBuilder callGraphBuilder = Util.makeZeroContainerCFABuilder(options, cache, classHierarchy, scope);

        try {
            LinkerLogger.logDetail("Create call graph");
            this.callGraph = callGraphBuilder.makeCallGraph(options, null);

            LinkerLogger.logDetail("Create point analysis");
            this.pointerAnalysis = callGraphBuilder.getPointerAnalysis();
            LinkerLogger.logDetail("Create call graph done");
        } catch (Throwable ex) {
            LinkerLogger.logDetail("Call graph cannot be created for " + appJar.toString());
            throw ex;
        }
    }

    private Iterable<Entrypoint> getEntryPoint(ClassHierarchy classHierarchy, HashSet<String> classes) {
        final HashSet<Entrypoint> result = HashSetFactory.make();
        Iterator i$ = classHierarchy.iterator();

        while(i$.hasNext()) {
            IClass iclass = (IClass)i$.next();
            String walaName = iclass.getName().toString();
            String fullClassName = this.analyzeUtils.getJavaClassName(walaName);
            if(!classes.contains(fullClassName))
                continue;

            Collection<IMethod> allMethods = iclass.getAllMethods();
            for(IMethod method : allMethods) {
                result.add(new DefaultEntrypoint(method, classHierarchy));
            }
        }

        return () -> result.iterator();
    }



    private AnalysisScope getAnalysisScope(List<String> paths) throws IOException {
        ClassLoader classLoader = getClass().getClassLoader();
        URL resourceLocation = classLoader.getResource("exclusions.txt");
        if(resourceLocation == null)
            throw new IOException("Cannot find exclusions.txt in recourse location");

        File exclusionsFile = new File(resourceLocation.getFile());

        AnalysisScope scope = null;

        for(int i = 1; i < paths.size(); i++) {
            try  {
                if(scope == null) {
                    scope = AnalysisScopeReader.makeJavaBinaryAnalysisScope(paths.get(i), exclusionsFile);
                    continue;
                }

                ClassLoaderReference loader = scope.getLoader(AnalysisScope.APPLICATION);
                AnalysisScopeReader.addClassPathToScope(paths.get(i), scope, loader);
            } catch (UnimplementedError ignore) {
                LinkerLogger.logWarning("Wala does not support this class: " + ignore.getMessage() + " on : " + paths.get(i));
            }
        }
        return scope;
    }
}