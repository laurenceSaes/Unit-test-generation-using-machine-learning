package cache;

import com.ibm.wala.ipa.callgraph.CallGraphBuilderCancelException;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import slicer.ClassAnalyzer;

import java.io.IOException;
import java.util.List;

public class ClassAnalyzerCache extends BaseCache<ClassAnalyzer> {
    public ClassAnalyzerCache() {
        super(ClassAnalyzer.class);
    }

    public ClassAnalyzer getCachedClassAnalyzer(List<String> appJar, List<String> mainMethods) throws IOException {
        String storeLocationName = getCacheStoreLocation(String.join(",", appJar) + "|" + String.join(",", mainMethods));
        return this.getCachedItem(storeLocationName);
    }

    public ClassAnalyzer getClassAnalyzer(List<String> appJar, List<String> mainMethods) throws IOException, ClassHierarchyException, CallGraphBuilderCancelException, NullPointerException {
        ClassAnalyzer cachedUnit = this.getCachedClassAnalyzer(appJar, mainMethods);
        if(cachedUnit != null) {
            return cachedUnit;
        }

        String storeLocationName = getCacheStoreLocation(String.join(",", appJar) + "|" + String.join(",", mainMethods));
        ClassAnalyzer classAnalyzer = new ClassAnalyzer(appJar, mainMethods);

        this.writeToCache(storeLocationName, classAnalyzer);

        return classAnalyzer;
    }
}
