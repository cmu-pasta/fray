import cmu.edu.pasta.fray.junit.annotations.Analyze;
import cmu.edu.pasta.fray.junit.annotations.FrayTest;

import java.util.concurrent.atomic.AtomicInteger;

@FrayTest
public class SimpleTest {

    @Analyze
    public void test() {
        System.out.println("Hello, world!");
    }

    private static class FrayExample extends Thread {
        static Object o = new Object();
        static AtomicInteger a = new AtomicInteger();
        static volatile int b;

        public void run() {
            int x = a.getAndIncrement();
            synchronized (o) { // monitorenter
                if (x == 0) {
                    try {
                        o.wait();
                    } catch (InterruptedException ignore) {
                    }
                } else {
                    o.notify();
                }
            } // monitorexit
            b = x;
        }
    }

    @Analyze
    public void testFrayExample() throws Exception {
        FrayExample.a = new AtomicInteger();
        FrayExample.b = 0;
        FrayExample[] threads = {new FrayExample(), new FrayExample()};
        for (var thread : threads) thread.start();
        for (var thread : threads) thread.join();
        assert(FrayExample.b == 1);
    }
}
