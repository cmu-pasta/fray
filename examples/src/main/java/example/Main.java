package example;

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
    public static void main(String[] args) throws InterruptedException {
//        testUninitializedThis();
        T t = new T();
        t.start();

        synchronized (t) {
            t.blocked = true;
            while (t.blocked) {
                t.wait();
            }
        }
//        t.t.unblockThread();
        t.join();
        System.out.println(t.isAlive());
        System.exit(0);
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