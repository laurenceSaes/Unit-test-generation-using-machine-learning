package parser;

import application.utilities.ObjectCreation;
import com.github.javaparser.ast.*;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.comments.BlockComment;
import com.github.javaparser.ast.comments.JavadocComment;
import com.github.javaparser.ast.comments.LineComment;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.modules.*;
import com.github.javaparser.ast.stmt.*;
import com.github.javaparser.ast.type.*;
import com.github.javaparser.ast.visitor.GenericVisitorAdapter;
import com.github.javaparser.ast.visitor.Visitable;
import logger.LinkerLogger;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.*;

public class StructureBasedTraversal extends GenericVisitorAdapter<String, String> {

    int tabs = 0;

    private boolean optimalizations;

    private Map<String, List<String>> lookupParameterOrder = new HashMap<>();

    public StructureBasedTraversal(boolean optimaizations) {
        this.optimalizations = optimaizations;
        createLookupTable();
        lookupParameterOrder.get("NodeList").add("innerList");
        lookupParameterOrder.put("ArrayType", Arrays.asList("componentType", "origin", "annotations"));
    }


    //TODO: SBT, you also can ignore constructor and lust fill the fields
    private String displayNode(Object n, String visitorArg) {
        return displayNode(n, visitorArg, null);
    }

    private String displayNode(Object objectToProcess, String visitorArg, List<String> fieldToProcess) {

        Constructor allFieldConstructor = ObjectCreation.findAllFieldConstructor(objectToProcess.getClass());
        String objectToProcessName = objectToProcess.getClass().getSimpleName();
        if (objectToProcessName.equals("NodeList")) {
            Constructor<?>[] constructors = objectToProcess.getClass().getConstructors();
            allFieldConstructor = constructors[2];
        }

        if (allFieldConstructor == null) {
            return null;
        }

        HashMap<String, Object> memberFields = getAllFields(objectToProcess, fieldToProcess);
        List<Object> parameters = orderFields(objectToProcessName, memberFields);
        if(parameters == null)
            return null;

        List<String> strRep = new ArrayList<>();
        for (Object member : parameters) {
            if (member instanceof String)
                strRep.add(escape((String) member));
        }
        String name = objectToProcessName + (strRep.isEmpty() ? "" : "_" + String.join("_", strRep));


        //Less tokens?
        if(optimalizations && objectToProcessName.equals("NodeList") && !((NodeList)objectToProcess).iterator().hasNext()) {
            return "(NodeList)NodeList" + visitorArg;
        }


        StringBuilder sbt = new StringBuilder().append("(").append(name);

        for (Object param : parameters) {
            if (param instanceof String)
                continue;

            if (param instanceof Visitable) {
                Visitable visitable = (Visitable) param;
                String accept = visitable.accept(this, visitorArg);
                if(accept == null)
                    return null;
                sbt.append(accept);
            } else if (param instanceof ArrayList || param instanceof List && !((List) param).isEmpty()) {
                if(!optimalizations || !objectToProcessName.equals("NodeList"))
                    sbt.append("(ArrayList");

                for (Object obj : (List) param) {
                    if (obj instanceof Visitable) {
                        Visitable visitable = (Visitable) obj;
                        String accept = visitable.accept(this, visitorArg);
                        if(accept == null)
                            return null;
                        sbt.append(accept);
                    }
                }
                if(!optimalizations || !objectToProcessName.equals("NodeList"))
                    sbt.append(")ArrayList");
            } else {
                if (param != null) {
                    if (param instanceof EnumSet) {
                        String simpleName =  "EnumSet";
                        for( Object enumName : ((EnumSet)param))
                            simpleName += "_" + escape(enumName.toString());

                        sbt.append("(").append(simpleName).append(")").append(simpleName);
                        continue;
                    }

                    Class<?> paramClass = param.getClass();
                    String useClassName = paramClass.getName().substring(paramClass.getName().lastIndexOf('.') + 1).trim();
                    String simpleName = useClassName + "_" + escape(param.toString());
                    sbt.append("(").append(simpleName).append(")").append(simpleName);
                } else {
                    sbt.append("(null)null");
                }
            }
        }
        sbt.append(")").append(name);

        return visitorArg + sbt.toString();
    }

    public static String escape(String member) {
        return member.replace("\\", "\\\\").replace("_", "\\_").replace("(", "\\(").replace(")", "\\)");
    }

    public static String undoEscape(String input) {
        return input.replaceAll("(?<!\\\\)((\\\\))(?!\\\\)","").replace("\\\\", "\\").replace("\\_", "_").replace("\\(", "(").replace("\\)", ")");
    }

    private List<Object> orderFields(String className, HashMap<String, Object> memberFields) {


        List<String> paramLookup = lookupParameterOrder.get(className);
        if(paramLookup == null) {
            LinkerLogger.logError(className + " is missing a parameter!");
            return null;
        }

        List<Object> output = new ArrayList<>();
        for(String lookup : paramLookup) {
            if(!memberFields.containsKey(lookup)) {
                LinkerLogger.logError(className + " is missing a parameter!");
                return null;
            }
            Object param = memberFields.get(lookup);
            output.add(param);
        }

        return output;

    }


    private HashMap<String, Object> getAllFields(Object objectToProcess, List<String> fieldToProcess) {
        Class<?> current = objectToProcess.getClass();
        HashMap<String, Object> memberFields = new HashMap<>();
        while (current.getSuperclass() != null) {
            Field[] fields = current.getDeclaredFields();
            for (Field field : fields) {
                if (field.getDeclaringClass().getName().endsWith("ast.Node"))
                    continue;

                String fieldName = field.getName();
                if (fieldName.contains("parentNode"))
                    continue;

                if (fieldToProcess != null && !fieldToProcess.contains(fieldName))
                    continue;

                field.setAccessible(true);
                try {
                    memberFields.put(fieldName, field.get(objectToProcess));
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }

            }
            current = current.getSuperclass();
        }
        return memberFields;
    }

    @Override
    public String visit(NodeList n, String arg) {
        return displayNode(n, arg, Arrays.asList("innerList"));
    }

    @Override
    public String visit(AnnotationDeclaration n, String arg) {
        return displayNode(n, arg);
    }

    @Override
    public String visit(AnnotationMemberDeclaration n, String arg) {
        return displayNode(n, arg);
    }

    @Override
    public String visit(ArrayAccessExpr n, String arg) {
        return displayNode(n, arg);
    }

    @Override
    public String visit(ArrayCreationExpr n, String arg) {
        return displayNode(n, arg);
    }

    @Override
    public String visit(ArrayCreationLevel n, String arg) {
        return displayNode(n, arg);
    }

    @Override
    public String visit(ArrayInitializerExpr n, String arg) {
        return displayNode(n, arg);
    }

    @Override
    public String visit(ArrayType n, String arg) {
        return displayNode(n, arg);
    }

    @Override
    public String visit(AssertStmt n, String arg) {
        return displayNode(n, arg);
    }

    @Override
    public String visit(AssignExpr n, String arg) {
        return displayNode(n, arg);
    }

    @Override
    public String visit(BinaryExpr n, String arg) {
        return displayNode(n, arg);
    }

    @Override
    public String visit(BlockComment n, String arg) {
        return displayNode(n, arg);
    }

    @Override
    public String visit(BlockStmt n, String arg) {
        if(optimalizations)
            return n.getStatements().accept(this, arg);
        return displayNode(n, arg);
    }

    @Override
    public String visit(BooleanLiteralExpr n, String arg) {
        return displayNode(n, arg);
    }

    @Override
    public String visit(BreakStmt n, String arg) {
        return displayNode(n, arg);
    }

    @Override
    public String visit(CastExpr n, String arg) {
        return displayNode(n, arg);
    }

    @Override
    public String visit(CatchClause n, String arg) {
        return displayNode(n, arg);
    }

    @Override
    public String visit(CharLiteralExpr n, String arg) {
        return displayNode(n, arg);
    }

    @Override
    public String visit(ClassExpr n, String arg) {
        return displayNode(n, arg);
    }

    @Override
    public String visit(ClassOrInterfaceDeclaration n, String arg) {
        return displayNode(n, arg);
        //return n.getMembers().accept(this, arg);
    }

    @Override
    public String visit(ClassOrInterfaceType n, String arg) {
        return displayNode(n, arg);
    }

    @Override
    public String visit(CompilationUnit n, String arg) {
        return n.accept(this, arg);

//        StringBuilder output = new StringBuilder();
//        NodeList<TypeDeclaration<?>> types = n.getTypes();
//        for(TypeDeclaration t : types) {
//            NodeList members = t.getMembers();
//            for(Object member : members) {
//                String accept = ((Visitable) member).accept(this, arg);
//                if(accept == null)
//                    return null;
//                output.append(accept);
//            }
//        }
//        return output.toString();
    }

    @Override
    public String visit(ConditionalExpr n, String arg) {
        return displayNode(n, arg);
    }

    @Override
    public String visit(ConstructorDeclaration n, String arg) {
        return displayNode(n, arg);
    }

    @Override
    public String visit(ContinueStmt n, String arg) {
        return displayNode(n, arg);
    }

    @Override
    public String visit(DoStmt n, String arg) {
        return displayNode(n, arg);
    }

    @Override
    public String visit(DoubleLiteralExpr n, String arg) {
        return displayNode(n, arg);
    }

    @Override
    public String visit(EmptyStmt n, String arg) {
        return displayNode(n, arg);
    }

    @Override
    public String visit(EnclosedExpr n, String arg) {
        return displayNode(n, arg);
    }

    @Override
    public String visit(EnumConstantDeclaration n, String arg) {
        return displayNode(n, arg);
    }

    @Override
    public String visit(EnumDeclaration n, String arg) {
        return displayNode(n, arg);
    }

    @Override
    public String visit(ExplicitConstructorInvocationStmt n, String arg) {
        return displayNode(n, arg);
    }

    @Override
    public String visit(ExpressionStmt n, String arg) {
//        if(optimalizations)
        //           return n.getExpression().accept(this, arg);
        return displayNode(n, arg);
    }

    @Override
    public String visit(FieldAccessExpr n, String arg) {
        return displayNode(n, arg);
    }

    @Override
    public String visit(FieldDeclaration n, String arg) {
        return displayNode(n, arg);
    }

    @Override
    public String visit(ForStmt n, String arg) {
        return displayNode(n, arg);
    }

    @Override
    public String visit(ForeachStmt n, String arg) {
        return displayNode(n, arg);
    }

    @Override
    public String visit(IfStmt n, String arg) {
        return displayNode(n, arg);
    }

    @Override
    public String visit(ImportDeclaration n, String arg) {
        return displayNode(n, arg);
    }

    @Override
    public String visit(InitializerDeclaration n, String arg) {
        return displayNode(n, arg);
    }

    @Override
    public String visit(InstanceOfExpr n, String arg) {
        return displayNode(n, arg);
    }

    @Override
    public String visit(IntegerLiteralExpr n, String arg) {
        return displayNode(n, arg);
    }

    @Override
    public String visit(IntersectionType n, String arg) {
        return displayNode(n, arg);
    }

    @Override
    public String visit(JavadocComment n, String arg) {
        return displayNode(n, arg);
    }

    @Override
    public String visit(LabeledStmt n, String arg) {
        return displayNode(n, arg);
    }

    @Override
    public String visit(LambdaExpr n, String arg) {
        return displayNode(n, arg);
    }

    @Override
    public String visit(LineComment n, String arg) {
        return displayNode(n, arg);
    }

    @Override
    public String visit(LocalClassDeclarationStmt n, String arg) {
        return displayNode(n, arg);
    }

    @Override
    public String visit(LongLiteralExpr n, String arg) {
        return displayNode(n, arg);
    }

    @Override
    public String visit(MarkerAnnotationExpr n, String arg) {
        return displayNode(n, arg);
    }

    @Override
    public String visit(MemberValuePair n, String arg) {
        return displayNode(n, arg);
    }

    @Override
    public String visit(MethodCallExpr n, String arg) {
        return displayNode(n, arg);
    }

    @Override
    public String visit(MethodDeclaration n, String arg) {
        return displayNode(n, arg);
    }

    @Override
    public String visit(MethodReferenceExpr n, String arg) {
        return displayNode(n, arg);
    }

    @Override
    public String visit(NameExpr n, String arg) {
        if(optimalizations) {
            return n.getName().accept(this, arg);
        }
        return displayNode(n, arg);
    }

    @Override
    public String visit(Name n, String arg) {
        return displayNode(n, arg);
    }

    @Override
    public String visit(NormalAnnotationExpr n, String arg) {
        return displayNode(n, arg);
    }

    @Override
    public String visit(NullLiteralExpr n, String arg) {
        return displayNode(n, arg);
    }

    @Override
    public String visit(ObjectCreationExpr n, String arg) {
        return displayNode(n, arg);
    }

    @Override
    public String visit(PackageDeclaration n, String arg) {
        return displayNode(n, arg);
    }

    @Override
    public String visit(Parameter n, String arg) {
        return displayNode(n, arg);
    }

    @Override
    public String visit(PrimitiveType n, String arg) {
        return displayNode(n, arg, Arrays.asList("type", "annotations"));
    }

    @Override
    public String visit(ReturnStmt n, String arg) {
        return displayNode(n, arg);
    }

    @Override
    public String visit(SimpleName n, String arg) {
        return displayNode(n, arg);
    }

    @Override
    public String visit(SingleMemberAnnotationExpr n, String arg) {
        return displayNode(n, arg);
    }

    @Override
    public String visit(StringLiteralExpr n, String arg) {
        return displayNode(n, arg);
    }

    @Override
    public String visit(SuperExpr n, String arg) {
        return displayNode(n, arg);
    }

    @Override
    public String visit(SwitchEntryStmt n, String arg) {
        return displayNode(n, arg);
    }

    @Override
    public String visit(SwitchStmt n, String arg) {
        return displayNode(n, arg);
    }

    @Override
    public String visit(SynchronizedStmt n, String arg) {
        return displayNode(n, arg);
    }

    @Override
    public String visit(ThisExpr n, String arg) {
        return displayNode(n, arg);
    }

    @Override
    public String visit(ThrowStmt n, String arg) {
        return displayNode(n, arg);
    }

    @Override
    public String visit(TryStmt n, String arg) {
        return displayNode(n, arg);
    }

    @Override
    public String visit(TypeExpr n, String arg) {
        return displayNode(n, arg);
    }

    @Override
    public String visit(TypeParameter n, String arg) {
        return displayNode(n, arg);
    }

    @Override
    public String visit(UnaryExpr n, String arg) {
        return displayNode(n, arg);
    }

    @Override
    public String visit(UnionType n, String arg) {
        return displayNode(n, arg, Arrays.asList("elements"));
    }

    @Override
    public String visit(UnknownType n, String arg) {
        return displayNode(n, arg, new ArrayList<>());
    }

    @Override
    public String visit(VariableDeclarationExpr n, String arg) {
        return displayNode(n, arg);
    }

    @Override
    public String visit(VariableDeclarator n, String arg) {
        return displayNode(n, arg);
    }

    @Override
    public String visit(VoidType n, String arg) {
        return displayNode(n, arg, new ArrayList<>());
    }

    @Override
    public String visit(WhileStmt n, String arg) {
        return displayNode(n, arg);
    }

    @Override
    public String visit(WildcardType n, String arg) {
        return displayNode(n, arg);
    }

    @Override
    public String visit(ModuleDeclaration n, String arg) {
        return displayNode(n, arg);
    }

    @Override
    public String visit(ModuleRequiresStmt n, String arg) {
        return displayNode(n, arg);
    }

    @Override
    public String visit(ModuleExportsStmt n, String arg) {
        return displayNode(n, arg);
    }

    @Override
    public String visit(ModuleProvidesStmt n, String arg) {
        return displayNode(n, arg);
    }

    @Override
    public String visit(ModuleUsesStmt n, String arg) {
        return displayNode(n, arg);
    }

    @Override
    public String visit(ModuleOpensStmt n, String arg) {
        return displayNode(n, arg);
    }

    @Override
    public String visit(UnparsableStmt n, String arg) {
        return displayNode(n, arg);
    }

    @Override
    public String visit(ReceiverParameter n, String arg) {
        return displayNode(n, arg);
    }

    @Override
    public String visit(VarType n, String arg) {
        return displayNode(n, arg);
    }


    private void createLookupTable() {
        lookupParameterOrder.put("Observable", new ArrayList());
        lookupParameterOrder.put("AstObserverAdapter", new ArrayList());
        lookupParameterOrder.put("null", new ArrayList());
        lookupParameterOrder.put("PropagatingAstObserver", new ArrayList());
        lookupParameterOrder.put("AstObserver", new ArrayList());
        lookupParameterOrder.put("ExplicitConstructorInvocationStmt", Arrays.asList("typeArguments","isThis","expression","arguments"));
        lookupParameterOrder.put("Statement", new ArrayList());
        lookupParameterOrder.put("AssertStmt", Arrays.asList("check","message"));
        lookupParameterOrder.put("ThrowStmt", Arrays.asList("expression"));
        lookupParameterOrder.put("WhileStmt", Arrays.asList("condition","body"));
        lookupParameterOrder.put("BreakStmt", Arrays.asList("label"));
        lookupParameterOrder.put("DoStmt", Arrays.asList("body","condition"));
        lookupParameterOrder.put("EmptyStmt", new ArrayList());
        lookupParameterOrder.put("ExpressionStmt", Arrays.asList("expression"));
        lookupParameterOrder.put("UnparsableStmt", new ArrayList());
        lookupParameterOrder.put("BlockStmt", Arrays.asList("statements"));
        lookupParameterOrder.put("SwitchStmt", Arrays.asList("selector","entries"));
        lookupParameterOrder.put("TryStmt", Arrays.asList("resources","tryBlock","catchClauses","finallyBlock"));
        lookupParameterOrder.put("IfStmt", Arrays.asList("condition","thenStmt","elseStmt"));
        lookupParameterOrder.put("ContinueStmt", Arrays.asList("label"));
        lookupParameterOrder.put("ForStmt", Arrays.asList("initialization","compare","update","body"));
        lookupParameterOrder.put("LabeledStmt", Arrays.asList("label","statement"));
        lookupParameterOrder.put("SynchronizedStmt", Arrays.asList("expression","body"));
        lookupParameterOrder.put("ForeachStmt", Arrays.asList("variable","iterable","body"));
        lookupParameterOrder.put("CatchClause", Arrays.asList("parameter","body"));
        lookupParameterOrder.put("LocalClassDeclarationStmt", Arrays.asList("classDeclaration"));
        lookupParameterOrder.put("ReturnStmt", Arrays.asList("expression"));
        lookupParameterOrder.put("SwitchEntryStmt", Arrays.asList("label","statements"));
        lookupParameterOrder.put("null", new ArrayList());
        lookupParameterOrder.put("LineComment", Arrays.asList("content"));
        lookupParameterOrder.put("Comment", Arrays.asList("content"));
        lookupParameterOrder.put("CommentsCollection", new ArrayList());
        lookupParameterOrder.put("BlockComment", Arrays.asList("content"));
        lookupParameterOrder.put("JavadocComment", Arrays.asList("content"));
        lookupParameterOrder.put("DataKey", new ArrayList());
        lookupParameterOrder.put("ModuleExportsStmt", Arrays.asList("name","moduleNames"));
        lookupParameterOrder.put("ModuleStmt", new ArrayList());
        lookupParameterOrder.put("ModuleRequiresStmt", Arrays.asList("modifiers","name"));
        lookupParameterOrder.put("ModuleUsesStmt", Arrays.asList("type"));
        lookupParameterOrder.put("ModuleProvidesStmt", Arrays.asList("type","withTypes"));
        lookupParameterOrder.put("ModuleOpensStmt", Arrays.asList("name","moduleNames"));
        lookupParameterOrder.put("ModuleDeclaration", Arrays.asList("annotations","name","isOpen","moduleStmts"));
        lookupParameterOrder.put("PostOrderIterator", new ArrayList());
        lookupParameterOrder.put("WildcardType", Arrays.asList("extendedType","superType","annotations"));
        lookupParameterOrder.put("VoidType", new ArrayList());
        lookupParameterOrder.put("ClassOrInterfaceType", Arrays.asList("scope","name","typeArguments","annotations"));
        lookupParameterOrder.put("Type", Arrays.asList("annotations"));
        lookupParameterOrder.put("UnknownType", new ArrayList());
        lookupParameterOrder.put("PrimitiveType", Arrays.asList("type","annotations"));
        lookupParameterOrder.put("TypeParameter", Arrays.asList("name","typeBound","annotations"));
        lookupParameterOrder.put("UnionType", Arrays.asList("elements"));
        lookupParameterOrder.put("ReferenceType", Arrays.asList("annotations"));
        lookupParameterOrder.put("VarType", new ArrayList());
        lookupParameterOrder.put("IntersectionType", Arrays.asList("elements"));
        lookupParameterOrder.put("ArrayBracketPair", Arrays.asList("componentType","origin","annotations"));
        lookupParameterOrder.put("PackageDeclaration", Arrays.asList("annotations","name"));
        lookupParameterOrder.put("EnumDeclaration", Arrays.asList("modifiers","annotations","name","implementedTypes","entries","members"));
        lookupParameterOrder.put("AnnotationDeclaration", Arrays.asList("modifiers","annotations","name","members"));
        lookupParameterOrder.put("TypeDeclaration", Arrays.asList("modifiers","annotations","name","members"));
        lookupParameterOrder.put("InitializerDeclaration", Arrays.asList("isStatic","body"));
        lookupParameterOrder.put("ConstructorDeclaration", Arrays.asList("modifiers","annotations","typeParameters","name","parameters","thrownExceptions","body","receiverParameter"));
        lookupParameterOrder.put("VariableDeclarator", Arrays.asList("type","name","initializer"));
        lookupParameterOrder.put("Signature", Arrays.asList("modifiers","annotations","typeParameters","name","parameters","thrownExceptions","receiverParameter"));
        lookupParameterOrder.put("BodyDeclaration", Arrays.asList("annotations"));
        lookupParameterOrder.put("FieldDeclaration", Arrays.asList("modifiers","annotations","variables"));
        lookupParameterOrder.put("Parameter", Arrays.asList("modifiers","annotations","type","isVarArgs","varArgsAnnotations","name"));
        lookupParameterOrder.put("AnnotationMemberDeclaration", Arrays.asList("modifiers","annotations","type","name","defaultValue"));
        lookupParameterOrder.put("MethodDeclaration", Arrays.asList("modifiers","annotations","typeParameters","type","name","parameters","thrownExceptions","body","receiverParameter"));
        lookupParameterOrder.put("ReceiverParameter", Arrays.asList("annotations","type","name"));
        lookupParameterOrder.put("EnumConstantDeclaration", Arrays.asList("annotations","name","arguments","classBody"));
        lookupParameterOrder.put("ClassOrInterfaceDeclaration", Arrays.asList("modifiers","annotations","isInterface","name","typeParameters","extendedTypes","implementedTypes","members"));
        lookupParameterOrder.put("NodeList", new ArrayList());
        lookupParameterOrder.put("null", new ArrayList());
        lookupParameterOrder.put("Validator", new ArrayList());
        lookupParameterOrder.put("Validators", new ArrayList());
        lookupParameterOrder.put("ProblemReporter", new ArrayList());
        lookupParameterOrder.put("Java7Validator", new ArrayList());
        lookupParameterOrder.put("TypedValidator", new ArrayList());
        lookupParameterOrder.put("UnderscoreKeywordValidator", new ArrayList());
        lookupParameterOrder.put("ModifierValidator", new ArrayList());
        lookupParameterOrder.put("NoUnderscoresInIntegerLiteralsValidator", new ArrayList());
        lookupParameterOrder.put("CommonValidators", new ArrayList());
        lookupParameterOrder.put("NoBinaryIntegerLiteralsValidator", new ArrayList());
        lookupParameterOrder.put("VarValidator", new ArrayList());
        lookupParameterOrder.put("SingleNodeTypeValidator", new ArrayList());
        lookupParameterOrder.put("VisitorValidator", new ArrayList());
        lookupParameterOrder.put("Java9Validator", new ArrayList());
        lookupParameterOrder.put("NoProblemsValidator", new ArrayList());
        lookupParameterOrder.put("Java1_2Validator", new ArrayList());
        lookupParameterOrder.put("Java11Validator", new ArrayList());
        lookupParameterOrder.put("Java1_1Validator", new ArrayList());
        lookupParameterOrder.put("Java1_0Validator", new ArrayList());
        lookupParameterOrder.put("Java5Validator", new ArrayList());
        lookupParameterOrder.put("Java1_3Validator", new ArrayList());
        lookupParameterOrder.put("TreeVisitorValidator", new ArrayList());
        lookupParameterOrder.put("Java8Validator", new ArrayList());
        lookupParameterOrder.put("Java1_4Validator", new ArrayList());
        lookupParameterOrder.put("Java10Validator", new ArrayList());
        lookupParameterOrder.put("ReservedKeywordValidator", new ArrayList());
        lookupParameterOrder.put("Java6Validator", new ArrayList());
        lookupParameterOrder.put("SimpleValidator", new ArrayList());
        lookupParameterOrder.put("GenericVisitor", new ArrayList());
        lookupParameterOrder.put("CloneVisitor", new ArrayList());
        lookupParameterOrder.put("VoidVisitorWithDefaults", new ArrayList());
        lookupParameterOrder.put("NoCommentHashCodeVisitor", new ArrayList());
        lookupParameterOrder.put("GenericVisitorWithDefaults", new ArrayList());
        lookupParameterOrder.put("HashCodeVisitor", new ArrayList());
        lookupParameterOrder.put("TreeVisitor", new ArrayList());
        lookupParameterOrder.put("GenericVisitorAdapter", new ArrayList());
        lookupParameterOrder.put("ModifierVisitor", new ArrayList());
        lookupParameterOrder.put("ObjectIdentityEqualsVisitor", new ArrayList());
        lookupParameterOrder.put("GenericListVisitorAdapter", new ArrayList());
        lookupParameterOrder.put("VoidVisitor", new ArrayList());
        lookupParameterOrder.put("Visitable", new ArrayList());
        lookupParameterOrder.put("VoidVisitorAdapter", new ArrayList());
        lookupParameterOrder.put("NoCommentEqualsVisitor", new ArrayList());
        lookupParameterOrder.put("EqualsVisitor", new ArrayList());
        lookupParameterOrder.put("ObjectIdentityHashCodeVisitor", new ArrayList());
        lookupParameterOrder.put("ArrayCreationLevel", Arrays.asList("dimension","annotations"));
        lookupParameterOrder.put("Storage", Arrays.asList("packageDeclaration","imports","types","module"));
        lookupParameterOrder.put("null", new ArrayList());
        lookupParameterOrder.put("ImportDeclaration", Arrays.asList("name","isStatic","isAsterisk"));
        lookupParameterOrder.put("NodeWithBlockStmt", new ArrayList());
        lookupParameterOrder.put("NodeWithPublicModifier", new ArrayList());
        lookupParameterOrder.put("NodeWithStaticModifier", new ArrayList());
        lookupParameterOrder.put("NodeWithPrivateModifier", new ArrayList());
        lookupParameterOrder.put("NodeWithFinalModifier", new ArrayList());
        lookupParameterOrder.put("NodeWithAbstractModifier", new ArrayList());
        lookupParameterOrder.put("NodeWithAccessModifiers", new ArrayList());
        lookupParameterOrder.put("NodeWithProtectedModifier", new ArrayList());
        lookupParameterOrder.put("NodeWithStrictfpModifier", new ArrayList());
        lookupParameterOrder.put("NodeWithImplements", new ArrayList());
        lookupParameterOrder.put("NodeWithOptionalLabel", new ArrayList());
        lookupParameterOrder.put("NodeWithTokenRange", new ArrayList());
        lookupParameterOrder.put("NodeWithSimpleName", new ArrayList());
        lookupParameterOrder.put("NodeWithJavadoc", new ArrayList());
        lookupParameterOrder.put("NodeWithModifiers", new ArrayList());
        lookupParameterOrder.put("NodeWithCondition", new ArrayList());
        lookupParameterOrder.put("NodeWithParameters", new ArrayList());
        lookupParameterOrder.put("NodeWithConstructors", new ArrayList());
        lookupParameterOrder.put("NodeWithOptionalScope", new ArrayList());
        lookupParameterOrder.put("NodeWithType", new ArrayList());
        lookupParameterOrder.put("NodeWithRange", new ArrayList());
        lookupParameterOrder.put("NodeWithThrownExceptions", new ArrayList());
        lookupParameterOrder.put("NodeWithArguments", new ArrayList());
        lookupParameterOrder.put("NodeWithOptionalBlockStmt", new ArrayList());
        lookupParameterOrder.put("NodeWithTypeParameters", new ArrayList());
        lookupParameterOrder.put("NodeWithMembers", new ArrayList());
        lookupParameterOrder.put("NodeWithExtends", new ArrayList());
        lookupParameterOrder.put("NodeWithStatements", new ArrayList());
        lookupParameterOrder.put("NodeWithIdentifier", new ArrayList());
        lookupParameterOrder.put("NodeWithAnnotations", new ArrayList());
        lookupParameterOrder.put("NodeWithTypeArguments", new ArrayList());
        lookupParameterOrder.put("NodeWithBody", new ArrayList());
        lookupParameterOrder.put("NodeWithTraversableScope", new ArrayList());
        lookupParameterOrder.put("NodeWithDeclaration", new ArrayList());
        lookupParameterOrder.put("Helper", new ArrayList());
        lookupParameterOrder.put("NodeWithScope", new ArrayList());
        lookupParameterOrder.put("NodeWithExpression", new ArrayList());
        lookupParameterOrder.put("NodeWithName", new ArrayList());
        lookupParameterOrder.put("Expression", new ArrayList());
        lookupParameterOrder.put("SuperExpr", Arrays.asList("classExpr"));
        lookupParameterOrder.put("StringLiteralExpr", Arrays.asList("value"));
        lookupParameterOrder.put("LiteralStringValueExpr", Arrays.asList("value"));
        lookupParameterOrder.put("NormalAnnotationExpr", Arrays.asList("name","pairs"));
        lookupParameterOrder.put("ArrayInitializerExpr", Arrays.asList("values"));
        lookupParameterOrder.put("UnaryExpr", Arrays.asList("expression","operator"));
        lookupParameterOrder.put("AnnotationExpr", Arrays.asList("name"));
        lookupParameterOrder.put("MemberValuePair", Arrays.asList("name","value"));
        lookupParameterOrder.put("ClassExpr", Arrays.asList("type"));
        lookupParameterOrder.put("ArrayCreationExpr", Arrays.asList("elementType","levels","initializer"));
        lookupParameterOrder.put("BinaryExpr", Arrays.asList("left","right","operator"));
        lookupParameterOrder.put("FieldAccessExpr", Arrays.asList("scope","typeArguments","name"));
        lookupParameterOrder.put("SingleMemberAnnotationExpr", Arrays.asList("name","memberValue"));
        lookupParameterOrder.put("CastExpr", Arrays.asList("type","expression"));
        lookupParameterOrder.put("InstanceOfExpr", Arrays.asList("expression","type"));
        lookupParameterOrder.put("MethodReferenceExpr", Arrays.asList("scope","typeArguments","identifier"));
        lookupParameterOrder.put("NullLiteralExpr", new ArrayList());
        lookupParameterOrder.put("MarkerAnnotationExpr", Arrays.asList("name"));
        lookupParameterOrder.put("DoubleLiteralExpr", Arrays.asList("value"));
        lookupParameterOrder.put("IntegerLiteralExpr", Arrays.asList("value"));
        lookupParameterOrder.put("VariableDeclarationExpr", Arrays.asList("modifiers","annotations","variables"));
        lookupParameterOrder.put("TypeExpr", Arrays.asList("type"));
        lookupParameterOrder.put("ConditionalExpr", Arrays.asList("condition","thenExpr","elseExpr"));
        lookupParameterOrder.put("AssignExpr", Arrays.asList("target","value","operator"));
        lookupParameterOrder.put("Name", Arrays.asList("qualifier","identifier","annotations"));
        lookupParameterOrder.put("LiteralExpr", new ArrayList());
        lookupParameterOrder.put("BooleanLiteralExpr", Arrays.asList("value"));
        lookupParameterOrder.put("NameExpr", Arrays.asList("name"));
        lookupParameterOrder.put("MethodCallExpr", Arrays.asList("scope","typeArguments","name","arguments"));
        lookupParameterOrder.put("SimpleName", Arrays.asList("identifier"));
        lookupParameterOrder.put("ObjectCreationExpr", Arrays.asList("scope","type","typeArguments","arguments","anonymousClassBody"));
        lookupParameterOrder.put("LongLiteralExpr", Arrays.asList("value"));
        lookupParameterOrder.put("ArrayAccessExpr", Arrays.asList("name","index"));
        lookupParameterOrder.put("ThisExpr", Arrays.asList("classExpr"));
        lookupParameterOrder.put("LambdaExpr", Arrays.asList("parameters","body","isEnclosingParameters"));
        lookupParameterOrder.put("EnclosedExpr", Arrays.asList("inner"));
        lookupParameterOrder.put("CharLiteralExpr", Arrays.asList("value"));
    }
}
