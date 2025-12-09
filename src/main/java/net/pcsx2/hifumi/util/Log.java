package net.pcsx2.hifumi.util;

import org.slf4j.Logger;
import org.slf4j.simple.SimpleLoggerFactory;

public class Log {

    private static Logger log;

    public static void init() {
        SimpleLoggerFactory slf = new SimpleLoggerFactory();
        log = slf.getLogger("HifumiBot");
    }

    public static void info(String str) {
        if (log != null) {
            log.atInfo().log(str);
        }
    }

    public static void warn(String str) {
        if (log != null) {
            log.atWarn().log(str);
        }
    }

    public static void error(String str) {
        if (log != null) {
            log.atError().log(str);
        }
    }

    public static void error(Exception e) {
        log.atError().log(e.getMessage());

        StringBuilder sb = new StringBuilder();

        if (e.getCause() != null) {
            for (StackTraceElement ste : e.getCause().getStackTrace()) {
                sb.append(ste.toString()).append("\n");
            }
        }

        log.atError().log(sb.toString());
    }
}
