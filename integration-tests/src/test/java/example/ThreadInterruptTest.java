package example;

public class ThreadInterruptTest {
    public static void testInterruptBeforeWait() throws InterruptedException {
        final Object lock = new Object();
        final Object lock2 = new Object();
        Thread t = new Thread() {
            @Override
            public void run() {
                try {
                    synchronized (lock) {
                        lock.wait();
                    }
                } catch (InterruptedException e) {
                    System.out.println("Wow! I am interrupted!");
                }
            }
        };
        Thread t2 = new Thread() {
            @Override
            public void run() {
                t.interrupt();
                synchronized (lock2) {
                    lock2.notify();
                }
            }
        };
        t.start();
        t2.start();
        synchronized (lock) {
            synchronized (lock2) {
                lock2.wait();
            }
        }
        t.join();
        t2.join();
    }
}
