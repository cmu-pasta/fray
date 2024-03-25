package example;

import cmu.pasta.sfuzz.core.GlobalContext;

public class Utils {
    public static void log(String format, Object... args) {
        GlobalContext.INSTANCE.log(format, args);
    }
}
