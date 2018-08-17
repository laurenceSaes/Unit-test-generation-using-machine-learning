package application;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import logger.LinkerLogger;

import java.io.FileNotFoundException;
import java.util.Scanner;

public class isParsable {


    public static void main(String[] args) throws FileNotFoundException {

        LinkerLogger.disable();

        if (args.length < 1) {
            LinkerLogger.logDetail("<log per line>");
            args = new String[]{"1"};
            return;
        }

        boolean logperLine = args[0].equals("1");
        int parsable = 0, notParsable = 0;

        Scanner input = new Scanner(System.in);
        input.useDelimiter(System.getProperty("line.separator"));

        while (input.hasNext()) {
            String code = input.next();
            String methodWithClass = "class tmp { " + code + "}";
            CompilationUnit parse = null;
            try {
                parse = JavaParser.parse(methodWithClass);
            } catch (Exception ignored) {

            }

            if(logperLine)
                System.out.println((parse != null ? "1" : "0"));

            if (parse != null) {
                ++parsable;
            } else {
                ++notParsable;
            }
        }

        if(!logperLine)
            System.out.println("parsable: " + parsable + "\nnotParsable: " + notParsable);
    }

}
