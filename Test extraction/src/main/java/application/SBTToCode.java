package application;

import static application.utilities.MethodTransformation.sbtToCodeTransformation;

public class SBTToCode {

    public static void main(String[] args) {

        if(args.length != 2) {
            System.out.println("<SBT_code> <optimizations>");
            args = new String[] {"(MethodDeclaration(VoidTypeVoidType(MethodCallExpr(SimpleName_failSimpleName_fail(NullLiteralExprNullLiteralExprMethodCallExpr(EnumSet_PUBLIC_STATIC)EnumSet_PUBLIC_STATIC(SimpleName_failSimpleName_failMethodDeclaration"};
            return;
        }

        String sbtSpecification = args[0];
        boolean optimizations = args[1].equals("1");

        System.out.println(sbtToCodeTransformation(sbtSpecification, optimizations));
    }

}
