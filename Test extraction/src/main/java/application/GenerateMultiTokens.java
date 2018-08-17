package application;

import tokenize.compression.TokenCompression;
import application.fileHelper.FileHelper;
import logger.LinkerLogger;
import tokenize.Dictionary;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GenerateMultiTokens {

    public static void main(String[] args) throws IOException {

        if(args.length != 3) {
            LinkerLogger.logDetail("<files> <dictionary> <src.tokenize.compression-times>");
            args = new String[] {"src-train.txt", "test", "10"};
            return;
        }

        String tokenDictionaryLocation = args[1];
        TokenCompression.clearDictionary(tokenDictionaryLocation);

        String[] itemsToCovert = args[0].split(";");

        int compressAmount = Integer.parseInt(args[2]);
        Dictionary dictionary = new Dictionary(tokenDictionaryLocation);
        for (String fileName : itemsToCovert) {
            try {
                List<Integer> allTokens = new ArrayList<>();
                FileHelper.actionOnFile(fileName, input -> {
                    List<Integer> newTokens = GenerateMultiTokens.createTokenList(input, dictionary);
                    newTokens.add(-1); // \n!
                    allTokens.addAll(newTokens);
                });

                String tokenOutput = TokenCompression.tokenStreamToString(TokenCompression.compress(compressAmount, allTokens, dictionary));
                FileHelper.writeOutput(fileName + ".multi.token", tokenOutput);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        dictionary.export();
        TokenCompression.storeDictionaryLookupTable(dictionary, tokenDictionaryLocation + ".tab");
        TokenCompression.storeListOfAllTokens(dictionary, tokenDictionaryLocation + ".vocab");

    }


    public static List<Integer> createTokenList(String line, Dictionary dictionary) {
        if(line == null || line.isEmpty())
            return new ArrayList<>();

        Pattern extractPattern = Pattern.compile("([A-Z]?[a-z]+|([A-Z]+)(?=[A-Z][a-z])|[A-Z]+|.)");
        Matcher matcher = extractPattern.matcher(line);

        List<Integer> output = new ArrayList<>();
        while (matcher.find()) {
            String found = matcher.group(1);
            output.add(dictionary.addWord(found));
        }
        return output;
    }
}
