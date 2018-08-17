package analysis;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SubProjectSpecification {
    public String projectName;
    public String sourceFolder;
    public Set classPaths = new HashSet();
    public List<String> tests = new ArrayList<>();

    public SubProjectSpecification(String projectName, String sourceFolder) {
        this.projectName = projectName;
        this.sourceFolder = sourceFolder;
    }

    public void addTest(String test) {
        tests.add(test);
    }

    public void addClassPaths(List<String> paths) {
        classPaths.addAll(paths);
    }

    public String getProjectName() {
        return projectName;
    }

    public String getSourceFolder() {
        return sourceFolder;
    }

    public ArrayList<String> getClassPaths() {
        return new ArrayList<String>(classPaths);
    }

    public List<String> getTests() {
        return tests;
    }
}
