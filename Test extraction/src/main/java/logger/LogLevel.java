package logger;

public enum LogLevel {
    NONE(1),
    ERROR(2),
    WARNING(3),
    DETAILS(4),
    INFO(5);

    private int numVal;

    LogLevel(int numVal) {
        this.numVal = numVal;
    }

    public int getNumVal() {
        return numVal;
    }
}
