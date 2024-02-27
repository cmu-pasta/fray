package example;

import java.sql.Time;
import jdk.internal.misc.Blocker;

public class Main {
    public static class T extends Thread {
        public ThreadBlocker t = new ThreadBlocker();
        @Override
        public void run() {
            System.out.println("Hello world!");
            System.out.println("I am blocked!");
//            try {
//                t.blockThread();
//            } catch (InterruptedException e) {
//                throw new RuntimeException(e);
//            }
            System.out.println("I am unblocked!");
        }
    }
    public static void main(String[] args) throws InterruptedException {
        T t = new T();
        t.start();
        Thread.sleep(1000);
//        t.t.unblockThread();
        t.join();
    }

    public static void testSync() throws InterruptedException {
        Object o = new Object();
        synchronized (o) {
            o.wait();
        }
    }

    public static class ThreadBlocker {
        private final Object monitor2 = new Object();

        public void blockThread() throws InterruptedException {
            synchronized (monitor2) {
                monitor2.wait();
            }
        }

        public void unblockThread() {
            synchronized (monitor2) {
                monitor2.notify();
            }
        }
    }
}