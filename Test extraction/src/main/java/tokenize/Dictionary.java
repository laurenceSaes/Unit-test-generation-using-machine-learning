package tokenize;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.*;
import java.util.*;


/**
 * Dictionary that can exist out of multiple groups. Each group builds another Dictionary
 */
public class Dictionary {

    private final String fileLocation;

    private File jsonFile;

    private ArrayList<ArrayList<String>> lookup = new ArrayList<>();

    private Gson gson = new Gson();

    public Dictionary() {
        this.jsonFile = null;
        this.fileLocation = null;
    }

    public Dictionary(String fileLocation) throws IOException {
        this.fileLocation = fileLocation;
        this.jsonFile = new File(this.fileLocation);
        jsonFile.createNewFile();
        if (new BufferedReader(new FileReader(fileLocation)).readLine() != null) {
            FileReader jsonFile = new FileReader(this.jsonFile);
            this.lookup = gson.fromJson(jsonFile, this.lookup.getClass());

        }
    }

    public int addWord(String word) {
        return this.addWord(0, word);
    }

    public int addWord(int wordGroup, String word) {
        return this.addWord(wordGroup, word, null);
    }

    public int addWord(int wordGroup, String word, Map<String, Integer> lookupTable) {
        //Add groups till the given word group exists
        while(wordGroup >= this.lookup.size())
            this.lookup.add(new ArrayList<>());

        List<String> selectedGroup = this.lookup.get(wordGroup);
        int lookup;
        if(lookupTable != null) {
            Integer fastLookup = lookupTable.get(word);
            lookup = fastLookup != null ? fastLookup : -1;
        } else {
            lookup = selectedGroup.indexOf(word);
        }

        if(lookup != -1)
            return lookup;

        selectedGroup.add(word);
        return selectedGroup.size() - 1;
    }

    public int looupWord(int wordGroup, String word) {
        if(wordGroup >= this.lookup.size())
            return -2;

        List<String> selectedGroup = this.lookup.get(wordGroup);
        int index = selectedGroup.indexOf(word);
        return index == -1 ? -2 : index;
    }

    public Map<Integer, Integer> addMany(int wordGroup, List<Integer> tokens) {
        while (wordGroup >= this.lookup.size()) {
            for (int i = this.lookup.size() - 1; i < wordGroup; i++) {
                this.lookup.add(new ArrayList<>());
            }
        }

        List<String> selectedGroup = this.lookup.get(wordGroup);

        Map<Integer, Integer> lookup = new HashMap<>();
        int startSize = selectedGroup.size();

        for (Integer token : new HashSet<>(tokens)) {
            if (token != -1) {
                selectedGroup.add(token.toString());
                lookup.put(token, startSize++);
            }
        }
        return lookup;
    }


    public void export() throws IOException {
        try (Writer writer = new FileWriter(this.fileLocation)) {
            Gson gson = new GsonBuilder().create();
            gson.toJson(this.lookup, writer);
        }
    }

    public String getWord(int wordIndex) {
        return this.getWord(0, wordIndex);
    }

    public String getWord(int wordGroup, int wordIndex) {
        return this.lookup.get(wordGroup).get(wordIndex);
    }

    public boolean exist(int group, String tokens) {
        if(group >= this.lookup.size())
            return false;
        return this.lookup.get(group).contains(tokens);
    }

    public int getGroups() {
        return this.lookup.size();
    }

    public List<String> getAllWords(int dictionary) {
        if(dictionary >= this.lookup.size())
            return new ArrayList<>();
        return this.lookup.get(dictionary);
    }

    public int getIndex(int wordGroup, String word) {
        List<String> selectedGroup = this.lookup.get(wordGroup);
        return selectedGroup.indexOf(word);
    }

    public Map<String, Integer> getLookupTable(int group) {
        Map<String, Integer> lookupTable = new HashMap<>();
        ArrayList<String> tokens = this.lookup.get(group);
        for(int i = 0; i < tokens.size(); i++) {
            lookupTable.put(tokens.get(i), i);
        }
        return lookupTable;
    }

}
