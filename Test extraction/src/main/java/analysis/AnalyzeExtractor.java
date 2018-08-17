package analysis;

import logger.LinkerLogger;

import java.io.File;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class AnalyzeExtractor {

    private String projectSearchPath;
    private String projectName;

    private Map<String, SubProjectSpecification> subProjectSpecifications = new HashMap<>();

    public AnalyzeExtractor(String projectName, String projectSearchPath) {
        this.projectSearchPath = projectSearchPath;
        this.projectName = projectName;
    }

    public List<SubProjectSpecification> processProjectWithoutReport(String projectLocation) {
        String projectRoot = projectSearchPath + "/" + projectLocation + "/";

        List<String> reportFile = ReportTools.readFile(projectRoot + "tests.txt");

//        String runningClass = null;
        for(String reportLine : reportFile) {

            String classPackage = null;
            List<String> javaFile = ReportTools.readFile(reportLine);
            for(String javaLine : javaFile) {
                if (!javaLine.startsWith("package "))
                    continue;

                Pattern packageExtract = Pattern.compile("package (.*?);");
                Matcher matcher = packageExtract.matcher(javaLine);
                if (!matcher.find()) {
                    continue;
                }

                classPackage = matcher.group(1);
                break;
            }

            if(classPackage == null) {
                LinkerLogger.logError("No package for: " + reportLine);
                continue;
            }

            String testClassName = new File(reportLine).getName().replaceFirst("[.][^.]+$", "");

            String rootSourceLocation = getSourceLocation(projectRoot, testClassName, classPackage);
            if(rootSourceLocation  == null) {
                LinkerLogger.logWarning("Cannot find source root " + projectRoot + " - " + classPackage + " - " + testClassName);
                continue;
            }
            SubProjectSpecification specification = getSpecificationForPath(rootSourceLocation);
            specification.addTest(classPackage + "." + testClassName);
        }

        return new ArrayList<>(subProjectSpecifications.values());
    }


    public List<SubProjectSpecification> processProject(String projectLocation) {
        String projectRoot = projectSearchPath + "/" + projectLocation + "/";

        List<String> reportFile = ReportTools.readFile(projectRoot + "report.txt");

        String runningClass = null;
        for(String reportLine : reportFile ) {
            if(reportLine.contains("Running")) {
                runningClass = ReportTools.getRunningClass(reportLine);
                continue;
            }

            String fullTestClassName = ReportTools.getSucceedingTestClass(reportLine, runningClass);
            runningClass = null;
            if(fullTestClassName == null)
                continue;

            String testClassName = ReportTools.getClassName(fullTestClassName);
            String classPackage = ReportTools.getClassPackageName(fullTestClassName);

            String rootSourceLocation = getSourceLocation(projectRoot, testClassName, classPackage);
            if(rootSourceLocation  == null) {
                LinkerLogger.logWarning("Cannot find source root " + projectRoot + " - " + classPackage + " - " + testClassName);
                continue;
            }

            List<String> classRoot = getClassRoot(projectRoot, testClassName, classPackage);
            if(classRoot.size() == 0) {
                LinkerLogger.logWarning("Cannot find class root " + projectRoot + " - " + classPackage);
                continue;
            }

            List<String> jarFiles = getJarFiles(projectRoot);

            SubProjectSpecification specification = getSpecificationForPath(rootSourceLocation);
            specification.addClassPaths(classRoot);
            specification.addClassPaths(jarFiles);
            specification.addTest(fullTestClassName);
        }

        return new ArrayList<>(subProjectSpecifications.values());
    }

    private List<String> getJarFiles(String classRoot) {
        List<String> javaFileLocations = ReportTools.findFile(classRoot, "", "src/jar");
        return javaFileLocations;
    }

    private List<String> getClassPaths(String classRoot) {
        List<String> output = new ArrayList<>();
        String[] projectDirectories = new File(classRoot).list((current, name) -> new File(current, name).isDirectory());
        for(String classFolder : projectDirectories) {
            output.add(classRoot + "/" + classFolder);
        }

        return output;
    }

    private SubProjectSpecification getSpecificationForPath(String rootSourceLocation) {
        SubProjectSpecification find = subProjectSpecifications.get(rootSourceLocation);
        if(find != null)
            return find;

        SubProjectSpecification specification = new SubProjectSpecification(this.projectName, rootSourceLocation);
        subProjectSpecifications.put(rootSourceLocation, specification);
        return specification;
    }

    private String getSourceLocation(String projectPath, String testClassName, String classPackage) {
        List<String> sourceLocations = ReportTools.getSourceLocations(projectPath, testClassName, classPackage, "java");
        if(sourceLocations.size() != 1)
            return null;
        return ReportTools.trimAfterFolder(sourceLocations.get(0));
    }

    private List<String> getClassRoot(String projectPath, String testClassName, String classPackage) {
        List<String> classLocations = ReportTools.getSourceLocations(projectPath, testClassName, classPackage, "class");

        Set<String> returnList = new HashSet<>();
        for (String classLocation : classLocations) {
            if (classLocation == null)
                continue;
            String rootFolder = ReportTools.getRootFolder(classLocation);

            File[] files = new File(rootFolder).listFiles(File::isDirectory);
            if(files == null)
                continue;
            for( File folder : files) {
                returnList.add(folder.getAbsolutePath());
            }
        }

        return new ArrayList<>(returnList);
    }


}
