package example;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.LockSupport;

public class Main {
    public static class T extends Thread {
        public boolean blocked = false;
        public ThreadBlocker t = new ThreadBlocker();
        public synchronized void unblock() throws InterruptedException {
            blocked = false;
            this.notify();
        }
        private static synchronized void test2() {
            System.out.println("???asdfasdfasdf");
        }
        @Override
        public void run() {
            try {
                unblock();
                test2();
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
            System.out.println("I am unblocked!");
        }
    }

    public static void testMultipleThreadWait() throws InterruptedException {
        Object o = new Object();
        Object j = new Object();
        Thread t1 = new Thread() {
            @Override
            public void run() {
                synchronized (o) {
                    try {
                        o.wait();
                        System.out.println("t1 is unblocked!");
//                            j.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        };

        Thread t2 = new Thread() {
            @Override
            public void run() {
                synchronized (o) {
                    try {
                        o.wait();
                        System.out.println("t2 is unblocked!");
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        };

        Thread t3 = new Thread() {
            @Override
            public void run() {
                synchronized (o) {
                    try {
                        synchronized (j) {
                            j.notify();
                        }
                        o.wait();
                        System.out.println("t3 is unblocked!");
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        };

        t1.start();
        t2.start();
        t3.start();
        synchronized (j) {
            j.wait();
        }
        synchronized (o) {
            o.notifyAll();
        }

        t1.join();
        t2.join();
        t3.join();
    }

    public static void main(String[] args) throws InterruptedException {
        testThreadInterruption();
//        testConcurrentHashMap();
//        testMultipleThreadWait(/**/);
//        Thread t = Thread.currentThread();
//        LockSupport.unpark(t);
//        LockSupport.unpark(t);
//        LockSupport.unpark(t);
//        LockSupport.park();
//        LockSupport.park();
//        testConcurrentHashMap();
//        testUninitializedThis();
//        T t = new T();
//        t.start();
//
//        synchronized (t) {
//            t.blocked = true;
//            while (t.blocked) {
//                t.wait();
//            }
//        }
////        t.t.unblockThread();
//        t.join();
////        System.out.println(t.isAlive());
    }
    public static void testThreadInterruption() throws InterruptedException {
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


    public static void testConcurrentHashMap() throws InterruptedException {
        final Object lock = new Object();
        try (ExecutorService service = Executors.newFixedThreadPool(1)) {
            service.submit(new Runnable() {
                @Override
                public void run() {
                    System.out.println("Thread 1");
                }
            });

            service.submit(new Runnable() {
                @Override
                public void run() {
                    synchronized (lock) {
                        lock.notify();
                        System.out.println("Thread 1");
                    }
                }
            });

            synchronized (lock) {
                lock.wait();
            }
            service.shutdown();
        }
        System.out.println("service shutdown.");
    }

    public static void testThread() {
        Thread t1 = new Thread(new Runnable() {
            @Override
            public void run() {
            }
        });
        t1.start();
    }


    public static class O {
        public O() {
            System.out.println(System.identityHashCode(this));
        }

    }

    public static void testUninitializedThis() {
        O o = new O();
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