package application;

import tokenize.compression.BpeBuilder;
import tokenize.compression.TokenCompression;
import application.fileHelper.FileHelper;
import logger.LinkerLogger;
import tokenize.Dictionary;

import java.io.IOException;
import java.util.HashSet;

public class ConvertToBPETokens {
    private static Dictionary dictionary;


    public static void main(String[] args) throws IOException {

        if(args.length != 2) {
            LinkerLogger.logDetail("<files> <dictionary>");
            args = new String[] {"src-train.txt.multi.token", "test"};
            //return;
        }

        String tokenDictionaryLocation = args[1];
        dictionary = new Dictionary(tokenDictionaryLocation);

        if(dictionary.getGroups() > 1) {
            LinkerLogger.logDetail("Compression not supported");
            return;
        }

        String[] decodeLocations = args[0].split(";");

        for(String decodeLocation : decodeLocations) {
            String tokenInput = FileHelper.convertFile(decodeLocation, input -> input);
            BpeBuilder decompressed = TokenCompression.decompressBPE(tokenInput, "@@", dictionary, 0);
            FileHelper.writeOutput(decodeLocation + ".bpe", decompressed.getBPEString().toString());

            HashSet<String> vocab = decompressed.getVocab();
            StringBuilder vocabBuilder = new StringBuilder();
            for(String vocabItem : vocab) {
                vocabBuilder.append(vocabItem).append("\n");
            }
            FileHelper.writeOutput(tokenDictionaryLocation + ".bpe", vocabBuilder.toString());
        }
    }
}
