package cmu.pasta.fray.it;

import cmu.pasta.fray.core.GlobalContext;

public class Utils {
    public static void log(String format, Object... args) {
        GlobalContext.INSTANCE.log(format, args);
    }
}
