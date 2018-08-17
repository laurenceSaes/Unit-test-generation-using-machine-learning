package tokenize.compression;

import application.fileHelper.FileHelper;
import tokenize.Dictionary;

import java.io.*;
import java.util.*;

public class TokenCompression {


    public static void clearDictionary(String fileLocation) throws FileNotFoundException {
        PrintWriter writer = new PrintWriter(fileLocation);
        writer.print("");
        writer.close();
    }

    public static List<Integer> compress(int compressionTimes, List<Integer> tokens, Dictionary dictionary) {

        for (int compressStep = 1; compressStep <= compressionTimes; compressStep++) {
            Map<Map.Entry<Integer, Integer>, Integer> wordCounts = countTokenCombinations(tokens);

            //Add all (none compressed) tokens to the dictionary
            Map<Integer, Integer> singleTokenLookup = dictionary.addMany(compressStep, tokens);

            //Compressed element dictionary
            Map<String, Integer> compressedTokenLookup = new HashMap<>();
            for (Map.Entry<Map.Entry<Integer, Integer>, Integer> entry : wordCounts.entrySet()) {
                if (entry.getValue() < 2)
                    continue;
                String word = entry.getKey().getKey() + " " + entry.getKey().getValue();
                int index = dictionary.addWord(compressStep, word, compressedTokenLookup);
                compressedTokenLookup.put(word, index);
            }

            //Create new token list. Every combinations = covnerted when in dictionary. Otherwise the first token is added in the stream
            List<Integer> compressedTokens = new ArrayList<>();
            for (int tokenIndex = 0; tokenIndex < tokens.size(); tokenIndex++) {

                Integer currentToken = tokens.get(tokenIndex);

                //-1 indicates that the next tokens in another training example
                if (currentToken == -1) {
                    compressedTokens.add(-1);
                    continue;
                }

                //Get the next token for the compressed string
                boolean nextIsOutOfBound = tokenIndex + 1 >= tokens.size();
                String combinedTokens = !nextIsOutOfBound ? currentToken + " " + tokens.get(tokenIndex + 1) : null;
                Integer index = compressedTokenLookup.get(combinedTokens);
                boolean combinedTokenExists = !nextIsOutOfBound && index != null;
                if (combinedTokenExists)
                    tokenIndex++;
                else
                    index = singleTokenLookup.get(currentToken);

                compressedTokens.add(index);
            }

            //The compressed tokens are the input for the next iteration
            tokens = compressedTokens;
        }

        return tokens;
    }

    public static String tokenStreamToString(List<Integer> tokens) {
        StringBuilder output = new StringBuilder();
        boolean spaceSkip = true;
        for (Integer token : tokens) {
            if (token == -1) {
                spaceSkip = true;
                output.append("\n");
                continue;
            }
            output.append(!spaceSkip ? " " + token.toString() : token.toString());
            spaceSkip = false;
        }

        return output.toString().trim();
    }


    public static String decompress(String tokenInput, Dictionary dictionary) {
        List<String> tokens = new ArrayList<>(Arrays.asList(tokenInput.split(" ")));

        List<String> decompress = new ArrayList<>(tokens);
        int dictionaryGroups = dictionary.getGroups();
        for (int compressIndex = dictionaryGroups - 1; compressIndex >= 0; compressIndex--) {
            List<String> decompressBuffer = new ArrayList<>();

            for (String item : decompress) {
                if (item.equals("\n"))
                    decompressBuffer.add("\n");
                else {
                    if (item.isEmpty())
                        continue;
                    String word = dictionary.getWord(compressIndex, Integer.parseInt(item));
                    //Index 0 = text. Tokens are not split inside the dictionary anymore on spaces
                    String[] compressIndexSplit = compressIndex != 0 ? word.split(" ") : new String[]{word};
                    decompressBuffer.addAll(Arrays.asList(compressIndexSplit));
                }
            }
            decompress = decompressBuffer;
        }

        return String.join("", decompress);
    }

    public static void storeListOfAllTokens(Dictionary dictionary, String outputLocation) throws IOException {
        int groups = dictionary.getGroups();
        if (groups == 0)
            return;

        int lastDictIndex = groups - 1;
        List<String> lastDictionary = dictionary.getAllWords(lastDictIndex);
        StringBuilder output = new StringBuilder();
        for (int wordIndex = 0; wordIndex < lastDictionary.size(); wordIndex++) {
            output.append(wordIndex);
            output.append("\n");
        }

        FileHelper.writeOutput(outputLocation, output.toString());
    }

    public static void storeDictionaryLookupTable(Dictionary dictionary, String outputLocation) throws IOException {
        int groups = dictionary.getGroups();
        if (groups == 0)
            return;

        int lastDictIndex = groups - 1;
        List<String> lastDictionary = dictionary.getAllWords(lastDictIndex);
        StringBuilder output = new StringBuilder();
        for (int wordIndex = 0; wordIndex < lastDictionary.size(); wordIndex++) {

            //Lookup every word
            String tokenToLookup = dictionary.getWord(lastDictIndex, wordIndex);
            String word = String.join("", recursiveTokenLookup(tokenToLookup, dictionary, lastDictIndex - 1));

            output.append(word);
            output.append("\t");
            output.append(wordIndex);
            output.append("\n");
        }

        FileHelper.writeOutput(outputLocation, output.toString());
    }


    public static BpeBuilder decompressBPE(String tokenInput, String split, Dictionary dictionary, int useDictionaryGroup) {
        BpeBuilder outputBuffer = new BpeBuilder();

        List<String> decompress = new ArrayList<>(Arrays.asList(tokenInput.split(" ")));
        for (int tokenIndex = 0; tokenIndex < decompress.size(); tokenIndex++) {

            String item = decompress.get(tokenIndex);
            if (item.equals("\n")) {
                outputBuffer.addNewLine();
                continue;
            }

            String itemWord = dictionary.getWord(useDictionaryGroup, Integer.parseInt(item));
            if (itemWord.equals(" ")) {
                continue;
            }

            String tokenToAdd;

            String nextItem = tokenIndex + 1 < decompress.size() ? decompress.get(tokenIndex + 1) : null;
            if (nextItem != null && isNoneSpace(item, dictionary, useDictionaryGroup) && isNoneSpace(nextItem, dictionary, useDictionaryGroup)) {
                tokenToAdd = itemWord + split;
            } else {
                tokenToAdd = itemWord;
            }

            outputBuffer.addToken(tokenToAdd);
        }

        return outputBuffer;
    }

    private static boolean isNoneSpace(String wordIndex, Dictionary dictionary, int useDictionaryGroup) {
        if (wordIndex.equals("\n"))
            return false;
        return !dictionary.getWord(useDictionaryGroup, Integer.parseInt(wordIndex)).equals(" ");
    }

    private static List<String> recursiveTokenLookup(String tokenToLookup, Dictionary dictionary, int dictionaryIndex) {
        if (dictionaryIndex < 0)
            return new ArrayList<>(Collections.singletonList(tokenToLookup));

        List<String> lookup = new ArrayList<>();
        for (String token : tokenToLookup.split(" ")) {
            String nextWord = dictionary.getWord(dictionaryIndex, Integer.parseInt(token));

            if (dictionaryIndex > 0)
                lookup.addAll(recursiveTokenLookup(nextWord, dictionary, dictionaryIndex - 1));
            else
                lookup.add(nextWord);
        }

        return lookup;
    }

    private static Map<Map.Entry<Integer, Integer>, Integer> countTokenCombinations(List<Integer> words) {
        Map<Map.Entry<Integer, Integer>, Integer> wordCounts = new HashMap<>();
        for (int i = 0; i + 1 < words.size(); i++) {
            Integer firstWord = words.get(i);
            Integer secondWord = words.get(i + 1);
            if (secondWord == -1 || firstWord == -1) {
                continue;
            }
            AbstractMap.SimpleEntry<Integer, Integer> combination = new AbstractMap.SimpleEntry<>(firstWord, secondWord);
            Integer count = wordCounts.get(combination);
            if (count == null) {
                count = 0;
            }
            wordCounts.put(combination, count + 1);
        }
        return wordCounts;
    }

}
