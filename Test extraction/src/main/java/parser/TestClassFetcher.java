package parser;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import linker.JavaMethodReference;

import java.util.*;

public class TestClassFetcher extends JavaParserVisitor<Void> {

    private String className;

    private Map<String, List<JavaMethodReference>> testClasses = new HashMap<>();

    private CompilationUnit compilationUnit;

    public TestClassFetcher(String packageName, CompilationUnit compilationUnit) {
        super(packageName);
        this.compilationUnit = compilationUnit;
    }

    @Override
    public void visit(MethodDeclaration n, Void arg) {
        if(isTestMethod(n)) {
            TestMethodFetcher testMethodFetcher = new TestMethodFetcher(null, this.getPackageName());
            this.compilationUnit.accept(testMethodFetcher, null);
            this.testClasses.put(className, new ArrayList<>(testMethodFetcher.getTestMethods()));
        }
    }

    private boolean isTestMethod(MethodDeclaration n) {
        for(AnnotationExpr annotationExpr : n.getAnnotations()) {
            if(annotationExpr.getName().asString().endsWith("Test")) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void visit(ClassOrInterfaceDeclaration n, Void arg) {
        this.className = this.getFullClassPath(n); // n.getName().toString();
        super.visit(n, arg);
    }

    public Map<String, List<JavaMethodReference>> getTestClasses() {
        return testClasses;
    }
}
