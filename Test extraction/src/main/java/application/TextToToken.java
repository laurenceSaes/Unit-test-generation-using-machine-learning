package application;

import tokenize.compression.TokenCompression;
import logger.LinkerLogger;
import tokenize.Dictionary;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TextToToken {

    public static void main(String[] args) throws IOException {

        if (args.length != 1) {
            LinkerLogger.logDetail("<dictionary>");
            args = new String[]{"test"};
            return;
        }

        String tokenDictionaryLocation = args[0];
        Dictionary dictionary = new Dictionary(tokenDictionaryLocation);

        Scanner input = new Scanner(System.in);
        input.useDelimiter(System.getProperty("line.separator"));

        while (input.hasNext()) {
            String code = input.next();
            List<Integer> tokens = createTokenList(code, dictionary);
            String tokenOutput = TokenCompression.tokenStreamToString(tokens);
            System.out.println(tokenOutput);
        }
    }

    private static List<Integer> createTokenList(String line, Dictionary dictionary) {
        if(line == null || line.isEmpty())
            return new ArrayList<>();

        Pattern extractPattern = Pattern.compile("([A-Z]?[a-z]+|([A-Z]+)(?=[A-Z][a-z])|[A-Z]+|.)");
        Matcher matcher = extractPattern.matcher(line);

        List<Integer> output = new ArrayList<>();
        while (matcher.find()) {
            String found = matcher.group(1);
            output.add(dictionary.looupWord(dictionary.getGroups() - 1, found));
        }
        return output;
    }
}
