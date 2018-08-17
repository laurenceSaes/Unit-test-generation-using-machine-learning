package parser.visitor;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

public class MethodExtractVisitor extends VoidVisitorAdapter<Void> {
    private String methodCode = "";

    @Override
    public void visit(MethodDeclaration n, Void arg) {
        methodCode = n.toString();
        super.visit(n, arg);
    }

    public String getMethodCode() {
        return methodCode;
    }
}
