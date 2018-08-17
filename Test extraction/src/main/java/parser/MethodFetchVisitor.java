package parser;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.comments.BlockComment;
import com.github.javaparser.ast.comments.JavadocComment;
import com.github.javaparser.ast.comments.LineComment;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import linker.JavaMethodReference;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class MethodFetchVisitor extends JavaParserVisitor<Void> {

    private String className = "";

    private Map<JavaMethodReference,String> resultAsString = new HashMap<>();
    private Map<JavaMethodReference,String> resultAsSBT = new HashMap<>();
    private HashSet<String> containedClasses = new HashSet<String>();


    public MethodFetchVisitor(String packageName) {
        super(packageName);
    }

    @Override
    public void visit(EnumDeclaration n, Void arg) {
        removeCommends(n);
        JavaMethodReference reference = new JavaMethodReference(this.className, n.getName().toString());
        storeResult(reference, n);
        super.visit(n, arg);
    }

    @Override
    public void visit(ConstructorDeclaration n, Void arg) {
        removeCommends(n);
        JavaMethodReference reference = new JavaMethodReference(this.className, "<init>");
        storeResult(reference, n);
        super.visit(n, arg);
    }

    @Override
    public void visit(MethodDeclaration n, Void arg) {
        if(n.getBody().isPresent()) {
            removeCommends(n);
            JavaMethodReference reference = new JavaMethodReference(this.className, n.getName().toString());
            storeResult(reference, n);
        }

        super.visit(n, arg);
    }

    @Override
    public void visit(ClassOrInterfaceDeclaration n, Void arg) {
        this.className = this.getFullClassPath(n);
        super.visit(n, arg);
        this.className = "";
    }

    private void storeResult(JavaMethodReference reference, Node n) {
        this.containedClasses.add(reference.getClassName());
        this.resultAsString.put(reference, nodeToStringRepresentation(n));
        this.resultAsSBT.put(reference, nodeToSBTRepresentation(n));
    }

    private String nodeToSBTRepresentation(Node n) {
        try {
            StructureBasedTraversal sbt = new StructureBasedTraversal(false);
            String asString  = n.accept(sbt, null);
            return asString.replaceAll("[\\r\\n]+", " ").trim().replaceAll(" +", " ");
        } catch(Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    private String nodeToStringRepresentation(Node n) {
        String asString = n.toString();
        return asString.replaceAll("[\\r\\n]+"," ").trim().replaceAll(" +", " ");
    }

    private void removeCommends(Node n) {
        n.accept(new VoidVisitorAdapter<Void>() {
            @Override
            public void visit(BlockComment n, Void arg) {
                n.remove();
                super.visit(n, arg);
            }

            @Override
            public void visit(JavadocComment n, Void arg) {
                n.remove();
                super.visit(n, arg);
            }

            @Override
            public void visit(LineComment n, Void arg) {
                n.remove();
                super.visit(n, arg);
            }

        }, null);
    }

    public Map<JavaMethodReference,String> getResultAsString() {
        return this.resultAsString;
    }


    public Map<JavaMethodReference,String> getResultAsSBT() {
        return this.resultAsSBT;
    }

    public HashSet<String> getContainedClasses() {
        return containedClasses;
    }
}
