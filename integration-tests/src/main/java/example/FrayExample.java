package example;
import java.util.concurrent.atomic.AtomicInteger;

public class FrayExample extends Thread {
    static Object o = new Object();
    static AtomicInteger a = new AtomicInteger();
    static volatile int b;
    public void run() {
        int x = a.getAndIncrement();
        synchronized(o) { // monitorenter
            if (x == 0) {
                try { o.wait(); } catch (InterruptedException ignore) { }
            } else {
                o.notify();
            }
        } // monitorexit
        b = x;
    }
    public static void main(String[] args) throws Exception {
        FrayExample[] threads = {new FrayExample(), new FrayExample()};
        for (var thread : threads) thread.start();
        for (var thread : threads) thread.join();
        assert(b == 1);
    }
}