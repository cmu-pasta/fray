package example;

import java.util.HashMap;

/** for CounterTest */
public class CounterMap {
    HashMap<String, Integer> updateMe;
    public final Object LOCK = "LOCK";

    public CounterMap() {
        updateMe = new HashMap<>();
    }

    private void log (String format, Object... args) {
//        GlobalContext.INSTANCE.log(format, args);
    }

    public void putOrIncrement(String s) {
        log("putOrIncrement(%s) called", s);
        if(containsKey(s)) {
            log("map contains %s, trying to acquire lock", s);
            synchronized(LOCK) {
                log("acquired lock, updating map");
                updateMe.put("hello", updateMe.get("hello") + 1);
            }
        } else {
            log("map does not contain %s, trying to acquire lock", s);
            synchronized(LOCK) {
                log("acquired lock, adding to map");
                updateMe.put("hello", 1);
            }
        }
    }

    public void putOrDecrement(String s) {
        log("putOrDecrement(%s) called", s);
        if(containsKey(s)) {
            log("map contains %s, trying to acquire lock", s);
            synchronized(LOCK) {
                log("acquired lock, updating map");
                updateMe.put("hello", updateMe.get("hello") - 1);
            }
        } else {
            log("map does not contain %s, trying to acquire lock", s);
            synchronized(LOCK) {
                log("acquired lock, adding to map");
                updateMe.put("hello", -1);
            }
        }
    }

    public int getValue(String s) {
        synchronized (LOCK) {
            if(!updateMe.containsKey("hello")) return Integer.MIN_VALUE;
            return updateMe.get("hello");
        }
    }

    public boolean containsKey(String s) {
        log("containsKey(%s) called, trying to acquire lock", s);
        synchronized (LOCK) {
            log("acquired lock, returning %s", updateMe.containsKey("hello"));
            return updateMe.containsKey("hello");
        }
    }
}
