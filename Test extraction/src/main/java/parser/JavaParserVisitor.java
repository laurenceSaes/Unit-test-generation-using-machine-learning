package parser;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

import java.util.Optional;

public class JavaParserVisitor<T> extends VoidVisitorAdapter<T> {

    private String packageName;

    public JavaParserVisitor(String packageName) {
        this.packageName = packageName;
    }

    protected String getFullClassPath(ClassOrInterfaceDeclaration n) {
        StringBuilder parentClasses = new StringBuilder();
        Node node = n;
        while(node != null) {
            Optional<Node> parentNodeOpt = node.getParentNode();
            if (parentNodeOpt.isPresent()) {
                Node parentNode = parentNodeOpt.get();
                if(parentNode instanceof ClassOrInterfaceDeclaration) {
                    parentClasses.append(((ClassOrInterfaceDeclaration) parentNode).getName().toString()).append("$");
                }
                node = parentNode;
                continue;
            }
            break;
        }

        return (!this.packageName.isEmpty() ? this.packageName + "." : "") + parentClasses + n.getName().toString();
    }

    public String getPackageName() {
        return packageName;
    }
}
