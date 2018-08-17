package application;

import database.MysqlConnect;
import database.MysqlSettings;
import logger.LinkerLogger;
import analysis.AnalyzeExtractor;
import analysis.SubProjectSpecification;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;


public class AddTestsToQueue {

    private static MysqlConnect mysqlConnect;


    public static void main(String[] args)  {

        LinkerLogger.logDetail("Starting");

        String searchPath, filter;
        int instance, maxInstances;
        boolean ignoreTestReport;
        if(args.length != 6) {
            LinkerLogger.logDetail("<location> <instance> <maxInstances> <ignoreTestReport> <database> <filter>");
            searchPath = "/home/eigenaar/Documents/tech-projects";
            instance = 0;
            maxInstances = 1;
            ignoreTestReport = false;
            filter = "";
            return;
        } else {
            searchPath = args[0];
            instance = Integer.parseInt(args[1]);
            maxInstances = Integer.parseInt(args[2]);
            ignoreTestReport = args[3].equals("1");
            MysqlSettings.setDatabaseUrl(args[4]);
            filter = args[5];
        }

        mysqlConnect = new MysqlConnect(
                MysqlSettings.databaseDriver,
                MysqlSettings.databaseUrl,
                MysqlSettings.username,
                MysqlSettings.password,
                MysqlSettings.maxPool
        );
        Connection connect = mysqlConnect.connect();

        LinkerLogger.logDetail(ignoreTestReport ? "Queue filled without report" : "Test report is used");


        File file = new File(searchPath);
        String[] projectDirectories = file.list((current, name) -> new File(current, name).isDirectory());

        if(projectDirectories == null) {
            LinkerLogger.logDetail("No projects in " + searchPath);
            return;
        }

        int counter = 0;
        for(String projectName : projectDirectories) {
            if(counter++ % maxInstances != instance )
                continue;

            if(!projectName.contains(filter))
                continue;

            LinkerLogger.logDetail("Working on " + projectName + "\n");
            try {
                processProject(connect, searchPath, projectName, ignoreTestReport);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static void processProject(Connection connect, String searchPath, String projectName, boolean ignoreTestReport) throws IOException {
        String projectLocation = searchPath + "/" + projectName;
        File skipWhenExist = new File(projectLocation + "/addedToQueue" + (ignoreTestReport ? "ignore" : "") + ".txt");
        int notOlderThan = 7;
        boolean fileNotOlderThanXDays = skipWhenExist.exists() && new Date(Files.readAttributes(skipWhenExist.toPath(), BasicFileAttributes.class).lastModifiedTime().toMillis()).after(new Date(System.currentTimeMillis() - notOlderThan * 24 * 60 * 60 * 1000));
        if(fileNotOlderThanXDays)
            return;

        if(ignoreTestReport && !new File(projectLocation + "/tests.txt").exists())
            return;

        int writtenToDB = 0;
        try {
            LinkerLogger.logDetail("Reading project report");
            AnalyzeExtractor analyzeExtractor = new AnalyzeExtractor(projectName, searchPath);
            List<SubProjectSpecification> subProjectSpecifications = ignoreTestReport ? analyzeExtractor.processProjectWithoutReport(projectName) : analyzeExtractor.processProject(projectName);
            LinkerLogger.logDetail("Reading project report done");
            if (subProjectSpecifications.size() == 0) {
                return;
            }

            String projectClassPath = null;
            for (SubProjectSpecification specification : subProjectSpecifications) {
                for (String test : specification.getTests()) {
                    try {
                        PreparedStatement doesNotExist = connect.prepareStatement("SELECT * FROM queue WHERE test_class = ? AND project_name = ?");
                        doesNotExist.setString(1, test);
                        doesNotExist.setString(2, specification.getProjectName());
                        ResultSet doesExistResult = doesNotExist.executeQuery();

                        if (doesExistResult.next()) {
                            LinkerLogger.logDetail("Skip " + test);
                            continue;
                        }

                        if (!ignoreTestReport && projectClassPath == null) {
                            LinkerLogger.logDetail("Fetching class path");
                            projectClassPath = getProjectClassPath(searchPath + "/" + projectName);
                            LinkerLogger.logDetail("Fetching class path done");
                        }

                        writtenToDB++;
                        LinkerLogger.logDetail("Adding test in queue");

                        PreparedStatement addTest = connect.prepareStatement("INSERT INTO `queue`(`id`, `project_name`, `test_class`, `source_path`, `class_path`, `complete`, `bussy`) VALUES " +
                                "(NULL,?,?,?,?,0,0)");
                        addTest.setString(1, specification.getProjectName());
                        addTest.setString(2, test);
                        addTest.setString(3, specification.getSourceFolder());
                        String classPath = String.join(";", specification.getClassPaths());
                        classPath = classPath.isEmpty() ? projectClassPath : projectClassPath + ";" + classPath;

                        addTest.setString(4, classPath);
                        addTest.executeUpdate();
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }
            }
        } finally {
            skipWhenExist.createNewFile();
            BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(skipWhenExist));
            bufferedWriter.write("Added to db: " + writtenToDB + ". reset was skipped because they were duplicate" );
            bufferedWriter.close();

        }
    }

    private static String getProjectClassPath(String projectPath) {
        try {
            List<Path> classPaths = getCPFiles(projectPath);
            if(classPaths.size() == 0) {
                createClassFile(projectPath);
                classPaths = getCPFiles(projectPath);
            }

            Set<String> allPaths = new HashSet<>();
            for(Path classPath : classPaths) {
                byte[] encoded = Files.readAllBytes(classPath);
                allPaths.addAll(Arrays.asList(new String(encoded, "UTF8").split(":")));
            }

            return String.join(";", allPaths);
        } catch (InterruptedException  | IOException e) {
            e.printStackTrace();
            return "";
        }
    }

    private static List<Path> getCPFiles(String projectPath) throws IOException {
        List<Path> classPaths = new ArrayList<>();
        Files.find(Paths.get(projectPath),
                Integer.MAX_VALUE,
                (filePath, fileAttr) -> fileAttr.isRegularFile() && filePath.getFileName().toString().equals("cp.txt"))
                .forEach(classPaths::add);
        return classPaths;
    }

    private static void createClassFile(String path) throws IOException, InterruptedException {
        // mvn dependency:build-classpath -Dmdep.outputFile=cp.txt
        File classPathFile = new File(path);
        String param1 = "dependency:build-classpath";
        String param2 = "-Dmdep.outputFile=cp.txt";
        String command = "mvn";
        ProcessBuilder pb = new ProcessBuilder(command, param1, param2);
        pb.directory(classPathFile);
        Process process = pb.start();
        process.waitFor();
    }
}
