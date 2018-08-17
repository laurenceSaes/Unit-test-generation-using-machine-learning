package parser;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import linker.JavaMethodReference;

import java.util.HashSet;
import java.util.Set;

public class TestMethodFetcher extends JavaParserVisitor<Void> {
    private String className;

    private Set<JavaMethodReference> testMethods = new HashSet<>();

    public TestMethodFetcher(String forClass, String packageName) {
        super(packageName);
        this.className = forClass;
    }

    @Override
    public void visit(ClassOrInterfaceDeclaration n, Void arg) {

        String fullClassPath = this.getFullClassPath(n);
        if(className == null || className.equals(fullClassPath)) {
            Set<JavaMethodReference> methods = new HashSet<>();
            n.accept(new VoidVisitorAdapter<Void>() {
                @Override
                public void visit(MethodDeclaration n, Void arg) {
                    boolean isTestMethod = false;
                    for(AnnotationExpr annotationExpr : n.getAnnotations()) {
                        if(annotationExpr.getName().asString().endsWith("Test")) {
                            isTestMethod = true;
                        }
                    }

                    if(isTestMethod)
                        methods.add(new JavaMethodReference(fullClassPath, n.getName().asString()));
                }
            }, null);
            this.testMethods.addAll(methods);
        }
    }


    public Set<JavaMethodReference> getTestMethods() {
        return testMethods;
    }
}
