package application.fileHelper;

import java.io.*;

public class FileHelper {

    public static void writeOutput(String fileName, String tokenOutput) throws IOException {
        FileWriter f2 = new FileWriter(new File(fileName), false);
        f2.write(tokenOutput);
        f2.close();
    }

    public static void actionOnFile(String fileName, IAction action) throws IOException {
        File file = new File(fileName);
        FileReader fileReader = new FileReader(file);
        BufferedReader bufferedReader = new BufferedReader(fileReader);
        String line;
        while ((line = bufferedReader.readLine()) != null) {
            action.action(line);

        }
        fileReader.close();
    }

    public static String convertFile(String fileName, IConvert converter) throws IOException {
        File file = new File(fileName);
        FileReader fileReader = new FileReader(file);
        BufferedReader bufferedReader = new BufferedReader(fileReader);
        StringBuffer stringBuffer = new StringBuffer();
        String line;
        while ((line = bufferedReader.readLine()) != null) {
            String convertedLine = converter.convert(line);
            stringBuffer.append(convertedLine);
            stringBuffer.append(" \n ");
        }
        fileReader.close();
        return stringBuffer.toString();
    }
}
