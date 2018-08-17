package parser;

import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.AnnotationDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.type.ClassOrInterfaceType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class BaseClassFetcher extends JavaParserVisitor<Void> {
    private String className;

    private List<String> baseClasses;

    private HashMap<String, String> imports = new HashMap<>();

    public BaseClassFetcher(String packageName) {
        super(packageName);
        this.baseClasses = new ArrayList<>();
        this.className = "";
    }

    @Override
    public void visit(ImportDeclaration n, Void arg) {

        String importName = n.getName().toString();

        int classSeparator = importName.lastIndexOf('.');
        String className = classSeparator == -1 ? importName : importName.substring(classSeparator + 1).trim();
        imports.put(className, importName);
        super.visit(n, arg);
    }

    @Override
    public void visit(AnnotationDeclaration n, Void arg) {
        super.visit(n, arg);
    }

    @Override
    public void visit(ClassOrInterfaceDeclaration n, Void arg) {
        this.className = n.getName().toString();
        NodeList<ClassOrInterfaceType> extendedTypes = n.getExtendedTypes();
        for (ClassOrInterfaceType extendedType : extendedTypes) {
            String className = extendedType.getName().toString();
            String fullName = this.imports.get(className);
            if(fullName == null) {
                String packageName = this.getPackageName();
                fullName = (!packageName.isEmpty() ? packageName + "." : "") + className;
            }

            //Find the import for this extends
            this.baseClasses.add(fullName);
        }
    }

    public String getClassName() {
        String packageName = this.getPackageName();
        return (!packageName.isEmpty() ? packageName + "." : "") + this.className;
    }

    public List<String> getBaseClasses() {
        return this.baseClasses;
    }
}
