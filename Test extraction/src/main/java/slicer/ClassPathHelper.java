package slicer;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ClassPathHelper {

    public static List<String> resolvePathAndFixMaven(List<String> paths) {
        List<String> newPaths = new ArrayList<>();
        for(String path : paths) {
            String newPath = resolvePathAndFixMaven(path);
            if(newPath != null)
                newPaths.add(newPath);
        }

        return newPaths;
    }

    public static String resolvePathAndFixMaven(String path) {
        if(path.endsWith(".src.jar"))
            return null;

        if(path.isEmpty())
            return null;

        String alternativePath = path.replace("/repository", "");
        if(new File(path).exists())
            return path;
        else if (new File(alternativePath).exists())
            return alternativePath;

        return null;
    }

}
