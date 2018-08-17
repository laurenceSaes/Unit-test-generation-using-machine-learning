package cache;

import parser.JavaSourceMapper;

import java.io.IOException;

public class ParserCache extends BaseCache<JavaSourceMapper> {

    public ParserCache() {
        super(JavaSourceMapper.class);
    }

    public JavaSourceMapper getCachedParser(String projectLocation) throws IOException {
        return this.getCachedItem(projectLocation);
    }

    public JavaSourceMapper getParser(String projectLocation) throws IOException {
        JavaSourceMapper javaSourceMapper = this.getCachedParser(projectLocation);
        if(javaSourceMapper != null) {
            return javaSourceMapper;
        }

        String storeLocationName = getCacheStoreLocation(projectLocation);
        javaSourceMapper = new JavaSourceMapper(projectLocation);

        this.writeToCache(storeLocationName, javaSourceMapper);

        return javaSourceMapper;
    }

}
