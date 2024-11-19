package org.pastalab.fray.test.fail.monitor;

public class SynchronizedMethodDeadlock {

    public static class C1 {
        public synchronized void m(C2 c2) {
            System.out.println(Thread.currentThread().getName() + " entered C1.m()");
            try {
                // Simulate some work with sleep
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println(Thread.currentThread().getName() + " trying to call C2.last()");
            c2.last();
        }

        public synchronized void last() {
            System.out.println(Thread.currentThread().getName() + " entered C1.last()");
        }
    }

    public static class C2 {
        public synchronized void m(C1 c1) {
            System.out.println(Thread.currentThread().getName() + " entered C2.m()");
            try {
                // Simulate some work with sleep
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println(Thread.currentThread().getName() + " trying to call C1.last()");
            c1.last();
        }

        public synchronized void last() {
            System.out.println(Thread.currentThread().getName() + " entered C2.last()");
        }
    }

    public static void main(String[] args) {
        final C1 c1 = new C1();
        final C2 c2 = new C2();

        Thread t1 = new Thread(() -> c1.m(c2), "Thread-1");
        Thread t2 = new Thread(() -> c2.m(c1), "Thread-2");

        t1.start();
        t2.start();
    }
}