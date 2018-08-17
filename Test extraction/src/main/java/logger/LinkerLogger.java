package logger;

import java.io.PrintStream;
import java.util.Date;
import java.util.GregorianCalendar;

public class LinkerLogger {

    private static LogLevel logLevel = LogLevel.DETAILS;
    private static Date lastLog = null;

    public LinkerLogger() {
        lastLog = new GregorianCalendar().getTime();
    }

    public static void logInfo(String message) {
        if(logLevel.getNumVal() >= LogLevel.INFO.getNumVal()) {
            log(message, System.out);
        }
    }

    static void log(String message, PrintStream out) {
        lastLog = new GregorianCalendar().getTime();
        out.println(lastLog.toString() + ": " + message);
    }

    public static void logDetail(String message) {
        if(logLevel.getNumVal() >= LogLevel.DETAILS.getNumVal())
            log(message, System.out);
    }

    public static void logWarning(String message) {
        if(logLevel.getNumVal() >= LogLevel.WARNING.getNumVal())
            log(message, System.out);
    }

    public static void logError(String message) {
        if(logLevel.getNumVal() >= LogLevel.ERROR.getNumVal())
            log(message, System.err);
    }

    public static Date getLastLog() {
        return lastLog;
    }

    public static void disable() {
        logLevel = LogLevel.NONE;
    }
}
