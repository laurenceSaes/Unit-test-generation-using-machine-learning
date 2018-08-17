package application.utilities;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseProblemException;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CodePointCharStream;
import org.antlr.v4.runtime.CommonTokenStream;
import parser.StructureBasedTraversal;
import parser.visitor.MethodBodyExtractVisitor;
import parser.visitor.MethodStringExtractorVisitor;
import sbt.grammar.SBTLexer;
import sbt.grammar.SBTParser;

public class MethodTransformation {
    public static String getCleanCode(String methodCode) {
        MethodBodyExtractVisitor methodBodyExtractVisitor = new MethodBodyExtractVisitor();
        String methodWithClass = "class tmp { " + methodCode + "}";
        CompilationUnit parse = JavaParser.parse(methodWithClass);
        parse.accept(methodBodyExtractVisitor, null);
        return methodBodyExtractVisitor.getMethodCode();
    }

    public static String getCodeWithoutString(String methodCode) {
        try {
            MethodStringExtractorVisitor methodStringExtractorVisitor = new MethodStringExtractorVisitor();
            String methodWithClass = "class tmp { " + methodCode + "}";
            CompilationUnit parse = JavaParser.parse(methodWithClass);
            parse.accept(methodStringExtractorVisitor, null);

            for (TypeDeclaration t : parse.getTypes()) {
                for (BodyDeclaration body : (NodeList<BodyDeclaration<?>>) t.getMembers()) {
                    if(! (body instanceof MethodDeclaration))
                        continue;

                    return body.toString().replace("\n", " ").replace("\t", "");
                }
            }
        } catch (Exception ignored) {

        }
        return null;
    }

    public static String covertToSBT(String methodCode, boolean optimizations) {
        try {
            StructureBasedTraversal structureBasedTraversal = new StructureBasedTraversal(optimizations);
            String methodWithClass = "class tmp { " + methodCode + "}";
            CompilationUnit parse = JavaParser.parse(methodWithClass);

            //Find the method declaration
            for (TypeDeclaration t : parse.getTypes()) {
                for (Object member : t.getMembers()) {
                    if (member instanceof MethodDeclaration)
                        return ((Node) member).accept(structureBasedTraversal, "");
                }
            }

            return null;

        } catch (ParseProblemException ignored) {
            return null;
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    public static String sbtToCodeTransformation(String sbtSpecification, boolean optimizations) {
        if(sbtSpecification == null)
            return null;

        CodePointCharStream codePointCharStream = CharStreams.fromString(sbtSpecification);

        SBTLexer lexer = new SBTLexer(codePointCharStream);
        CommonTokenStream tokens = new CommonTokenStream(lexer);

        SBTParser parser = new SBTParser(tokens);

        ObjectCreation.setOptimizations(optimizations);
        SBTParser.NodeContext node = parser.node();
        if(node == null)
            return null;
        //showTree(src.parser, node);

        return node.result.toString();
    }

}
