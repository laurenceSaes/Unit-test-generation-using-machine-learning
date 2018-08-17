package linker.methods;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.resolution.declarations.*;
import com.github.javaparser.resolution.types.ResolvedReferenceType;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.resolution.types.ResolvedTypeVariable;
import com.github.javaparser.symbolsolver.javaparsermodel.JavaParserFacade;
import com.github.javaparser.resolution.UnsolvedSymbolException;
import com.github.javaparser.symbolsolver.model.resolution.SymbolReference;
import linker.JavaMethodReference;
import logger.LinkerLogger;
import parser.JavaParserVisitor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ASTCalls extends JavaParserVisitor<Void> {

    private final JavaParserFacade javaParserFacade;

    private JavaMethodReference forMethod;

    private boolean wasForThisClass = false;

    private List<JavaMethodReference> result = new ArrayList<>();

    public ASTCalls(String packageName, JavaMethodReference forMethod, JavaParserFacade javaParserFacade) {
        super(packageName);
        this.forMethod = forMethod;
        this.javaParserFacade = javaParserFacade;
    }

    @Override
    public void visit(MethodCallExpr n, Void arg) {
        try {
            Optional<Expression> scope = n.getScope();
            if (!scope.isPresent())
                return;

            String className = findCalledObject(scope);
            if(className == null)
                return;

            this.result.add(new JavaMethodReference(className, n.getName().toString()));
        } catch ( StackOverflowError | UnsolvedSymbolException ignore) {

        } finally {
            super.visit(n, arg);
        }


    }

    private String getObjectTypeClass(ResolvedType objectType) {
        String className = null;

        if (objectType instanceof ResolvedReferenceType) {
            className = ((ResolvedReferenceType) objectType).getQualifiedName();
        }

        if(objectType instanceof ResolvedTypeVariable) {
            className = ((ResolvedTypeVariable)objectType).qualifiedName();
        }

        if(className == null) {
            LinkerLogger.logError("IS NOT ResolvedReferenceType: it is " + objectType.getClass().getName());
        }

        return className;
    }

    private String findCalledObject(Optional<Expression> scope) {
        if(!scope.isPresent())
            return null;


        try {

            Expression expression = scope.get();

            if (expression instanceof MethodCallExpr) {
                MethodCallExpr parentCall = ((MethodCallExpr) expression);
                SymbolReference<ResolvedMethodDeclaration> solve = javaParserFacade.solve(parentCall);
                if (!solve.isSolved())
                    return null;
                return getObjectTypeClass(solve.getCorrespondingDeclaration().getReturnType());
            }
            if (expression instanceof FieldAccessExpr) {
                FieldAccessExpr fieldCall = ((FieldAccessExpr) expression);
                SymbolReference<ResolvedFieldDeclaration> solve = javaParserFacade.solve(fieldCall);
                if (!solve.isSolved())
                    return null;
                return getObjectTypeClass(solve.getCorrespondingDeclaration().getType());
            }
            if (expression instanceof ArrayAccessExpr) {
                ArrayAccessExpr arrayAccessExpr = ((ArrayAccessExpr) expression);
                Optional<Node> parentNode = arrayAccessExpr.getParentNode();
                if(parentNode.isPresent() && parentNode.get() instanceof MethodCallExpr) {
                    return getParentMethodClassName(parentNode);
                }

                return null;
            }
            if (expression instanceof ClassExpr) {
                return null;
            }
            if (expression instanceof EnclosedExpr) {
                return null;
            }

            if (expression instanceof ObjectCreationExpr) {
                ObjectCreationExpr objectCreationExpr = ((ObjectCreationExpr) expression);
                SymbolReference<ResolvedConstructorDeclaration> solve = javaParserFacade.solve(objectCreationExpr);
                if (!solve.isSolved())
                    return null;
                ResolvedReferenceTypeDeclaration resolvedReferenceTypeDeclaration = solve.getCorrespondingDeclaration().declaringType();
                return resolvedReferenceTypeDeclaration.getPackageName() + "." + resolvedReferenceTypeDeclaration.getClassName();
            }

            //Use the symbol solver to resolve
            SymbolReference<? extends ResolvedValueDeclaration> solvedSymbol = getSymbolReference(scope);

            if (solvedSymbol == null) {
                //Is it a static class call?
                if(expression instanceof NameExpr) {
                    NameExpr nameExpr = (NameExpr)expression;
                    Optional<Node> parentNode = nameExpr.getParentNode();
                    if(parentNode.isPresent() && parentNode.get() instanceof MethodCallExpr) {
                        return getParentMethodClassName(parentNode);
                    }
                }

                return null;
            }

            ResolvedValueDeclaration objectDeclaration = solvedSymbol.getCorrespondingDeclaration();
            return getObjectTypeClass(objectDeclaration.getType());
        } catch (RuntimeException ex) {
            LinkerLogger.logError("Runtime ex with AST linking: " + ex.getMessage());
            return null;
        }
    }

    private String getParentMethodClassName(Optional<Node> parentNode) {
        SymbolReference<ResolvedMethodDeclaration> solve = javaParserFacade.solve((MethodCallExpr)parentNode.get());
        ResolvedMethodDeclaration correspondingDeclaration = solve.getCorrespondingDeclaration();
        String packageName = correspondingDeclaration.getPackageName();
        String className = correspondingDeclaration.getClassName();
        return packageName + "." + className;
    }

    private SymbolReference<? extends ResolvedValueDeclaration> getSymbolReference(Optional<Expression> scope) {
        SymbolReference<? extends ResolvedValueDeclaration> initSolved;
        try {
            initSolved = javaParserFacade.solve(scope.get());
            return initSolved.isSolved() ? initSolved : null;
        } catch (IllegalArgumentException ex) {
            LinkerLogger.logError("We do not support call resolve for: " + ex.getMessage());
            return null;
        }
    }

    @Override
    public void visit(EnumDeclaration n, Void arg) {
        if (!n.getName().toString().equals(forMethod.getMethodName()))
            return;
        super.visit(n, arg);
    }

    @Override
    public void visit(ConstructorDeclaration n, Void arg) {
        if (!n.getName().toString().equals(forMethod.getMethodName()))
            return;
        super.visit(n, arg);
    }

    @Override
    public void visit(MethodDeclaration n, Void arg) {
        if (!n.getName().toString().equals(forMethod.getMethodName()))
            return;
        super.visit(n, arg);
    }

    @Override
    public void visit(ClassOrInterfaceDeclaration n, Void arg) {
        String className = this.getFullClassPath(n);
        if (!className.equals(forMethod.getClassName()))
            return;

        this.wasForThisClass = true;
        super.visit(n, arg);
    }

    public boolean isWasForThisClass() {
        return wasForThisClass;
    }

    public List<JavaMethodReference> getResult() {
        return result;
    }
}
