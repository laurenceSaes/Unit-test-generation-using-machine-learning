package cache;

import com.google.gson.Gson;
import logger.LinkerLogger;

import java.io.*;
import java.net.URLEncoder;

public class BaseCache<T> {

    private Gson gson = new Gson();

    private Class<T> genericClass;

    public BaseCache(Class<T> genericClass) {
        this.genericClass = genericClass;
    }

    protected void writeToCache(String fileLocation, T objectToStore) throws IOException {

        File jsonFile = new File(fileLocation);
        jsonFile.getParentFile().mkdirs();

        if(Serializable.class.isAssignableFrom(genericClass)) {
            try (ObjectOutputStream writer = new ObjectOutputStream(new FileOutputStream(jsonFile))) {
                writer.writeObject(objectToStore);
            }
        } else {
            try (Writer writer = new FileWriter(jsonFile)) {
                gson.toJson(objectToStore, writer);
            }
        }
    }

    protected T readFromCache(String fileLocation) throws IOException {
        File jsonFile = new File(fileLocation);
        if(!jsonFile.exists())
            return null;

        if(Serializable.class.isAssignableFrom(genericClass)) {
            try (FileInputStream in = new FileInputStream(fileLocation)) {
                try (ObjectInputStream fileIn = new ObjectInputStream(in)) {
                    return (T) fileIn.readObject();
                } catch (ClassNotFoundException e) {
                    LinkerLogger.logError(e.getMessage());
                    return null;
                }
            }
        } else {
            try (FileReader reader = new FileReader(jsonFile)) {
                return gson.fromJson(reader, this.genericClass);
            }
        }
    }

    protected T getCachedItem(String javaFileLocation) throws IOException {
        String storeLocationName = getCacheStoreLocation(javaFileLocation);
        return this.readFromCache(storeLocationName);
    }

    protected String getCacheStoreLocation(String basedOnString) {
        try {
            return "src/cache/" + genericClass.getName() + "/" +  URLEncoder.encode(basedOnString, "UTF-8").hashCode();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return null;
        }
    }
}
