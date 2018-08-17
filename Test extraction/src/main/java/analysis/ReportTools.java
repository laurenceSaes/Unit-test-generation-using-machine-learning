package analysis;

import logger.LinkerLogger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class ReportTools {

    private static final Pattern matchTestWithIn = Pattern.compile("Tests run:.*?([0-9]*),.*?Failures:.*?([0-9]*),.*?Errors:.*?([0-9]*),.*?Skipped:.*?([0-9]*),.*?in (.*)");
    private static final Pattern matchTest = Pattern.compile("Tests run:.*?([0-9]*),.*?Failures:.*?([0-9]*),.*?Errors:.*?([0-9]*),.*?Skipped:.*?([0-9]*),");
    private static final Pattern matchRunningClass = Pattern.compile("Running (.*)");


    public static String getRootFolder(String location) {
        String trimmed = location.substring(0, location.length() - 1);
        String linuxLike = trimmed.replace("\\", "/");
        int folderEnd = linuxLike.lastIndexOf("/");
        return location.substring(0, folderEnd + 1);
    }

    public static String trimAfterFolder(String sourceLocation) {
        String folder = "src";
        int itemLocation = sourceLocation.replace("\\","/").indexOf("/" + folder + "/");
        if(itemLocation == -1)
            return null;
        return sourceLocation.substring(0, itemLocation + folder.length() + 2);
    }

    public static List<String> getSourceLocations(String projectPath, String testClassName, String getClassPackage, String extension) {
        List<String> javaFileLocations = findFile(projectPath, testClassName, extension);

        List<String> returnList = new ArrayList<>();
        for(String fileLocation : javaFileLocations) {
            String classLikePath = fileLocation.replaceAll("[\\/]", ".");
            int findPackageInDirectories;

            if(getClassPackage.isEmpty())
                findPackageInDirectories = classLikePath.lastIndexOf(((testClassName != null) ? testClassName : "") + "." + extension);
            else
                findPackageInDirectories = classLikePath.lastIndexOf(getClassPackage);

            if (findPackageInDirectories == -1)
                continue;

            returnList.add(fileLocation.substring(0, findPackageInDirectories));
        }

        return returnList;
    }

    public static List<String> findFile(String searchPath, String fileName, String extension) {

        try {
            List<String> found = new ArrayList<>();
            Files.find(Paths.get(searchPath),
                    Integer.MAX_VALUE,
                    (filePath, fileAttr) -> {
                        String end = (fileName != null ? fileName : "") + "." + extension;
                        boolean endWith = filePath.toString().replace("\\","/").toLowerCase().endsWith("/" + end.toLowerCase());
                        return fileAttr.isRegularFile() && endWith;
                    })
                    .forEach(path -> found.add(path.toAbsolutePath().toString()));
            return found;
        } catch (IOException e) {
            return new ArrayList<>();
        }

    }

    public static String getClassPackageName(String fullTestClassName) {
        int lastDot = fullTestClassName.lastIndexOf('.');
        if(lastDot == -1)
            return "";
        return fullTestClassName.substring(0, lastDot).trim();
    }

    public static String getClassName(String fullTestClassName) {
        return fullTestClassName.substring(fullTestClassName.lastIndexOf('.') + 1).trim();
    }

    public static String getSucceedingTestClass(String reportLine, String runningClass) {

        //Early out to speedup regex
        if(!reportLine.contains("Tests run:"))
            return null;

        Matcher matches;
        boolean hasIn = false;
        if(runningClass == null) {
            matches = getTestMatch(reportLine, matchTestWithIn);
            hasIn = true;
        } else {
            matches = getTestMatch(reportLine, matchTest);
        }

        if (matches == null) {
            if(runningClass != null)
                LinkerLogger.logError("Cannot get tests amount from line: " + reportLine + " runningClass: " + runningClass);
            return null;
        }

        int tests = Integer.parseInt(matches.group(1));
        int failures = Integer.parseInt(matches.group(2));
        int errors = Integer.parseInt(matches.group(3));
        int skipped = Integer.parseInt(matches.group(4));
        if(tests == 0 || failures > 0 || errors > 0 )
            return null;

        return hasIn ? matches.group(5) : runningClass;
    }

    private static Matcher getTestMatch(String reportLine, Pattern pattern) {
        Matcher matches;
        String reportLineClean = cleanLogString(reportLine);
        matches = pattern.matcher(reportLineClean);
        if (!matches.find()) {
            return null;
        }
        return matches;
    }

    private static String cleanLogString(String reportLine) {
        String reportLineClean = reportLine.replaceAll("[^\\x00-\\x7F]", "");
        reportLineClean = reportLineClean.replace("\u001B[m", "").replace("\u001B[1m", "").replace("\u001B[m", "").replace("\u001B[0;1;32m", "").replace("^[[0;1;32m", "").replace("^[[m", "").replace("^[[1m", "");
        return reportLineClean;
    }

    public static List<String> readFile(String location) {
        try {
            return Files.readAllLines(Paths.get(location), StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public static void readFile(String location, Consumer<String> callback) {
        try (Stream<String> stream = Files.lines(Paths.get(location))) {
            stream.forEach(callback);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String getRunningClass(String reportLine) {
        if(!reportLine.contains("Running"))
            return null;

        Matcher matches;
        String reportLineClean = cleanLogString(reportLine);
        matches = matchRunningClass.matcher(reportLineClean);
        if (!matches.find()) {
            return null;
        }
        return matches.group(1);
    }
}
