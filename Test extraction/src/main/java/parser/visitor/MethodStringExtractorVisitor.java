package parser.visitor;

import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.visitor.ModifierVisitor;
import com.github.javaparser.ast.visitor.Visitable;

import java.util.Arrays;
import java.util.List;

public class MethodStringExtractorVisitor extends ModifierVisitor<Void> {
    @Override
    public Visitable visit(StringLiteralExpr n, Void arg) {
        n.setString("");
        return super.visit(n, arg);
    }
}
