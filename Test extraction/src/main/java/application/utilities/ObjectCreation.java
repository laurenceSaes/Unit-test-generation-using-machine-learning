package application.utilities;

import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.type.PrimitiveType;
import logger.LinkerLogger;
import sun.awt.image.ImageWatched;

import javax.management.ReflectionException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.util.*;

import static parser.StructureBasedTraversal.undoEscape;

public class ObjectCreation {

    private static boolean optimizations = false;

    public static void setOptimizations(boolean optimizations) {
        ObjectCreation.optimizations = optimizations;
    }

    public static Object createObjectFromSBTString(String sbtName, List<Object> objectParameters) {

        List<String> split = splitNameParameter(sbtName);
        if (split.isEmpty())
            throw new IllegalArgumentException("The sbt name cannot be empty");

        String elementName = split.get(0).trim();
        List<String> stringParameters = split.subList(1, split.size());
        return createObj(elementName, objectParameters, stringParameters);
    }

    private static List<String> splitNameParameter(String sbtName) {
        List<String> output = new ArrayList<>();
        int lastSplit = 0;
        for (int i = 0; i < sbtName.length(); i++) {
            //Do not split when the split character is escaped
            if (sbtName.charAt(i) == '_' && ((i == 0) || sbtName.charAt(i - 1) != '\\')) {
                output.add(undoEscape(sbtName.substring(lastSplit, i)));
                lastSplit = i + 1;
            }
        }

        output.add(undoEscape(sbtName.substring(lastSplit)));

        return output;
    }


    private static Object createObj(String nameClass, List<Object> objectParameters, List<String> stringParameters) {

        Iterator<Object> objectIterator = objectParameters.iterator();
        Iterator<String> stringIterator = stringParameters.iterator();

        //Optimizer will break the algorithm. Fix object creation for shortcuts
        Object noneNodeObject = processNoneNodeTypes(nameClass.trim(), objectParameters, stringParameters);
        if (noneNodeObject != null)
            return noneNodeObject;

        if(optimizations) {
            Object optimizedNode = optimizationForNodeTypes(nameClass, objectParameters, stringParameters);
            if(optimizedNode != null)
                return optimizedNode;
        }

        if (nameClass.equals("null")) {
            return null;
        }

        if(nameClass.equals("List")) {
            List<Object> list = new ArrayList<>();
            list.addAll(objectParameters);
            list.addAll(stringParameters);
            return list;
        }

        List<String> classPaths = new ArrayList<>(
                Arrays.asList("com.github.javaparser.ast",
                        "com.github.javaparser.ast.body",
                        "com.github.javaparser.ast.comments",
                        "com.github.javaparser.ast.expr",
                        "com.github.javaparser.ast.modules",
                        "com.github.javaparser.ast.nodeTypes",
                        "com.github.javaparser.ast.nodeTypes.modifiers",
                        "com.github.javaparser.ast.observer",
                        "com.github.javaparser.ast.stmt",
                        "com.github.javaparser.ast.type",
                        "com.github.javaparser.ast.validator",
                        "com.github.javaparser.ast.validator.chunks",
                        "com.github.javaparser.ast.visitor")
        );

        for (String classPath : classPaths) {

            Class<?> clazz;
            try {
                clazz = Class.forName(classPath + "." + nameClass.trim());
            } catch (ClassNotFoundException ignored) {
                continue;
            }

            Constructor constructorToUse = findAllFieldConstructor(clazz);

            //We cannot find constructors for enums
            if (constructorToUse == null && stringParameters.size() == 1) {
                Object constItem = resolveAsEnum(stringParameters, clazz);
                if (constItem != null)
                    return constItem;
            }

            //NodeList does not have an all field constructor
            if (constructorToUse == null && nameClass.equals("NodeList")) {
                if (objectParameters.size() == 0 && stringParameters.size() == 0)
                    return new NodeList<>();
                constructorToUse = clazz.getConstructors()[2];
            }

            if (constructorToUse == null) {
                LinkerLogger.logError("Cannot find constructor for " + nameClass);
                return null;
            }

            Class[] parameterTypes = constructorToUse.getParameterTypes();
            Object[] parameters = new Object[parameterTypes.length];
            int counter = 0;
            for (Class parameterType : parameterTypes) {

                parameters[counter] = parameterType.equals(String.class) ? stringIterator.next() : objectIterator.next();

                if (parameters[counter] != null) {
                    String parameterSimpleName = parameterType.getSimpleName();
                    boolean typeMismatch = !parameterType.isInstance(parameters[counter]);
                    boolean preventNameErrors = !parameterSimpleName.toLowerCase().equals(parameters[counter].getClass().getSimpleName().toLowerCase());
                    if (typeMismatch && preventNameErrors) {
                        Object newParam = restoreOptimizedNode(parameterSimpleName, parameters[counter]);
                        if (newParam != null)
                            parameters[counter] = newParam;
                    }
                }

                counter++;
            }

            try {
                return constructorToUse.newInstance(parameters);
            } catch (Exception ex) {
                LinkerLogger.logError("Cannot create instance of " + nameClass + " " + ex.getMessage());
            }

        }

        return null;
    }


    private static Object restoreOptimizedNode(String expectedClass, Object parameter) {

        //Nodelist optimization
        if (expectedClass.equals("NodeList")) {
            return new NodeList((ArrayList) parameter);
        }

        //Block statement optimization
        if ((expectedClass.equals("Statement") || expectedClass.equals("BlockStmt")) && parameter.getClass().getSimpleName().equals("NodeList")) {
            return new BlockStmt((NodeList<Statement>) parameter);
        }

        //Name expression optimalization
        if (expectedClass.equals("SimpleName") && parameter.getClass().getSimpleName().equals("NameExpr")) {
            return ((NameExpr) parameter).getName();
        }

        if (expectedClass.equals("NameExpr") && parameter.getClass().getSimpleName().equals("String")) {
            return new NameExpr(new SimpleName((String)parameter));
        }

        return null;
    }


    private static Object optimizationForNodeTypes(String expectedClass, List<Object> objectIterator, List<String> stringIterator) {
        if (expectedClass.equals("NodeList")) {
            NodeList nodeList = new NodeList();
            for (Object next : objectIterator) {
                if (next instanceof NodeList) {
                    nodeList.add(new BlockStmt((NodeList) next));
                } else {
                    nodeList.add((Node) next);
                }
            }
            return nodeList;
        }

        if (expectedClass.equals("BlockStmt") && !objectIterator.isEmpty()) {
            return restoreOptimizedNode(BlockStmt.class.getSimpleName(), objectIterator.get(0));
        }
//
//        if (expectedClass.equals("ExpressionStmt")) {
//            return restoreOptimizedNode(ExpressionStmt.class.getSimpleName(), objectIterator.next());
//        }
//
        if (expectedClass.equals("SimpleName")  && objectIterator.isEmpty() && !stringIterator.isEmpty()) {
            return restoreOptimizedNode(NameExpr.class.getSimpleName(), stringIterator.get(0));
        }

        return null;
    }

    private static Object processNoneNodeTypes(String className, List<Object> objectIterator, List<String> stringIterator) {

        if (className.equals("Boolean") && !stringIterator.isEmpty()) {
            return new Boolean(Boolean.parseBoolean(stringIterator.get(0)));
        }

        if (className.equals("Integer") && !stringIterator.isEmpty()) {
            return new Integer(Integer.parseInt(stringIterator.get(0)));
        }

        if (className.equals("Primitive") && !stringIterator.isEmpty()) {
            return new PrimitiveType(PrimitiveType.Primitive.valueOf(stringIterator.get(0))).getType();
        }

        if(className.endsWith("Operator")) {
            String enumClass = className.substring(0, className.indexOf("$"));

            if(enumClass.equals("BinaryExpr") && !stringIterator.isEmpty())
                return BinaryExpr.Operator.valueOf(stringIterator.get(0));

            if(enumClass.equals("AssignExpr") && !stringIterator.isEmpty())
                return AssignExpr.Operator.valueOf(stringIterator.get(0));

            if(enumClass.equals("UnaryExpr") && !stringIterator.isEmpty())
                return UnaryExpr.Operator.valueOf(stringIterator.get(0));
        }

        if (className.equals("ArrayList")) {
            List list = new ArrayList();
            list.addAll(objectIterator);
            list.addAll(stringIterator);
            return list;
        }

        if (className.equals("EnumSet")) {
            EnumSet<Modifier> modifiers = EnumSet.noneOf(Modifier.class);
            for (String modifier : stringIterator) {
                modifiers.add(Modifier.valueOf(modifier));
            }
            return modifiers;
        }

        return null;
    }

    private static Object resolveAsEnum(List<String> stringParameters, Class<?> clazz) {
        String constName = stringParameters.get(0).trim();
        Object[] constItems = clazz.getEnumConstants();
        for (Object constItem : constItems) {
            String constString = constItem.toString().toLowerCase();
            if (constString.equals(constName.toLowerCase()))
                return constItem;
        }
        return null;
    }

    public static Constructor findAllFieldConstructor(Class<?> clazz) {
        Constructor[] constructors = clazz.getConstructors();
        for (Constructor constructor : constructors) {
            Annotation[] annotations = constructor.getAnnotations();
            if (annotations.length != 0 && annotations[0].toString().contains("ast.AllFieldsConstructor()"))
                return constructor;
        }
        return null;
    }
}
