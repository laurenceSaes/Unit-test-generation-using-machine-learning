package application;

import tokenize.compression.TokenCompression;
import application.fileHelper.FileHelper;
import logger.LinkerLogger;
import tokenize.Dictionary;

import java.io.IOException;

public class ReadMultiTokens {



    public static void main(String[] args) throws IOException {

        if(args.length < 2) {
            LinkerLogger.logDetail("<files> <dictionary> <to std out> <ech predict - exptected>");
            args = new String[] {"transTest.txt", "compress.dict", "1", "1"};
            return;
        }

        boolean toStdOut = (args.length >= 3 && args[2].equals("1"));
        boolean firstPredictThanWhatItShouldBe = (args.length >= 4 && args[3].equals("1"));

        String tokenDictionaryFileLocation = args[1];
        Dictionary dictionary = new Dictionary(tokenDictionaryFileLocation);

        String[] decodeLocations = args[0].split(";");

        int counter = 0;
        for(String decodeLocation : decodeLocations) {

            String tokenInput = FileHelper.convertFile(decodeLocation, input -> input);
            String decompressed = TokenCompression.decompress(tokenInput, dictionary);


            if(firstPredictThanWhatItShouldBe) {
                String[] splits = decompressed.split("\n");
                StringBuilder newDecode = new StringBuilder();
                for(String split : splits) {
                    if (counter++ % 2 == 0)
                        split = "Predict: " + split + "\n";
                    else
                        split = "Exptected: " + split + "\n\n";
                    newDecode.append(split);
                }

                decompressed = newDecode.toString();
            }

            if(toStdOut)
                System.out.println(decompressed);
            else
                FileHelper.writeOutput(decodeLocation + ".txt", decompressed);
        }
    }
}
