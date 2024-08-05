package cmu.pasta.fray.it;

import cmu.pasta.fray.runtime.Runtime;

public class Utils {
    public static void log(String format, Object... args) {
        Runtime.log(format, args);
    }
}
