package tokenize.compression;

import java.util.HashSet;

public class BpeBuilder {

    private StringBuilder output = new StringBuilder();

    private HashSet<String> vocab = new HashSet<>();

    public BpeBuilder() {
    }

    public void addToken(String token) {
        output.append(token).append(" ");
        vocab.add(token);
    }

    public StringBuilder getBPEString() {
        return output;
    }

    public HashSet<String> getVocab() {
        return vocab;
    }

    public void addNewLine() {
        output.append("\n");
    }
}
