package org.pastalab.fray.it;

import org.pastalab.fray.runtime.Runtime;

public class Utils {
    public static void log(String format, Object... args) {
        Runtime.log(format, args);
    }
}
