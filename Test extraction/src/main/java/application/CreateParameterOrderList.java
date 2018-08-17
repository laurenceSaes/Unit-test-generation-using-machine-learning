package application;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import logger.LinkerLogger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class CreateParameterOrderList {

    public static void main(String[] args) throws IOException {

        if (args.length < 1) {
            LinkerLogger.logDetail("<ast-location>");
            args = new String[]{"/home/eigenaar/Documents/javaparser/javaparser-core/src/main/java/com/github/javaparser/ast"};
            //return
        }

        List<Path> allASTfiles = new ArrayList<>();
        Files.find(Paths.get(args[0]),
                Integer.MAX_VALUE,
                (filePath, fileAttr) -> fileAttr.isRegularFile())
                .forEach((Path path) -> { if(path.toString().endsWith(".java")) allASTfiles.add(path); });

        for(Path path : allASTfiles) {

            List<String> parameters = new ArrayList<>();
            String[] className = new String[1];

            CompilationUnit parse = JavaParser.parse(path);
            parse.accept(new VoidVisitorAdapter<Void>() {
                @Override
                public void visit(ClassOrInterfaceDeclaration n, Void arg) {
                    className[0] = n.getName().getIdentifier();
                    super.visit(n, arg);
                }

                @Override
                public void visit(ConstructorDeclaration n, Void arg) {
                    if(isFullConstructor(n)) {
                        for(Parameter p : n.getParameters()) {
                            parameters.add(p.getName().getIdentifier());
                        }
                    }
                }
            }, null);

            if(parameters.size() != 0)
                System.out.println("lookupParameterOrder.put(\""+className[0]+"\", Arrays.asList(\"" + String.join("\",\"", parameters) + "\"));");
            else
                System.out.println("lookupParameterOrder.put(\""+className[0]+"\", new ArrayList());");

        }

    }

    private static boolean isFullConstructor(ConstructorDeclaration n) {
        for(AnnotationExpr annotationExpr : n.getAnnotations()) {
            if(annotationExpr.getName().asString().endsWith("AllFieldsConstructor")) {
                return true;
            }
        }
        return false;
    }

}
