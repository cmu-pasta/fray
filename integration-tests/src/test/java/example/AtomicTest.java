package example;

import cmu.pasta.sfuzz.core.GlobalContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;


public class AtomicTest {
    private Integer x;
    private CompareAndSetLock l = new CompareAndSetLock();

    private void log (String format, Object... args) {
        GlobalContext.INSTANCE.log(format, args);
    }

    public static void main(String[] args) {
        AtomicTest t = new AtomicTest();
        t.testAtomic();
    }

    public void testAtomic() {
        x = 0;
        Thread t1 = new Thread(() -> {
            log("Trying to acquire lock.");
            l.lock();
            log("Acquired lock.");
            Thread.yield();
            x += 19;
            Thread.yield();
            log("Trying to unlock.");
            l.unlock();
            log("Unlocked.");
        });
        Thread t2 = new Thread(() -> {
            log("Trying to acquire lock.");
            l.lock();
            log("Acquired lock.");
            Thread.yield();
            x += 25;
            Thread.yield();
            log("Trying to unlock.");
            l.unlock();
            log("Unlocked.");
        });
        t1.start();
        t2.start();

        try {
            t1.join();
            t2.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        assertEquals(44, (int) x);
    }
}