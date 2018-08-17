package application.linking;

public class QueueElement {

    private int id;
    private String testClassName;
    private String sourcePath;

    public QueueElement(int id, String testClassName, String sourcePath) {
        this.id = id;
        this.testClassName = testClassName;
        this.sourcePath = sourcePath;
    }

    public int getId() {
        return id;
    }

    public String getTestClassName() {
        return testClassName;
    }

    public String getSourcePath() {
        return sourcePath;
    }
}
