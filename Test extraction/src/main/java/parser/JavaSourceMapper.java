package parser;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseProblemException;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.comments.Comment;
import linker.JavaMethodReference;
import logger.LinkerLogger;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class JavaSourceMapper implements Serializable {

    private String projectRootFolder;

    private Map<JavaMethodReference, String> sourceCodeMapping = new HashMap<>();
    private Map<JavaMethodReference, String> SBTMapping = new HashMap<>();

    private Map<String, List<String>> baseClasses = new HashMap<>();

    private HashSet<String> containedClasses = new HashSet<>();

    Map<String, List<JavaMethodReference>> testClasses = new HashMap<>();

    public JavaSourceMapper(String projectRootFolder) throws IOException {
        this.projectRootFolder = projectRootFolder;
        Path path = Paths.get(projectRootFolder);
        Files.find(path,
                Integer.MAX_VALUE,
                (filePath, fileAttr) -> fileAttr.isRegularFile() && filePath.toString().toLowerCase().endsWith(".java"))
                .forEach(this::addJavaFile);
    }

    public void addJavaFile(Path javaFileLocation) {
        try {
            CompilationUnit compilationUnit = getCompilationUnit(javaFileLocation);
            if(compilationUnit == null)
                return;

            Optional<PackageDeclaration> packageDeclaration = compilationUnit.getPackageDeclaration();
            String packageName = "";
            if(packageDeclaration.isPresent()) {
                packageName = packageDeclaration.get().getName().toString();
            }

            BaseClassFetcher baseClassFetcher = new BaseClassFetcher(packageName);
            compilationUnit.accept(baseClassFetcher, null);
            this.baseClasses.put(baseClassFetcher.getClassName(), baseClassFetcher.getBaseClasses());

            MethodFetchVisitor visitor = new MethodFetchVisitor(packageName);
            compilationUnit.accept(visitor, null);
            this.sourceCodeMapping.putAll(visitor.getResultAsString());
            this.SBTMapping.putAll(visitor.getResultAsSBT());
            this.containedClasses.addAll(visitor.getContainedClasses());

            TestClassFetcher testClassFetcher = new TestClassFetcher(packageName, compilationUnit);
            compilationUnit.accept(testClassFetcher, null);
            testClasses.putAll(testClassFetcher.getTestClasses());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private CompilationUnit getCompilationUnit(Path javaFileLocation) throws IOException {
        try {
            InputStream javaFile = new FileInputStream(javaFileLocation.toString());
            CompilationUnit compilationUnit = JavaParser.parse(javaFile);

            //Remove comments in whole unit
            compilationUnit.getAllContainedComments().forEach(Comment::remove);
            compilationUnit = JavaParser.parse(compilationUnit.toString());

            javaFile.close();
            return compilationUnit;
        } catch (ParseProblemException parseProblemException) {
            LinkerLogger.logError("Cannot parse: " + parseProblemException.getMessage());
            return null;
        }
    }

    public String getMethodCode(JavaMethodReference reference) {
        return fetchCode(reference, this.sourceCodeMapping);
    }

    private String fetchCode(JavaMethodReference reference, Map<JavaMethodReference, String> mapping) {
        String code = mapping.get(reference);
        if(code != null)
            return code;

        List<String> baseClasses = this.baseClasses.get(reference.getClassName());
        if(baseClasses == null)
            return null;

        for(String baseClass : baseClasses) {
            if(baseClass.equals(reference.getClassName()))
                continue;

            String baseCode = fetchCode(new JavaMethodReference(baseClass, reference.getMethodName()), mapping);
            if(baseCode != null)
                return baseCode;
        }

        return null;
    }

    public String getMethodCodeSBT(JavaMethodReference reference) {
        return fetchCode(reference, this.SBTMapping);

    }

    public List<String> getBaseClasses(List<String> getBaseClass) {
        if(getBaseClass.size() == 0)
            return new ArrayList<>();

        List<String> baseClasses = new ArrayList<>();
        for(String findBase : getBaseClass) {
            List<String> baseClassList = this.baseClasses.get(findBase);
            if(baseClassList != null)
                baseClasses.addAll(baseClassList);
        }

        baseClasses.addAll(this.getBaseClasses(baseClasses));

        return baseClasses;

    }

    public String getProjectLocation() {
        return projectRootFolder;
    }

    public boolean isClassContained(String className) {
        return this.containedClasses.contains(className);
    }

    public List<JavaMethodReference> getTestMethods(String testMethodClass) {
        return this.testClasses.get(testMethodClass);
    }

    public boolean isTestClassesEmpty() {
        return this.testClasses.isEmpty();
    }
}
